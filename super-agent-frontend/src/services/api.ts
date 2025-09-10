import axios, { AxiosResponse } from 'axios';
import { ApiResponse, UserCredit, CreditTransaction, UserInfo, PageResponse } from '@/types/user';

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

// 用户相关API
export const userApi = {
  // 获取当前用户信息
  getCurrentUser: (): Promise<AxiosResponse<ApiResponse<UserInfo>>> => {
    return api.get('/user/current');
  },
  
  // 获取用户信息
  getUserInfo: (userId: string): Promise<AxiosResponse<ApiResponse<UserInfo>>> => {
    return api.get(`/users/${userId}`);
  },
};

// 积分相关API
export const creditApi = {
  // 获取用户积分信息
  getUserCredit: (userId: string): Promise<AxiosResponse<ApiResponse<UserCredit>>> => {
    return api.get(`/user-credit/${userId}`);
  },
  
  // 获取积分交易记录
  getCreditTransactions: (
    userId: string,
    page: number = 1,
    size: number = 10
  ): Promise<AxiosResponse<ApiResponse<PageResponse<CreditTransaction>>>> => {
    return api.get(`/user-credit/${userId}/transactions`, {
      params: { pageNum: page, pageSize: size }
    });
  },
  
  // 检查用户是否有足够积分
  checkUserCredit: (userId: string, amount: number): Promise<AxiosResponse<ApiResponse<boolean>>> => {
    return api.get(`/user-credit/${userId}/available`, {
      params: { amount }
    });
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

export default api;
