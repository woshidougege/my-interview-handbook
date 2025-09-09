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

// 模拟API - 用于开发测试
export const mockApi = {
  // 模拟当前用户信息
  getCurrentUser: (): Promise<UserInfo> => {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          id: '1001',
          username: 'testuser',
          email: 'test@example.com',
          phone: '13800138000',
          nickname: '测试用户',
          avatar: 'https://via.placeholder.com/64',
          status: 1,
          createTime: '2024-01-01 10:00:00',
          updateTime: '2024-01-01 10:00:00',
        });
      }, 500);
    });
  },
  
  // 模拟用户积分信息
  getUserCredit: (): Promise<UserCredit> => {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          id: '2001',
          userId: '1001',
          totalCredits: 1000,
          availableCredits: 850,
          frozenCredits: 50,
          expiredCredits: 100,
          createTime: '2024-01-01 10:00:00',
          updateTime: '2024-01-01 10:00:00',
        });
      }, 500);
    });
  },
  
  // 模拟积分交易记录
  getCreditTransactions: (): Promise<CreditTransaction[]> => {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve([
          {
            id: '3001',
            userId: '1001',
            transactionType: 1,
            transactionTypeDesc: '免费套餐每日赠送',
            amount: 30,
            balanceAfter: 880,
            description: '每日免费积分赠送',
            createTime: '2024-01-05 00:00:30',
          },
          {
            id: '3002',
            userId: '1001',
            transactionType: 4,
            transactionTypeDesc: 'Token消费',
            amount: -20,
            balanceAfter: 850,
            description: 'AI对话消费',
            relatedId: '4001',
            createTime: '2024-01-04 15:30:20',
          },
          {
            id: '3003',
            userId: '1001',
            transactionType: 1,
            transactionTypeDesc: '免费套餐每日赠送',
            amount: 30,
            balanceAfter: 870,
            description: '每日免费积分赠送',
            createTime: '2024-01-04 00:00:30',
          },
        ]);
      }, 500);
    });
  },
};

export default api;
