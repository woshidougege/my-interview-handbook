import axios, { AxiosResponse } from 'axios';
import { ApiResponse } from '@/types/user';

// 创建axios实例
const api = axios.create({
  baseURL: process.env.NODE_ENV === 'development' ? 'http://localhost:8081/super-agent/api' : '/super-agent/api',
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
    return api.post('/v1/auth/password-login', data);
  },
  
  // 手机验证码登录
  phoneLogin: (data: { phone: string; smsCode: string }): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.post('/v1/auth/phone-login', data);
  },
  
  // 发送短信验证码
  sendSms: (data: { phone: string }): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.post('/v1/auth/send-sms', data);
  },
  
  // 用户注册
  register: (data: { username: string; password: string; phone: string }): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.post('/v1/auth/register', data);
  },

  // 获取系统客户端信息（包含SM2公钥）
  getSysClientInfo: (data: { serviceCode: string }): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.post('/v1/auth/get-sys-client-info', data);
  },

  // 获取公钥信息
  getPublicKey: (): Promise<AxiosResponse<ApiResponse<any>>> => {
    return api.get('/v1/auth/public-key');
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