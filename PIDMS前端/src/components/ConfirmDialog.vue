<template>
  <div v-if="visible" class="confirm-overlay" @mousedown.self="$emit('cancel')">
    <div class="confirm" role="alertdialog" aria-modal="true">
      <div class="confirm__title">{{ title }}</div>
      <div class="confirm__msg">{{ message }}</div>
      <div class="confirm__foot">
        <button class="btn" @click="$emit('cancel')">取消</button>
        <button class="btn" :class="danger ? 'btn--danger' : 'btn--primary'" @click="$emit('confirm')">
          {{ confirmText }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  visible: { type: Boolean, default: false },
  title: { type: String, default: "确认操作" },
  message: { type: String, default: "" },
  confirmText: { type: String, default: "确定" },
  danger: { type: Boolean, default: true },
});
defineEmits(["confirm", "cancel"]);
</script>
