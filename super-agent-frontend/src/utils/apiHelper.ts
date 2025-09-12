/**
 * API辅助工具
 * 提供API调用的工具函数和常用操作
 * 
 * @author AI Assistant
 * @since 1.0.0
 */

import { buildApiUrl, API_ENDPOINTS, getAllEndpoints } from '@/config/apiEndpoints';
import { ApiResponse, PageRequest, PageResponse } from '@/types/api';

// 构建完整API URL的示例用法
export const getFullApiUrl = (endpoint: string): string => {
  return buildApiUrl(endpoint);
};

// 打印所有端点（调试用）
export const logAllEndpoints = (): void => {
  console.group('📋 API端点列表');
  getAllEndpoints().forEach(endpoint => {
    console.log(`🔗 ${endpoint}`);
  });
  console.groupEnd();
};

// 构建分页参数
export const buildPageParams = (pageRequest: PageRequest) => {
  return {
    pageNum: pageRequest.pageNum || 1,
    pageSize: pageRequest.pageSize || 10,
    ...(pageRequest.keyword && { keyword: pageRequest.keyword }),
  };
};

// 处理API响应
export const handleApiResponse = <T>(response: ApiResponse<T>): T => {
  if (response.code === 200) {
    return response.data;
  }
  throw new Error(response.message || '请求失败');
};

// 处理分页响应
export const handlePageResponse = <T>(response: ApiResponse<PageResponse<T>>): PageResponse<T> => {
  if (response.code === 200) {
    return response.data;
  }
  throw new Error(response.message || '分页请求失败');
};

// API端点验证
export const validateEndpoint = (endpoint: string): boolean => {
  return endpoint.startsWith('/v1/') || endpoint.startsWith('/auth/') || endpoint.startsWith('/payment/');
};

// 生成工作空间相关的API路径
export const generateWorkspaceApiPaths = (workspaceId: string) => ({
  chatTasks: {
    list: API_ENDPOINTS.CHAT_TASK.LIST(workspaceId),
    create: API_ENDPOINTS.CHAT_TASK.CREATE(workspaceId),
    getById: (taskId: string) => API_ENDPOINTS.CHAT_TASK.GET_BY_ID(workspaceId, taskId),
    update: (taskId: string) => API_ENDPOINTS.CHAT_TASK.UPDATE(workspaceId, taskId),
    delete: (taskId: string) => API_ENDPOINTS.CHAT_TASK.DELETE(workspaceId, taskId),
    favorite: (taskId: string) => API_ENDPOINTS.CHAT_TASK.FAVORITE(workspaceId, taskId),
    getFavorites: API_ENDPOINTS.CHAT_TASK.GET_FAVORITES(workspaceId),
  },
  ai: {
    generateTitle: API_ENDPOINTS.AI.GENERATE_TITLE(workspaceId),
  },
});

// API错误处理辅助
export const createApiError = (message: string, code?: number) => {
  const error = new Error(message);
  if (code) {
    (error as any).code = code;
  }
  return error;
};

// 导出常用端点
export const COMMON_ENDPOINTS = {
  // 认证相关
  login: API_ENDPOINTS.AUTH.LOGIN,
  logout: API_ENDPOINTS.AUTH.LOGOUT,
  
  // 用户相关
  userProfile: API_ENDPOINTS.USER.PROFILE,
  userPublicKey: API_ENDPOINTS.USER.GET_PUBLIC_KEY,
  
  // 积分相关
  creditBalance: API_ENDPOINTS.USER_CREDIT.BALANCE,
  creditHistory: API_ENDPOINTS.USER_CREDIT.HISTORY,
  
  // 支付相关
  paymentCreate: API_ENDPOINTS.PAYMENT.CREATE,
  
  // 订阅相关
  subscriptionCurrent: API_ENDPOINTS.SUBSCRIPTION.CURRENT,
  subscriptionList: API_ENDPOINTS.SUBSCRIPTION.LIST,
} as const;

// 开发环境调试信息
if (process.env.NODE_ENV === 'development') {
  console.group('🛠️ API Helper Loaded');
  console.log('Common endpoints:', COMMON_ENDPOINTS);
  console.log('Use logAllEndpoints() to see all available endpoints');
  console.groupEnd();
}
