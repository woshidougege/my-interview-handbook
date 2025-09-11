import React, { useState, useEffect } from 'react';
import {
  Modal,
  Button,
  message,
  Spin,
  QRCode
} from 'antd';
import {
  WechatOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined
} from '@ant-design/icons';

interface PaymentModalProps {
  visible: boolean;
  onClose: () => void;
  planId: string;
  planName: string;
  amount: number;
  billingCycle: 'monthly' | 'yearly';
  onSuccess?: () => void;
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

const PaymentModal: React.FC<PaymentModalProps> = ({
  visible,
  onClose,
  planId,
  planName,
  amount,
  billingCycle,
  onSuccess
}) => {
  const [loading, setLoading] = useState(false);
  const [paymentData, setPaymentData] = useState<PaymentData | null>(null);
  const [paymentStatus, setPaymentStatus] = useState<'pending' | 'paid' | 'failed' | 'expired'>('pending');
  const [countdown, setCountdown] = useState(0);
  const [polling, setPolling] = useState<NodeJS.Timeout | null>(null);

  // 清理定时器
  useEffect(() => {
    return () => {
      if (polling) {
        clearInterval(polling);
      }
    };
  }, [polling]);

  // 创建订单并发起支付
  const handleCreatePayment = async () => {
    try {
      setLoading(true);
      
      const response = await fetch('/api/payment/create?userId=322604385681035264', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          planId: parseInt(planId),
          billingCycle,
          paymentMethod: 'wechat'
        }),
      });

      const result = await response.json();
      
      if (result.success) {
        setPaymentData(result.data);
        startPaymentPolling(result.data.orderNo);
        startCountdown(result.data.expiredAt);
      } else {
        message.error(result.message || '创建支付失败');
      }
    } catch (error) {
      console.error('创建支付失败:', error);
      message.error('创建支付失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  // 开始轮询支付状态
  const startPaymentPolling = (orderNo: string) => {
    const interval = setInterval(async () => {
      try {
        const response = await fetch(`/api/payment/status/${orderNo}`);
        const result = await response.json();
        
        if (result.success) {
          const status = result.data.status;
          setPaymentStatus(status);
          
          if (status === 'paid') {
            clearInterval(interval);
            setPolling(null);
            message.success('支付成功！');
            onSuccess?.();
            setTimeout(() => {
              onClose();
            }, 2000);
          } else if (status === 'expired' || status === 'cancelled') {
            clearInterval(interval);
            setPolling(null);
            setPaymentStatus(status);
          }
        }
      } catch (error) {
        console.error('查询支付状态失败:', error);
      }
    }, 3000); // 每3秒查询一次
    
    setPolling(interval);
  };

  // 开始倒计时
  const startCountdown = (expiredAt: string) => {
    const expiredTime = new Date(expiredAt).getTime();
    
    const countdownInterval = setInterval(() => {
      const now = new Date().getTime();
      const timeLeft = expiredTime - now;
      
      if (timeLeft <= 0) {
        setCountdown(0);
        setPaymentStatus('expired');
        clearInterval(countdownInterval);
      } else {
        setCountdown(Math.floor(timeLeft / 1000));
      }
    }, 1000);
  };

  // 格式化倒计时
  const formatCountdown = (seconds: number) => {
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}`;
  };

  // 重置状态
  const resetState = () => {
    setPaymentData(null);
    setPaymentStatus('pending');
    setCountdown(0);
    if (polling) {
      clearInterval(polling);
      setPolling(null);
    }
  };

  // 关闭弹窗
  const handleClose = () => {
    resetState();
    onClose();
  };

  // 自动创建支付
  useEffect(() => {
    if (visible && !paymentData) {
      handleCreatePayment();
    }
  }, [visible]);

  // 获取价格显示
  const getPriceDisplay = () => {
    if (planName.includes('积分')) {
      return `¥ ${amount}`;
    }
    
    if (billingCycle === 'yearly') {
      const monthlyPrice = Math.round(amount / 12);
      return `¥ ${amount}`;
    }
    
    return `¥ ${amount}`;
  };

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
                  当前状态：免费版用户
                </div>
                <div style={{ fontSize: '14px', color: '#ccc' }}>
                  开通时长：会员有效期到2026-09-02 17:09:25
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

              {/* 支付状态 */}
              {paymentStatus === 'success' && (
                <div style={{ textAlign: 'center', marginTop: '20px' }}>
                  <CheckCircleOutlined style={{ fontSize: '48px', color: '#52c41a', marginBottom: '16px' }} />
                  <div style={{ fontSize: '18px', fontWeight: 600, color: '#52c41a' }}>
                    支付成功！
                  </div>
                </div>
              )}

              {(paymentStatus === 'expired' || paymentStatus === 'failed') && (
                <div style={{ textAlign: 'center', marginTop: '20px' }}>
                  <CloseCircleOutlined style={{ fontSize: '48px', color: '#ff4d4f', marginBottom: '16px' }} />
                  <div style={{ fontSize: '18px', fontWeight: 600, color: '#ff4d4f' }}>
                    {paymentStatus === 'expired' ? '支付已过期' : '支付失败'}
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
                {paymentStatus === 'pending' && countdown > 0 && (
                  <div style={{ fontSize: '12px', color: '#ccc' }}>
                    {formatCountdown(countdown)}
                  </div>
                )}
              </div>

              {paymentData?.qrCode && paymentStatus === 'pending' ? (
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