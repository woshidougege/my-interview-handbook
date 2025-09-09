import React, { useState } from 'react';
import {
  Modal,
  Button,
  Card,
  Tag
} from 'antd';
import {
  CheckOutlined,
  CloseOutlined
} from '@ant-design/icons';
import PaymentModal from './PaymentModal';

interface SubscriptionModalProps {
  visible: boolean;
  onClose: () => void;
}

const SubscriptionModal: React.FC<SubscriptionModalProps> = ({ visible, onClose }) => {
  const [billingCycle, setBillingCycle] = useState<'monthly' | 'yearly'>('monthly');
  const [paymentVisible, setPaymentVisible] = useState(false);
  const [selectedPlan, setSelectedPlan] = useState<{
    id: string;
    name: string;
    amount: number;
  } | null>(null);

  const plans = [
    {
      id: '1',
      name: '免费版',
      price: { monthly: 0, yearly: 0 },
      isCurrent: true,
      buttonText: '当前计划',
      buttonType: 'default' as const,
      features: [
        { text: '每日可获取30刷新积分', included: true, highlight: false },
        { text: '基础AI功能', included: true, highlight: false },
        { text: '社区支持', included: true, highlight: false },
        { text: '约分析1-2个表格', included: true, highlight: false }
      ]
    },
    {
      id: '2',
      name: 'PRO版',
      price: { monthly: 29, yearly: 27 }, // 年付节省7%
      isCurrent: false,
      buttonText: '订阅',
      buttonType: 'primary' as const,
      features: [
        { text: '一次性获得1000积分', included: true, highlight: false },
        { text: '每日可获取30刷新积分', included: true, highlight: false },
        { text: '所有AI功能', included: true, highlight: false },
        { text: '优先客服支持', included: true, highlight: false },
        { text: '更多模型选择', included: true, highlight: false },
        { text: '约分析3-5个表格', included: true, highlight: false }
      ]
    },
    {
      id: '3',
      name: 'PRO+版',
      price: { monthly: 59, yearly: 55 }, // 年付节省7%
      isCurrent: false,
      buttonText: '订阅',
      buttonType: 'primary' as const,
      features: [
        { text: '一次性获得2500积分', included: true, highlight: true },
        { text: '每日可获取30刷新积分', included: true, highlight: false },
        { text: '所有AI功能', included: true, highlight: false },
        { text: '专属客服支持', included: true, highlight: true },
        { text: '所有模型', included: true, highlight: false },
        { text: 'API访问权限', included: true, highlight: true },
        { text: '高级分析功能', included: true, highlight: true },
        { text: '约分析4-7个表格', included: true, highlight: false }
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
      width={1200}
      centered
      styles={{
        content: { 
          background: '#fff',
          borderRadius: '12px',
          overflow: 'hidden',
          padding: '0'
        },
        body: { 
          padding: '0'
        }
      }}
    >
      <div style={{ padding: '40px', textAlign: 'center' }}>
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
            background: '#f5f5f5',
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
            
            {/* 美化的分隔线 */}
            <div style={{
              width: '1px',
              background: 'linear-gradient(to bottom, transparent, #ddd 20%, #ddd 80%, transparent)',
              margin: '8px 0',
              opacity: 0.6
            }} />
            
            <div
              style={{
                padding: '12px 20px',
                borderRadius: '6px',
                background: '#f5f5f5',
                color: '#999',
                fontSize: '14px',
                fontWeight: 500,
                cursor: 'not-allowed',
                opacity: 0.6,
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
                  borderRadius: '4px',
                  opacity: 0.7
                }}
              >
                节省7%
              </Tag>
            </div>
          </div>
        </div>

        {/* 套餐卡片 */}
        <div style={{ 
          display: 'grid', 
          gridTemplateColumns: 'repeat(3, 1fr)', 
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
                flexDirection: 'column'
              }}
              bodyStyle={{ 
                padding: '24px',
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
                marginBottom: '16px',
                textAlign: 'center'
              }}>
                {plan.name}
              </h3>

              {/* 价格 */}
              <div style={{ textAlign: 'center', marginBottom: '24px' }}>
                <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'center' }}>
                  <span style={{ fontSize: '32px', fontWeight: 600, color: '#333' }}>
                    ¥{plan.price[billingCycle]}
                  </span>
                  <span style={{ fontSize: '14px', color: '#666', marginLeft: '4px' }}>
                    {billingCycle === 'monthly' ? ' / 月' : ' / 年'}
                  </span>
                </div>
              </div>

              {/* 功能列表 - 使用flex-grow占据剩余空间 */}
              <div style={{ textAlign: 'left', flexGrow: 1, marginBottom: '24px' }}>
                {plan.features.map((feature, index) => (
                  <div key={index} style={{ 
                    display: 'flex', 
                    alignItems: 'flex-start', 
                    marginBottom: '12px',
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

              {/* 订阅按钮 - 固定在底部 */}
              <Button
                type={plan.buttonType}
                block
                size="large"
                disabled={plan.isCurrent}
                onClick={() => handleSubscribe(plan.id)}
                style={{
                  height: '44px',
                  borderRadius: '8px',
                  fontWeight: 500,
                  background: plan.isCurrent ? '#f5f5f5' : undefined,
                  borderColor: plan.isCurrent ? '#d9d9d9' : undefined,
                  marginTop: 'auto'
                }}
              >
                {plan.buttonText}
              </Button>
            </Card>
          ))}
        </div>
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
