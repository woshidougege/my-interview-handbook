// 用户信息类型定义
export interface UserInfo {
  id: string;
  username: string;
  email?: string;
  phone?: string;
  nickname?: string;
  avatar?: string;
  status: number;
  createTime: string;
  updateTime: string;
}

// 用户积分信息类型定义
export interface UserCredit {
  id: string;
  userId: string;
  totalCredits: number;
  availableCredits: number;
  frozenCredits: number;
  expiredCredits: number;
  createTime: string;
  updateTime: string;
}

// 积分交易记录类型定义
export interface CreditTransaction {
  id: string;
  userId: string;
  transactionType: number;
  transactionTypeDesc: string;
  amount: number;
  balanceAfter: number;
  description: string;
  relatedId?: string;
  createTime: string;
}

// 订阅套餐类型定义
export interface SubscriptionPlan {
  id: string;
  planName: string;
  description: string;
  price: number;
  creditAmount: number;
  validityDays: number;
  enabled: number;
  sortOrder: number;
}

// 用户订阅信息类型定义
export interface UserSubscription {
  id: string;
  userId: string;
  planId: string;
  planName: string;
  status: number;
  startTime: string;
  endTime: string;
  createTime: string;
}

// API响应类型定义
export interface ApiResponse<T = any> {
  code: number;
  message: string;
  data: T;
  timestamp: string;
}

// 分页响应类型定义
export interface PageResponse<T = any> {
  records: T[];
  total: number;
  current: number;
  size: number;
  pages: number;
}
