// 用户信息类型定义
export interface UserInfo {
  id: string;
  phone?: string;
  username?: string;
  status: number;
  statusDesc?: string;
}

// 用户积分信息类型定义
export interface UserCredit {
  userId: string;
  totalBalance: number;
  freeBalance: number;
  subscriptionBalance: number;
  permanentBalance: number;
  totalEarned: number;
  totalSpent: number;
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
  success: boolean;
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
