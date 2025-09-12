import axios, { AxiosResponse } from 'axios';
import { ApiResponse } from '@/types/user';
import { API_CONFIG, API_ENDPOINTS } from '@/config/apiEndpoints';
import {
  ApiPromise,
  LoginRequest,
  LoginResponse,
  UserProfileRequest,
  ChangePasswordRequest,
  PublicKeyResponse,
  WorkspaceCreateRequest,
  WorkspaceUpdateRequest,
  WorkspaceInfo,
  ChatTaskCreateRequest,
  ChatTaskUpdateRequest,
  ChatTaskInfo,
  ChatTitleGenerateRequest,
  ChatTitleGenerateResponse,
  PaymentCreateRequest,
  PaymentInfo,
  SubscriptionCreateRequest,
  SubscriptionInfo,
  CreditBalance,
  CreditTransaction,
  CreditConsumeRequest,
  CreditRechargeRequest,
  ResourceUsageReport,
  PageRequest,
  PageResponse,
} from '@/types/api';

// 创建axios实例
const api = axios.create({
  baseURL: API_CONFIG.BASE_URL,
  timeout: API_CONFIG.TIMEOUT,
  headers: API_CONFIG.DEFAULT_HEADERS,
});

// 请求拦截器
api.interceptors.request.use(
  (config) => {
    // 可以在这里添加token等认证信息
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器
api.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const { data } = response;
    if (data.code === 200) {
      return response;
    } else {
      throw new Error(data.message || '请求失败');
    }
  },
  (error) => {
    const message = error.response?.data?.message || error.message || '网络错误';
    throw new Error(message);
  }
);

// ========== 认证相关API ==========
export const authApi = {
  // 用户登录
  login: (data: LoginRequest): ApiPromise<LoginResponse> => {
    return api.post(API_ENDPOINTS.AUTH.LOGIN, data);
  },
  
  // 用户登出
  logout: (): ApiPromise<void> => {
    return api.post(API_ENDPOINTS.AUTH.LOGOUT);
  },
  
  // 刷新Token
  refreshToken: (refreshToken: string): ApiPromise<{ token: string; refreshToken: string }> => {
    return api.post(API_ENDPOINTS.AUTH.REFRESH_TOKEN, { refreshToken });
  },
  
  // 验证Token
  verifyToken: (): ApiPromise<{ valid: boolean }> => {
    return api.get(API_ENDPOINTS.AUTH.VERIFY_TOKEN);
  },
  
  // 修改密码
  changePassword: (data: ChangePasswordRequest): ApiPromise<void> => {
    return api.post(API_ENDPOINTS.AUTH.CHANGE_PASSWORD, data);
  },

  // 密码登录（向后兼容）
  passwordLogin: (data: { username: string; password: string }): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.post(API_ENDPOINTS.LEGACY.AUTH_PASSWORD_LOGIN, data);
  },
  
  // 手机验证码登录（向后兼容）
  phoneLogin: (data: { phone: string; smsCode: string }): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.post(API_ENDPOINTS.LEGACY.AUTH_PHONE_LOGIN, data);
  },
  
  // 发送短信验证码（向后兼容）
  sendSms: (data: { phone: string }): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.post(API_ENDPOINTS.LEGACY.AUTH_SEND_SMS, data);
  },
  
  // 用户注册（向后兼容）
  register: (data: { username: string; password: string; phone: string }): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.post(API_ENDPOINTS.LEGACY.AUTH_REGISTER, data);
  },

  // 获取系统客户端信息（包含SM2公钥）（向后兼容）
  getSysClientInfo: (data: { serviceCode: string }): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.post(API_ENDPOINTS.LEGACY.AUTH_GET_SYS_CLIENT_INFO, data);
  },

  // 获取公钥信息（向后兼容）
  getPublicKey: (): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.get(API_ENDPOINTS.LEGACY.AUTH_PUBLIC_KEY);
  },
};

// ========== 订阅相关API ==========
export const subscriptionApi = {
  // 获取订阅列表
  getSubscriptions: (): ApiPromise<SubscriptionInfo[]> => {
    return api.get(API_ENDPOINTS.SUBSCRIPTION.LIST);
  },
  
  // 获取当前订阅
  getCurrentSubscription: (userId?: string): ApiPromise<SubscriptionInfo> => {
    const params = userId ? { userId } : {};
    return api.get(API_ENDPOINTS.SUBSCRIPTION.CURRENT, { params });
  },
  
  // 获取套餐列表
  getPlans: (): ApiPromise<any[]> => {
    return api.get(API_ENDPOINTS.SUBSCRIPTION.PLANS);
  },
  
  // 创建订阅
  createSubscription: (data: SubscriptionCreateRequest): ApiPromise<SubscriptionInfo> => {
    return api.post(API_ENDPOINTS.SUBSCRIPTION.CREATE, data);
  },
  
  // 取消订阅
  cancelSubscription: (subscriptionId: string): ApiPromise<void> => {
    return api.post(API_ENDPOINTS.SUBSCRIPTION.CANCEL(subscriptionId));
  },
  
  // 续费订阅
  renewSubscription: (subscriptionId: string): ApiPromise<SubscriptionInfo> => {
    return api.post(API_ENDPOINTS.SUBSCRIPTION.RENEW(subscriptionId));
  },

  // 向后兼容的API
  getPlansOld: (): Promise<AxiosResponse<ApiResponse<any[]>>> => {
    return api.get(API_ENDPOINTS.LEGACY.SUBSCRIPTION_PLANS);
  },
};

// ========== 用户相关API ==========
export const userApi = {
  // 获取用户资料
  getProfile: (): ApiPromise<any> => {
    return api.get(API_ENDPOINTS.USER.PROFILE);
  },
  
  // 更新用户资料
  updateProfile: (data: UserProfileRequest): ApiPromise<any> => {
    return api.put(API_ENDPOINTS.USER.UPDATE_PROFILE, data);
  },
  
  // 获取公钥
  getPublicKey: (): ApiPromise<PublicKeyResponse> => {
    return api.get(API_ENDPOINTS.USER.GET_PUBLIC_KEY);
  },

  // 向后兼容的API
  getCurrentUser: (): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.get(API_ENDPOINTS.LEGACY.AUTH_USER_CURRENT);
  },
  
  updateUser: (data: any): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.put(API_ENDPOINTS.LEGACY.USERS_UPDATE, data);
  },
};

// ========== 积分相关API ==========
export const creditApi = {
  // 获取积分余额
  getBalance: (): ApiPromise<CreditBalance> => {
    return api.get(API_ENDPOINTS.USER_CREDIT.BALANCE);
  },
  
  // 获取积分历史记录
  getHistory: (params: PageRequest): ApiPromise<PageResponse<CreditTransaction>> => {
    return api.get(API_ENDPOINTS.USER_CREDIT.HISTORY, { params });
  },
  
  // 消费积分
  consume: (data: CreditConsumeRequest): ApiPromise<void> => {
    return api.post(API_ENDPOINTS.USER_CREDIT.CONSUME, data);
  },
  
  // 充值积分
  recharge: (data: CreditRechargeRequest): ApiPromise<void> => {
    return api.post(API_ENDPOINTS.USER_CREDIT.RECHARGE, data);
  },

  // 向后兼容的API
  getUserCredit: (userId: string): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.get(API_ENDPOINTS.LEGACY.USER_CREDIT(userId));
  },
  
  getCreditTransactions: (userId: string, pageNum: number = 1, pageSize: number = 10): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.get(API_ENDPOINTS.LEGACY.USER_CREDIT_TRANSACTIONS(userId), {
      params: { pageNum, pageSize }
    });
  },
};

// ========== 支付相关API ==========
export const paymentApi = {
  // 创建支付订单
  create: (data: PaymentCreateRequest): ApiPromise<PaymentInfo> => {
    return api.post(API_ENDPOINTS.PAYMENT.CREATE, data);
  },
  
  // 确认支付
  confirm: (paymentId: string, data: any): ApiPromise<PaymentInfo> => {
    return api.post(API_ENDPOINTS.PAYMENT.CONFIRM, { paymentId, ...data });
  },
  
  // 查询支付状态
  getStatus: (paymentId: string): ApiPromise<PaymentInfo> => {
    return api.get(API_ENDPOINTS.PAYMENT.STATUS(paymentId));
  },
  
  // 取消支付
  cancel: (paymentId: string): ApiPromise<void> => {
    return api.post(API_ENDPOINTS.PAYMENT.CANCEL(paymentId));
  },

  // 向后兼容的API
  createOrder: (userId: string, data: {
    planId: number;
    billingCycle: string;
    paymentMethod: string;
  }): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.post(API_ENDPOINTS.LEGACY.PAYMENT_CREATE, data, {
      params: { userId }
    });
  },
  
  queryStatus: (orderNo: string): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.get(API_ENDPOINTS.LEGACY.PAYMENT_STATUS(orderNo));
  },
  
  cancelOrder: (orderNo: string): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.post(API_ENDPOINTS.LEGACY.PAYMENT_CANCEL(orderNo));
  },
  
  getUserOrders: (userId: number): Promise<AxiosResponse<ApiResponse<any[]>>> => {
    return api.get(API_ENDPOINTS.LEGACY.PAYMENT_ORDERS(userId));
  },
  
  applyRefund: (refundData: {
    orderNo: string;
    refundAmount: number;
    refundReason?: string;
  }): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.post(API_ENDPOINTS.LEGACY.PAYMENT_REFUND, refundData);
  },
  
  queryRefundStatus: (orderNo: string): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.get(API_ENDPOINTS.LEGACY.PAYMENT_REFUND_STATUS(orderNo));
  },
};

// ========== AI服务相关API ==========
export const aiApi = {
  // 生成对话标题
  generateChatTitle: (workspaceId: string, data: ChatTitleGenerateRequest): ApiPromise<ChatTitleGenerateResponse> => {
    return api.post(API_ENDPOINTS.AI.GENERATE_TITLE(workspaceId), data);
  },
};

// ========== 工作空间相关API ==========
export const workspaceApi = {
  // 获取工作空间列表
  getWorkspaces: (): ApiPromise<WorkspaceInfo[]> => {
    return api.get(API_ENDPOINTS.WORKSPACE.LIST);
  },
  
  // 创建工作空间
  createWorkspace: (data: WorkspaceCreateRequest): ApiPromise<WorkspaceInfo> => {
    return api.post(API_ENDPOINTS.WORKSPACE.CREATE, data);
  },
  
  // 获取工作空间详情
  getWorkspace: (id: string): ApiPromise<WorkspaceInfo> => {
    return api.get(API_ENDPOINTS.WORKSPACE.GET_BY_ID(id));
  },
  
  // 更新工作空间
  updateWorkspace: (id: string, data: WorkspaceUpdateRequest): ApiPromise<WorkspaceInfo> => {
    return api.put(API_ENDPOINTS.WORKSPACE.UPDATE(id), data);
  },
  
  // 删除工作空间
  deleteWorkspace: (id: string): ApiPromise<void> => {
    return api.delete(API_ENDPOINTS.WORKSPACE.DELETE(id));
  },
};

// ========== 对话任务相关API ==========
export const chatTaskApi = {
  // 创建对话任务
  create: (workspaceId: string, data: ChatTaskCreateRequest): ApiPromise<ChatTaskInfo> => {
    return api.post(API_ENDPOINTS.CHAT_TASK.CREATE(workspaceId), data);
  },
  
  // 获取对话任务列表
  getList: (workspaceId: string, params: PageRequest): ApiPromise<PageResponse<ChatTaskInfo>> => {
    return api.get(API_ENDPOINTS.CHAT_TASK.LIST(workspaceId), { params });
  },
  
  // 获取对话任务详情
  getById: (workspaceId: string, taskId: string): ApiPromise<ChatTaskInfo> => {
    return api.get(API_ENDPOINTS.CHAT_TASK.GET_BY_ID(workspaceId, taskId));
  },
  
  // 更新对话任务
  update: (workspaceId: string, taskId: string, data: ChatTaskUpdateRequest): ApiPromise<ChatTaskInfo> => {
    return api.put(API_ENDPOINTS.CHAT_TASK.UPDATE(workspaceId, taskId), data);
  },
  
  // 删除对话任务
  delete: (workspaceId: string, taskId: string): ApiPromise<void> => {
    return api.delete(API_ENDPOINTS.CHAT_TASK.DELETE(workspaceId, taskId));
  },
  
  // 收藏对话任务
  favorite: (workspaceId: string, taskId: string): ApiPromise<void> => {
    return api.post(API_ENDPOINTS.CHAT_TASK.FAVORITE(workspaceId, taskId));
  },
  
  // 取消收藏对话任务
  unfavorite: (workspaceId: string, taskId: string): ApiPromise<void> => {
    return api.delete(API_ENDPOINTS.CHAT_TASK.UNFAVORITE(workspaceId, taskId));
  },
  
  // 获取收藏的对话任务
  getFavorites: (workspaceId: string, params: PageRequest): ApiPromise<PageResponse<ChatTaskInfo>> => {
    return api.get(API_ENDPOINTS.CHAT_TASK.GET_FAVORITES(workspaceId), { params });
  },

  // 向后兼容的API
  createChatTask: (workspaceId: string, data: {
    workspaceId: string;
    title: string;
    description?: string;
  }): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.post(API_ENDPOINTS.CHAT_TASK.CREATE(workspaceId), data);
  },
  
  getChatTasks: (workspaceId: string, params: {
    pageNum?: number;
    pageSize?: number;
    keyword?: string;
  } = {}): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.get(API_ENDPOINTS.CHAT_TASK.LIST(workspaceId), { params });
  },
  
  getChatTask: (workspaceId: string, taskId: string): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.get(API_ENDPOINTS.CHAT_TASK.GET_BY_ID(workspaceId, taskId));
  },
  
  updateChatTask: (workspaceId: string, taskId: string, data: {
    title?: string;
    description?: string;
  }): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.put(API_ENDPOINTS.CHAT_TASK.UPDATE(workspaceId, taskId), data);
  },
  
  deleteChatTask: (workspaceId: string, taskId: string): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.delete(API_ENDPOINTS.CHAT_TASK.DELETE(workspaceId, taskId));
  },
  
  favoriteChatTask: (workspaceId: string, taskId: string): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.post(API_ENDPOINTS.CHAT_TASK.FAVORITE(workspaceId, taskId));
  },
  
  unfavoriteChatTask: (workspaceId: string, taskId: string): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.delete(API_ENDPOINTS.CHAT_TASK.UNFAVORITE(workspaceId, taskId));
  },
  
  getFavoriteChatTasks: (workspaceId: string, params: {
    pageNum?: number;
    pageSize?: number;
  } = {}): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.get(API_ENDPOINTS.CHAT_TASK.GET_FAVORITES(workspaceId), { params });
  },
};

// ========== 积分管理相关API（管理员） ==========
export const creditManagementApi = {
  // 获取用户列表
  getUsers: (params: PageRequest): ApiPromise<PageResponse<any>> => {
    return api.get(API_ENDPOINTS.CREDIT_MANAGEMENT.USERS, { params });
  },
  
  // 调整用户积分
  adjustCredit: (data: {
    userId: string;
    amount: number;
    type: 'ADD' | 'SUBTRACT';
    reason: string;
  }): ApiPromise<void> => {
    return api.post(API_ENDPOINTS.CREDIT_MANAGEMENT.ADJUST, data);
  },
  
  // 获取积分统计
  getStatistics: (): ApiPromise<any> => {
    return api.get(API_ENDPOINTS.CREDIT_MANAGEMENT.STATISTICS);
  },
};

// ========== 资源使用相关API ==========
export const resourceUsageApi = {
  // 上报资源使用
  report: (data: ResourceUsageReport): ApiPromise<void> => {
    return api.post(API_ENDPOINTS.RESOURCE_USAGE.REPORT, data);
  },
  
  // 获取使用统计
  getStatistics: (params?: {
    startDate?: string;
    endDate?: string;
    resourceType?: string;
  }): ApiPromise<any> => {
    return api.get(API_ENDPOINTS.RESOURCE_USAGE.STATISTICS, { params });
  },
  
  // 获取使用历史
  getHistory: (params: PageRequest & {
    startDate?: string;
    endDate?: string;
    resourceType?: string;
  }): ApiPromise<PageResponse<ResourceUsageReport>> => {
    return api.get(API_ENDPOINTS.RESOURCE_USAGE.HISTORY, { params });
  },
};

export default api;