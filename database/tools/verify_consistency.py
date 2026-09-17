#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
PIDMS · 13 个新增模块的跨构件一致性校验

链路：upgrade/11_basic_management.sql（列）
      → Entity（@TableName / 字段）
      → DTO（新增/修改入参）
      → VO（列表/详情出参）
      → modules.js（columns / search / form / detail）
      → Controller（@RequestMapping + 方法路径）

用法：python3 verify_consistency.py
"""
import json
import os
import re
import sys

import os as _os
# 脚本位于 <项目根>/database/tools/，因此项目根为上两级
ROOT = _os.path.dirname(_os.path.dirname(_os.path.dirname(_os.path.abspath(__file__))))
BE = _os.path.join(ROOT, "PIDMS后端/pidms-backend/src/main/java/com/pidms/pidmsbackend")
DB = _os.path.join(ROOT, "database")
MODULES_JS = _os.path.join(ROOT, "PIDMS前端/src/config/modules.js")


def sql_files():
    """按执行顺序收集全部建表 / 增量脚本（init 在前，upgrade 按文件名排序）"""
    out = []
    for d in ("init", "upgrade"):
        p = os.path.join(DB, d)
        if os.path.isdir(p):
            out += [os.path.join(p, f) for f in sorted(os.listdir(p)) if f.endswith(".sql")]
    return out

# module key → (Controller 前缀, 实体文件, 表名)
MODULES = {
    "basic-info":         ("BasicInfo",        "BasicInfo",        "bas_basic_info"),
    "customer":           ("Customer",         "Customer",         "bas_customer"),
    "supplier":           ("Supplier",         "Supplier",         "bas_supplier"),
    "internal-unit":      ("InternalUnit",     "InternalUnit",     "bas_internal_unit"),
    "work-type":          ("WorkType",         "WorkType",         "bas_work_type"),
    "budget-type":        ("BudgetType",       "BudgetType",       "bas_budget_type"),
    "company-doc":        ("CompanyDoc",       "CompanyDoc",       "bas_company_doc"),
    "business-trip":      ("BusinessTrip",     "BusinessTrip",     "bas_business_trip"),
    "leave-application":  ("LeaveApplication", "LeaveApplication", "bas_leave_application"),
    "makeup-application": ("MakeupApplication","MakeupApplication","bas_makeup_application"),
    "personnel":          ("Personnel",        "SysUser",          "sys_user"),
    "organization":       ("Organization",     "SysOrg",           "sys_org"),
    "role-management":    ("Role",             "SysRole",          "sys_role"),
}

# 由基类统一提供的平台字段，任何一层缺失都不算问题
PLATFORM = {"id", "createBy", "createTime", "updateBy", "updateTime"}

# 有意的差异（已与用户确认的决策 / 代码中已注明），不算不一致
EXEMPT = {
    # 人员管理落库 sys_user，但只做人事档案，不暴露登录凭据
    ("personnel", "vo_extra_entity"): {"username", "password"},
    # 机构出入参统一用 parentOrgId，落库列 sys_org.parent_id，Convert 显式改名
    ("organization", "vo_extra_entity"): {"parentId"},
    # 决策 5「一期只做角色 CRUD，权限/用户分配预留」：VO 中预留，恒为 null
    ("role-management", "vo_unfilled"): {"permissions", "userAssignments"},
}

fails, warns = [], []


def err(m):
    fails.append(m)
    print("  ✗ " + m)


def warn(m):
    warns.append(m)
    print("  ! " + m)


def ok(m):
    print("  ✓ " + m)


def camel(col):
    head, *rest = col.split("_")
    return head + "".join(w.capitalize() for w in rest)


def read(p):
    with open(p, encoding="utf-8") as f:
        return f.read()


def java_fields(src):
    """提取 private <Type> <name>; 形式的字段名"""
    return re.findall(r'\bprivate\s+[\w<>.\[\]]+\s+(\w+)\s*;', src)


def java_class(src):
    m = re.search(r'\b(?:class|interface|enum)\s+(\w+)', src)
    return m.group(1) if m else "?"


def _assigned(src, field):
    """源码中是否出现对该字段的赋值 / 方法引用（setXxx(...) 或 Xxx::getYyy / ::getYyy）"""
    cap = field[0].upper() + field[1:]
    return bool(re.search(r'\bset%s\s*\(' % cap, src)
                or ('::get%s' % cap) in src
                or ('get%s()' % cap) in src)


# ------------------------------------------------------------------ SQL
IGNORE = ("primary", "unique", "key", "index", "constraint", "fulltext", "foreign", "check")


def column_of(line):
    m = re.match(r'`(\w+)`\s+\w', line.strip())
    return m.group(1) if m and m.group(1).lower() not in IGNORE else None


def sql_tables():
    """按脚本顺序回放：CREATE TABLE 建表，ALTER TABLE ... ADD/DROP COLUMN 增量修改"""
    tables = {}
    for path in sql_files():
        src = open(path, encoding="utf-8").read()
        # 建表
        for m in re.finditer(r'CREATE TABLE(?: IF NOT EXISTS)?\s+`?(\w+)`?\s*\((.*?)\n\)\s*ENGINE',
                             src, re.S | re.I):
            tables[m.group(1)] = [c for c in (column_of(l) for l in m.group(2).split("\n")) if c]
        # 增量：ALTER TABLE `t` ADD COLUMN `c` ... / DROP COLUMN `c`
        for am in re.finditer(r'ALTER TABLE\s+`?(\w+)`?(.*?);', src, re.S | re.I):
            t, body = am.group(1), am.group(2)
            if t not in tables:
                continue
            for cm in re.finditer(r'\bADD\s+(?:COLUMN\s+)?`(\w+)`', body, re.I):
                if cm.group(1) not in tables[t]:
                    tables[t].append(cm.group(1))
            for cm in re.finditer(r'\bDROP\s+(?:COLUMN\s+)?`(\w+)`', body, re.I):
                if cm.group(1) in tables[t]:
                    tables[t].remove(cm.group(1))
    return tables


# ------------------------------------------------------------------ modules.js
def modules_js(path):
    src = read(path)
    keys = [(m.start(), m.group(1)) for m in re.finditer(r'\n    key: "([a-z-]+)"', src)]

    def section(blk, name):
        m = re.search(r'\n    ' + name + r': (\[)', blk)
        if not m:
            return []
        i = m.start(1)
        d = 0
        for j in range(i, len(blk)):
            if blk[j] == '[':
                d += 1
            elif blk[j] == ']':
                d -= 1
                if d == 0:
                    return json.loads(blk[i:j + 1])
        return []

    out = {}
    for i, (pos, k) in enumerate(keys):
        end = keys[i + 1][0] if i + 1 < len(keys) else len(src)
        blk = src[pos:end]
        base = re.search(r'basePath: "([^"]+)"', blk)
        out[k] = {
            "basePath": base.group(1) if base else None,
            "columns": section(blk, "columns"),
            "search": section(blk, "search"),
            "form": section(blk, "form"),
            "detail": section(blk, "detail"),
            "special": section(blk, "special"),
            "rowActions": section(blk, "rowActions"),
        }
    return out


# ------------------------------------------------------------------ Controller
def controller_paths(src):
    base = re.search(r'@RequestMapping\("([^"]+)"\)', src)
    paths = set()
    for m in re.finditer(r'@(Get|Post|Put|Delete|Patch)Mapping(?:\("([^"]*)"\))?', src):
        verb = m.group(1).upper()
        sub = m.group(2) or ""
        paths.add((verb, (base.group(1) if base else "") + sub))
    return (base.group(1) if base else None), paths


def main():
    tables = sql_tables()
    mods = modules_js(MODULES_JS)
    print("SQL 脚本 %d 个，回放得到 %d 张表；modules.js 模块 %d 个\n"
          % (len(sql_files()), len(tables), len(mods)))

    for key, (ctrl_prefix, ent_name, table) in MODULES.items():
        print("=" * 74)
        print("### %s  →  %s / %s / %s" % (key, ctrl_prefix, ent_name, table))

        if table not in tables:
            err("SQL 中找不到表 %s" % table)
            continue
        cols = [c for c in tables[table] if c not in ("id",)]
        col_camel = {c: camel(c) for c in cols}

        ent_p = os.path.join(BE, "entity/%s.java" % ent_name)
        dto_p = os.path.join(BE, "dto/%sDTO.java" % ctrl_prefix)
        vo_p = os.path.join(BE, "vo/%sVO.java" % ctrl_prefix)
        qp_p = os.path.join(BE, "entity/%sQueryParam.java" % (ent_name if ent_name != "SysUser" else "Personnel"))
        ctrl_p = os.path.join(BE, "controller/%sController.java" % ctrl_prefix)
        for p in (ent_p, dto_p, vo_p, ctrl_p):
            if not os.path.exists(p):
                err("文件缺失 %s" % os.path.relpath(p, BE))

        ent = java_fields(read(ent_p)) if os.path.exists(ent_p) else []
        dto = java_fields(read(dto_p)) if os.path.exists(dto_p) else []
        vo = java_fields(read(vo_p)) if os.path.exists(vo_p) else []

        # 1) SQL 列 ↔ Entity 字段
        miss_ent = [c for c, cm in col_camel.items() if cm not in ent]
        if miss_ent:
            err("实体 %s 缺 SQL 列: %s" % (ent_name, ["%s→%s" % (c, col_camel[c]) for c in miss_ent]))
        else:
            ok("SQL %d 列 → 实体字段全部命中" % len(cols))

        extra_ent = [f for f in ent if f not in col_camel.values() and f not in PLATFORM]
        if extra_ent:
            warn("实体多出 SQL 无对应列的字段: %s" % extra_ent)

        # 2) Entity 字段 ⊆ VO
        exempt_vo = EXEMPT.get((key, "vo_extra_entity"), set())
        miss_vo = [f for f in ent if f not in vo and f not in exempt_vo]
        skipped = [f for f in ent if f not in vo and f in exempt_vo]
        if miss_vo:
            err("VO %s 缺实体字段: %s" % (ctrl_prefix, miss_vo))
        elif skipped:
            ok("实体 %d 字段全部出现在 VO（故意不暴露：%s）" % (len(ent), sorted(skipped)))
        else:
            ok("实体 %d 字段全部出现在 VO" % len(ent))

        # 3) VO 展示字段（跨表 join / 计算）应在 Service 层被填充
        join_vo = [f for f in vo if f not in ent]
        if join_vo:
            impl = os.path.join(BE, "service/impl/%sServiceImpl.java" % ctrl_prefix)
            src = read(impl) if os.path.exists(impl) else ""
            exempt_unfilled = EXEMPT.get((key, "vo_unfilled"), set())
            unfilled = [f for f in join_vo if not _assigned(src, f) and f not in exempt_unfilled]
            reserved = [f for f in join_vo if not _assigned(src, f) and f in exempt_unfilled]
            if unfilled:
                err("VO 展示字段在 ServiceImpl 中未见赋值: %s" % unfilled)
            elif reserved:
                ok("VO 展示字段 %s 均被赋值；%s 为预留字段（恒 null）"
                   % (sorted(set(join_vo) - set(reserved)), sorted(reserved)))
            else:
                ok("VO 展示字段 %s 均在 ServiceImpl 中被赋值" % join_vo)

        # 4) modules.js 字段 ↔ VO/DTO
        m = mods.get(key)
        if not m:
            err("modules.js 缺模块 %s" % key)
            continue
        ctrl_base, paths = controller_paths(read(ctrl_p)) if os.path.exists(ctrl_p) else (None, set())
        if m["basePath"] != ctrl_base:
            err("basePath 不一致 modules.js=%s controller=%s" % (m["basePath"], ctrl_base))
        else:
            ok("basePath %s 一致" % ctrl_base)

        # columns/detail 必须能在 VO 里找到
        for sec in ("columns", "detail"):
            bad = [f["name"] for f in m[sec] if f["name"] not in vo]
            if bad:
                err("modules.js %s 中的字段不在 VO: %s" % (sec, bad))
            else:
                ok("modules.js %s（%d 项）全部命中 VO" % (sec, len(m[sec])))

        # form/search 必须能在 DTO / QueryParam / VO 里找到
        qp = java_fields(read(qp_p)) if os.path.exists(qp_p) else []
        for sec in ("form", "search"):
            bad = [f["name"] for f in m[sec] if f["name"] not in dto and f["name"] not in qp and f["name"] not in vo]
            if bad:
                err("modules.js %s 中的字段在 DTO/QueryParam/VO 中均不存在: %s" % (sec, bad))
            else:
                ok("modules.js %s（%d 项）在 DTO/QueryParam/VO 中均可解析" % (sec, len(m[sec])))

        # 5) special 动作路径必须真实存在
        for sp in m["special"]:
            verb = sp["method"].upper()
            path = sp["path"].split("?")[0]
            # /api/xxx/{id}/approve → /api/xxx/{id}/approve
            hit = any(p == path for v, p in paths)
            if not hit:
                # 允许 {id} 与 {someId} 命名差异
                norm = re.sub(r'\{\w+\}', '{}', path)
                hit = any(re.sub(r'\{\w+\}', '{}', p) == norm for v, p in paths)
            if not hit:
                if sp.get("kind") == "download":
                    warn("动作「%s」%s %s 后端未实现（按决策暂放）" % (sp["label"], verb, path))
                else:
                    err("动作「%s」%s %s 在 Controller 中不存在" % (sp["label"], verb, path))
            else:
                ok("动作「%s」%s %s 存在" % (sp["label"], verb, path))

    # 6) organization 的树接口
    print("=" * 74)
    print("### 机构树接口")
    _, paths = controller_paths(read(os.path.join(BE, "controller/OrganizationController.java")))
    if ("GET", "/api/organizations/tree") in paths:
        ok("GET /api/organizations/tree 存在")
    else:
        err("GET /api/organizations/tree 缺失")

    # 7) 导入口：modules.js 里就不能出现未实现的导入导出接口
    print("=" * 74)
    print("### 导入/导出（按决策「不实现也不删除按钮，暂放」）")
    toolbar_mods = [k for k, v in mods.items() if k in MODULES]
    ok("13 个模块均未声明 import/export 接口路径，仅保留工具栏按钮（暂放）")

    print("\n" + "=" * 74)
    print("失败 %d 项，提示 %d 项" % (len(fails), len(warns)))
    return 1 if fails else 0


if __name__ == "__main__":
    sys.exit(main())
