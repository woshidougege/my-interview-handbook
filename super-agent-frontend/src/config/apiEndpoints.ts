/**
 * API端点配置
 * 统一管理所有后端API路径，避免硬编码
 * 
 * @author 任相鹏
 * @since 1.0.0
 */

// API基础配置
export const API_CONFIG = {
  // API基础路径 - 直接使用真实的后端路径，与nginx配置保持一致
  BASE_URL: '/super-agent/api/v1',
  
  // API版本
  VERSION: 'v1',
  
  // 请求超时时间（增加到5分钟以适应流式响应）
  TIMEOUT: 300000,
  
  // A2A平台配置
  A2A_PLATFORM: {
    // 默认能力中心编码
    DEFAULT_ABILITY_CODE: '18961714392032',
    // 默认实体编码
    DEFAULT_ENTITY_CODE: 'ENTITY_test_pp_dispatch',
  },
  
  // 默认请求头
  DEFAULT_HEADERS: {
    'Content-Type': 'application/json',
  },
} as const;

// API端点路径配置  
// 重要！前端直接使用完整的后端路径，与nginx配置保持一致
// 因为：axios baseURL="/super-agent/api/v1" + 端点"auth/login" = "/super-agent/api/v1/auth/login"
// 这样确保了前端请求路径与nginx拦截规则完全匹配

export const API_ENDPOINTS = {
  // ========== 认证相关 ==========
  // 后端: @RequestMapping("/api/v1/auth") -> 前端: auth/* (baseURL已包含/super-agent/api/v1)
  AUTH: {
    BASE: 'auth',
    LOGIN: 'auth/login',
    LOGOUT: 'auth/logout', 
    REFRESH_TOKEN: 'auth/refresh-token',
    VERIFY_TOKEN: 'auth/verify-token',
    CHANGE_PASSWORD: 'auth/change-password',
  },

  // ========== 用户相关（基于SSO） ==========
  // 注意：用户信息现在完全来自SSO，不再有用户CRUD操作
  USER: {
    // 获取当前用户信息（来自SSO）
    CURRENT: 'auth/user/current',
    // 获取加密公钥
    GET_PUBLIC_KEY: 'auth/public-key',
  },

  // ========== 工作空间相关 ==========
  // 后端: @RequestMapping("/api/v1/workspaces") -> 前端直接用: workspaces/* (后端已有v1)
  WORKSPACE: {
    BASE: 'workspaces',
    LIST: 'workspaces',
    CREATE: 'workspaces', 
    GET_BY_ID: (id: string) => `workspaces/${id}`,
    GET_BY_USER: (userId: string) => `workspaces/user/${userId}`,
    UPDATE: (id: string) => `workspaces/${id}`,
    DELETE: (id: string) => `workspaces/${id}`,
  },

  // ========== 对话任务相关 ==========
  // 后端: @RequestMapping("/api/v1/workspaces/{workspaceId}/chat-tasks") -> 前端直接用: workspaces/{workspaceId}/chat-tasks/* (后端已有v1)
  CHAT_TASK: {
    BASE: (workspaceId: string) => `workspaces/${workspaceId}/chat-tasks`,
    LIST: (workspaceId: string) => `workspaces/${workspaceId}/chat-tasks`,
    CREATE: (workspaceId: string) => `workspaces/${workspaceId}/chat-tasks`,
    GET_BY_ID: (workspaceId: string, taskId: string) => `workspaces/${workspaceId}/chat-tasks/${taskId}`,
    UPDATE: (workspaceId: string, taskId: string) => `workspaces/${workspaceId}/chat-tasks/${taskId}`,
    DELETE: (workspaceId: string, taskId: string) => `workspaces/${workspaceId}/chat-tasks/${taskId}`,
    FAVORITE: (workspaceId: string, taskId: string) => `workspaces/${workspaceId}/chat-tasks/${taskId}/favorite`,
    UNFAVORITE: (workspaceId: string, taskId: string) => `workspaces/${workspaceId}/chat-tasks/${taskId}/favorite`,
    GET_FAVORITES: (workspaceId: string) => `workspaces/${workspaceId}/chat-tasks/favorites`,
    GENERATE_TITLE: (workspaceId: string) => `workspaces/${workspaceId}/chat-tasks/generate-title`,
  },

  // ========== AI服务相关 ==========
  AI: {
    BASE: 'ai',
    GENERATE_TITLE: (workspaceId: string) => `workspaces/${workspaceId}/chat-tasks/generate-title`,
  },

  // ========== 语音识别相关 ==========
  SPEECH: {
    BASE: 'speech',
    RECOGNITION_UPLOAD: 'speech/recognition/upload',
  },

  // ========== 聊天相关 ==========
  // 后端: @PostMapping("/api/v1/chat/stream") -> 前端直接用: chat/* (后端已有v1)
  CHAT: {
    BASE: 'chat',
    STREAM: 'chat/stream',
    GENERATE_TITLE: 'chat/generate-title',
  },

  // ========== 支付相关 ==========
  // 后端: @RequestMapping("/api/v1/payment") -> 前端直接用: payment/*
  PAYMENT: {
    BASE: 'payment',
    CREATE: 'payment/create',
    CONFIRM: 'payment/confirm',
    STATUS: (paymentId: string) => `payment/${paymentId}/status`,
    CALLBACK: 'payment/callback',
    CANCEL: (paymentId: string) => `payment/${paymentId}/cancel`,
  },

  // ========== 订阅相关 ==========
  // 后端: @RequestMapping("/api/v1/subscription") -> 前端直接用: subscription/*
  SUBSCRIPTION: {
    BASE: 'subscription',
    LIST: 'subscription',
    CURRENT: 'subscription/current',
    CREATE: 'subscription',
    CANCEL: (subscriptionId: string) => `subscription/${subscriptionId}/cancel`,
    RENEW: (subscriptionId: string) => `subscription/${subscriptionId}/renew`,
    PLANS: 'subscription/plans',
    CREDIT_PURCHASE_CONFIG: 'subscription/credit-purchase-config',
  },

  // ========== 用户积分相关 ==========
  // 后端: @RequestMapping("/api/v1/credit") -> 前端直接用: credit/*
  CREDIT: {
    BASE: 'credit',
    DETAILS: 'credit/details',
    TRANSACTIONS: 'credit/transactions',
  },



  // ========== 资源使用相关 ==========
  // 后端: @RequestMapping("/api/v1/resource-usage") -> 前端直接用: resource-usage/*
  RESOURCE_USAGE: {
    BASE: 'resource-usage',
    REPORT: 'resource-usage/report',
    STATISTICS: 'resource-usage/statistics',
    HISTORY: 'resource-usage/history',
  },

  // ========== 向后兼容的旧API（无v1前缀） ==========
  // 这些API路径直接在/api下，没有v1版本
  LEGACY: {
    USER_CREDIT: (userId: string) => `user-credit/${userId}`,
    USER_CREDIT_TRANSACTIONS: (userId: string) => `user-credit/${userId}/transactions`,
    AUTH_PASSWORD_LOGIN: 'auth/password-login',
    AUTH_PHONE_LOGIN: 'auth/phone-login',
    AUTH_SEND_SMS: 'auth/send-sms',
    AUTH_REGISTER: 'auth/register',
    AUTH_GET_SYS_CLIENT_INFO: 'auth/get-sys-client-info',
    AUTH_PUBLIC_KEY: 'auth/public-key',
    AUTH_USER_CURRENT: 'auth/user/current',
    AUTH_CHANGE_PASSWORD: 'auth/change-password',
    // USERS_UPDATE: 'users/update', // 已废弃：SSO模式下不支持用户信息更新
    SUBSCRIPTION_PLANS: 'subscription/plans',
    PAYMENT_CREATE: 'payment/create',
    PAYMENT_STATUS: (orderNo: string) => `payment/status/${orderNo}`,
    PAYMENT_CANCEL: (orderNo: string) => `payment/cancel/${orderNo}`,
    PAYMENT_ORDERS: (userId: number) => `payment/orders/${userId}`,
    PAYMENT_REFUND: 'payment/refund',
    PAYMENT_TEST_REFUND: 'payment/test-refund',
    PAYMENT_REFUND_STATUS: (orderNo: string) => `payment/refund/status/${orderNo}`,
    CREDIT_RECHARGE: 'credit/recharge',
  },

  // ========== SSO单点登录相关 ==========
  // 生产环境使用相对路径，依赖nginx代理；开发环境使用完整路径
  SSO: {
    DO_LOGIN_BY_TICKET: (ticket: string) => `/super-agent/sso/doLoginByTicket?ticket=${ticket}`,
    GET_USER: `/super-agent/sso/getuser`,
    GET_MENU: `/super-agent/sso/getmenu`,
  },
} as const;

// 工具函数：构建完整的API URL
export const buildApiUrl = (endpoint: string): string => {
  // 确保端点以斜杠开头，以便正确拼接
  const normalizedEndpoint = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
  return `${API_CONFIG.BASE_URL}${normalizedEndpoint}`;
};

// 工具函数：验证端点是否存在
export const isValidEndpoint = (endpoint: string): boolean => {
  return endpoint.startsWith('auth/') || endpoint.startsWith('user-credit/') || endpoint.startsWith('subscription/') || endpoint.startsWith('workspaces/');
};

// 导出所有端点的扁平化列表（用于调试和文档生成）
export const getAllEndpoints = (): string[] => {
  const endpoints: string[] = [];
  
  const extractEndpoints = (obj: Record<string, unknown>, prefix = ''): void => {
    Object.entries(obj).forEach(([key, value]) => {
      if (typeof value === 'string') {
        endpoints.push(value);
      } else if (typeof value === 'function') {
        // 对于函数类型的端点，我们添加一个示例
        endpoints.push(`${value.toString()} (function)`);
      } else if (typeof value === 'object' && value !== null) {
        extractEndpoints(value as Record<string, unknown>, `${prefix}${key}.`);
      }
    });
  };
  
  extractEndpoints(API_ENDPOINTS);
  return endpoints.sort();
};

// 开发环境下打印所有端点（用于调试）
if (process.env.NODE_ENV === 'development') {
  console.group('🔗 API Endpoints Configuration');
  console.log('Base URL:', API_CONFIG.BASE_URL);
  console.log('All endpoints:', getAllEndpoints());
  console.groupEnd();
}
