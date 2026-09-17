<template>
  <div class="toasts" aria-live="polite">
    <div v-for="t in toasts" :key="t.id" class="toast" :class="`toast--${t.type}`">
      <span>{{ t.message }}</span>
    </div>
  </div>
</template>

<script setup>
import { useToast } from "@/composables/useToast.js";
const { toasts } = useToast();
</script>
