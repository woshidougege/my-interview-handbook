import React, { useState, useEffect } from 'react';
import {
  Modal,
  Button,
  Card,
  Tag,
  Spin,
  message
} from 'antd';
import {
  CheckOutlined,
  CloseOutlined
} from '@ant-design/icons';
import PaymentModal from './PaymentModal';
import { subscriptionApi } from '../services/api';

interface SubscriptionModalProps {
  visible: boolean;
  onClose: () => void;
}

interface PlanFeature {
  text: string;
  included: boolean;
  highlight: boolean;
}

interface Plan {
  id: string;
  name: string;
  price: { monthly: number; yearly: number };
  isCurrent: boolean;
  buttonText: string;
  buttonType: 'default' | 'primary';
  isCreditsOnly?: boolean;
  features: PlanFeature[];
}

const SubscriptionModal: React.FC<SubscriptionModalProps> = ({ visible, onClose }) => {
  const [billingCycle, setBillingCycle] = useState<'monthly' | 'yearly'>('monthly');
  const [paymentVisible, setPaymentVisible] = useState(false);
  const [selectedPlan, setSelectedPlan] = useState<{
    id: string;
    name: string;
    amount: number;
  } | null>(null);
  const [plans, setPlans] = useState<Plan[]>([]);
  const [loading, setLoading] = useState(false);

  // 从API加载套餐数据
  useEffect(() => {
    if (visible) {
      loadPlans();
    }
  }, [visible]); // eslint-disable-line react-hooks/exhaustive-deps

  const loadPlans = async () => {
    try {
      setLoading(true);
      const response = await subscriptionApi.getPlans();
      const apiPlans = response.data.data || [];
      
      // 转换API数据为组件需要的格式  
      const formattedPlans: Plan[] = apiPlans.map((plan: Record<string, any>) => ({
        id: plan.id.toString(),
        name: plan.planName,
        price: { 
          monthly: plan.monthlyPrice || 0, 
          yearly: plan.yearlyPrice || 0 
        },
        isCurrent: false, // TODO: 从用户订阅状态判断
        buttonText: plan.planName === '免费版' ? '当前计划' : '订阅',
        buttonType: plan.planName === '免费版' ? 'default' as const : 'primary' as const,
        isCreditsOnly: plan.planName.includes('积分'),
        features: plan.features ? JSON.parse(plan.features).map((text: string) => ({
          text,
          included: true,
          highlight: text.includes('积分') || text.includes('专属')
        })) : []
      }));
      
      setPlans(formattedPlans);
    } catch (error) {
      console.error('加载套餐失败:', error);
      message.error('加载套餐失败，请重试');
      // 使用默认套餐数据作为后备
      setPlans(getDefaultPlans());
    } finally {
      setLoading(false);
    }
  };

  // 默认套餐数据（作为后备）
  const getDefaultPlans = (): Plan[] => [
    {
      id: '1',
      name: '免费版',
      price: { monthly: 0, yearly: 0 },
      isCurrent: true,
      buttonText: '当前计划',
      buttonType: 'default' as const,
      features: [
        { text: '新用户赠送1000积分（90天有效）', included: true, highlight: false },
        { text: '每日登录赠300积分', included: true, highlight: false }
      ]
    },
    {
      id: '2',
      name: '基础版',
      price: { monthly: 39, yearly: 388 },
      isCurrent: false,
      buttonText: '订阅',
      buttonType: 'primary' as const,
      features: [
        { text: '一次性获得1900永久积分', included: true, highlight: true }
      ]
    }
  ];

  const handleSubscribe = (planId: string) => {
    const plan = plans.find(p => p.id === planId);
    if (!plan) return;

    if (plan.isCurrent) {
      return; // 当前计划不需要处理
    }

    setSelectedPlan({
      id: planId,
      name: plan.name,
      amount: plan.price[billingCycle]
    });
    setPaymentVisible(true);
  };

  const handlePaymentSuccess = () => {
    setPaymentVisible(false);
    onClose();
    // TODO: 刷新用户套餐信息
  };

  return (
    <Modal
      title={null}
      open={visible}
      onCancel={onClose}
      footer={null}
      width={1400}
      centered
      styles={{
        content: { 
          background: '#f8f9fa',
          borderRadius: '12px',
          overflow: 'hidden',
          padding: '0'
        },
        body: { 
          padding: '0'
        }
      }}
    >
      <div style={{ 
        padding: '40px', 
        textAlign: 'center',
        background: '#f8f9fa',
        minHeight: '100%'
      }}>
        {/* 标题区域 */}
        <div style={{ marginBottom: '32px' }}>
          <h1 style={{ 
            fontSize: '28px', 
            fontWeight: 600, 
            color: '#333',
            marginBottom: '8px'
          }}>
            解锁Super Agent进阶能力
          </h1>
          <p style={{ 
            fontSize: '18px', 
            color: '#666',
            margin: 0
          }}>
            获取更多积分
          </p>
        </div>

        {/* 计费周期切换 */}
        <div style={{ marginBottom: '40px' }}>
          <div style={{
            display: 'inline-flex',
            background: '#e9ecef',
            borderRadius: '8px',
            padding: '4px',
            position: 'relative'
          }}>
            <div
              onClick={() => setBillingCycle('monthly')}
              style={{
                padding: '12px 24px',
                borderRadius: '6px',
                background: billingCycle === 'monthly' ? '#fff' : 'transparent',
                color: billingCycle === 'monthly' ? '#333' : '#666',
                fontSize: '14px',
                fontWeight: 500,
                cursor: 'pointer',
                transition: 'all 0.2s ease',
                boxShadow: billingCycle === 'monthly' ? '0 2px 4px rgba(0,0,0,0.1)' : 'none'
              }}
            >
              按月
            </div>
            
            <div
              onClick={() => setBillingCycle('yearly')}
              style={{
                padding: '12px 20px',
                borderRadius: '6px',
                background: billingCycle === 'yearly' ? '#fff' : 'transparent',
                color: billingCycle === 'yearly' ? '#333' : '#666',
                fontSize: '14px',
                fontWeight: 500,
                cursor: 'pointer',
                transition: 'all 0.2s ease',
                boxShadow: billingCycle === 'yearly' ? '0 2px 4px rgba(0,0,0,0.1)' : 'none',
                display: 'flex',
                alignItems: 'center',
                gap: '8px'
              }}
            >
              按年
              <Tag 
                color="blue"
                style={{ 
                  fontSize: '12px',
                  fontWeight: 500,
                  borderRadius: '4px'
                }}
              >
                节省17%
              </Tag>
            </div>
          </div>
        </div>

        {/* 套餐卡片 */}
        {loading ? (
          <div style={{ textAlign: 'center', padding: '60px 0' }}>
            <Spin size="large" />
            <div style={{ marginTop: '16px', color: '#666' }}>加载套餐中...</div>
          </div>
        ) : (
          <div style={{ 
            display: 'grid', 
            gridTemplateColumns: `repeat(${Math.min(plans.length, 4)}, 1fr)`, 
            gap: '24px',
            marginBottom: '32px',
            alignItems: 'stretch'
          }}>
            {plans.map((plan) => (
            <Card
              key={plan.id}
              style={{
                borderRadius: '12px',
                border: plan.isCurrent ? '2px solid #1890ff' : '1px solid #f0f0f0',
                position: 'relative',
                height: '100%',
                display: 'flex',
                flexDirection: 'column',
                background: '#ffffff'
              }}
              bodyStyle={{ 
                padding: '20px',
                display: 'flex',
                flexDirection: 'column',
                height: '100%'
              }}
            >
              {/* 套餐名称 */}
              <h3 style={{ 
                fontSize: '18px', 
                fontWeight: 600, 
                color: '#333',
                marginBottom: '12px',
                textAlign: 'center'
              }}>
                {plan.name}
              </h3>

              {/* 价格 */}
              <div style={{ textAlign: 'center', marginBottom: '16px' }}>
                <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'center' }}>
                  <span style={{ fontSize: '32px', fontWeight: 600, color: '#333' }}>
                    ¥{plan.price[billingCycle]}
                  </span>
                  <span style={{ fontSize: '14px', color: '#666', marginLeft: '4px' }}>
                    {plan.isCreditsOnly ? ' / 10000积分' : (billingCycle === 'monthly' ? ' / 月' : ' / 年')}
                  </span>
                </div>
              </div>

              {/* 订阅按钮 - 在价格下方 */}
              <div style={{ textAlign: 'center', marginBottom: '20px' }}>
                <Button
                  type={plan.buttonType}
                  block
                  size="middle"
                  disabled={plan.isCurrent}
                  onClick={() => handleSubscribe(plan.id)}
                  style={{
                    height: '40px',
                    borderRadius: '6px',
                    fontWeight: 500,
                    fontSize: '14px',
                    background: plan.isCurrent 
                      ? '#f5f5f5' 
                      : plan.buttonType === 'primary' 
                      ? '#000000'
                      : '#ffffff',
                    borderColor: plan.isCurrent 
                      ? '#d9d9d9' 
                      : '#000000',
                    color: plan.isCurrent 
                      ? '#999' 
                      : plan.buttonType === 'primary' 
                      ? '#ffffff'
                      : '#000000',
                    border: plan.isCurrent ? '1px solid #d9d9d9' : '1px solid #000000'
                  }}
                >
                  {plan.buttonText}
                </Button>
              </div>

              {/* 功能列表 - 使用flex-grow占据剩余空间 */}
              <div style={{ textAlign: 'left', flexGrow: 1 }}>
                {plan.features.map((feature, index) => (
                  <div key={index} style={{ 
                    display: 'flex', 
                    alignItems: 'flex-start', 
                    marginBottom: '10px',
                    fontSize: '14px',
                    minHeight: '20px'
                  }}>
                    {feature.included ? (
                      <CheckOutlined style={{ 
                        color: feature.highlight ? '#52c41a' : '#1890ff', 
                        marginRight: '8px',
                        marginTop: '2px',
                        fontSize: '12px',
                        flexShrink: 0
                      }} />
                    ) : (
                      <CloseOutlined style={{ 
                        color: '#ccc', 
                        marginRight: '8px',
                        marginTop: '2px',
                        fontSize: '12px',
                        flexShrink: 0
                      }} />
                    )}
                    <span style={{ 
                      color: feature.included ? '#333' : '#999',
                      fontWeight: feature.highlight ? 600 : 400,
                      flex: 1,
                      lineHeight: '1.5'
                    }}>
                      {feature.text}
                    </span>
                  </div>
                ))}
              </div>
            </Card>
            ))}
          </div>
        )}
      </div>

      {/* 支付弹窗 */}
      {selectedPlan && (
        <PaymentModal
          visible={paymentVisible}
          onClose={() => setPaymentVisible(false)}
          planId={selectedPlan.id}
          planName={selectedPlan.name}
          amount={selectedPlan.amount}
          billingCycle={billingCycle}
          onSuccess={handlePaymentSuccess}
        />
      )}
    </Modal>
  );
};

export default SubscriptionModal;
