// 前端 mock 层端到端自检：加载真实 modules.js + mock.js，对 13 个模块跑通增删改查与审批动作
const fs = require('fs');
const path = require('path');

// 脚本位于 <项目根>/database/tools/，前端源码在 <项目根>/PIDMS前端/src
const SRC = path.resolve(__dirname, '../../PIDMS前端/src');

let ms = fs.readFileSync(path.join(SRC, 'config/modules.js'), 'utf8');
ms = ms.replace(/export const DASHBOARD_KEY[\s\S]*$/, '').replace('export const MODULES =', 'global.MODULES =');
eval(ms);

let mk = fs.readFileSync(path.join(SRC, 'api/mock.js'), 'utf8');
mk = mk.replace(/^import .*$/m, '').replace('export function mockRequest', 'global.mockRequest = function mockRequest')
       .replace(/export default mockRequest;?\s*$/, '');
eval(mk);

const TARGETS = ['basic-info', 'customer', 'supplier', 'internal-unit', 'work-type', 'budget-type',
  'company-doc', 'business-trip', 'leave-application', 'makeup-application',
  'personnel', 'organization', 'role-management'];

const today = '2026-09-14';
function payload(mod) {
  const p = {};
  mod.form.forEach((f) => {
    if (f.name === 'image' || f.name === 'attachments') { p[f.name] = ''; return; }
    if (f.type === 'checkbox') { p[f.name] = []; return; }
    if (f.enumFrom) { p[f.name] = 1; return; }
    if (f.options && f.options.length) { p[f.name] = f.options[0].value; return; }
    if (f.type === 'number') { p[f.name] = 12; return; }
    if (f.type === 'date') { p[f.name] = today; return; }
    if (f.type === 'textarea') { p[f.name] = '自检示例内容'; return; }
    p[f.name] = '自检-' + f.name;
  });
  return p;
}

let fails = 0;
const err = (m) => { console.log('  ✗ ' + m); fails++; };

for (const key of TARGETS) {
  const mod = MODULES.find((m) => m.key === key);
  console.log('\n=== ' + key + '  [' + mod.basePath + ']');
  try {
    const page = mockRequest(mod.basePath, { method: 'GET', params: { page: 1, pageSize: 20 } });
    console.log('  list total=' + page.total + ' rows=' + page.list.length);
    const first = page.list[0];
    if (!first) { err('列表为空'); continue; }

    // 列表列字段是否都真实存在（undefined 视为缺字段，展示字段允许为 null）
    (mod.columns || []).forEach((c) => {
      if (!(c.name in first)) err('列表缺字段 ' + c.name);
    });

    const detail = mockRequest(mod.basePath + '/' + first.id, { method: 'GET' });
    (mod.detail || []).forEach((d) => {
      if (!(d.name in detail)) err('详情缺字段 ' + d.name);
    });

    // 枚举列的值必须能在 column.options 里查到中文
    (mod.columns || []).filter((c) => c.type === 'enum' && c.options).forEach((c) => {
      const v = first[c.name];
      if (v === null || v === undefined || v === '') return;
      if (!c.options.some((o) => String(o.value) === String(v)))
        err('列 ' + c.name + ' 的值 ' + JSON.stringify(v) + ' 不在 options 中');
    });

    // 新增
    const created = mockRequest(mod.basePath, { method: 'POST', body: payload(mod) });
    if (!created || !created.id) { err('新增未返回 id'); continue; }
    console.log('  create id=' + created.id + ' -> ' + (created.applyNo || created.billNo || created.docName || ''));

    // 必填与自动补全校验
    mod.form.filter((f) => f.required).forEach((f) => {
      const v = created[f.name];
      if (v === null || v === undefined || v === '') err('必填字段 ' + f.name + ' 新增后为空');
    });
    if (key === 'leave-application' && created.leaveDays !== 1) err('请假天数未按后端口径重算: ' + created.leaveDays);
    if (key === 'business-trip' || key === 'leave-application' || key === 'makeup-application') {
      if (created.approvalStatus !== 'pending') err('审批状态默认值应为 pending，实际 ' + created.approvalStatus);
      if (!created.applyNo) err('申请单号未生成');
    }
    if (key === 'company-doc' && !created.uploadBy) err('上传人未自动补全');
    if (key === 'personnel' && !created.roleName) err('角色展示名未回填');
    if (key === 'personnel' && !created.orgName) err('机构展示名未回填');
    if (key === 'organization' && created.parentOrgId === 1 && created.parentOrgName !== '江南轨道装备制造有限公司')
      err('上级机构展示名未回填: ' + created.parentOrgName);
    if (key === 'business-trip' && !created.projectName) err('项目展示名未回填');

    // 编辑
    const edited = mockRequest(mod.basePath + '/' + created.id, { method: 'PUT', body: payload(mod) });
    if (!edited || edited.id !== created.id) err('编辑失败');

    // 特殊动作：逐个执行并断言各自的落库效果
    const EXPECT = {
      '封存': ['status', 'sealed'], '解封': ['status', 'enabled'],
      '归档': ['archiveStatus', 'archived'],
      '审批通过': ['approvalStatus', 'approved'], '驳回': ['approvalStatus', 'rejected'],
    };
    (mod.special || []).forEach((sp) => {
      if (sp.kind === 'download') return;
      const body = sp.kind === 'reject' ? { opinion: '自检驳回' } : {};
      try { mockRequest(sp.path.replace('{id}', created.id), { method: sp.method, body }); }
      catch (e) { err('动作 ' + sp.label + ' 失败: ' + e.message); return; }
      const ex = EXPECT[sp.label];
      if (!ex) return;
      const after = mockRequest(mod.basePath + '/' + created.id, { method: 'GET' });
      if (after[ex[0]] !== ex[1]) err(sp.label + ' 后 ' + ex[0] + ' 应为 ' + ex[1] + '，实际 ' + after[ex[0]]);
      if (sp.kind === 'approve' && !after.approver) err('审批人未写入');
      if (sp.kind === 'reject' && after.approvalOpinion !== '自检驳回') err('审批意见未写入');
    });

    // 删除
    const ok = mockRequest(mod.basePath + '/' + created.id, { method: 'DELETE' });
    if (ok !== true) err('删除失败');
    console.log('  ok');
  } catch (e) {
    err('异常: ' + e.message + '\n' + e.stack.split('\n')[1]);
  }
}

// 机构树
try {
  const tree = mockRequest('/api/organizations/tree', { method: 'GET' });
  console.log('\n=== 机构树 roots=' + tree.length + ' 一级子节点=' + tree[0].children.length);
  if (tree.length !== 1) fails++;
  if (tree[0].children.length !== 3) fails++;
} catch (e) { console.log('  ✗ 机构树异常 ' + e.message); fails++; }

// 其它 15 个既有模块不能被顺手改坏
console.log('\n=== 回归：既有 15 个模块列表 ===');
MODULES.filter((m) => !TARGETS.includes(m.key)).forEach((m) => {
  try {
    const p = mockRequest(m.basePath, { method: 'GET', params: { page: 1, pageSize: 5 } });
    process.stdout.write(m.key + '(' + p.total + ') ');
  } catch (e) { console.log('\n  ✗ ' + m.key + ' ' + e.message); fails++; }
});
console.log('\n' + (fails ? 'FAILED: ' + fails : 'ALL PASS'));
process.exit(fails ? 1 : 0);
