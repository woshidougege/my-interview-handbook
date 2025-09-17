/**
 * API相关的TypeScript类型定义
 * 
 * @author AI Assistant
 * @since 1.0.0
 */

import { AxiosResponse } from 'axios';

// ========== 通用API响应类型 ==========
export interface ApiResponse<T = any> {
  code: number;
  message: string;
  data: T;
  timestamp?: string;
  path?: string;
}

// ========== 分页相关类型 ==========
export interface PageRequest {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
}

export interface PageResponse<T> {
  records: T[];
  total: number;
  pageNum: number;
  pageSize: number;
  pages?: number;
}

// ========== API调用相关类型 ==========
export type ApiPromise<T = any> = Promise<AxiosResponse<ApiResponse<T>>>;

export interface ApiRequestConfig {
  timeout?: number;
  headers?: Record<string, string>;
  params?: Record<string, any>;
}

// ========== 认证相关类型 ==========
export interface LoginRequest {
  username: string;
  password: string;
  encryptedData?: string;
}

export interface LoginResponse {
  token: string;
  refreshToken: string;
  user: UserInfo;
}

export interface UserInfo {
  id: string;
  username: string;
  email?: string;
  nickname?: string;
  avatar?: string;
  roles?: string[];
}

// ========== 用户相关类型 ==========
export interface UserProfileRequest {
  nickname?: string;
  email?: string;
  avatar?: string;
}

export interface ChangePasswordRequest {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export interface PublicKeyResponse {
  publicKey: string;
  keyId: string;
}

// ========== 工作空间相关类型 ==========
export interface WorkspaceInfo {
  id: string;
  name: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

export interface WorkspaceCreateRequest {
  userId: string;
  name: string;
  description?: string;
}

export interface WorkspaceUpdateRequest {
  name?: string;
  description?: string;
}

// ========== 对话任务相关类型 ==========
export interface ChatTaskInfo {
  id: string;
  title: string;
  description?: string;
  workspaceId: string;
  favorite: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ChatTaskCreateRequest {
  workspaceId: string;
  title: string;
  description?: string;
}

export interface ChatTaskUpdateRequest {
  title?: string;
  description?: string;
}

// ========== AI服务相关类型 ==========
export interface ChatTitleGenerateRequest {
  question: string;
  async?: boolean;
}

export interface ChatTitleGenerateResponse {
  title: string;
  async: boolean;
  duration?: number;
}

// ========== 支付相关类型 ==========
export interface PaymentCreateRequest {
  amount: number;
  billingCycle: string;
  paymentMethod: string;
}

export interface PaymentInfo {
  id: string;
  amount: number;
  status: string;
  paymentMethod: string;
  createdAt: string;
}

// ========== 订阅相关类型 ==========
export interface SubscriptionInfo {
  id: string;
  planName: string;
  status: string;
  startDate: string;
  endDate: string;
  autoRenew: boolean;
}

export interface SubscriptionCreateRequest {
  planId: string;
  billingCycle: string;
  paymentMethod: string;
}

// ========== 用户订阅响应类型 ==========
export interface UserSubscriptionResponse {
  subscription: SubscriptionInfo | null;
  planName: string;
  planCode: string;
}

// ========== 积分相关类型 ==========
export interface CreditBalance {
  availableCredits: number;
  totalCredits: number;
  usedCredits: number;
}

// ========== 用户积分详情响应类型 ==========
export interface UserCreditDetailsResponse {
  totalBalance: string; // 总积分
  dailyRefreshCredits: string; // 当日刷新积分
  permanentCredits: string; // 永久积分
  limitedCredits: string; // 限时积分
  hasCreditAccount: boolean; // 是否有积分账户
}

export interface CreditTransaction {
  id: string;
  type: 'CONSUME' | 'RECHARGE' | 'ADJUST';
  amount: number;
  description: string;
  createdAt: string;
}

export interface CreditConsumeRequest {
  amount: number;
  description: string;
}

export interface CreditRechargeRequest {
  amount: number;
  paymentMethod: string;
}

// ========== 积分购买配置相关类型 ==========
export interface CreditPurchaseConfig {
  planName: string;
  planCode: string;
  creditValidityDays: number; // 积分有效期天数，0表示永久有效
  availableCredits: string; // 可用积分总数
  dailyRefreshCredits: string; // 每日刷新积分
  limitedCredits: string; // 限时积分
  creditPackages: CreditPackageConfig[];
}

export interface CreditPackageConfig {
  id: string;
  name: string;
  code: string;
  creditsAmount: number;
  price: number;
  isRecommended: boolean;
  features: FeatureConfig[];
}

export interface FeatureConfig {
  text: string;
  highlight: boolean;
}

// ========== 资源使用相关类型 ==========
export interface ResourceUsageReport {
  userId: string;
  resourceType: string;
  amount: number;
  description?: string;
  timestamp: string;
}

export interface ResourceUsageStatistics {
  totalUsage: number;
  dailyUsage: number;
  weeklyUsage: number;
  monthlyUsage: number;
}

// ========== 错误处理相关类型 ==========
export interface ApiError {
  code: number;
  message: string;
  details?: string;
  timestamp: string;
  path: string;
}

// ========== HTTP方法类型 ==========
export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';

// ========== API服务接口定义 ==========
export interface ApiServiceInterface {
  get<T = any>(url: string, config?: ApiRequestConfig): ApiPromise<T>;
  post<T = any>(url: string, data?: any, config?: ApiRequestConfig): ApiPromise<T>;
  put<T = any>(url: string, data?: any, config?: ApiRequestConfig): ApiPromise<T>;
  delete<T = any>(url: string, config?: ApiRequestConfig): ApiPromise<T>;
  patch<T = any>(url: string, data?: any, config?: ApiRequestConfig): ApiPromise<T>;
}
