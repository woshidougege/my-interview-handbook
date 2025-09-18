import React, { useState, useEffect, useCallback, useRef } from 'react';
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
  const [currentCredits, setCurrentCredits] = useState<number>(0);
  const [countdown, setCountdown] = useState(0);
  const [countdownTimer, setCountdownTimer] = useState<NodeJS.Timeout | null>(null);
  const sseListenerRef = useRef<PaymentSSEListener | null>(null);

  // 获取用户信息
  useEffect(() => {
    const loadUserInfo = async () => {
      try {
        const userResponse = await userApi.getCurrentUser();
        const user = userResponse.data.data;
        // 用户信息获取成功（如果需要userId可以在这里处理）
        console.log('用户信息:', user);
      } catch (error) {
        console.error('获取用户信息失败:', error);
        message.error('获取用户信息失败，请重新登录');
      }
    };
    
    if (visible) {
      loadUserInfo();
    }
  }, [visible]);

  // 获取积分购买配置和用户积分
  useEffect(() => {
    const loadConfig = async () => {
      if (!visible) return;
      
      setConfigLoading(true);
      try {
        // 获取积分购买配置（包含用户积分信息）
        const configResponse = await subscriptionApi.getCreditPurchaseConfig();
        
        setConfig(configResponse.data.data);
        
        // 默认选择推荐的套餐
        if (configResponse.data.data.creditPackages.length > 0) {
          const recommended = configResponse.data.data.creditPackages.find(pkg => pkg.isRecommended);
          const selectedId = recommended ? recommended.id : configResponse.data.data.creditPackages[0].id;
          setSelectedPackage(selectedId);
        }
        
        // 设置当前积分余额（从积分购买配置接口获取）
        setCurrentCredits(parseInt(configResponse.data.data.availableCredits || '0'));
        
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
  }, [countdownTimer]);

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
  const handleCreatePayment = useCallback(async (packageId?: string) => {
    const targetPackage = packageId || selectedPackage;
    
    if (!targetPackage) {
      message.error('请选择积分套餐');
      return;
    }

    try {
      setLoading(true);
      
      const response = await paymentApi.createOrder({
        planId: 100, // 固定使用购买积分套餐ID
        billingCycle: 'monthly', // 积分包使用月计费
        paymentMethod: 'wechat',
        creditPackageId: targetPackage // 传入选择的积分包ID
      });

      const result = response.data;
      
      if (result.success) {
        setPaymentData(result.data);
        startPaymentSSEListener(result.data.orderNo);
        startCountdown(result.data.expiredAt);
      } else {
        message.error(result.message || '创建支付失败');
      }
    } catch (error: unknown) {
      console.error('创建支付失败:', error);
      message.error('创建支付失败：' + (error instanceof Error ? error.message : '请重试'));
    } finally {
      setLoading(false);
    }
  }, [selectedPackage, startPaymentSSEListener, startCountdown]);

  // 自动创建订单 - 当积分包选择后立即创建订单
  useEffect(() => {
    if (selectedPackage && config && !paymentData && !loading && visible) {
      handleCreatePayment(selectedPackage);
    }
  }, [selectedPackage, config, paymentData, loading, visible, handleCreatePayment]);

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
      sseListenerRef.current = null;
    }
    
    // 清理倒计时定时器
    if (countdownTimer) {
      clearInterval(countdownTimer);
      setCountdownTimer(null);
    }
  };

  // 关闭弹窗
  const handleClose = async () => {
    // 如果有待支付的订单，主动取消
    if (paymentData && paymentData.orderNo && paymentStatus === 'waiting') {
      try {
        await paymentApi.cancelOrder(paymentData.orderNo);
        console.log('订单已自动取消:', paymentData.orderNo);
        // 静默取消，不显示提示信息，避免打扰用户体验
      } catch (error) {
        console.warn('取消订单失败:', error);
        // 不阻塞关闭操作，因为用户已经决定要关闭了
      }
    }
    
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
      width={700}
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
          <div>
            {/* 顶部信息 */}
            <div style={{ marginBottom: '20px' }}>
              <div style={{ fontSize: '16px', marginBottom: '8px' }}>
                剩余积分：{currentCredits}
              </div>
              <div style={{ fontSize: '14px', color: '#ccc' }}>
                注意：{config?.planName || '基础版'}用户每日可获得***积分，此购买会在其基础上进行叠加
              </div>
            </div>

            {/* 主要内容区域：左右分栏 */}
            <div style={{ display: 'flex', gap: '30px' }}>
              {/* 左侧：积分包选择和功能特性 */}
              <div style={{ flex: 1 }}>
                {/* 积分包选择 - 始终显示 */}
                {config && (
                  <div style={{ marginBottom: '20px' }}>
                    <div style={{ fontSize: '16px', fontWeight: 600, marginBottom: '12px' }}>
                      选择积分包
                    </div>
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '12px' }}>
                      {config.creditPackages.map((pkg) => (
                        <div
                          key={pkg.id}
                          style={{
                            background: selectedPackage === pkg.id ? 'rgba(255,255,255,0.15)' : 'rgba(255,255,255,0.08)',
                            border: selectedPackage === pkg.id ? '2px solid #4a90e2' : '1px solid rgba(255,255,255,0.2)',
                            borderRadius: '8px',
                            padding: '16px',
                            cursor: 'pointer',
                            textAlign: 'center',
                            position: 'relative',
                            transition: 'all 0.3s ease'
                          }}
                          onClick={() => {
                            // 清除当前支付数据，重新创建订单
                            setPaymentData(null);
                            setPaymentStatus('waiting');
                            setSelectedPackage(pkg.id);
                          }}
                        >
                          {pkg.isRecommended && (
                            <div style={{
                              position: 'absolute',
                              top: '-8px',
                              right: '-8px',
                              background: '#f5222d',
                              color: '#fff',
                              padding: '4px 8px',
                              borderRadius: '10px',
                              fontSize: '12px',
                              fontWeight: 600,
                              transform: 'rotate(15deg)'
                            }}>
                              推荐
                            </div>
                          )}
                          <div style={{ fontSize: '16px', fontWeight: 600, marginBottom: '8px' }}>
                            {pkg.name}
                          </div>
                          <div style={{ fontSize: '20px', fontWeight: 700, color: '#fff', marginBottom: '4px' }}>
                            ¥ {pkg.price}
                          </div>
                          <div style={{ fontSize: '12px', color: '#ccc' }}>
                            {pkg.creditsAmount.toLocaleString()}积分
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* 选中积分包的功能特性 */}
                {selectedPackageInfo && (
                  <div style={{ marginBottom: '20px' }}>
                    <div style={{ fontSize: '16px', fontWeight: 600, marginBottom: '12px' }}>
                      {selectedPackageInfo.name} 功能特性
                    </div>
                    <div style={{ padding: '12px', background: 'rgba(255,255,255,0.05)', borderRadius: '8px' }}>
                      <div style={{ fontSize: '14px', color: '#ccc', marginBottom: '8px' }}>
                        {selectedPackageInfo.description}
                      </div>
                      <div style={{ fontSize: '12px', color: '#999', marginBottom: '12px' }}>
                        {selectedPackageInfo.creditsAmount.toLocaleString()}积分（永久有效）
                      </div>
                      {selectedPackageInfo.features && selectedPackageInfo.features.map((feature, index) => (
                        <div key={index} style={{ 
                          display: 'flex', 
                          alignItems: 'center', 
                          marginBottom: '6px',
                          fontSize: '13px'
                        }}>
                          <span style={{ 
                            marginRight: '8px', 
                            color: feature.highlight ? '#52c41a' : '#1890ff' 
                          }}>✓</span>
                          <span style={{
                            fontWeight: feature.highlight ? 600 : 400
                          }}>{feature.text}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* 支付状态显示 */}
                {paymentStatus === 'paid' && (
                  <div style={{ textAlign: 'center', marginTop: '20px' }}>
                    <CheckCircleOutlined style={{ fontSize: '48px', color: '#52c41a', marginBottom: '16px' }} />
                    <div style={{ fontSize: '18px', fontWeight: 600, color: '#52c41a' }}>
                      支付成功！积分已充值
                    </div>
                  </div>
                )}

                {(paymentStatus === 'expired' || paymentStatus === 'failed' || paymentStatus === 'cancelled') && (
                  <div style={{ textAlign: 'center', marginTop: '20px' }}>
                    <CloseCircleOutlined style={{ fontSize: '24px', color: '#ff4d4f', marginBottom: '8px' }} />
                    <div style={{ fontSize: '14px', fontWeight: 600, color: '#ff4d4f', marginBottom: '8px' }}>
                      {paymentStatus === 'expired' && '支付已过期'}
                      {paymentStatus === 'failed' && '支付失败'}
                      {paymentStatus === 'cancelled' && '支付已取消'}
                    </div>
                    <div style={{ fontSize: '12px', color: '#ccc' }}>
                      请重新选择积分包或刷新二维码
                    </div>
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

                {paymentStatus === 'paid' ? (
                  <div style={{
                    width: '168px',
                    height: '168px',
                    background: 'rgba(82, 196, 26, 0.1)',
                    borderRadius: '8px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    marginBottom: '16px',
                    margin: '0 auto 16px'
                  }}>
                    <CheckCircleOutlined style={{ fontSize: '48px', color: '#52c41a' }} />
                  </div>
                ) : (paymentStatus === 'expired' || paymentStatus === 'failed' || paymentStatus === 'cancelled') ? (
                  <div style={{
                    width: '168px',
                    height: '168px',
                    background: 'rgba(255, 77, 79, 0.1)',
                    borderRadius: '8px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    marginBottom: '16px',
                    margin: '0 auto 16px'
                  }}>
                    <CloseCircleOutlined style={{ fontSize: '48px', color: '#ff4d4f' }} />
                  </div>
                ) : paymentData?.qrCode && (paymentStatus === 'waiting' || paymentStatus === 'pending') ? (
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
                    width: '168px',
                    height: '168px',
                    background: 'rgba(255,255,255,0.1)',
                    borderRadius: '8px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    marginBottom: '16px',
                    margin: '0 auto 16px'
                  }}>
                    {loading ? (
                      <Spin size="large" />
                    ) : (
                      <WechatOutlined style={{ fontSize: '48px', color: '#07c160' }} />
                    )}
                  </div>
                )}

                {selectedPackageInfo && (
                  <div style={{ fontSize: '24px', fontWeight: 700 }}>
                    ¥ {selectedPackageInfo.price}
                  </div>
                )}
              </div>
            </div>

            {/* 注意事项 */}
            {config && (
              <div style={{ 
                textAlign: 'center', 
                marginTop: '20px', 
                paddingTop: '15px', 
                borderTop: '1px solid #444',
                fontSize: '12px',
                color: '#ccc',
                backgroundColor: 'rgba(255, 255, 255, 0.05)',
                borderRadius: '6px',
                padding: '12px'
              }}>
                <div style={{ marginBottom: '4px', color: '#999' }}>
                  <span style={{ color: '#ff7875' }}>注意：</span>
                  {config.planName}用户每日可获得{config.planDailyRefreshCredits}积分，此购买会在其基础上进行叠加
                </div>
              </div>
            )}

            {/* 底部协议 */}
            <div style={{ 
              textAlign: 'center', 
              marginTop: '20px', 
              paddingTop: '20px', 
              borderTop: '1px solid #8b8b8b',
              fontSize: '12px',
              color: '#ccc'
            }}>
              支付即视为你同意《Super Agent会员协议》
            </div>
          </div>
        )}
      </div>
    </Modal>
  );
};

export default CreditPurchaseModal;
