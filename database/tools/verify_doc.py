#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
PIDMS · 《数据库设计文档.md》§5 表结构明细 ↔ 增量 SQL 一致性校验

校验范围：§5.4 基础管理域 10 张 bas_ 表（本次新增）——
每一张表在文档里列出的字段集合，必须与回放 SQL 后得到的业务列集合完全一致。
同时校验 §3 模块↔表 映射与 §6 字典登记。
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from verify_consistency import sql_tables, DB  # noqa: E402

DOC = os.path.join(DB, "数据库设计文档.md")

PLATFORM = {"id", "create_by", "create_time", "update_by", "update_time"}
fails, warns = [], []


def doc_tables(src):
    """返回 {表名: [文档中列出的字段名]}"""
    out = {}
    blocks = re.split(r'\n#### ', src)
    for b in blocks:
        m = re.match(r'[\d.]+\s+`(\w+)`', b)
        if not m:
            continue
        cols = re.findall(r'^\|\s*`(\w+)`\s*\|', b, re.M)
        out[m.group(1)] = cols
    return out


def main():
    src = open(DOC, encoding="utf-8").read()
    doc = doc_tables(src)
    sql = sql_tables()

    print("文档 §5 列出 %d 张表，SQL 回放得到 %d 张表\n" % (len(doc), len(sql)))

    bas = sorted(t for t in sql if t.startswith("bas_"))
    print("### §5.4 基础管理域 10 张 bas_ 表：文档字段 ↔ SQL 业务列")
    for t in bas:
        if t not in doc:
            print("  ✗ §5 文档缺少表 %s" % t); fails.append(t); continue
        sql_cols = {c for c in sql[t] if c not in PLATFORM}
        doc_cols = {c for c in doc[t] if c not in PLATFORM}
        missing = sql_cols - doc_cols          # SQL 有、文档没写
        extra = doc_cols - sql_cols            # 文档写了、SQL 没有
        if missing or extra:
            print("  ✗ %-24s 文档缺: %s ; 文档多: %s" % (t, sorted(missing), sorted(extra)))
            fails.append(t)
        else:
            print("  ✓ %-24s %2d 个业务列完全一致" % (t, len(sql_cols)))

    # §5.1 sys_user 的登录列（由 upgrade/05 增量补入）
    if "sys_user" in doc:
        sql_cols = {c for c in sql["sys_user"] if c not in PLATFORM}
        doc_cols = {c for c in doc["sys_user"] if c not in PLATFORM}
        missing = sql_cols - doc_cols
        if missing:
            print("  ✗ sys_user：SQL 有但 §5.1.3 未列出的业务列 %s" % sorted(missing))
            fails.append("sys_user")
        else:
            print("  ✓ %-24s §5.1.3 已列出全部业务列（含升级补入的 %s）"
                  % ("sys_user", sorted(sql_cols & {"username", "password"})))

    # §3 模块 ↔ 表 映射
    print("\n### §3 模块 ↔ 表 映射")
    sec3 = src[src.index("## 3. 模块 ↔ 表 映射"):src.index("## 4. ER 关系总览")]
    for t in bas + ["sys_org", "sys_role", "sys_user"]:
        if "`%s`" % t in sec3:
            print("  ✓ §3 提到 %s" % t)
        else:
            print("  ✗ §3 未提到 %s" % t); fails.append(t)

    # §6 字典登记：以 12_seed_basic_management.sql 实际登记的字典为准做交叉核对
    print("\n### §6 字典登记 ↔ upgrade/12_seed_basic_management.sql")
    seed = open(os.path.join(DB, "upgrade/12_seed_basic_management.sql"), encoding="utf-8").read()
    block = re.search(r'INSERT IGNORE INTO `sys_dict_type`.*?VALUES(.*?);', seed, re.S)
    registered = re.findall(r"\(\s*\d+\s*,\s*'(\w+)'", block.group(1)) if block else []
    tail = src[src.index("## 6."):] if "## 6." in src else ""
    for d in registered:
        if "`%s`" % d in tail:
            print("  ✓ §6 已登记 %s（种子脚本同步）" % d)
        else:
            print("  ✗ §6 缺字典 %s（种子脚本已登记）" % d)
            fails.append(d)

    # bas_work_type.status 复用通用 status 字典，不应另立字典类型
    if "work_type_status" in tail:
        print("  ! §6 出现了 work_type_status，但种子脚本未登记该字典（工种状态应复用 status）")
        warns.append("work_type_status")
    else:
        print("  ✓ 工种状态复用通用 `status` 字典，未另立字典类型")

    print("\n失败 %d 项，提示 %d 项" % (len(fails), len(warns)))
    return 1 if fails else 0


if __name__ == "__main__":
    sys.exit(main())
