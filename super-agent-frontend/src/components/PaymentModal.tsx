import React, { useState, useEffect, useCallback, useRef } from 'react';
import {
  Modal,
  Button,
  message,
  Spin,
  QRCode,
  Input
} from 'antd';
import {
  WechatOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined
} from '@ant-design/icons';
import { PaymentSSEListener } from '../utils/paymentSSE';
import { userApi, paymentApi } from '../services/api';

interface PaymentModalProps {
  visible: boolean;
  onClose: () => void;
  planId: string;
  planName: string;
  amount: number;
  billingCycle: 'monthly' | 'yearly';
  onSuccess?: () => void;
  currentSubscription?: any; // 当前订阅状态
}

interface PaymentData {
  orderId: string;
  orderNo: string;
  amount: number;
  paymentMethod: string;
  status: string;
  qrCode?: string;
  paymentUrl?: string;
  thirdPartyOrderNo?: string;
  expiredAt: string;
}

// 支付状态类型定义
type PaymentStatusType = 'waiting' | 'paid' | 'failed' | 'expired' | 'cancelled' | 'pending';

const PaymentModal: React.FC<PaymentModalProps> = ({
  visible,
  onClose,
  planId,
  planName,
  amount,
  billingCycle,
  onSuccess,
  currentSubscription
}) => {
  const [loading, setLoading] = useState(false);
  const [paymentData, setPaymentData] = useState<PaymentData | null>(null);
  const [paymentStatus, setPaymentStatus] = useState<PaymentStatusType>('waiting');
  const [countdown, setCountdown] = useState(0);
  const [countdownTimer, setCountdownTimer] = useState<NodeJS.Timeout | null>(null);
  const [sseListener, setSseListener] = useState<PaymentSSEListener | null>(null);
  const sseListenerRef = useRef<PaymentSSEListener | null>(null);
  const [userId, setUserId] = useState<string | null>(null);

  // 根据订阅数据获取当前状态信息
  const getCurrentSubscriptionInfo = () => {
    if (!currentSubscription) {
      return {
        statusText: '免费版用户',
        validUntil: '暂无有效期'
      };
    }

    // 根据planId判断套餐类型
    let planTypeName = '免费版';
    if (currentSubscription.planId === '2') {
      planTypeName = '基础版';
    } else if (currentSubscription.planId === '3') {
      planTypeName = '高级版';
    }

    // 根据status判断状态
    const isActive = currentSubscription.status === 1;
    const statusText = isActive ? `${planTypeName}会员` : '免费版用户';
    
    // 格式化有效期
    let validUntil = '暂无有效期';
    if (isActive && currentSubscription.endTime) {
      const endDate = new Date(currentSubscription.endTime);
      validUntil = `会员有效期到${endDate.toLocaleDateString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      })}`;
    }

    return { statusText, validUntil };
  };

  // 获取用户信息
  useEffect(() => {
    const loadUserInfo = async () => {
      try {
        const userResponse = await userApi.getCurrentUser();
        const user = userResponse.data.data;
        setUserId(user.userId);
      } catch (error) {
        console.error('获取用户信息失败:', error);
        message.error('获取用户信息失败，请重新登录');
      }
    };
    
    if (visible) {
      loadUserInfo();
    }
  }, [visible]);

  // 清理SSE连接和定时器 - 仅在组件卸载时清理
  useEffect(() => {
    return () => {
      if (sseListenerRef.current) {
        sseListenerRef.current.stopListening();
      }
      if (countdownTimer) {
        clearInterval(countdownTimer);
      }
    };
  }, []); // 移除依赖项，仅在组件卸载时执行清理

  // 开始SSE监听支付状态
  const startPaymentSSEListener = useCallback((orderNo: string) => {
    // 先停止旧的SSE连接
    if (sseListenerRef.current) {
      sseListenerRef.current.stopListening();
    }
    
    const listener = new PaymentSSEListener(orderNo);
    
    listener.startListening({
      onConnection: () => {
        // 连接建立成功
      },
      
      onPaymentStatus: (event) => {
        setPaymentStatus(event.status as PaymentStatusType);
        
        switch (event.status) {
          case 'paid':
            message.destroy();
            message.success('支付成功！');
            onSuccess?.();
            setTimeout(() => {
              onClose();
            }, 2000);
            break;
          case 'failed':
            message.destroy();
            message.error('支付失败，请重试');
            break;
          case 'expired':
            message.destroy();
            message.warning('支付超时，请重新发起支付');
            break;
          case 'cancelled':
            message.destroy();
            message.warning('支付已取消');
            break;
        }
      },
      
      onError: () => {
        message.error('连接异常，请刷新页面重试');
      },
      
      onClose: () => {
        // 连接关闭
      }
    });
    
    setSseListener(listener);
    sseListenerRef.current = listener;
  }, [onSuccess, onClose]);

  // 开始倒计时
  const startCountdown = useCallback((expiredAt: string) => {
    // 清理现有定时器
    if (countdownTimer) {
      clearInterval(countdownTimer);
    }
    
    const expiredTime = new Date(expiredAt).getTime();
    
    const interval = setInterval(() => {
      const now = new Date().getTime();
      const timeLeft = expiredTime - now;
      
      if (timeLeft <= 0) {
        setCountdown(0);
        setPaymentStatus('expired');
        clearInterval(interval);
        setCountdownTimer(null);
      } else {
        setCountdown(Math.floor(timeLeft / 1000));
      }
    }, 1000);
    
    setCountdownTimer(interval);
  }, [countdownTimer]);

  // 创建订单并发起支付
  const handleCreatePayment = useCallback(async () => {
    if (!userId) {
      message.error('用户信息未加载，请稍后重试');
      return;
    }
    
    try {
      setLoading(true);
      
      const response = await paymentApi.createOrder(userId, {
        planId: parseInt(planId),
        billingCycle,
        paymentMethod: 'wechat'
      });

      const result = response.data;
      
      if (result.success) {
        setPaymentData(result.data);
        startPaymentSSEListener(result.data.orderNo);
        startCountdown(result.data.expiredAt);
      } else {
        message.error(result.message || '创建支付失败');
      }
    } catch (error: any) {
      console.error('创建支付失败:', error);
      message.error('创建支付失败：' + (error.message || '请重试'));
    } finally {
      setLoading(false);
    }
  }, [userId, planId, billingCycle, startPaymentSSEListener, startCountdown]);

  // 格式化倒计时
  const formatCountdown = (seconds: number) => {
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}`;
  };

  // 重置状态
  const resetState = () => {
    setPaymentData(null);
    setPaymentStatus('waiting');
    setCountdown(0);
    setUserId(null);
    
    // 清理SSE连接
    if (sseListenerRef.current) {
      sseListenerRef.current.stopListening();
      setSseListener(null);
      sseListenerRef.current = null;
    }
    
    // 清理倒计时定时器
    if (countdownTimer) {
      clearInterval(countdownTimer);
      setCountdownTimer(null);
    }
  };

  // 关闭弹窗
  const handleClose = () => {
    resetState();
    onClose();
  };

  // 自动创建支付
  useEffect(() => {
    if (visible && userId && !paymentData) {
      handleCreatePayment();
    }
  }, [visible, userId, paymentData, handleCreatePayment]);

  // 获取价格显示
  const getPriceDisplay = () => `¥ ${amount}`;

  const getCycleDisplay = () => {
    if (planName.includes('积分')) {
      return '';
    }
    return billingCycle === 'monthly' ? '' : `${Math.round(amount / 12)}元/月`;
  };

  return (
    <Modal
      title="开通订阅"
      open={visible}
      onCancel={handleClose}
      footer={null}
      width={640}
      centered
      destroyOnClose
      styles={{
        content: { 
          background: '#6b6b6b',
          color: '#fff',
          borderRadius: '12px'
        },
        header: {
          background: '#6b6b6b',
          borderBottom: '1px solid #8b8b8b'
        }
      }}
    >
      <div style={{ padding: '20px 0', background: '#6b6b6b', color: '#fff' }}>
        {loading && !paymentData ? (
          <div style={{ textAlign: 'center', padding: '60px 0' }}>
            <Spin size="large" />
            <div style={{ marginTop: '16px', color: '#ccc' }}>正在创建支付...</div>
          </div>
        ) : (
          <div style={{ display: 'flex', gap: '40px' }}>
            {/* 左侧：订单信息 */}
            <div style={{ flex: 1 }}>
              <div style={{ marginBottom: '16px' }}>
                <div style={{ fontSize: '14px', color: '#ccc', marginBottom: '4px' }}>
                  当前状态：{getCurrentSubscriptionInfo().statusText}
                </div>
                <div style={{ fontSize: '14px', color: '#ccc' }}>
                  开通时长：{getCurrentSubscriptionInfo().validUntil}
                </div>
              </div>

              <div style={{
                background: 'rgba(255,255,255,0.1)',
                borderRadius: '8px',
                padding: '20px',
                border: '1px solid rgba(255,255,255,0.2)',
                position: 'relative'
              }}>
                {billingCycle === 'yearly' && (
                  <div style={{
                    position: 'absolute',
                    top: '-8px',
                    left: '16px',
                    background: '#666',
                    color: '#fff',
                    padding: '4px 12px',
                    borderRadius: '4px',
                    fontSize: '12px'
                  }}>
                    按年
                  </div>
                )}
                
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: '18px', fontWeight: 600, marginBottom: '12px' }}>
                    {planName}
                  </div>
                  
                  <div style={{ fontSize: '32px', fontWeight: 600, marginBottom: '8px' }}>
                    {getPriceDisplay()}
                  </div>
                  
                  {getCycleDisplay() && (
                    <div style={{ fontSize: '14px', color: '#ccc' }}>
                      {getCycleDisplay()}
                    </div>
                  )}
                </div>
              </div>

              {/* 支付状态显示 */}
              {paymentStatus === 'paid' && (
                <div style={{ textAlign: 'center', marginTop: '20px' }}>
                  <CheckCircleOutlined style={{ fontSize: '48px', color: '#52c41a', marginBottom: '16px' }} />
                  <div style={{ fontSize: '18px', fontWeight: 600, color: '#52c41a' }}>
                    支付成功！
                  </div>
                </div>
              )}


              {(paymentStatus === 'expired' || paymentStatus === 'failed' || paymentStatus === 'cancelled') && (
                <div style={{ textAlign: 'center', marginTop: '20px' }}>
                  <CloseCircleOutlined style={{ fontSize: '48px', color: '#ff4d4f', marginBottom: '16px' }} />
                  <div style={{ fontSize: '18px', fontWeight: 600, color: '#ff4d4f' }}>
                    {paymentStatus === 'expired' && '支付已过期'}
                    {paymentStatus === 'failed' && '支付失败'}
                    {paymentStatus === 'cancelled' && '支付已取消'}
                  </div>
                  <Button
                    type="primary"
                    style={{ marginTop: '16px' }}
                    onClick={() => {
                      resetState();
                      handleCreatePayment();
                    }}
                  >
                    重新支付
                  </Button>
                </div>
              )}
            </div>

            {/* 右侧：微信支付二维码 */}
            <div style={{ width: '200px', textAlign: 'center' }}>
              <div style={{ marginBottom: '16px' }}>
                <div style={{ fontSize: '16px', fontWeight: 600, marginBottom: '8px' }}>
                  微信扫码支付
                </div>
                {(paymentStatus === 'waiting' || paymentStatus === 'pending') && countdown > 0 && (
                  <div style={{ fontSize: '12px', color: '#ccc' }}>
                    剩余时间: {formatCountdown(countdown)}
                  </div>
                )}
              </div>

              {paymentData?.qrCode && (paymentStatus === 'waiting' || paymentStatus === 'pending') ? (
                <div style={{
                  background: '#fff',
                  padding: '16px',
                  borderRadius: '8px',
                  marginBottom: '16px'
                }}>
                  <QRCode value={paymentData.qrCode} size={168} />
                </div>
              ) : (
                <div style={{
                  width: '200px',
                  height: '200px',
                  background: 'rgba(255,255,255,0.1)',
                  borderRadius: '8px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  marginBottom: '16px'
                }}>
                  <WechatOutlined style={{ fontSize: '48px', color: '#07c160' }} />
                </div>
              )}

              <div style={{ fontSize: '18px', fontWeight: 600 }}>
                ¥ {amount}
              </div>
            </div>
          </div>
        )}


        <div style={{ 
          textAlign: 'center', 
          marginTop: '30px', 
          paddingTop: '20px', 
          borderTop: '1px solid #8b8b8b',
          fontSize: '12px',
          color: '#ccc'
        }}>
          支付即视为你同意《Super Agent会员协议》
        </div>
      </div>
    </Modal>
  );
};

export default PaymentModal;