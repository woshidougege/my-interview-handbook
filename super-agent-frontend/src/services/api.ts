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
  UserSubscriptionResponse,
  CreditBalance,
  CreditTransaction,
  CreditConsumeRequest,
  CreditRechargeRequest,
  UserCreditDetailsResponse,
  CreditPurchaseConfig,
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

  // 修改密码（使用后端API代理SSO）
  changePasswordSSO: (data: { newPassword: string }): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.post(API_ENDPOINTS.LEGACY.AUTH_CHANGE_PASSWORD, data);
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
  
  // 获取当前订阅信息（仅返回订阅信息，积分信息通过独立接口获取）
  getCurrentSubscription: (): ApiPromise<UserSubscriptionResponse> => {
    // 注意：该接口现在仅返回订阅信息，积分信息需要单独调用 creditApi.getCreditDetails
    // 返回格式: { subscription: SubscriptionInfo, planName: string, planCode: string }
    return api.get(API_ENDPOINTS.SUBSCRIPTION.CURRENT);
  },
  
  // 获取套餐列表
  getPlans: (): ApiPromise<any[]> => {
    return api.get(API_ENDPOINTS.SUBSCRIPTION.PLANS);
  },

  // 根据计费周期获取套餐列表（已废弃，现在直接调用getPlans即可）
  getPlansWithBillingCycle: (billingCycle: 'monthly' | 'yearly'): ApiPromise<{yearlyDiscountRate: number, plans: any[]}> => {
    // 新接口已包含所有价格信息，不再需要billingCycle参数
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

  // 获取积分购买配置
  getCreditPurchaseConfig: (): ApiPromise<CreditPurchaseConfig> => {
    return api.get(API_ENDPOINTS.SUBSCRIPTION.CREDIT_PURCHASE_CONFIG);
  },
};

// ========== 积分相关API ==========
export const creditApi = {
  // 获取用户积分详情
  getCreditDetails: (): ApiPromise<UserCreditDetailsResponse> => {
    return api.get(API_ENDPOINTS.CREDIT.DETAILS);
  },
  
  // 获取积分交易记录
  getCreditTransactions: (pageNum?: number, pageSize?: number): ApiPromise<PageResponse<CreditTransaction>> => {
    return api.get(API_ENDPOINTS.CREDIT.TRANSACTIONS, {
      params: { pageNum, pageSize }
    });
  },
};

// ========== 用户相关API（基于SSO） ==========
export const userApi = {
  // 获取当前用户信息（直接从SSO）
  getCurrentUser: (): Promise<any> => {
    // 直接调用SSO的getuser接口，不走后端API
    return fetch(API_ENDPOINTS.SSO.GET_USER, {
      method: 'GET',
      headers: {
        'satoken': document.cookie.split('; ')
          .find(row => row.startsWith('satoken='))?.split('=')[1] || '',
      },
    }).then(response => response.json())
    .then(result => {
      // 字段映射：统一SSO字段名和前端使用的字段名
      if (result.code === 200 && result.data) {
        const user = result.data;
        // 映射字段名
        user.username = user.userName;  // userName -> username
        user.phone = user.phonenumber;  // phonenumber -> phone
        user.id = user.userId;          // userId -> id (向后兼容)
      }
      // 为了兼容现有代码的 data.data 结构，包装一下返回结果
      return { data: result };
    });
  },
  
  // 获取加密公钥
  getPublicKey: (): ApiPromise<PublicKeyResponse> => {
    return api.get(API_ENDPOINTS.USER.GET_PUBLIC_KEY);
  },

  // 向后兼容的API（逐步废弃）
  getProfile: (): Promise<any> => {
    // 重定向到SSO获取用户信息
    return userApi.getCurrentUser();
  },
  
  updateProfile: (data: UserProfileRequest): ApiPromise<any> => {
    // SSO模式下，用户信息由SSO管理，不支持更新
    throw new Error('用户信息更新请在SSO系统中进行');
  },
  
  updateUser: (data: any): Promise<AxiosResponse<ApiResponse<any>>> => {
    // SSO模式下，用户信息由SSO管理，不支持更新
    throw new Error('用户信息更新请在SSO系统中进行');
  },

  // 获取用户积分详情
  getCreditDetails: (): ApiPromise<any> => {
    return api.get(API_ENDPOINTS.CREDIT.DETAILS);
  },

  // 分页查询积分交易记录
  getCreditTransactions: (pageNum: number = 1, pageSize: number = 10): ApiPromise<any> => {
    return api.get(API_ENDPOINTS.CREDIT.TRANSACTIONS, {
      params: { pageNum, pageSize }
    });
  }
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
  createOrder: (data: {
    planId: number;
    billingCycle: string;
    paymentMethod: string;
    creditPackageId?: string;
  }): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.post(API_ENDPOINTS.LEGACY.PAYMENT_CREATE, data);
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
  
  testRefund: (refundData: {
    orderNo: string;
    refundAmount: number;
    refundReason?: string;
  }): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.post(API_ENDPOINTS.LEGACY.PAYMENT_TEST_REFUND, refundData);
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
  // 获取当前用户的工作空间列表
  getWorkspaces: async (): Promise<AxiosResponse<ApiResponse<WorkspaceInfo[]>>> => {
    // 先获取当前用户信息
    const userResponse = await userApi.getCurrentUser();
    const userId = userResponse.data.data.userId;
    // 调用按用户获取工作空间的接口
    return api.get(API_ENDPOINTS.WORKSPACE.GET_BY_USER(userId.toString()));
  },

  // 根据用户ID获取工作空间列表  
  getWorkspacesByUserId: (userId: string): ApiPromise<WorkspaceInfo[]> => {
    return api.get(API_ENDPOINTS.WORKSPACE.GET_BY_USER(userId));
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