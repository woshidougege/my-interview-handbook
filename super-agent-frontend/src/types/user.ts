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

// 套餐功能特性类型定义
export interface PlanFeature {
  text: string;
  highlight: boolean;
  included: boolean;
}

// 订阅套餐类型定义
export interface SubscriptionPlan {
  id: string;
  planName: string;
  description: string;
  features: PlanFeature[];
  price: number;
  monthlyPrice: number;
  yearlyPrice: number;
  creditAmount: number;
  monthlyCreditAmount: number;
  yearlyCreditAmount: number;
  dailyRefreshCredit: number;
  validityDays: number;
  enabled: boolean;
  isRecommended: boolean;
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

// 支付状态类型定义
export type PaymentStatusType = 
  | 'waiting' 
  | 'pending' 
  | 'paid' 
  | 'failed' 
  | 'cancelled' 
  | 'expired'
  | 'refund_processing'
  | 'refund_success'
  | 'refund_fail'
  | 'refund_closed'
  | 'refund_abnormal';

// 订单记录类型定义
export interface OrderRecord {
  orderNo: string;
  status: PaymentStatusType;
  amount: number;
  paymentMethod: string;
  createdAt: string;
  expiredAt: string;
  message: string;
}

// 退款请求类型定义
export interface RefundRequest {
  orderNo: string;
  refundAmount: number;
  refundReason?: string;
}

// 退款响应类型定义
export interface RefundResponse {
  orderNo: string;
  refundNo: string;
  refundId?: string;
  refundStatus: string;
  refundAmount: number;
  totalAmount: number;
  refundReason?: string;
  refundTime?: string;
  successTime?: string;
  message: string;
}

// 支付状态事件类型定义
export interface PaymentStatusEvent {
  orderNo: string;
  status: PaymentStatusType;
  amount?: number;
  paymentMethod?: string;
  message: string;
  eventTime: string;
  extra?: any;
}