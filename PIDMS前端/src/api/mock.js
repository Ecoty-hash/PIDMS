// =========================================================================
// 本地 Mock 数据层（内存数据库）
// 与「PIDMS接口文档」响应结构同构，支持分页查询、详情、新增、编辑、删除，
// 以及归档/封存/启停/审批等特殊操作。刷新页面后数据重置。
// 真实后端接入后此文件不再被调用（CONFIG.useMock = false）。
// =========================================================================
import { MODULES } from "@/config/modules.js";

// ---------- 通用种子字段值 ----------
const NAME_POOL = [
  "墨西哥蒙特雷地铁6号线延长线项目",
  "墨西哥蒙特雷地铁4、6号线项目",
  "蒙特雷地铁6号线综合管网改造项目",
  "蒙特雷市政道路改造项目",
  "蒙特雷地铁车站广场改造项目",
];

function pick(arr, i) {
  return arr[i % arr.length];
}

function fieldSample(field, idx) {
  const n = field.name;
  const label = field.label;
  const type = field.type;
  // enumFrom 字段（外键/字典）不生成示例值，改由 CURATED 种子或 fillRefs 联查补全
  if (field.enumFrom) return null;
  if (field.options && field.options.length) return pick(field.options, idx).value;
  if (type === "number") return (idx + 1) * 3 % 9 + 2;
  if (type === "date") return "2025-0" + ((idx % 7) + 1) + "-1" + ((idx % 9) + 1);
  if (type === "datetime") return "2025-0" + ((idx % 7) + 1) + "-1" + ((idx % 9) + 1) + " 09:00:00";
  if (type === "time") return "09:00";
  if (type === "textarea") return label + "：这是示例内容，用于演示展示效果。";
  if (type === "file") return [];
  if (type === "checkbox") return [];
  if (type === "radio") return field.options?.[0]?.value || "是";
  // string
  if (/name|名称|标题/.test(n) || /名称|标题/.test(label)) return pick(NAME_POOL, idx);
  if (/code|编号|单号|no|No/.test(n) || /编号|单号/.test(label)) return "XM-2025-00" + (idx + 1);
  if (/phone|电话/.test(n) || /电话/.test(label)) return "138 0013 800" + idx;
  if (/date|日期/.test(n) || /日期/.test(label)) return "2025-07-1" + ((idx % 9) + 1);
  if (/email/.test(n)) return "user" + idx + "@example.com";
  if (/person|人|联系人|负责人/.test(n) || /负责人|联系人/.test(label)) return ["吴十", "张文琦", "王五", "赵六", "李四"][idx % 5];
  if (/dept|部门|单位/.test(n) || /部门|单位/.test(label)) return ["项目部", "技术部", "综合管理部", "财务部"][idx % 4];
  if (/progress|进度|percent|百分比/.test(n) || /进度|百分比/.test(label)) return (idx + 1) * 17 % 95 + 5;
  return label + "示例";
}

function makeId(db, resource) {
  const arr = db[resource] || [];
  return arr.length ? Math.max(...arr.map((r) => r.id)) + 1 : 1;
}

function seedRecord(mod, idx) {
  const rec = { id: idx + 1 };
  mod.form.forEach((f) => {
    rec[f.name] = fieldSample(f, idx);
  });
  // 公共字段
  rec.createBy = "系统管理员";
  rec.createTime = "2025-0" + ((idx % 6) + 1) + "-15 09:00:00";
  rec.updateBy = "系统管理员";
  rec.updateTime = "2025-0" + ((idx % 6) + 1) + "-16 10:30:00";
  if (mod.key === "project") {
    rec.projectStatus = rec.projectStatus || "in-progress";
    rec.archiveStatus = rec.archiveStatus || "未归档";
    rec.participationStatus = rec.participationStatus || "未参与";
  }
  return rec;
}

// ---------- 手工种子（关键模块的示例数据） ----------
const CURATED = {
  project: [
    { id: 1, projectName: "墨西哥蒙特雷地铁6号线延长线项目", projectCode: "XM-2025-001", ownerUnit: "蒙特雷地铁运营公司", startDate: "2025-07-19", completionDate: "", projectStatus: "in-progress", constructionUnit: "蒙特雷地铁建设公司", constructionAddress: "墨西哥蒙特雷", projectLeader: "吴十", projectMembers: "张三、李四、王五", archiveStatus: "未归档", participationStatus: "未参与", projectOverview: "全长12公里，设站8座，总投资10亿美元。", remark: "演示数据", createBy: "系统管理员", createTime: "2025-01-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-07-01 09:00:00" },
    { id: 2, projectName: "墨西哥蒙特雷地铁4、6号线项目", projectCode: "XM-2022-002", ownerUnit: "蒙特雷地铁运营公司", startDate: "2022-09-30", completionDate: "", projectStatus: "in-progress", constructionUnit: "蒙特雷地铁建设公司", constructionAddress: "墨西哥蒙特雷", projectLeader: "张文琦", projectMembers: "赵六、孙七", archiveStatus: "未归档", participationStatus: "参与", projectOverview: "四号线与六号线换乘改造工程。", remark: "", createBy: "系统管理员", createTime: "2022-09-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-06-15 09:00:00" },
    { id: 3, projectName: "蒙特雷地铁6号线综合管网改造项目", projectCode: "XM-2025-003", ownerUnit: "蒙特雷市政局", startDate: "2025-03-01", completionDate: "2025-12-31", projectStatus: "in-progress", constructionUnit: "中交一公局", constructionAddress: "墨西哥蒙特雷", projectLeader: "王五", projectMembers: "李四、周八", archiveStatus: "已归档", participationStatus: "参与", projectOverview: "配套管网迁改工程。", remark: "", createBy: "系统管理员", createTime: "2025-02-20 09:00:00", updateBy: "系统管理员", updateTime: "2025-08-01 09:00:00" },
  ],
  "project-budget": [
    { id: 1, projectName: "墨西哥蒙特雷地铁6号线延长线项目", budgetVersion: "V1.0", budgetTotal: 268000000, laborBudget: 65000000, materialBudget: 118000000, equipmentBudget: 32000000, expenseBudget: 15000000, subcontractBudget: 38000000, otherBudget: null, secondaryBudgetSummary: "土建、轨道、车站", approvalStatus: "approved", revisionStatus: "unrevised", approver: "系统管理员", ccPerson: "吴十,张文琦", remark: "2026年度施工图预算", createBy: "系统管理员", createTime: "2026-03-10 09:00:00", updateBy: "系统管理员", updateTime: "2026-03-10 09:00:00" },
    { id: 2, projectName: "墨西哥蒙特雷地铁6号线延长线项目", budgetVersion: "V1.1", budgetTotal: 273500000, laborBudget: 82000000, materialBudget: 110000000, equipmentBudget: 42000000, expenseBudget: 39500000, subcontractBudget: null, otherBudget: null, secondaryBudgetSummary: "土建、轨道、车站、机电", approvalStatus: "approved", revisionStatus: "revised", approver: "系统管理员", ccPerson: "吴十,张文琦", remark: "根据设计变更调整后版本", createBy: "系统管理员", createTime: "2026-04-15 14:30:00", updateBy: "系统管理员", updateTime: "2026-04-16 10:30:00" },
    { id: 3, projectName: "墨西哥蒙特雷地铁4号线延长线项目", budgetVersion: "V1.0", budgetTotal: 96500000, laborBudget: 26000000, materialBudget: 38000000, equipmentBudget: 19000000, expenseBudget: 13500000, subcontractBudget: null, otherBudget: null, secondaryBudgetSummary: "高架区间、车站", approvalStatus: "pending", revisionStatus: "unrevised", approver: null, ccPerson: "吴十", remark: "", createBy: "系统管理员", createTime: "2026-05-01 09:00:00", updateBy: "系统管理员", updateTime: "2026-05-01 09:00:00" },
    { id: 4, projectName: "蒙特雷地铁6号线延长线信号系统升级项目", budgetVersion: "V1.0", budgetTotal: 42800000, laborBudget: 11800000, materialBudget: 16000000, equipmentBudget: 9000000, expenseBudget: 6000000, subcontractBudget: null, otherBudget: null, secondaryBudgetSummary: "信号系统设备及安装", approvalStatus: "approved", revisionStatus: "unrevised", approver: "系统管理员", ccPerson: "张文琦", remark: "", createBy: "系统管理员", createTime: "2026-06-01 09:00:00", updateBy: "系统管理员", updateTime: "2026-06-01 09:00:00" },
    { id: 5, projectName: "蒙特雷都市区轨道交通车辆采购项目", budgetVersion: "V1.0", budgetTotal: 1560000000, laborBudget: 330000000, materialBudget: 640000000, equipmentBudget: 380000000, expenseBudget: 210000000, subcontractBudget: null, otherBudget: null, secondaryBudgetSummary: "30列新型城轨车辆采购", approvalStatus: "pending", revisionStatus: "unrevised", approver: null, ccPerson: "王五", remark: "待业主审批采购方案", createBy: "系统管理员", createTime: "2026-07-01 09:00:00", updateBy: "系统管理员", updateTime: "2026-07-01 09:00:00" },
  ],
  customer: [
    { id: 1, customerCode: "KH-001", customerName: "蒙特雷地铁运营公司", customerType: "state-owned", contactPerson: "Juan", contactPhone: "+52-81-1111-2222", address: "墨西哥蒙特雷市政府大楼", officeAddress: "墨西哥蒙特雷市政府大楼", invoiceTitle: "蒙特雷地铁运营公司", taxNo: "MTY-88010001", phone: "+52-81-1111-2222", bankName: "BBVA 银行", bankAccount: "0123456789012345", status: "enabled", remark: "长期合作客户", image: "", attachments: "", createBy: "系统管理员", createTime: "2025-01-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-01-01 09:00:00" },
    { id: 2, customerCode: "KH-002", customerName: "蒙特雷市政局", customerType: "government", contactPerson: "Maria", contactPhone: "+52-81-3333-4444", address: "墨西哥蒙特雷市政厅", officeAddress: "墨西哥蒙特雷市政厅", invoiceTitle: "蒙特雷市政局", taxNo: "MTY-88020002", phone: "+52-81-3333-4444", bankName: "Banorte 银行", bankAccount: "0987654321098765", status: "enabled", remark: "", image: "", attachments: "", createBy: "系统管理员", createTime: "2025-01-05 09:00:00", updateBy: "系统管理员", updateTime: "2025-01-05 09:00:00" },
    { id: 3, customerCode: "KH-003", customerName: "墨西哥城轨装备有限公司", customerType: "enterprise", contactPerson: "Carlos", contactPhone: "+52-55-2222-8888", address: "墨西哥城改革大道 1200 号", officeAddress: "墨西哥城改革大道 1200 号", invoiceTitle: "墨西哥城轨装备有限公司", taxNo: "CDMX-99100033", phone: "+52-55-2222-8888", bankName: "Santander 银行", bankAccount: "1122334455667788", status: "disabled", remark: "合作暂停", image: "", attachments: "", createBy: "系统管理员", createTime: "2025-02-11 09:00:00", updateBy: "系统管理员", updateTime: "2025-06-01 09:00:00" },
  ],
  supplier: [
    { id: 1, supplierCode: "GYS-001", supplierName: "中铁物资集团", supplierType: "material", contactPerson: "王强", contactPhone: "13800138001", contactAddress: "北京朝阳区建国路 88 号", officeAddress: "北京朝阳区建国路 88 号", quoter: "李报价", quoterPhone: "13900139002", accountName: "中铁物资集团有限公司", bankName: "工商银行", bankAccount: "6222020000001234", status: "enabled", remark: "", image: "", attachments: "", createBy: "系统管理员", createTime: "2025-01-10 09:00:00", updateBy: "系统管理员", updateTime: "2025-01-10 09:00:00" },
    { id: 2, supplierCode: "GYS-002", supplierName: "卡特彼勒工程机械", supplierType: "equipment", contactPerson: "张总", contactPhone: "13800138002", contactAddress: "上海浦东新区世纪大道 1 号", officeAddress: "上海浦东新区世纪大道 1 号", quoter: "陈报价", quoterPhone: "13900139003", accountName: "卡特彼勒（中国）机械有限公司", bankName: "建设银行", bankAccount: "6217000000005678", status: "enabled", remark: "", image: "", attachments: "", createBy: "系统管理员", createTime: "2025-02-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-02-01 09:00:00" },
    { id: 3, supplierCode: "GYS-003", supplierName: "墨西哥华工劳务分包公司", supplierType: "labor", contactPerson: "刘经理", contactPhone: "13800138003", contactAddress: "墨西哥蒙特雷工业园", officeAddress: "墨西哥蒙特雷工业园", quoter: "刘经理", quoterPhone: "13800138003", accountName: "SINO-LABOR SA de CV", bankName: "BBVA 银行", bankAccount: "5566778899001122", status: "enabled", remark: "劳务分包", image: "", attachments: "", createBy: "系统管理员", createTime: "2025-03-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-03-01 09:00:00" },
  ],
  personnel: [
    { id: 1, name: "吴十", gender: "male", employeeNo: "P001", phone: "13800138001", roleId: 2, roleName: "项目经理", orgId: 2, orgName: "项目三部", status: "enabled", image: "", remark: "", createBy: "系统管理员", createTime: "2025-01-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-01-01 09:00:00" },
    { id: 2, name: "张文琦", gender: "female", employeeNo: "P002", phone: "13800138002", roleId: 3, roleName: "工程师", orgId: 4, orgName: "技术部", status: "enabled", image: "", remark: "", createBy: "系统管理员", createTime: "2025-01-02 09:00:00", updateBy: "系统管理员", updateTime: "2025-01-02 09:00:00" },
    { id: 3, name: "王五", gender: "male", employeeNo: "P003", phone: "13800138003", roleId: 2, roleName: "项目经理", orgId: 5, orgName: "海外事业部", status: "enabled", image: "", remark: "", createBy: "系统管理员", createTime: "2025-01-03 09:00:00", updateBy: "系统管理员", updateTime: "2025-01-03 09:00:00" },
    { id: 4, name: "赵六", gender: "female", employeeNo: "P004", phone: "13800138004", roleId: 4, roleName: "普通员工", orgId: 3, orgName: "蒙特雷项目组", status: "disabled", image: "", remark: "已离职", createBy: "系统管理员", createTime: "2025-01-08 09:00:00", updateBy: "系统管理员", updateTime: "2025-07-01 09:00:00" },
  ],
  organization: [
    { id: 1, parentOrgId: null, orgName: "江南轨道装备制造有限公司", orgCode: "CRRC-PZ", sortOrder: 1, description: "总公司", status: "enabled", createBy: "系统管理员", createTime: "2025-01-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-01-01 09:00:00" },
    { id: 2, parentOrgId: 1, orgName: "项目三部", orgCode: "XM-003", sortOrder: 1, description: "", status: "enabled", createBy: "系统管理员", createTime: "2025-01-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-01-01 09:00:00" },
    { id: 3, parentOrgId: 2, orgName: "蒙特雷项目组", orgCode: "XM-003-01", sortOrder: 1, description: "", status: "enabled", createBy: "系统管理员", createTime: "2025-01-02 09:00:00", updateBy: "系统管理员", updateTime: "2025-01-02 09:00:00" },
    { id: 4, parentOrgId: 1, orgName: "技术部", orgCode: "JS-001", sortOrder: 2, description: "", status: "enabled", createBy: "系统管理员", createTime: "2025-01-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-01-01 09:00:00" },
    { id: 5, parentOrgId: 1, orgName: "海外事业部", orgCode: "HW-001", sortOrder: 3, description: "", status: "enabled", createBy: "系统管理员", createTime: "2025-01-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-01-01 09:00:00" },
  ],
  "warning-rule": [
    { id: 1, ruleName: "蒙特雷项目进度预警规则", projectName: "墨西哥蒙特雷地铁6号线延长线项目", planName: "施工进度计划", triggerTime: "2025-08-25 09:00:00", progressDeviationThreshold: 5, warningType: "进度停滞预警", levelRuleConfig: "连续2周实际进度为0触发黄色预警", status: "启用", remark: "", createBy: "系统管理员", createTime: "2025-08-01 09:00:00" },
    { id: 2, ruleName: "蒙特雷项目质量预警规则", projectName: "墨西哥蒙特雷地铁4、6号线项目", planName: "质量检查计划", triggerTime: "2025-08-29 17:00:00", progressDeviationThreshold: 10, warningType: "质量问题预警", levelRuleConfig: "质量合格率低于80%触发红色预警", status: "启用", remark: "", createBy: "系统管理员", createTime: "2025-08-01 09:00:00" },
    { id: 3, ruleName: "安全巡检逾期预警", projectName: "蒙特雷地铁6号线综合管网改造项目", planName: "安全巡检计划", triggerTime: "2025-08-26 08:30:00", progressDeviationThreshold: 0, warningType: "安全预警", levelRuleConfig: "超过3天未巡检触发预警", status: "禁用", remark: "", createBy: "系统管理员", createTime: "2025-08-10 09:00:00" },
  ],
  "project-status": [
    { id: 1, statusName: "规划中", statusCode: "planning", status: "enabled", createBy: "系统管理员", createTime: "2025-01-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-06-01 09:00:00" },
    { id: 2, statusName: "在建", statusCode: "in-progress", status: "enabled", createBy: "系统管理员", createTime: "2025-01-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-06-01 09:00:00" },
    { id: 3, statusName: "已完工", statusCode: "completed", status: "enabled", createBy: "系统管理员", createTime: "2025-01-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-06-01 09:00:00" },
    { id: 4, statusName: "已暂停", statusCode: "suspended", status: "enabled", createBy: "系统管理员", createTime: "2025-01-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-06-01 09:00:00" },
  ],
  "seal-type": [
    { id: 1, typeName: "公章", status: "enabled", createBy: "系统管理员", createTime: "2025-01-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-06-01 09:00:00" },
    { id: 2, typeName: "合同章", status: "enabled", createBy: "系统管理员", createTime: "2025-01-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-06-01 09:00:00" },
    { id: 3, typeName: "财务章", status: "enabled", createBy: "系统管理员", createTime: "2025-01-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-06-01 09:00:00" },
    { id: 4, typeName: "项目部印章", status: "enabled", createBy: "系统管理员", createTime: "2025-01-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-06-01 09:00:00" },
    { id: 5, typeName: "分公司印章", status: "enabled", createBy: "系统管理员", createTime: "2025-01-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-06-01 09:00:00" },
    { id: 6, typeName: "总公司印章", status: "enabled", createBy: "系统管理员", createTime: "2025-01-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-06-01 09:00:00" },
  ],
  "seal-application": [
    { id: 1, projectName: "墨西哥蒙特雷地铁6号线延长线项目", title: "施工合同用印申请", billNo: "YY-2025-0001", applyDate: "2025-08-01", applicant: "吴十", sealDepartment: "项目三部", sealFileName: "施工总承包合同.pdf", fileCopies: 4, sealMethod: "原件盖章", sealTypeName: "公章", sealDescription: "用于业主备案", approvalStatus: "approved", approver: "系统管理员", remark: "", createBy: "系统管理员", createTime: "2025-08-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-08-01 09:00:00" },
    { id: 2, projectName: "墨西哥蒙特雷地铁4、6号线项目", title: "招标文件用印", billNo: "YY-2025-0002", applyDate: "2025-08-10", applicant: "张文琦", sealDepartment: "财务部", sealFileName: "招标文件.docx", fileCopies: 2, sealMethod: "复印件盖章", sealTypeName: "合同章", sealDescription: "", approvalStatus: "pending", approver: null, remark: "", createBy: "系统管理员", createTime: "2025-08-10 09:00:00", updateBy: "系统管理员", updateTime: "2025-08-10 09:00:00" },
  ],
  progress: [
    { id: 1, projectId: 1, projectName: "墨西哥蒙特雷地铁6号线延长线项目", progressName: "可行性研究阶段", progressCode: "JD-001", planStartDate: "2025-07-19", planEndDate: "2025-09-30", actualStartDate: "2025-07-19", completionPercent: 35, progressStatus: "in-progress", remark: "" },
    { id: 2, projectId: 1, projectName: "墨西哥蒙特雷地铁6号线延长线项目", progressName: "初步设计阶段", progressCode: "JD-002", planStartDate: "2025-10-01", planEndDate: "2026-01-31", completionPercent: 0, progressStatus: "in-progress", remark: "" },
    { id: 3, projectId: 2, projectName: "墨西哥蒙特雷地铁4、6号线项目", progressName: "地下管网基础施工", progressCode: "JD-003", planStartDate: "2025-02-01", planEndDate: "2025-04-30", actualStartDate: "2025-02-01", actualEndDate: "2025-04-20", completionPercent: 100, progressStatus: "completed", remark: "" },
  ],
  "quality-inspection": [
    { id: 1, projectName: "墨西哥蒙特雷地铁6号线延长线项目", inspectionNo: "ZL202503200001", inspectionType: "质量检查", inspectionDept: "质检部", inspectionDate: "2025-03-20", inspectionLocation: "3号隧道", inspector: "张质检", inspectionResult: "合格", inspectionDetail: "本次检查对隧道初支进行了全面检查，各项指标符合要求。", inspectionItems: "合格", createBy: "系统管理员", createTime: "2025-03-20 09:00:00" },
    { id: 2, projectName: "墨西哥蒙特雷地铁4、6号线项目", inspectionNo: "ZL202504010002", inspectionType: "质量检查", inspectionDept: "质检部", inspectionDate: "2025-04-01", inspectionLocation: "1号车站", inspector: "张质检", inspectionResult: "不合格", inspectionDetail: "发现局部钢筋间距偏差超标，已开具整改单。", inspectionItems: "不合格", createBy: "系统管理员", createTime: "2025-04-01 09:00:00" },
  ],
  "safety-inspection": [
    { id: 1, projectName: "墨西哥蒙特雷地铁6号线延长线项目", inspectionNo: "AQ202503200001", inspectionType: "安全检查", inspectionDept: "安全部", inspectionDate: "2025-03-20", inspectionLocation: "3号隧道", inspector: "张安全员", inspectionResult: "合格", inspectionDetail: "安全防护设施、临时用电、机械设备等检查合格。", inspectionItems: "合格", createBy: "系统管理员", createTime: "2025-03-20 09:00:00" },
  ],
  "quality-rectification": [
    { id: 1, projectName: "墨西哥蒙特雷地铁4、6号线项目", rectificationNo: "ZLZG202504010001", rectificationLocation: "1号车站", requiredCompleteDate: "2025-04-15", rectificationStatus: "整改中", rectificationContent: "对钢筋间距偏差进行返工处理。", relatedInspectionNo: "ZL202504010002", createBy: "系统管理员", createTime: "2025-04-01 09:00:00" },
  ],
  "safety-rectification": [
    { id: 1, projectName: "墨西哥蒙特雷地铁6号线延长线项目", rectificationNo: "AQZG202503200001", rectificationLocation: "2号竖井", requiredCompleteDate: "2025-03-25", rectificationStatus: "已整改", rectificationContent: "补充临边防护栏杆及警示标识。", relatedInspectionNo: "AQ202503190001", responsiblePerson: "王五", createBy: "系统管理员", createTime: "2025-03-20 09:00:00" },
  ],
  "construction-log": [
    { id: 1, projectName: "墨西哥蒙特雷地铁6号线延长线项目", planName: "施工阶段", logDate: "2025-08-20", weather: "晴", constructionLocation: "3号隧道", constructionContent: "隧道初支喷射混凝土", workload: 3, attendanceCount: 42, constructionDetail: "完成初支12米，喷射混凝土约80m³。", existingProblems: "局部渗水，已安排处理。", images: [], createBy: "系统管理员", createTime: "2025-08-20 18:00:00" },
    { id: 2, projectName: "墨西哥蒙特雷地铁6号线延长线项目", planName: "施工阶段", logDate: "2025-08-21", weather: "多云", constructionLocation: "1号车站", constructionContent: "车站主体钢筋绑扎", workload: 4, attendanceCount: 55, constructionDetail: "完成中板钢筋绑扎。", existingProblems: "", images: [], createBy: "系统管理员", createTime: "2025-08-21 18:00:00" },
  ],
  "company-doc": [
    { id: 1, docCode: "WD-001", docName: "蒙特雷项目管理规范", docTitle: "蒙特雷项目管理规范", docCategory: "regulation", version: "V1.0", publishDept: "综合管理部", publishDate: "2025-01-01", docDescription: "项目管理规范化要求。", uploadBy: "系统管理员", uploadTime: "2025-01-01 09:00:00", attachments: "", createBy: "系统管理员", createTime: "2025-01-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-01-01 09:00:00" },
    { id: 2, docCode: "WD-002", docName: "海外项目安全技术规范", docTitle: "海外项目安全技术规范", docCategory: "specification", version: "V2.0", publishDept: "安全部", publishDate: "2025-03-01", docDescription: "", uploadBy: "系统管理员", uploadTime: "2025-03-01 09:00:00", attachments: "", createBy: "系统管理员", createTime: "2025-03-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-03-01 09:00:00" },
    { id: 3, docCode: "WD-003", docName: "印章使用管理办法", docTitle: "印章使用管理办法", docCategory: "management", version: "V1.1", publishDept: "综合管理部", publishDate: "2025-05-20", docDescription: "规范各类印章的申请与使用流程。", uploadBy: "系统管理员", uploadTime: "2025-05-20 09:00:00", attachments: "", createBy: "系统管理员", createTime: "2025-05-20 09:00:00", updateBy: "系统管理员", updateTime: "2025-05-20 09:00:00" },
  ],
  "project-doc": [
    { id: 1, projectId: "墨西哥蒙特雷地铁6号线延长线项目", docName: "蒙特雷地铁6号线施工图纸", docCategory: "施工图纸", version: "V2.0", uploadBy: "系统管理员", uploadTime: "2025-01-01 09:00:00", docDescription: "" },
  ],
  "business-trip": [
    { id: 1, applyNo: "CC-20250820-0001", projectId: 1, projectName: "墨西哥蒙特雷地铁6号线延长线项目", applicant: "吴十", tripDestination: "墨西哥城", startDate: "2025-09-01", endDate: "2025-09-05", tripReason: "参加业主协调会", attachments: "", applyTime: "2025-08-20 09:00:00", approvalStatus: "approved", approver: "系统管理员", approvalOpinion: "同意", approvalTime: "2025-08-21 10:00:00", createBy: "系统管理员", createTime: "2025-08-20 09:00:00", updateBy: "系统管理员", updateTime: "2025-08-21 10:00:00" },
    { id: 2, applyNo: "CC-20250910-0002", projectId: 2, projectName: "墨西哥蒙特雷地铁4、6号线项目", applicant: "张文琦", tripDestination: "蒙特雷", startDate: "2025-09-15", endDate: "2025-09-18", tripReason: "现场技术交底", attachments: "", applyTime: "2025-09-10 09:00:00", approvalStatus: "pending", approver: null, approvalOpinion: null, approvalTime: null, createBy: "系统管理员", createTime: "2025-09-10 09:00:00", updateBy: "系统管理员", updateTime: "2025-09-10 09:00:00" },
  ],
  "leave-application": [
    { id: 1, applyNo: "QJ-20250821-0001", applicant: "张文琦", leaveType: "annual", startDate: "2025-09-10", endDate: "2025-09-12", leaveDays: 3, leaveReason: "家庭事务", attachments: "", applyTime: "2025-08-21 09:00:00", approvalStatus: "pending", approver: null, approvalOpinion: null, approvalTime: null, createBy: "系统管理员", createTime: "2025-08-21 09:00:00", updateBy: "系统管理员", updateTime: "2025-08-21 09:00:00" },
    { id: 2, applyNo: "QJ-20250701-0002", applicant: "王五", leaveType: "sick", startDate: "2025-07-02", endDate: "2025-07-03", leaveDays: 2, leaveReason: "感冒发热", attachments: "", applyTime: "2025-07-01 09:00:00", approvalStatus: "approved", approver: "系统管理员", approvalOpinion: "准假，注意休息", approvalTime: "2025-07-01 15:00:00", createBy: "系统管理员", createTime: "2025-07-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-07-01 15:00:00" },
  ],
  "makeup-application": [
    { id: 1, applyNo: "BK-20250818-0001", applicant: "王五", cardMissDate: "2025-08-18", cardMissType: "on-duty", cardMissTime: "08:30", makeupReason: "地铁故障迟到", witness: "李四", applyTime: "2025-08-18 09:00:00", approvalStatus: "approved", approver: "系统管理员", approvalOpinion: "情况属实", approvalTime: "2025-08-18 17:00:00", createBy: "系统管理员", createTime: "2025-08-18 09:00:00", updateBy: "系统管理员", updateTime: "2025-08-18 17:00:00" },
    { id: 2, applyNo: "BK-20250912-0002", applicant: "赵六", cardMissDate: "2025-09-12", cardMissType: "off-duty", cardMissTime: "18:00", makeupReason: "外出办事未打卡", witness: "吴十", applyTime: "2025-09-12 19:00:00", approvalStatus: "pending", approver: null, approvalOpinion: null, approvalTime: null, createBy: "系统管理员", createTime: "2025-09-12 19:00:00", updateBy: "系统管理员", updateTime: "2025-09-12 19:00:00" },
  ],
  "role-management": [
    { id: 1, roleName: "系统管理员", roleCode: "ADMIN", roleDescription: "拥有全部权限", status: "enabled", permissions: "项目-全部、系统-全部", permissionSettings: ["project:all", "system:all"], userAssignments: "吴十", createBy: "系统管理员", createTime: "2025-01-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-01-01 09:00:00" },
    { id: 2, roleName: "项目经理", roleCode: "PM", roleDescription: "项目级管理权限", status: "enabled", permissions: "项目-查看、进度-全部", permissionSettings: ["project:list", "progress:all"], userAssignments: "王五", createBy: "系统管理员", createTime: "2025-01-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-01-01 09:00:00" },
    { id: 3, roleName: "工程师", roleCode: "ENGINEER", roleDescription: "施工执行与填报", status: "enabled", permissions: "进度-填报、质量-填报", permissionSettings: ["progress:edit", "quality:edit"], userAssignments: "张文琦", createBy: "系统管理员", createTime: "2025-01-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-01-01 09:00:00" },
    { id: 4, roleName: "普通员工", roleCode: "STAFF", roleDescription: "只读查看", status: "disabled", permissions: "只读", permissionSettings: [], userAssignments: "赵六", createBy: "系统管理员", createTime: "2025-01-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-01-01 09:00:00" },
  ],
  "basic-info": [
    { id: 1, infoCode: "ZL-001", infoName: "HRB400 钢筋", infoCategory: "material", specification: "Φ25", unit: "吨", referencePrice: 4800, status: "active", remark: "", createBy: "系统管理员", createTime: "2025-01-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-01-01 09:00:00" },
    { id: 2, infoCode: "SB-001", infoName: "盾构机", infoCategory: "equipment", specification: "Φ6.28m", unit: "台", referencePrice: 35000000, status: "active", remark: "", createBy: "系统管理员", createTime: "2025-01-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-01-01 09:00:00" },
    { id: 3, infoCode: "GZ-001", infoName: "钢筋工", infoCategory: "worktype", specification: "—", unit: "工日", referencePrice: 480, status: "active", remark: "", createBy: "系统管理员", createTime: "2025-01-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-01-01 09:00:00" },
    { id: 4, infoCode: "ZL-002", infoName: "C40 商品混凝土", infoCategory: "material", specification: "C40", unit: "m³", referencePrice: 620, status: "archived", remark: "已被 C50 替代", createBy: "系统管理员", createTime: "2025-01-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-06-01 09:00:00" },
  ],
  "internal-unit": [
    { id: 1, unitCode: "DW-001", unitName: "项目三部", unitType: "branch", manager: "吴十", contactPerson: "张文琦", contactPhone: "13800138002", officeAddress: "南京市浦口区浦镇大道 1 号", status: "enabled", remark: "", image: "", attachments: "", createBy: "系统管理员", createTime: "2025-01-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-01-01 09:00:00" },
    { id: 2, unitCode: "DW-002", unitName: "蒙特雷项目部", unitType: "project", manager: "王五", contactPerson: "赵六", contactPhone: "13800138004", officeAddress: "墨西哥蒙特雷工业园 A 区", status: "enabled", remark: "", image: "", attachments: "", createBy: "系统管理员", createTime: "2025-02-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-02-01 09:00:00" },
    { id: 3, unitCode: "DW-003", unitName: "综合管理部", unitType: "department", manager: "李四", contactPerson: "李四", contactPhone: "13800138005", officeAddress: "南京市浦口区浦镇大道 1 号", status: "enabled", remark: "", image: "", attachments: "", createBy: "系统管理员", createTime: "2025-01-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-01-01 09:00:00" },
  ],
  "work-type": [
    { id: 1, typeName: "钢筋工", status: "enabled", createBy: "系统管理员", createTime: "2025-01-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-01-01 09:00:00" },
    { id: 2, typeName: "混凝土工", status: "enabled", createBy: "系统管理员", createTime: "2025-01-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-01-01 09:00:00" },
    { id: 3, typeName: "电焊工", status: "enabled", createBy: "系统管理员", createTime: "2025-01-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-01-01 09:00:00" },
    { id: 4, typeName: "架子工", status: "sealed", createBy: "系统管理员", createTime: "2025-01-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-06-01 09:00:00" },
  ],
  "budget-type": [
    { id: 1, budgetType: "labor", secondaryBudgetType: "钢筋工人工费", secondaryBudgetTypeCode: "RGF-GJ-001", status: "enabled", remark: "", createBy: "系统管理员", createTime: "2025-01-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-01-01 09:00:00" },
    { id: 2, budgetType: "material", secondaryBudgetType: "钢筋材料费", secondaryBudgetTypeCode: "CLF-GJ-001", status: "enabled", remark: "", createBy: "系统管理员", createTime: "2025-01-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-01-01 09:00:00" },
    { id: 3, budgetType: "equipment", secondaryBudgetType: "盾构机租赁费", secondaryBudgetTypeCode: "SBF-DG-001", status: "enabled", remark: "", createBy: "系统管理员", createTime: "2025-01-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-01-01 09:00:00" },
    { id: 4, budgetType: "subcontract", secondaryBudgetType: "隧道分包费", secondaryBudgetTypeCode: "FBF-SD-001", status: "sealed", remark: "一期不启用", createBy: "系统管理员", createTime: "2025-01-01 09:00:00", updateBy: "系统管理员", updateTime: "2025-06-01 09:00:00" },
  ],
};

// ---------- 内存数据库 ----------
const db = {};
let idSeq = 1000;

function initDb() {
  MODULES.forEach((mod) => {
    const curated = CURATED[mod.key];
    if (curated) {
      db[mod.resource] = curated.map((r) => ({ ...r }));
      idSeq = Math.max(idSeq, ...curated.map((r) => r.id)) + 1;
    } else {
      db[mod.resource] = [seedRecord(mod, 0), seedRecord(mod, 1)];
      idSeq = Math.max(idSeq, 3);
    }
  });
}
initDb();

// ---------- 列表查询 ----------
function matchRecord(rec, params) {
  if (!params) return true;
  const kw = (params.keyword || "").toString().trim().toLowerCase();
  if (kw) {
    const vals = Object.values(rec).map((v) => String(v ?? "")).join(" ");
    if (!vals.toLowerCase().includes(kw)) return false;
  }
  for (const [k, v] of Object.entries(params)) {
    if (k === "keyword" || k === "page" || k === "pageSize" || k === "sortBy" || k === "sortOrder") continue;
    if (v === undefined || v === null || v === "") continue;
    const cell = rec[k];
    if (cell === undefined) continue;
    if (String(cell) !== String(v)) return false;
  }
  return true;
}

function pageResult(arr, params) {
  const page = Math.max(1, parseInt(params?.page, 10) || 1);
  const pageSize = Math.min(100, Math.max(1, parseInt(params?.pageSize, 10) || 20));
  const start = (page - 1) * pageSize;
  return {
    total: arr.length,
    page,
    pageSize,
    list: arr.slice(start, start + pageSize),
  };
}

// 项目预算：按六类分项求和（与后端一致，客户端保存/编辑后本地回填 budgetTotal）
function sumBudget(rec) {
  const v = (x) => (x === null || x === undefined || x === "" ? 0 : Number(x));
  return ["laborBudget", "materialBudget", "equipmentBudget", "expenseBudget", "subcontractBudget", "otherBudget"]
    .reduce((s, k) => s + v(rec[k]), 0);
}

// ---------- 特殊操作处理 ----------
// 全站枚举统一存英文 code（与后端 / 数据库一致），中文名称由 modules.js 的 options 反查。
const ACTION_HANDLERS = {
  archive: (rec) => { rec.archiveStatus = "archived"; },
  unarchive: (rec) => { rec.archiveStatus = "unarchived"; },
  seal: (rec) => { rec.status = "sealed"; },
  unseal: (rec) => { rec.status = "enabled"; },
  enable: (rec) => { rec.status = "enabled"; },
  disable: (rec) => { rec.status = "disabled"; },
  submit: (rec) => { rec.approvalStatus = "pending"; },
  approve: (rec) => { rec.approvalStatus = "approved"; },
  reject: (rec) => { rec.approvalStatus = "rejected"; },
  download: () => {},
};

// 资源级特殊操作：按 code 值流转，并补全审批人 / 审批意见 / 审批时间。
function approvalHandler(targetStatus) {
  return (rec, body) => {
    rec.approvalStatus = targetStatus;
    rec.approver = "系统管理员";
    rec.approvalOpinion = (body && body.opinion) || rec.approvalOpinion || null;
    rec.approvalTime = "2025-08-25 09:00:00";
  };
}

const RESOURCE_HANDLERS = {
  "project-statuses": {
    seal: (rec) => { rec.status = "sealed"; },
    unseal: (rec) => { rec.status = "enabled"; },
  },
  "seal-types": {
    seal: (rec) => { rec.status = "sealed"; },
    unseal: (rec) => { rec.status = "enabled"; },
  },
  "work-types": {
    seal: (rec) => { rec.status = "sealed"; },
    unseal: (rec) => { rec.status = "enabled"; },
  },
};

// 四个审批类模块共用同一套流转语义
["seal-applications", "business-trips", "leave-applications", "makeup-applications"]
  .forEach((res) => {
    RESOURCE_HANDLERS[res] = {
      approve: approvalHandler("approved"),
      reject: approvalHandler("rejected"),
    };
  });

// 单号自动生成：前缀 + 4位流水（与后端 generateBillNo / nextApplyNo 一致）
function nextBillNo(coll) {
  return nextSeqNo("YY-" + new Date().getFullYear() + "-", coll, "billNo");
}

function nextSeqNo(prefix, coll, field) {
  let max = 0;
  (coll || []).forEach((r) => {
    const v = r[field];
    if (typeof v === "string" && v.startsWith(prefix)) {
      const n = parseInt(v.slice(prefix.length), 10);
      if (!Number.isNaN(n) && n > max) max = n;
    }
  });
  return prefix + String(max + 1).padStart(4, "0");
}

// 申请单号：CC 出差 / QJ 请假 / BK 补卡（与后端 nextApplyNo 一致）
function stamp() {
  const d = new Date();
  return "" + d.getFullYear() + String(d.getMonth() + 1).padStart(2, "0") +
    String(d.getDate()).padStart(2, "0");
}

function nextApplyNo(coll, prefix) {
  return nextSeqNo(prefix + "-" + stamp() + "-", coll, "applyNo");
}

// 展示字段回填：外键/枚举只存 code 或 ID，展示名由这里联查补全（与后端 Service 一致）
function fillRefs(resource, rec) {
  if (!rec) return rec;
  const byId = (res, id) => (db[res] || []).find((r) => String(r.id) === String(id));

  if (resource === "business-trips" || resource === "progress") {
    const p = byId("projects", rec.projectId);
    rec.projectName = p ? p.projectName : rec.projectName || null;
  }
  if (resource === "personnel") {
    const r = byId("roles", rec.roleId);
    rec.roleName = r ? r.roleName : rec.roleName || null;
    const o = byId("organizations", rec.orgId);
    rec.orgName = o ? o.orgName : rec.orgName || null;
  }
  if (resource === "organizations") {
    const p = byId("organizations", rec.parentOrgId);
    rec.parentOrgName = p ? p.orgName : null;
  }
  if (resource === "company-docs" && !rec.docName) {
    rec.docName = rec.docTitle || rec.docName;
  }
  return rec;
}

// 新增缺省值：与后端 Service 保持一致
function applyCreateDefaults(resource, rec) {
  const operator = "系统管理员";
  const now = "2025-08-25 09:00:00";

  if (resource === "project-statuses" || resource === "seal-types") {
    if (!rec.status) rec.status = "enabled";
  } else if (resource === "seal-applications") {
    if (!rec.approvalStatus) rec.approvalStatus = "pending";
    if (rec.fileCopies === undefined || rec.fileCopies === null || rec.fileCopies === "") rec.fileCopies = 1;
    if (!rec.billNo) rec.billNo = nextBillNo(db[resource]);
  } else if (resource === "progress") {
    // 与后端一致：状态默认 in-progress；进度只落 projectId，projectName 为展示字段由项目联查回填
    if (!rec.progressStatus) rec.progressStatus = "in-progress";
  } else if (resource === "basic-infos") {
    if (!rec.status) rec.status = "active";
  } else if (resource === "customers" || resource === "suppliers" ||
             resource === "internal-units" || resource === "work-types") {
    if (!rec.status) rec.status = "enabled";
  } else if (resource === "budget-types") {
    if (!rec.status) rec.status = "enabled";
  } else if (resource === "company-docs") {
    if (!rec.docName) rec.docName = rec.docTitle;
    rec.uploadBy = operator;
    rec.uploadTime = now;
  } else if (resource === "business-trips" || resource === "leave-applications" ||
             resource === "makeup-applications") {
    if (!rec.approvalStatus) rec.approvalStatus = "pending";
    if (!rec.applyNo) {
      const prefix = resource === "business-trips" ? "CC"
        : resource === "leave-applications" ? "QJ" : "BK";
      rec.applyNo = nextApplyNo(db[resource], prefix);
    }
    rec.applyTime = now;
  } else if (resource === "organizations" || resource === "roles") {
    if (!rec.status) rec.status = "enabled";
  } else if (resource === "personnel") {
    if (!rec.status) rec.status = "enabled";
    if (rec.gender === "男") rec.gender = "male";
    if (rec.gender === "女") rec.gender = "female";
  }

  // 请假天数由后端按起止日期重算（含首尾）
  if (resource === "leave-applications") {
    const s = rec.startDate, e = rec.endDate;
    if (s && e && e >= s) {
      rec.leaveDays = Math.round(
        (new Date(e + "T00:00:00") - new Date(s + "T00:00:00")) / 86400000
      ) + 1;
    }
  }

  fillRefs(resource, rec);
  return rec;
}

// ---------- 可视化看板数据 ----------
const TREND = {
  day: [
    { label: "D1", plan: 60, actual: 52 }, { label: "D2", plan: 64, actual: 58 },
    { label: "D3", plan: 68, actual: 63 }, { label: "D4", plan: 72, actual: 66 },
    { label: "D5", plan: 76, actual: 72 }, { label: "D6", plan: 80, actual: 75 },
    { label: "D7", plan: 84, actual: 78 },
  ],
  week: [
    { label: "W1", plan: 80, actual: 75 }, { label: "W2", plan: 85, actual: 82 },
    { label: "W3", plan: 90, actual: 88 }, { label: "W4", plan: 95, actual: 92 },
    { label: "W5", plan: 100, actual: 95 },
  ],
  month: [
    { label: "M1", plan: 20, actual: 15 }, { label: "M2", plan: 40, actual: 38 },
    { label: "M3", plan: 60, actual: 55 }, { label: "M4", plan: 80, actual: 78 },
    { label: "M5", plan: 100, actual: 92 },
  ],
};

// 机构树组装：parentOrgId 自关联，父节点不在集合内的自动提升为根
function buildOrgTree(rows) {
  const map = new Map();
  rows.forEach((r) => map.set(String(r.id), { ...r, children: [] }));
  const roots = [];
  map.forEach((vo) => {
    const parent = vo.parentOrgId === null || vo.parentOrgId === undefined
      ? null : map.get(String(vo.parentOrgId));
    if (parent && parent !== vo) {
      parent.children.push(vo);
      vo.parentOrgName = parent.orgName;
    } else {
      vo.parentOrgName = null;
      roots.push(vo);
    }
  });
  return roots;
}

function dashboardData() {
  return {
    overallProgress: 75.42,
    progressDelta: "+5.2% 较上周",
    ongoingProjects: 2,
    qualityCount: 12,
    safetyCount: 5,
    projectStatusDistribution: [
      { status: "在建", count: 2, percent: 45 },
      { status: "已完工", count: 1, percent: 30 },
      { status: "规划中", count: 1, percent: 15 },
      { status: "延期", count: 1, percent: 10 },
    ],
  };
}

// ---------- 主路由 ----------
export function mockRequest(path, { method = "GET", params, body } = {}) {
  // 兼容带 /api 前缀与不带前缀的调用
  const p = path.startsWith("/api") ? path : "/api" + path;

  // 可视化看板
  if (p.startsWith("/api/visualization/")) {
    const ep = p.replace("/api/visualization/", "");
    switch (ep) {
      case "dashboard": return dashboardData();
      case "progress-trend": return { granularity: params?.granularity || "week", series: TREND[params?.granularity] || TREND.week };
      case "inspection-stats": return { qualityRate: 92, safetyRate: 95, qualityCount: 12, safetyCount: 5 };
      case "key-nodes":
        return [
          { name: "项目启动", status: "completed", planStart: "2025-01-15", planEnd: "2025-01-15", progress: 100 },
          { name: "基础工程完成", status: "completed", planStart: "2025-04-01", planEnd: "2025-04-30", progress: 100 },
          { name: "主体结构施工", status: "current", planStart: "2025-05-01", planEnd: "2025-09-30", owner: "张文琦", progress: 75 },
          { name: "设备安装调试", status: "pending", planStart: "2025-10-01", planEnd: "2025-11-30", owner: "赵六", progress: 0 },
          { name: "竣工验收", status: "pending", planStart: "2025-12-01", planEnd: "2025-12-31", owner: "王五", progress: 0 },
        ];
      case "project-status": return dashboardData().projectStatusDistribution;
      default: throw new Error("未知看板接口: " + ep);
    }
  }

  // 机构树：按 parentOrgId 自关联组装
  if (p === "/api/organizations/tree") {
    return buildOrgTree(db["organizations"] || []);
  }

  // 进度总览(树) - 简化:按项目分组
  if (p === "/api/progress/overview") {
    const groups = {};
    db["progress"].forEach((r) => {
      (groups[r.projectName] = groups[r.projectName] || []).push(r);
    });
    return Object.entries(groups).map(([name, items]) => ({ projectName: name, children: items }));
  }

  // 通用 CRUD：/api/{resource} 与 /api/{resource}/{id}[/{action}]
  const parts = p.replace(/^\/api\//, "").split("/");
  const resource = parts[0];
  const coll = db[resource];
  if (!coll) throw new Error(`未知资源: ${resource}`);

  const seg1 = parts[1];
  const seg2 = parts[2];

  // 列表/导出
  if (!seg1) {
    if (method === "GET") {
      const hit = coll.filter((r) => matchRecord(r, params))
        .map((r) => fillRefs(resource, { ...r }));
      return pageResult(hit, params);
    }
    if (method === "POST") {
      const rec = { ...(body || {}), id: idSeq++, createBy: "系统管理员", createTime: "2025-08-25 09:00:00", updateBy: "系统管理员", updateTime: "2025-08-25 09:00:00" };
      if (resource === "project-budgets") rec.budgetTotal = sumBudget(rec);
      applyCreateDefaults(resource, rec);
      coll.push(rec);
      return fillRefs(resource, { ...rec });
    }
    throw new Error("不支持的方法: " + method);
  }

  // 特殊集合端点
  if (seg1 === "export") {
    return { fileName: `${resource}_export.xlsx`, count: coll.filter((r) => matchRecord(r, params)).length };
  }
  if (seg1 === "import") {
    return { successCount: 2, failCount: 0, errors: [] };
  }
  if (seg1 === "batch-number") {
    // 与后端契约一致：body 为裸 id 数组 [1,2,3]；也兼容 { ids: [...] }
    const idList = Array.isArray(body) ? body : (body?.ids || []);
    let updated = 0;
    idList.forEach((id) => {
      const rec = coll.find((r) => r.id === id);
      if (rec && !rec.projectCode) {
        rec.projectCode = "XM-AUTO-" + rec.id;
        updated++;
      }
    });
    return { updated };
  }
  if (seg1 === "uncategorized") {
    return pageResult(coll.filter((r) => !r.category || r.category === "未分类"), params);
  }

  // 详情/编辑/删除/特殊操作
  const id = Number(seg1);
  const rec = coll.find((r) => r.id === id);
  if (!rec) throw new Error(`记录不存在: ${resource}/${id}`);

  if (!seg2) {
    if (method === "GET") return fillRefs(resource, { ...rec });
    if (method === "PUT") {
      Object.assign(rec, body || {}, { updateBy: "系统管理员", updateTime: "2025-08-25 09:00:00" });
      if (resource === "project-budgets") rec.budgetTotal = sumBudget(rec);
      // 请假天数沿用后端口径重算（忽略客户端传值）
      if (resource === "leave-applications" && rec.startDate && rec.endDate && rec.endDate >= rec.startDate) {
        rec.leaveDays = Math.round(
          (new Date(rec.endDate + "T00:00:00") - new Date(rec.startDate + "T00:00:00")) / 86400000
        ) + 1;
      }
      if (resource === "company-docs" && !rec.docName && rec.docTitle) {
        rec.docName = rec.docTitle;
      }
      return fillRefs(resource, { ...rec });
    }
    if (method === "DELETE") {
      const i = coll.findIndex((r) => r.id === id);
      if (i >= 0) coll.splice(i, 1);
      return true;
    }
    throw new Error("不支持的方法: " + method);
  }

  // 特殊操作（资源级优先，回退通用）
  const resHandler = (RESOURCE_HANDLERS[resource] || {})[seg2];
  const handler = resHandler || ACTION_HANDLERS[seg2];
  if (handler) {
    handler(rec, body);
    rec.updateBy = "系统管理员";
    rec.updateTime = "2025-08-25 09:00:00";
    return fillRefs(resource, { ...rec });
  }
  throw new Error(`未知操作: ${seg2}`);
}

export default mockRequest;
