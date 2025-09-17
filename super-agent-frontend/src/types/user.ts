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
  // 新增字段
  availableCredits?: number;
  hasCreditAccount?: boolean;
  planName?: string;
  planCode?: string;
  limitedCredits?: number;
  dailyRefreshCredits?: number;
  permanentCredits?: number;
}

// 积分交易记录类型定义
export interface CreditTransaction {
  id: string;
  userId: string;
  transactionType: number;
  transactionTypeDesc: string;
  creditType: string;
  creditTypeDesc: string;
  amount: number;
  balanceBefore: number;
  balanceAfter: number;
  description: string;
  relatedOrderId?: string;
  relatedSubscriptionId?: string;
  expireTime?: string;
  createTime: string;
  changeType: string; // "+" 表示收入，"-" 表示支出
}

// 套餐功能特性类型定义
export interface PlanFeature {
  text: string;
  highlight: boolean;
}

// 订阅套餐类型定义
export interface SubscriptionPlan {
  id: string;
  planName: string;
  planCode: string;
  description: string;
  features: PlanFeature[];
  // 完整的6个价格字段（命名一致）
  monthlyOriginalPrice: number;    // 月价原价（未优惠）
  monthlyPrice: number;            // 月价最终价格（优惠后）
  monthlySavings: number;          // 月价优惠金额
  yearlyOriginalPrice: number;     // 年价原价（月价*12，未优惠）
  yearlyPrice: number;             // 年价最终价格（优惠后）
  yearlySavings: number;           // 年价优惠金额
  creditsAmount?: number;          // 积分数量（积分套餐专用）
  validityDays: number;
  enabled: boolean;
  isRecommended: boolean;
  isCurrentPlan: boolean;          // 是否为用户当前套餐
  isSubscribable: boolean;         // 是否可订阅（用于按钮状态控制）
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