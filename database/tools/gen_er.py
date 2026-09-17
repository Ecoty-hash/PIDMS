#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
PIDMS · ER 关系图生成脚本

产出 database/ER关系图.png，供《数据库设计文档》§4 引用。
配色沿用前端 src/styles/main.css 的「浅色蓝图纸风 v3」：
  墨蓝 ink #16324F / 蓝图蓝 blue #1D6FD1 / 合格绿 green #1E8E5E
  琥珀 amber #D98C0B / 发丝线 line #DBE3EE / 页面底 paper #F4F6F9

做法：graphviz 负责布局（每域一列，域内竖向排列，跨域连线用 constraint=false 不参与分层），
      再用 Pillow 按节点外接框补画四个域的浅色圆角面板与域标题。
"""
import os
import re
import tempfile
from subprocess import run

from PIL import Image, ImageDraw, ImageFont

# 输出到 database/ER关系图.png（本脚本位于 database/tools/）
BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(BASE, "ER关系图.png")
TMP = os.path.join(tempfile.gettempdir(), "pidms_er")
DPI = 200
FONT_CJK = "/usr/share/fonts/truetype/droid/DroidSansFallbackFull.ttf"
FONT_MONO = "DejaVu Sans Mono"

PAGE_BG = "#F6F8FB"
INK = "#16324F"

# 域 → (标题, 面板底色, 面板边框, 标题色, 节点边框, 节点文字色, 节点列表)
DOMAINS = [
    ("系统管理域 · sys_（9 张）", "#EEF3F9", "#B3C7DB", "#16324F", "#B3C7DB", "#16324F", [
        "sys_org", "sys_user", "sys_role", "sys_user_role", "sys_role_permission",
        "sys_dict_type", "sys_dict_data", "sys_attachment", "sys_operation_log"]),
    ("项目管理域 · pm_（7 张）", "#EAF3FC", "#A9C9EF", "#1559AD", "#A9C9EF", "#1559AD", [
        "pm_project", "pm_project_budget", "pm_project_budget_item", "pm_project_doc",
        "pm_project_status", "pm_seal_type", "pm_seal_application"]),
    ("施工管理域 · cm_（13 张）", "#EDF7F2", "#95C8B4", "#1E8E5E", "#95C8B4", "#1E8E5E", [
        "cm_progress", "cm_quality_check_item", "cm_quality_inspection",
        "cm_quality_inspection_item", "cm_quality_rectification",
        "cm_quality_rectification_item", "cm_safety_check_item", "cm_safety_inspection",
        "cm_safety_inspection_item", "cm_safety_rectification",
        "cm_safety_rectification_item", "cm_construction_log", "cm_warning_rule"]),
    ("基础管理域 · bas_（10 张）", "#FBF4E6", "#E6CFA6", "#B4720A", "#E6CFA6", "#B4720A", [
        "bas_basic_info", "bas_customer", "bas_supplier", "bas_internal_unit",
        "bas_work_type", "bas_budget_type", "bas_company_doc", "bas_business_trip",
        "bas_leave_application", "bas_makeup_application"]),
]

# 域内父子连线（实线箭头）
INNER_EDGES = [
    ("sys_org", "sys_org"), ("sys_org", "sys_user"), ("sys_user", "sys_user_role"),
    ("sys_role", "sys_user_role"), ("sys_role", "sys_role_permission"),
    ("pm_project", "pm_project_budget"), ("pm_project_budget", "pm_project_budget_item"),
    ("pm_project", "pm_project_doc"), ("pm_project", "pm_seal_application"),
    ("pm_seal_type", "pm_seal_application"),
    ("cm_quality_check_item", "cm_quality_inspection_item"),
    ("cm_quality_inspection", "cm_quality_inspection_item"),
    ("cm_quality_inspection", "cm_quality_rectification"),
    ("cm_quality_rectification", "cm_quality_rectification_item"),
    ("cm_safety_check_item", "cm_safety_inspection_item"),
    ("cm_safety_inspection", "cm_safety_inspection_item"),
    ("cm_safety_inspection", "cm_safety_rectification"),
    ("cm_safety_rectification", "cm_safety_rectification_item"),
]

# 跨域归属：pm_project → 各业务记录（虚线，不参与分层）
CROSS_EDGES = [
    ("pm_project", "cm_progress"), ("pm_project", "cm_quality_inspection"),
    ("pm_project", "cm_quality_rectification"), ("pm_project", "cm_safety_inspection"),
    ("pm_project", "cm_safety_rectification"), ("pm_project", "cm_construction_log"),
    ("pm_project", "cm_warning_rule"), ("pm_project", "bas_business_trip"),
]

MONO_FONT = None


def build_dot():
    L = ['digraph ER {']
    L.append('  graph [bgcolor="transparent", rankdir=TB, splines=spline,'
             ' nodesep=0.26, ranksep=0.52, pad=0.12];')
    L.append('  node [shape=box, style="rounded,filled", fillcolor="#FFFFFF",'
             ' penwidth=1.3, fontname="%s", fontsize=14, margin="0.18,0.10", height=0.40];'
             % FONT_MONO)
    L.append('  edge [color="#8EA2B8", penwidth=1.2, arrowsize=0.65, arrowhead=crow];')

    # 每个域一列：域内相邻节点用不可见边竖向串起来
    for _, _, _, _, _, _, nodes in DOMAINS:
        for a, b in zip(nodes, nodes[1:]):
            L.append('  "%s" -> "%s" [style=invis, weight=100];' % (a, b))

    # 四列顶端对齐
    tops = "; ".join('"%s"' % d[6][0] for d in DOMAINS)
    L.append('  { rank=same; %s; }' % tops)

    for a, b in INNER_EDGES:
        L.append('  "%s" -> "%s" [constraint=false];' % (a, b))
    for a, b in CROSS_EDGES:
        L.append('  "%s" -> "%s" [constraint=false, style=dashed, color="#C3D0DF"];' % (a, b))
    L.append('}')
    return "\n".join(L)


def parse_plain(txt):
    m = re.match(r'graph\s+(\S+)\s+(\S+)\s+(\S+)', txt.strip())
    gw, gh = float(m.group(2)), float(m.group(3))
    nodes = {}
    for line in txt.splitlines():
        f = line.split()
        if f and f[0] == 'node':
            nodes[f[1]] = (float(f[2]), float(f[3]), float(f[4]), float(f[5]))
    return gw, gh, nodes


def main():
    dot_src = build_dot()
    with open(TMP + '.dot', 'w', encoding='utf-8') as f:
        f.write(dot_src)

    plain = run(['dot', '-Tplain', TMP + '.dot'], capture_output=True, text=True).stdout
    gw, gh, nodes = parse_plain(plain)

    run(['dot', '-Tpng', '-Gdpi=%d' % DPI, TMP + '.dot', '-o', TMP + '.png'],
        check=True)
    graph = Image.open(TMP + '.png').convert('RGBA')

    scale = DPI
    W, H = int(gw * scale), int(gh * scale)

    def px(x, y):
        """plain 坐标（英寸，原点左下）→ 像素（原点左上）"""
        return x * scale, (gh - y) * scale

    LABEL_H = 58          # 面板标题预留高度
    PAD_X, PAD_B = 30, 30

    panels = []
    for title, fill, border, tcol, nborder, ncol, ns in DOMAINS:
        xs, ys = [], []
        for n in ns:
            cx, cy, w, h = nodes[n]
            x0, y0 = px(cx - w / 2, cy + h / 2)
            x1, y1 = px(cx + w / 2, cy - h / 2)
            xs += [x0, x1]
            ys += [y0, y1]
        panels.append((title, fill, border, tcol, min(xs) - PAD_X, min(ys) - LABEL_H,
                       max(xs) + PAD_X, max(ys) + PAD_B))

    # 画布整体外扩（含标题条 + 图例）
    LEGEND_H = 74
    ox = int(max(24, -min(p[4] for p in panels) + 24))
    oy = int(max(24, -min(p[5] for p in panels) + 24))
    total_w = int(max(p[6] for p in panels) + ox + 24)
    total_h = int(max(p[7] for p in panels) + oy + 24 + LEGEND_H)

    canvas = Image.new('RGB', (total_w, total_h), PAGE_BG)
    dr = ImageDraw.Draw(canvas, 'RGBA')

    for title, fill, border, tcol, x0, y0, x1, y1 in panels:
        dr.rounded_rectangle([x0 + ox, y0 + oy, x1 + ox, y1 + oy], radius=18,
                             fill=fill + "FF", outline=border + "FF", width=3)
        f = ImageFont.truetype(FONT_CJK, 27)
        dr.text((x0 + ox + 26, y0 + oy + 15), title, font=f, fill=tcol + "FF")

    # 贴图层（透明底）
    canvas.paste(graph, (ox, oy), graph)

    # 图例
    f = ImageFont.truetype(FONT_CJK, 24)
    lx, ly = ox + 26, total_h - LEGEND_H + 12
    dr.line([lx, ly + 12, lx + 46, ly + 12], fill="#8EA2B8", width=3)
    dr.polygon([(lx + 46, ly + 5), (lx + 58, ly + 12), (lx + 46, ly + 19)], fill="#8EA2B8")
    dr.text((lx + 70, ly), "外键引用（1 : N）", font=f, fill="#5A6B7C")

    lx2 = lx + 300
    for i in range(0, 50, 10):
        dr.line([lx2 + i, ly + 12, lx2 + i + 6, ly + 12], fill="#C3D0DF", width=3)
    dr.text((lx2 + 62, ly), "业务归属：各业务表通过 project_id 挂到 pm_project", font=f, fill="#5A6B7C")

    canvas.save(OUT)
    print('saved', OUT, canvas.size, 'aspect %.2f' % (canvas.size[0] / canvas.size[1]))


if __name__ == '__main__':
    main()
