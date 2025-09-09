import React, { useState, useEffect } from 'react';
import {
  Modal,
  Button,
  Radio,
  message,
  Spin,
  QRCode,
  Divider,
  Alert
} from 'antd';
import {
  WechatOutlined,
  AlipayOutlined,
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
  const [paymentMethod, setPaymentMethod] = useState<'wechat' | 'alipay'>('wechat');
  const [loading, setLoading] = useState(false);
  const [paymentData, setPaymentData] = useState<PaymentData | null>(null);
  const [paymentStatus, setPaymentStatus] = useState<'pending' | 'success' | 'failed' | 'expired'>('pending');
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
          paymentMethod
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

  return (
    <Modal
      title="订阅支付"
      open={visible}
      onCancel={handleClose}
      footer={null}
      width={500}
      centered
      destroyOnClose
    >
      <div style={{ padding: '20px 0' }}>
        {/* 订单信息 */}
        <div style={{ marginBottom: '24px' }}>
          <div style={{ 
            background: '#f8f9fa', 
            padding: '16px', 
            borderRadius: '8px',
            marginBottom: '16px'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
              <span>套餐：</span>
              <span style={{ fontWeight: 600 }}>{planName}</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
              <span>计费周期：</span>
              <span>{billingCycle === 'monthly' ? '按月' : '按年'}</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span>金额：</span>
              <span style={{ fontSize: '18px', fontWeight: 600, color: '#ff4d4f' }}>
                ¥{amount}
              </span>
            </div>
          </div>
        </div>

        {!paymentData ? (
          // 支付方式选择
          <div>
            <div style={{ marginBottom: '16px', fontSize: '16px', fontWeight: 600 }}>
              选择支付方式
            </div>
            <Radio.Group 
              value={paymentMethod} 
              onChange={(e) => setPaymentMethod(e.target.value)}
              style={{ width: '100%' }}
            >
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <Radio value="wechat">
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <WechatOutlined style={{ color: '#07c160', fontSize: '20px' }} />
                    <span>微信支付</span>
                  </div>
                </Radio>
                <Radio value="alipay">
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <AlipayOutlined style={{ color: '#1677ff', fontSize: '20px' }} />
                    <span>支付宝</span>
                  </div>
                </Radio>
              </div>
            </Radio.Group>

            <Divider />

            <Button
              type="primary"
              block
              size="large"
              loading={loading}
              onClick={handleCreatePayment}
              style={{ height: '48px', fontSize: '16px', fontWeight: 600 }}
            >
              立即支付
            </Button>
          </div>
        ) : (
          // 支付二维码
          <div style={{ textAlign: 'center' }}>
            {paymentStatus === 'pending' && (
              <>
                <div style={{ marginBottom: '16px' }}>
                  <div style={{ fontSize: '16px', fontWeight: 600, marginBottom: '8px' }}>
                    请使用{paymentMethod === 'wechat' ? '微信' : '支付宝'}扫码支付
                  </div>
                  <div style={{ color: '#666' }}>
                    订单将在 <span style={{ color: '#ff4d4f', fontWeight: 600 }}>
                      {formatCountdown(countdown)}
                    </span> 后过期
                  </div>
                </div>

                {paymentData.qrCode && (
                  <div style={{ 
                    display: 'flex', 
                    justifyContent: 'center', 
                    marginBottom: '16px',
                    padding: '20px',
                    background: '#fff',
                    border: '1px solid #f0f0f0',
                    borderRadius: '8px'
                  }}>
                    <QRCode value={paymentData.qrCode} size={200} />
                  </div>
                )}

                <Alert
                  message="请在新页面完成支付，支付完成前请不要关闭此页面"
                  type="info"
                  showIcon
                />
              </>
            )}

            {paymentStatus === 'paid' && (
              <div style={{ padding: '40px 0' }}>
                <CheckCircleOutlined style={{ fontSize: '48px', color: '#52c41a', marginBottom: '16px' }} />
                <div style={{ fontSize: '18px', fontWeight: 600, color: '#52c41a' }}>
                  支付成功！
                </div>
                <div style={{ color: '#666', marginTop: '8px' }}>
                  页面将自动关闭...
                </div>
              </div>
            )}

            {(paymentStatus === 'expired' || paymentStatus === 'failed') && (
              <div style={{ padding: '40px 0' }}>
                <CloseCircleOutlined style={{ fontSize: '48px', color: '#ff4d4f', marginBottom: '16px' }} />
                <div style={{ fontSize: '18px', fontWeight: 600, color: '#ff4d4f' }}>
                  {paymentStatus === 'expired' ? '支付已过期' : '支付失败'}
                </div>
                <Button
                  type="primary"
                  style={{ marginTop: '16px' }}
                  onClick={() => {
                    resetState();
                  }}
                >
                  重新支付
                </Button>
              </div>
            )}
          </div>
        )}
      </div>
    </Modal>
  );
};

export default PaymentModal;
