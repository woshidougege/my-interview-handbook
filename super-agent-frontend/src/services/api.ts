import axios, { AxiosResponse } from 'axios';
import { ApiResponse } from '@/types/user';

// 创建axios实例
const api = axios.create({
  baseURL: process.env.NODE_ENV === 'development' ? '/api' : '/super-agent/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
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

// 认证相关API
export const authApi = {
  // 密码登录
  passwordLogin: (data: { username: string; password: string }): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.post('/auth/password-login', data);
  },
  
  // 手机验证码登录
  phoneLogin: (data: { phone: string; smsCode: string }): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.post('/auth/phone-login', data);
  },
  
  // 发送短信验证码
  sendSms: (data: { phone: string }): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.post('/auth/send-sms', data);
  },
  
  // 用户注册
  register: (data: { username: string; password: string; phone: string }): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.post('/auth/register', data);
  },

  // 获取系统客户端信息（包含SM2公钥）
  getSysClientInfo: (data: { serviceCode: string }): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.post('/auth/get-sys-client-info', data);
  },

  // 获取公钥信息
  getPublicKey: (): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.get('/auth/public-key');
  },
};

// 订阅相关API
export const subscriptionApi = {
  // 获取套餐列表
  getPlans: (): Promise<AxiosResponse<ApiResponse<any[]>>> => {
    return api.get('/subscription/plans');
  },
  
  // 获取用户当前订阅
  getCurrentSubscription: (userId: string): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.get('/subscription/current', {
      params: { userId }
    });
  },
};

// 用户相关API
export const userApi = {
  // 获取当前用户信息
  getCurrentUser: (): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.get('/auth/user/current');
  },
  
  // 更新用户信息
  updateUser: (data: any): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.put('/users/update', data);
  },
};

// 积分相关API
export const creditApi = {
  // 获取用户积分信息
  getUserCredit: (userId: string): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.get(`/user-credit/${userId}`);
  },
  
  // 获取积分交易记录
  getCreditTransactions: (userId: string, pageNum: number = 1, pageSize: number = 10): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.get(`/user-credit/${userId}/transactions`, {
      params: { pageNum, pageSize }
    });
  },
  
  // 充值积分
  recharge: (data: { amount: number; paymentMethod: string }): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.post('/credit/recharge', data);
  },
};

// 支付相关API
export const paymentApi = {
  // 创建订单并发起支付
  createOrder: (userId: string, data: {
    planId: number;
    billingCycle: string;
    paymentMethod: string;
  }): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.post('/payment/create', data, {
      params: { userId }
    });
  },
  
  // 查询支付状态
  queryStatus: (orderNo: string): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.get(`/payment/status/${orderNo}`);
  },
  
  // 取消订单
  cancelOrder: (orderNo: string): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.post(`/payment/cancel/${orderNo}`);
  },
  
  // 获取用户订单列表
  getUserOrders: (userId: number): Promise<AxiosResponse<ApiResponse<any[]>>> => {
    return api.get(`/payment/orders/${userId}`);
  },
  
  // 申请退款
  applyRefund: (refundData: {
    orderNo: string;
    refundAmount: number;
    refundReason?: string;
  }): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.post('/payment/refund', refundData);
  },
  
  // 查询退款状态
  queryRefundStatus: (orderNo: string): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.get(`/payment/refund/status/${orderNo}`);
  },
  
};

export default api;