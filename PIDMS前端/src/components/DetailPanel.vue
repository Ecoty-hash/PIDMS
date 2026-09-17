<template>
  <div v-if="visible" class="drawer-overlay" @mousedown.self="$emit('close')">
    <div class="drawer" role="dialog" aria-modal="true">
      <div class="drawer__head">
        <div class="drawer__title">{{ title }} · 详情</div>
        <button class="modal__close" @click="$emit('close')" aria-label="关闭">✕</button>
      </div>
      <div class="drawer__body">
        <div v-if="!fields.length" class="empty">暂无可展示的详情字段</div>
        <div v-else class="detail-grid">
          <div
            v-for="f in fields"
            :key="f.name"
            class="detail-item"
            :class="{ 'detail-item--full': f.type === 'textarea' || f.type === 'file' }"
          >
            <span class="k">{{ f.label }}</span>
            <span class="v" :class="{ mono: /code|no|编号|单号|编码/.test(f.name) }">
              <StatusBadge v-if="f.type === 'enum'" :value="enumText(f, record[f.name])" />
              <template v-else>{{ format(optionText(f, record[f.name])) }}</template>
            </span>
          </div>
        </div>
      </div>
      <div class="drawer__foot">
        <button class="btn" @click="$emit('close')">关闭</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import StatusBadge from "./StatusBadge.vue";

defineProps({
  visible: { type: Boolean, default: false },
  title: { type: String, default: "" },
  fields: { type: Array, default: () => [] },
  record: { type: Object, default: null },
});
defineEmits(["close"]);

function format(v) {
  if (v === null || v === undefined || v === "") return "—";
  if (Array.isArray(v)) return v.length ? v.join("、") : "—";
  return String(v);
}

// 详情字段字典回显：字段若带 options（字典下拉），把存的 code 映射为中文名；无匹配回退原值
function optionText(f, val) {
  if (Array.isArray(val)) {
    if (!Array.isArray(f.options)) return val;
    return val.map((v) => {
      const hit = f.options.find((o) => String(o.value) === String(v));
      return hit ? hit.label : v;
    });
  }
  const opts = f.options;
  if (Array.isArray(opts) && val !== null && val !== undefined && val !== "") {
    const hit = opts.find((o) => String(o.value) === String(val));
    if (hit) return hit.label;
  }
  return val;
}
function enumText(f, val) {
  return optionText(f, val);
}
</script>
