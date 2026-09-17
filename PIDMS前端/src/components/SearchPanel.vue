<template>
  <div class="adv-search">
    <div class="adv-search__grid">
      <div v-for="f in fields" :key="f.name" class="form-field">
        <label>{{ f.label }}</label>
        <FieldControl :field="f" :model-value="filters[f.name]" @update:model-value="set(f.name, $event)" />
      </div>
    </div>
    <div class="adv-search__actions">
      <button class="btn btn--primary" @click="$emit('search')">搜索</button>
      <button class="btn" @click="reset">重置</button>
    </div>
  </div>
</template>

<script setup>
import { reactive } from "vue";
import FieldControl from "./FieldControl.vue";

const props = defineProps({
  fields: { type: Array, default: () => [] },
});
const emit = defineEmits(["search", "reset"]);

const filters = reactive({});
props.fields.forEach((f) => {
  filters[f.name] = "";
});

function set(name, val) {
  filters[name] = val;
}
function reset() {
  Object.keys(filters).forEach((k) => (filters[k] = ""));
  emit("reset");
}

defineExpose({ filters });
</script>
