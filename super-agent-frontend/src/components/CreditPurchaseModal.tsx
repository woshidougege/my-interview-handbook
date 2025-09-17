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
import { userApi, paymentApi, subscriptionApi } from '../services/api';
import { CreditPurchaseConfig } from '../types/api';

interface CreditPurchaseModalProps {
  visible: boolean;
  onClose: () => void;
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

// 支付状态类型定义
type PaymentStatusType = 'waiting' | 'paid' | 'failed' | 'expired' | 'cancelled' | 'pending';

const CreditPurchaseModal: React.FC<CreditPurchaseModalProps> = ({
  visible,
  onClose,
  onSuccess
}) => {
  const [loading, setLoading] = useState(false);
  const [configLoading, setConfigLoading] = useState(false);
  const [config, setConfig] = useState<CreditPurchaseConfig | null>(null);
  const [selectedPackage, setSelectedPackage] = useState<string | null>(null);
  const [paymentData, setPaymentData] = useState<PaymentData | null>(null);
  const [paymentStatus, setPaymentStatus] = useState<PaymentStatusType>('waiting');
  const [countdown, setCountdown] = useState(0);
  const [countdownTimer, setCountdownTimer] = useState<NodeJS.Timeout | null>(null);
  const [sseListener, setSseListener] = useState<PaymentSSEListener | null>(null);
  const sseListenerRef = useRef<PaymentSSEListener | null>(null);
  const [userId, setUserId] = useState<string | null>(null);

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

  // 获取积分购买配置
  useEffect(() => {
    const loadConfig = async () => {
      if (!visible) return;
      
      setConfigLoading(true);
      try {
        const response = await subscriptionApi.getCreditPurchaseConfig();
        setConfig(response.data.data);
        
        // 默认选择第一个套餐
        if (response.data.data.creditPackages.length > 0) {
          setSelectedPackage(response.data.data.creditPackages[0].id);
        }
      } catch (error) {
        console.error('获取积分购买配置失败:', error);
        message.error('获取配置失败，请重试');
      } finally {
        setConfigLoading(false);
      }
    };

    loadConfig();
  }, [visible]);

  // 清理SSE连接和定时器
  useEffect(() => {
    return () => {
      if (sseListenerRef.current) {
        sseListenerRef.current.stopListening();
      }
      if (countdownTimer) {
        clearInterval(countdownTimer);
      }
    };
  }, []);

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
    if (!selectedPackage) {
      message.error('请选择积分套餐');
      return;
    }

    try {
      setLoading(true);
      
      const response = await paymentApi.createOrder({
        planId: 100, // 固定使用购买积分套餐ID
        billingCycle: 'monthly', // 积分包使用月计费
        paymentMethod: 'wechat',
        creditPackageId: selectedPackage // 传入选择的积分包ID
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
  }, [selectedPackage, startPaymentSSEListener, startCountdown]);

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
    setSelectedPackage(null);
    
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

  // 获取选中套餐的信息
  const getSelectedPackageInfo = () => {
    if (!config || !selectedPackage) return null;
    return config.creditPackages.find(pkg => pkg.id === selectedPackage);
  };

  const selectedPackageInfo = getSelectedPackageInfo();

  return (
    <Modal
      title="购买积分"
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
        {configLoading || (loading && !paymentData) ? (
          <div style={{ textAlign: 'center', padding: '60px 0' }}>
            <Spin size="large" />
            <div style={{ marginTop: '16px', color: '#ccc' }}>
              {configLoading ? '正在加载配置...' : '正在创建支付...'}
            </div>
          </div>
        ) : (
          <div style={{ display: 'flex', gap: '40px' }}>
            {/* 左侧：配置信息 */}
            <div style={{ flex: 1 }}>
              <div style={{ marginBottom: '16px' }}>
                <div style={{ fontSize: '14px', color: '#ccc', marginBottom: '4px' }}>
                  当前状态：{config?.currentPlan || '免费版用户'}
                </div>
                <div style={{ fontSize: '14px', color: '#ccc' }}>
                  积分有效期：永久有效
                </div>
              </div>

              {/* 套餐选择 */}
              {!paymentData && config && (
                <div style={{ marginBottom: '20px' }}>
                  <div style={{ fontSize: '16px', fontWeight: 600, marginBottom: '12px' }}>
                    选择积分套餐：
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    {config.creditPackages.map((pkg) => (
                      <div
                        key={pkg.id}
                        style={{
                          background: selectedPackage === pkg.id ? 'rgba(255,255,255,0.2)' : 'rgba(255,255,255,0.1)',
                          border: selectedPackage === pkg.id ? '2px solid #1890ff' : '1px solid rgba(255,255,255,0.2)',
                          borderRadius: '8px',
                          padding: '16px',
                          cursor: 'pointer',
                          position: 'relative'
                        }}
                        onClick={() => setSelectedPackage(pkg.id)}
                      >
                        {pkg.isRecommended && (
                          <div style={{
                            position: 'absolute',
                            top: '-8px',
                            left: '16px',
                            background: '#ff6b35',
                            color: '#fff',
                            padding: '4px 12px',
                            borderRadius: '4px',
                            fontSize: '12px'
                          }}>
                            推荐
                          </div>
                        )}
                        
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <div>
                            <div style={{ fontSize: '18px', fontWeight: 600 }}>
                              {pkg.name}
                            </div>
                            <div style={{ fontSize: '14px', color: '#ccc', marginTop: '4px' }}>
                              {pkg.creditsAmount.toLocaleString()}积分
                            </div>
                          </div>
                          <div style={{ fontSize: '24px', fontWeight: 600 }}>
                            ¥{pkg.price}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* 选中套餐显示 */}
              {paymentData && selectedPackageInfo && (
                <div style={{
                  background: 'rgba(255,255,255,0.1)',
                  borderRadius: '8px',
                  padding: '20px',
                  border: '1px solid rgba(255,255,255,0.2)',
                }}>
                  <div style={{ textAlign: 'center' }}>
                    <div style={{ fontSize: '18px', fontWeight: 600, marginBottom: '12px' }}>
                      {selectedPackageInfo.name}
                    </div>
                    
                    <div style={{ fontSize: '32px', fontWeight: 600, marginBottom: '8px' }}>
                      ¥{selectedPackageInfo.price}
                    </div>
                    
                    <div style={{ fontSize: '14px', color: '#ccc' }}>
                      {selectedPackageInfo.creditsAmount.toLocaleString()}积分
                    </div>
                  </div>
                </div>
              )}

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
                      // 重新选择第一个套餐
                      if (config && config.creditPackages.length > 0) {
                        setSelectedPackage(config.creditPackages[0].id);
                      }
                    }}
                  >
                    重新购买
                  </Button>
                </div>
              )}

              {/* 购买按钮 */}
              {!paymentData && selectedPackage && (
                <div style={{ marginTop: '20px' }}>
                  <Button
                    type="primary"
                    size="large"
                    block
                    onClick={handleCreatePayment}
                    loading={loading}
                  >
                    立即购买
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

              {selectedPackageInfo && (
                <div style={{ fontSize: '18px', fontWeight: 600 }}>
                  ¥ {selectedPackageInfo.price}
                </div>
              )}
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

export default CreditPurchaseModal;
