#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
PIDMS · DLS原型图 修复脚本（基础管理域 10 + 系统管理域 3，共 13 个模块）

修复三类客观损坏（这些损坏正是 modules.js 早期版本里出现
`<option value="人工费>人工费`、英文字段名丢失、中文枚举值等问题的直接来源）：

  1. 畸形标签/属性   `<select " required>`、`placeholder="..." " required>`
  2. 畸形 option     `<option value="人工费>人工费</option>`（value 与文本被 `>` 吞并）
  3. 中文枚举值      应改为 modules.js / 后端 SQL 使用的英文 code
  4. 被污染的 label  「未分类」「分类」等被替换掉的原字段名；供应商 add 页的
                    银行卡按钮组使用了 <label>操作</label>，会被生成器误当成表单字段

保留 BOM 与换行，逐条精确替换；任何一条 old 未命中即报错退出。
"""
# ---------------------------------------------------------------------------
# 一次性修复记录（2026-09-14 已执行完毕）
#
# 本脚本是把 DLS原型图 中 13 个新模块的损坏项对齐到 modules.js 的一次性补丁，
# 已全部落盘。保留在此是为了留痕：它逐条列出了「原型里错在哪、正确值是什么」。
# 重复运行会因 old 串已不存在而立即报错退出（不做任何写入），这是刻意的安全行为。
# ---------------------------------------------------------------------------

import io
import os
import sys

DLS = "/sessions/eager-nice-goodall/mnt/PIDMS/PIDMS原型图/DLS原型图"

# (文件, 旧串, 新串, 期望命中次数)
EDITS = [
    # ---------------------------------------------------------- customer
    ("customer-add.html",
     '<label>未分类 <span class="required">*</span></label>',
     '<label>客户类型 <span class="required">*</span></label>', 1),
    ("customer-add.html",
     '<select " required>', '<select required>', 1),
    ("customer-add.html",
     '<option value="">请选择未分类</option>',
     '<option value="">请选择客户类型</option>', 1),

    # ---------------------------------------------------------- supplier
    ("supplier-add.html",
     '<input type="text" placeholder="请输入供应商名称" " required>',
     '<input type="text" placeholder="请输入供应商名称" required>', 1),
    ("supplier-add.html",
     '<label>未分类 <span class="required">*</span></label>',
     '<label>供应商类型 <span class="required">*</span></label>', 1),
    ("supplier-add.html",
     '<select " required>', '<select required>', 1),
    ("supplier-add.html",
     '<option value="">请选择未分类</option>',
     '<option value="">请选择供应商类型</option>', 1),
    # 银行卡按钮组的「操作」不是表单字段，改用 span，避免生成器误采
    ("supplier-add.html",
     '<label>操作</label>', '<span class="form-caption">操作</span>', 2),

    # ---------------------------------------------------------- basic-info
    ("basic-info-add.html",
     '<option value="材料分类">材料分类</option>',
     '<option value="material">材料分类</option>', 1),
    ("basic-info-add.html",
     '<option value="设备分类">设备分类</option>',
     '<option value="equipment">设备分类</option>', 1),
    ("basic-info-add.html",
     '<option value="工种分类">工种分类</option>',
     '<option value="worktype">工种分类</option>', 1),
    ("basic-info-edit.html",
     '<label>分类</label>', '<label>资料分类</label>', 1),
    ("basic-info-edit.html",
     '<option value="project" selected>项目资料</option>\n'
     '                                <option value="document">文档资料</option>',
     '<option value="material" selected>材料分类</option>\n'
     '                                <option value="equipment">设备分类</option>\n'
     '                                <option value="worktype">工种分类</option>', 1),

    # ---------------------------------------------------------- internal-unit
    ("internal-unit-edit.html",
     '<option value="">请选择</option>\n'
     '                                <option value="project" selected>项目部</option>\n'
     '                                <option value="department">部门</option>',
     '<option value="">请选择</option>\n'
     '                                <option value="branch">分公司</option>\n'
     '                                <option value="project" selected>项目部</option>\n'
     '                                <option value="department">部门</option>', 1),

    # ---------------------------------------------------------- work-type
    ("work-type-add.html",
     '<option value="active">启用</option>\n'
     '                                <option value="inactive">停用</option>',
     '<option value="enabled">启用</option>\n'
     '                                <option value="disabled">停用</option>', 1),
    ("work-type-edit.html",
     '<option value="active" selected>启用</option>\n'
     '                                <option value="inactive">停用</option>',
     '<option value="enabled" selected>启用</option>\n'
     '                                <option value="disabled">停用</option>', 1),

    # ---------------------------------------------------------- company-doc
    ("company-doc-add.html",
     '<option value="公司制度">公司制度</option>\n'
     '                            <option value="技术规范">技术规范</option>\n'
     '                            <option value="管理文件">管理文件</option>',
     '<option value="regulation">公司制度</option>\n'
     '                            <option value="specification">技术规范</option>\n'
     '                            <option value="management">管理文件</option>', 1),
    ("company-doc-edit.html",
     '<option value="公司制度" selected>公司制度</option>\n'
     '                            <option value="技术规范">技术规范</option>\n'
     '                            <option value="管理文件">管理文件</option>',
     '<option value="regulation" selected>公司制度</option>\n'
     '                            <option value="specification">技术规范</option>\n'
     '                            <option value="management">管理文件</option>', 1),

    # ---------------------------------------------------------- leave-application
    ("leave-application-add.html",
     '<option value="年假">年假</option>\n'
     '                                <option value="事假">事假</option>\n'
     '                                <option value="病假">病假</option>\n'
     '                                <option value="婚假">婚假</option>\n'
     '                                <option value="产假">产假</option>',
     '<option value="annual">年假</option>\n'
     '                                <option value="personal">事假</option>\n'
     '                                <option value="sick">病假</option>\n'
     '                                <option value="marriage">婚假</option>\n'
     '                                <option value="maternity">产假</option>\n'
     '                                <option value="compensatory">调休</option>', 1),
    ("leave-application-edit.html",
     '<option value="年假" selected>年假</option>\n'
     '                            <option value="事假">事假</option>\n'
     '                            <option value="病假">病假</option>\n'
     '                            <option value="婚假">婚假</option>\n'
     '                            <option value="产假">产假</option>',
     '<option value="annual" selected>年假</option>\n'
     '                            <option value="personal">事假</option>\n'
     '                            <option value="sick">病假</option>\n'
     '                            <option value="marriage">婚假</option>\n'
     '                            <option value="maternity">产假</option>\n'
     '                            <option value="compensatory">调休</option>', 1),

    # ---------------------------------------------------------- makeup-application
    ("makeup-application-add.html",
     '<option value="上班">上班</option>\n'
     '                                <option value="下班">下班</option>\n'
     '                                <option value="全天">全天</option>',
     '<option value="on-duty">上班</option>\n'
     '                                <option value="off-duty">下班</option>\n'
     '                                <option value="whole-day">全天</option>', 1),
    ("makeup-application-edit.html",
     '<option value="上班">上班</option>\n'
     '                            <option value="下班">下班</option>\n'
     '                            <option value="全天单>全天单</option>',
     '<option value="on-duty">上班</option>\n'
     '                            <option value="off-duty">下班</option>\n'
     '                            <option value="whole-day">全天</option>', 1),

    # ---------------------------------------------------------- personnel
    ("personnel-add.html",
     '<option value="男">男</option>\n'
     '                                    <option value="女">女</option>',
     '<option value="male">男</option>\n'
     '                                    <option value="female">女</option>', 1),
    ("personnel-add.html",
     '<option value="管理员">管理员</option>\n'
     '                                    <option value="项目经理">项目经理</option>\n'
     '                                    <option value="工程师">工程师</option>\n'
     '                                    <option value="普通员工">普通员工</option>',
     '<option value="ADMIN">管理员</option>\n'
     '                                    <option value="PM">项目经理</option>\n'
     '                                    <option value="ENGINEER">工程师</option>\n'
     '                                    <option value="STAFF">普通员工</option>', 1),
    ("personnel-add.html",
     '<option value="项目三部">项目三部</option>\n'
     '                                    <option value="技术部">技术部</option>\n'
     '                                    <option value="综合管理部">综合管理部</option>',
     '<option value="XM-003">项目三部</option>\n'
     '                                    <option value="JS-001">技术部</option>\n'
     '                                    <option value="ZH-001">综合管理部</option>', 1),
    ("personnel-add.html",
     '<option value="在职" selected>在职</option>\n'
     '                                    <option value="离职">离职</option>',
     '<option value="enabled" selected>在职</option>\n'
     '                                    <option value="disabled">离职</option>', 1),
    ("personnel-edit.html",
     '<option value="男" selected>男</option>\n'
     '                                    <option value="女">女</option>',
     '<option value="male" selected>男</option>\n'
     '                                    <option value="female">女</option>', 1),
    ("personnel-edit.html",
     '<option value="管理员">管理员</option>\n'
     '                                    <option value="项目经理" selected>项目经理</option>\n'
     '                                    <option value="工程师">工程师</option>\n'
     '                                    <option value="普通员工">普通员工</option>',
     '<option value="ADMIN">管理员</option>\n'
     '                                    <option value="PM" selected>项目经理</option>\n'
     '                                    <option value="ENGINEER">工程师</option>\n'
     '                                    <option value="STAFF">普通员工</option>', 1),
    ("personnel-edit.html",
     '<option value="项目三部" selected>项目三部</option>\n'
     '                                    <option value="技术部">技术部</option>\n'
     '                                    <option value="综合管理部">综合管理部</option>',
     '<option value="XM-003" selected>项目三部</option>\n'
     '                                    <option value="JS-001">技术部</option>\n'
     '                                    <option value="ZH-001">综合管理部</option>', 1),
    ("personnel-edit.html",
     '<option value="在职" selected>在职</option>\n'
     '                                    <option value="离职">离职</option>',
     '<option value="enabled" selected>在职</option>\n'
     '                                    <option value="disabled">离职</option>', 1),

    # ---------------------------------------------------------- role-management
    ("role-management-add.html",
     '<option value="启用" selected>启用</option>\n'
     '                                    <option value="禁用">禁用</option>',
     '<option value="enabled" selected>启用</option>\n'
     '                                    <option value="disabled">禁用</option>', 1),
    ("role-management-edit.html",
     '<option value="启用" selected>启用</option>\n'
     '                                    <option value="禁用">禁用</option>',
     '<option value="enabled" selected>启用</option>\n'
     '                                    <option value="disabled">禁用</option>', 1),
]

# budget-type 单独处理（同一文件多处，且行首缩进不同）
BUDGET_TYPE_BLOCK_ADD = """                            <select " required>
                                <option value="">请选择预算类型</option>
                                <option value="人工费>人工费</option>
                                <option value="材料费>材料费</option>
                                <option value="设备费>设备费</option>
                                <option value="管理单>管理单</option>
                                <option value="其他费用">其他费用</option>
                            </select>"""
BUDGET_TYPE_BLOCK_ADD_NEW = """                            <select required>
                                <option value="">请选择预算类型</option>
                                <option value="labor">人工费</option>
                                <option value="material">材料费</option>
                                <option value="equipment">设备费</option>
                                <option value="expense">管理费</option>
                                <option value="subcontract">分包费</option>
                                <option value="other">其他费用</option>
                            </select>"""

BUDGET_TYPE_BLOCK_EDIT = """                            <select required>
                                <option value="">请选择预算类型</option>
                                <option value="人工费 selected>人工费</option>
                                <option value="材料费>材料费</option>
                                <option value="设备费>设备费</option>
                                <option value="管理单>管理单</option>
                                <option value="其他费用">其他费用</option>
                            </select>"""
BUDGET_TYPE_BLOCK_EDIT_NEW = """                            <select required>
                                <option value="">请选择预算类型</option>
                                <option value="labor" selected>人工费</option>
                                <option value="material">材料费</option>
                                <option value="equipment">设备费</option>
                                <option value="expense">管理费</option>
                                <option value="subcontract">分包费</option>
                                <option value="other">其他费用</option>
                            </select>"""

BUDGET_TYPE_STATUS_ADD = """                            <select " required>
                                <option value="启用" selected>启用</option>
                                <option value="封存">封存</option>
                            </select>"""
BUDGET_TYPE_STATUS_EDIT = """                            <select required>
                                <option value="启用" selected>启用</option>
                                <option value="封存">封存</option>
                            </select>"""
BUDGET_TYPE_STATUS_NEW = """                            <select required>
                                <option value="enabled" selected>启用</option>
                                <option value="sealed">封存</option>
                            </select>"""

EDITS += [
    ("budget-type-add.html", BUDGET_TYPE_BLOCK_ADD, BUDGET_TYPE_BLOCK_ADD_NEW, 1),
    ("budget-type-add.html", BUDGET_TYPE_STATUS_ADD, BUDGET_TYPE_STATUS_NEW, 1),
    ("budget-type-add.html",
     '<input type="text" placeholder="请输入二级预算类单 " required>',
     '<input type="text" placeholder="请输入二级预算类型" required>', 1),
    ("budget-type-add.html",
     '<input type="text" placeholder="格式：XX-XX-XXX，如 RGF-GZ-001" " required>',
     '<input type="text" placeholder="格式：XX-XX-XXX，如 RGF-GZ-001" required>', 1),
    ("budget-type-edit.html", BUDGET_TYPE_BLOCK_EDIT, BUDGET_TYPE_BLOCK_EDIT_NEW, 1),
    ("budget-type-edit.html", BUDGET_TYPE_STATUS_EDIT, BUDGET_TYPE_STATUS_NEW, 1),
    ("budget-type-edit.html",
     '<input type="text" value="工资" placeholder="请输入二级预算类单 required>',
     '<input type="text" value="工资" placeholder="请输入二级预算类型" required>', 1),
]

CSS_APPEND = """
/* 表单内的非字段说明文字（如银行卡操作按钮组的标题），
   使用 span 以免被模块生成器误采为表单字段 */
.form-caption {
    display: block;
    font-size: 14px;
    color: #666;
    margin-bottom: 8px;
    font-weight: 500;
}
"""


class Html(object):
    """按 BOM + 换行风格安全读写的 HTML 文件（文件为 CRLF，须原样保留）"""

    def __init__(self, path):
        self.path = path
        raw = open(path, "rb").read()
        self.bom = raw.startswith(b"\xef\xbb\xbf")
        body = raw.decode("utf-8-sig")
        self.nl = "\r\n" if "\r\n" in body else "\n"
        # 统一成 \n 便于匹配，写回时再还原
        self.text = body.replace("\r\n", "\n")
        self.dirty = False

    def sub(self, old, new, times):
        old = old.replace("\r\n", "\n")
        new = new.replace("\r\n", "\n")
        got = self.text.count(old)
        if got != times:
            return "期望命中 %d 次，实际 %d 次\n    old=%r" % (times, got, old[:100])
        self.text = self.text.replace(old, new)
        self.dirty = True
        return None

    def save(self):
        body = self.text.replace("\n", self.nl) if self.nl == "\r\n" else self.text
        out = body.encode("utf-8")
        if self.bom:
            out = b"\xef\xbb\xbf" + out
        with open(self.path, "wb") as f:
            f.write(out)
        return len(out)


def main():
    handles = {}
    errors = []

    for name, old, new, times in EDITS:
        h = handles.get(name)
        if h is None:
            h = handles[name] = Html(os.path.join(DLS, name))
        err = h.sub(old, new, times)
        if err:
            errors.append("%s：%s" % (name, err))

    if errors:
        print("修复中止，以下条目未按预期匹配（未写入任何文件）：")
        for e in errors:
            print("  ✗ " + e)
        sys.exit(1)

    for name, h in sorted(handles.items()):
        if not h.dirty:
            continue
        print("  ✓ %-34s %d bytes" % (name, h.save()))

    # styles.css 追加 .form-caption（幂等）
    css = Html(os.path.join(DLS, "styles.css"))
    if ".form-caption" in css.text:
        print("  · styles.css 已包含 .form-caption，跳过")
    else:
        css.text = css.text.rstrip("\n") + "\n" + CSS_APPEND
        css.dirty = True
        print("  ✓ %-34s 追加 .form-caption (%d bytes)" % ("styles.css", css.save()))

    print("\n共修复 %d 处，涉及 %d 个 HTML 文件。" % (sum(t for _, _, _, t in EDITS), len(handles)))


if __name__ == "__main__":
    main()
