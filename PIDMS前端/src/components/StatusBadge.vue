<template>
  <span class="tag" :class="`tag--${tone}`">{{ text }}</span>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  value: { type: [String, Number], default: "" },
  type: { type: String, default: "enum" },
});

// 常见状态取值 → 中文标签
const VALUE_LABEL = {
  // 启停/封存
  active: "启用", inactive: "停用", enabled: "启用", disabled: "停用", sealed: "封存",
  // 归档
  archived: "已归档", unarchived: "未归档",
  // 审批
  pending: "待审批", approved: "已审批", rejected: "已拒绝",
  // 检查/整改
  qualified: "合格", unqualified: "不合格",
  rectified: "已整改", rectifying: "整改中",
  // 预算修订状态
  unrevised: "未修订", revising: "修订中", revised: "已修订",
  // 项目状态
  planning: "规划中", "in-progress": "在建", inprogress: "在建", completed: "已完工",
  suspended: "已暂停", "in-progress-2": "在建",
  // 性别
  male: "男", female: "女",
};

// 中文标签 → 色系
const TONES = {
  启用: "green", 在职: "green", 已审批: "green", 已通过: "green", 合格: "green",
  已完成: "green", 已整改: "green", 已归档: "green", 正常: "green", 已完工: "green",
  禁用: "gray", 停用: "gray", 封存: "gray", 待检查: "gray", 未归档: "gray", 离职: "gray",
  在建: "blue", 整改中: "blue", 审批中: "blue", 进行中: "blue", 参与: "blue",
  规划中: "amber", 待整改: "amber", 未开始: "amber", 已提交: "amber", 未参与: "amber", 延期: "red",
  不合格: "red", 已驳回: "red", 已拒绝: "red", 暂停: "red", 已逾期: "red",
};

const display = computed(() => {
  const v = props.value;
  if (v === null || v === undefined || v === "") return "—";
  const key = String(v);
  return VALUE_LABEL[key] || key;
});

const tone = computed(() => {
  const d = display.value;
  if (TONES[d]) return TONES[d];
  const raw = String(props.value);
  return TONES[raw] || "gray";
});

const text = computed(() => display.value);
</script>
