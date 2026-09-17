import { reactive } from "vue";

// 全局轻量 Toast
const toasts = reactive([]);
let seq = 0;

export function useToast() {
  function push(message, type = "info") {
    const id = ++seq;
    toasts.push({ id, message, type });
    setTimeout(() => {
      const i = toasts.findIndex((t) => t.id === id);
      if (i >= 0) toasts.splice(i, 1);
    }, 2600);
  }
  return {
    toasts,
    success: (m) => push(m, "success"),
    error: (m) => push(m, "error"),
    warn: (m) => push(m, "warn"),
    info: (m) => push(m, "info"),
  };
}
