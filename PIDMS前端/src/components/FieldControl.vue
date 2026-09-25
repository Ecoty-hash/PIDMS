<template>
  <!-- 文本 -->
  <input
    v-if="field.type === 'string'"
    type="text"
    :value="modelValue"
    :disabled="field.disabled"
    :placeholder="field.placeholder || `请输入${field.label}`"
    @input="$emit('update:modelValue', $event.target.value)"
  />

  <!-- 数字 -->
  <input
    v-else-if="field.type === 'number'"
    type="number"
    :value="modelValue"
    :disabled="field.disabled"
    :placeholder="field.placeholder"
    @input="$emit('update:modelValue', $event.target.value === '' ? null : Number($event.target.value))"
  />

  <!-- 日期 -->
  <input
    v-else-if="field.type === 'date'"
    type="date"
    :value="modelValue"
    @input="$emit('update:modelValue', $event.target.value)"
  />

  <!-- 日期时间 -->
  <input
    v-else-if="field.type === 'datetime'"
    type="datetime-local"
    :value="modelValue"
    @input="$emit('update:modelValue', $event.target.value)"
  />

  <!-- 时间 -->
  <input
    v-else-if="field.type === 'time'"
    type="time"
    :value="modelValue"
    @input="$emit('update:modelValue', $event.target.value)"
  />

  <!-- 多行文本 -->
  <textarea
    v-else-if="field.type === 'textarea'"
    :value="modelValue"
    :placeholder="field.placeholder || `请输入${field.label}`"
    @input="$emit('update:modelValue', $event.target.value)"
  />

  <!-- 下拉单选 -->
  <select
    v-else-if="field.type === 'enum'"
    :value="modelValue"
    :disabled="field.disabled"
    @change="$emit('update:modelValue', $event.target.value)"
  >
    <option value="">{{ field.emptyLabel || "请选择" }}</option>
    <option v-for="o in field.options" :key="o.value" :value="o.value">{{ o.label }}</option>
  </select>

  <!-- 单选组 -->
  <div v-else-if="field.type === 'radio'" class="radio-row">
    <label v-for="o in field.options" :key="o.value">
      <input type="radio" :value="o.value" :checked="modelValue === o.value" @change="$emit('update:modelValue', o.value)" />
      {{ o.label }}
    </label>
  </div>

  <!-- 复选组 -->
  <div v-else-if="field.type === 'checkbox'" class="check-row">
    <label v-for="o in field.options" :key="o.value">
      <input
        type="checkbox"
        :value="o.value"
        :checked="(modelValue || []).includes(o.value)"
        @change="toggleCheck(o.value)"
      />
      {{ o.label }}
    </label>
  </div>

  <!-- 文件 -->
  <div v-else-if="field.type === 'file'" class="file-drop">
    <input type="file" :id="'file-' + field.name" multiple @change="onFile" />
    <label :for="'file-' + field.name" style="cursor: pointer; display: block;">
      {{ fileNames || '点击选择文件上传' }}
    </label>
  </div>

  <!-- 兜底 -->
  <input
    v-else
    type="text"
    :value="modelValue"
    :placeholder="field.placeholder"
    @input="$emit('update:modelValue', $event.target.value)"
  />
</template>

<script setup>
import { ref, watch } from "vue";

const props = defineProps({
  field: { type: Object, required: true },
  modelValue: { type: [String, Number, Array, Object], default: "" },
});
const emit = defineEmits(["update:modelValue"]);

const fileNames = ref("");

function toggleCheck(val) {
  const arr = Array.isArray(props.modelValue) ? [...props.modelValue] : [];
  const i = arr.indexOf(val);
  if (i >= 0) arr.splice(i, 1);
  else arr.push(val);
  emit("update:modelValue", arr);
}

function onFile(e) {
  const files = Array.from(e.target.files || []).map((f) => f.name);
  emit("update:modelValue", files);
  fileNames.value = files.join("、");
}

watch(
  () => props.modelValue,
  (v) => {
    if (props.field.type === "file" && !v) fileNames.value = "";
  }
);
</script>

<style scoped>
/* 只读字段（如父节点的完成度 / 占总进度%，由子节点汇总得出） */
input:disabled,
select:disabled,
textarea:disabled {
  background: var(--paper-2);
  color: var(--muted);
  cursor: not-allowed;
}
</style>
