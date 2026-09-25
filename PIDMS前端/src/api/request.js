
import { mockRequest } from "./mock.js";

export const CONFIG = {
  useMock: false,
  baseUrl: "/api",
};

// =========================================================================
// 登录态：token / 用户信息持久化。
// 勾选"记住我"存 localStorage（刷新、重开浏览器都有效）；
// 不勾选存 sessionStorage（仅当前标签页会话有效）。
// =========================================================================
const TOKEN_KEY = "pidms_token";
const USER_KEY = "pidms_user";

function loadStored(key) {
  return localStorage.getItem(key) || sessionStorage.getItem(key) || "";
}

let TOKEN = loadStored(TOKEN_KEY);

/** 读取当前 token（供路由守卫等判断是否已登录） */
export function getToken() {
  return TOKEN;
}

export function setToken(token, remember = true) {
  TOKEN = token;
  const store = remember ? localStorage : sessionStorage;
  store.setItem(TOKEN_KEY, token);
}

export function clearToken() {
  TOKEN = "";
  localStorage.removeItem(TOKEN_KEY);
  sessionStorage.removeItem(TOKEN_KEY);
}

/** 读取当前登录用户（登录时由后端 data.user 写入） */
export function getUser() {
  try {
    const raw = loadStored(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function setUser(user, remember = true) {
  const store = remember ? localStorage : sessionStorage;
  store.setItem(USER_KEY, JSON.stringify(user));
}

export function clearUser() {
  localStorage.removeItem(USER_KEY);
  sessionStorage.removeItem(USER_KEY);
}

/** 退出登录：清空 token 和用户信息 */
export function clearAuth() {
  clearToken();
  clearUser();
}

/** 认证接口（后端同学按此契约实现）：
 *  POST /api/auth/login  body: { username, password }
 *    → data: { token, user: { username, realName, role } }
 *  POST /api/auth/logout
 *  POST /api/auth/forgot-password/verify  body: { username, phone }
 *    → data: { resetTicket, expiresIn }   // 校验用户名+手机号+账号启用，签发一次性票据
 *  POST /api/auth/reset-password  body: { resetTicket, newPassword }
 */
export const authApi = {
  login: (data) => request("/auth/login", { method: "POST", body: data }),
  logout: () => request("/auth/logout", { method: "POST" }),
  verifyForgotPassword: (data) => request("/auth/forgot-password/verify", { method: "POST", body: data }),
  resetPassword: (data) => request("/auth/reset-password", { method: "POST", body: data }),
};

async function realRequest(path, { method = "GET", params, body } = {}) {
  let url = CONFIG.baseUrl + path;
  if (params && Object.keys(params).length) {
    const qs = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== "") qs.set(k, v);
    });
    const s = qs.toString();
    if (s) url += "?" + s;
  }
  const isForm = typeof FormData !== "undefined" && body instanceof FormData;
  const res = await fetch(url, {
    method,
    headers: {
      // FormData（导入文件）时由浏览器自动生成 multipart 边界，不能手动指定 Content-Type
      ...(isForm ? {} : { "Content-Type": "application/json" }),
      ...(TOKEN ? { Authorization: `Bearer ${TOKEN}` } : {}),
    },
    body: body ? (isForm ? body : JSON.stringify(body)) : undefined,
  });
  // 兼容后端非 JSON 响应（如 Spring 默认错误页）：先读文本再解析
  const text = await res.text();
  let json;
  try {
    json = text ? JSON.parse(text) : {};
  } catch {
    throw new Error(`服务器返回异常（HTTP ${res.status}）`);
  }
  if (json.code !== 0) throw new Error(json.message || `请求失败（HTTP ${res.status}）`);
  return json.data;
}

const delay = (ms = 220) => new Promise((r) => setTimeout(r, ms));

/**
 * 统一请求入口。返回业务数据 data；失败时抛出带 message 的 Error。
 */
export async function request(path, { method = "GET", params, body } = {}) {
  if (CONFIG.useMock) {
    await delay();
    return mockRequest(path, { method, params, body });
  }
  return realRequest(path, { method, params, body });
}

export const http = {
  get: (path, params) => request(path, { method: "GET", params }),
  post: (path, body) => request(path, { method: "POST", body }),
  put: (path, body) => request(path, { method: "PUT", body }),
  del: (path) => request(path, { method: "DELETE" }),
};

/**
 * 生成某模块的 CRUD 接口对象（与接口文档一一对应）
 * @param {object} mod 模块配置（config/modules.js 中的一项）
 */
export function getModuleApi(mod) {
  const res = mod.resource;
  const base = `/${res}`;
  return {
    list: (params) => http.get(base, params),
    get: (id) => http.get(`${base}/${id}`),
    create: (data) => http.post(base, data),
    update: (id, data) => http.put(`${base}/${id}`, { id, ...data }),
    remove: (id) => http.del(`${base}/${id}`),
    // 批量操作：如 POST /api/projects/batch-number，body 直接传 id 数组
    batch: (sp, ids) => {
      let rel = sp.path;
      if (rel.startsWith(CONFIG.baseUrl)) rel = rel.slice(CONFIG.baseUrl.length);
      if (!rel.startsWith("/")) rel = "/" + rel;
      return http.post(rel, ids);
    },
    // 特殊操作：如 /api/projects/{id}/archive
    special: (sp, id, payload) => {
      // modules.js 里 special.path 带 /api 前缀（如 /api/projects/{id}/archive），
      // 而 request() 会再拼一次 CONFIG.baseUrl(/api)，这里去掉前缀，避免 /api/api/... 导致 404
      let rel = sp.path.replace("{id}", id);
      if (rel.startsWith(CONFIG.baseUrl)) rel = rel.slice(CONFIG.baseUrl.length);
      if (!rel.startsWith("/")) rel = "/" + rel;
      if (sp.method === "GET") return http.get(rel, payload);
      return http.post(rel, payload);
    },
    // 导出 / 导入
    export: (params) => http.get(`${base}/export`, params),
    import: (formData) => request(`${base}/import`, { method: "POST", body: formData }),
  };
}

// ---------------- 进度节点（项目进度详情页）接口 ----------------
export const progressApi = {
  // 项目进度详情：项目基础信息 + 进度汇总 + 节点列表 + 负责人候选
  board: (projectId) => http.get("/progress/board", { projectId }),
  // 上级节点候选：按项目过滤，编辑时传 excludeId 排除自身与子孙（防循环层级）
  parentOptions: (projectId, excludeId) =>
    http.get("/progress/parent-options", { projectId, excludeId }),
  create: (data) => http.post("/progress", data),
  update: (id, data) => http.put(`/progress/${id}`, { id, ...data }),
  remove: (id) => http.del(`/progress/${id}`),
};

// ---------------- 工作台（个人视角）接口 ----------------
export const workbenchApi = {
  // 待我审批 / 我的项目 / 逾期节点预警 / 最近施工日志 / 我发起的申请
  overview: () => http.get("/workbench/overview"),
};

// ---------------- 可视化管理（全局视角）接口 ----------------
export const dashboardApi = {
  // 项目规模、总体进度（实际 vs 计划）、状态分布、分项目进度对比
  overview: () => http.get("/visualization/dashboard"),
  progressTrend: (granularity) => http.get("/visualization/progress-trend", { granularity }),
  // 质量/安全检查合格率与整改闭环率
  inspectionStats: () => http.get("/visualization/inspection-stats"),
  projectStatus: () => http.get("/visualization/project-status"),
  keyNodes: (projectId) => http.get("/visualization/key-nodes", { projectId }),
};
