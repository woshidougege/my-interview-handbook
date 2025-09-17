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
  CheckOutlined
} from '@ant-design/icons';
import PaymentModal from './PaymentModal';
import CreditPurchaseModal from './CreditPurchaseModal';
import { subscriptionApi } from '../services/api';

interface SubscriptionModalProps {
  visible: boolean;
  onClose: () => void;
  currentSubscription?: { id: string; planName: string; status: string } | null; // 当前订阅状态
}

interface PlanFeature {
  text: string;
  included: boolean;
  highlight: boolean;
}

interface Plan {
  id: string;
  name: string;
  code: string;
  price: { monthly: number; yearly: number };
  monthlyOriginalPrice: number; // 月价原价（后端提供）
  monthlySavings: number; // 月价优惠金额（后端提供）
  yearlyOriginalPrice: number; // 年价原价（后端提供）
  yearlySavings: number; // 年价优惠金额（后端提供）
  yearlyMonthlySavings: number; // 年价月均优惠金额（后端提供）
  yearlyDiscountRate: number; // 优惠比例（后端提供）
  discountPercentageText: string; // 优惠百分比显示文本（后端提供）
  creditsAmount?: number; // 积分数量（积分套餐专用）
  isCurrent: boolean;
  buttonText: string;
  buttonType: 'default' | 'primary';
  isCreditsOnly?: boolean;
  features: PlanFeature[];
}

const SubscriptionModal: React.FC<SubscriptionModalProps> = ({ visible, onClose, currentSubscription }) => {
  const [billingCycle, setBillingCycle] = useState<'monthly' | 'yearly'>('monthly');
  const [paymentVisible, setPaymentVisible] = useState(false);
  const [creditPurchaseVisible, setCreditPurchaseVisible] = useState(false);
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
  }, [visible, billingCycle]); // eslint-disable-line react-hooks/exhaustive-deps


  const loadPlans = async () => {
    let globalDiscountRate = 0.17; // 默认优惠比例
    let discountText = "17%"; // 默认优惠显示文本
    
    try {
      setLoading(true);
      const response = await subscriptionApi.getPlansWithBillingCycle(billingCycle);
      const responseData = response.data.data || {};
      const apiPlans: Array<Record<string, any>> = responseData.plans || [];
      globalDiscountRate = responseData.yearlyDiscountRate || 0.17;
      discountText = (responseData as { discountPercentageText?: string }).discountPercentageText || "17%";
      
      // 转换API数据为组件需要的格式  
      // 直接使用API数据，不做复杂转换
      const formattedPlans: Plan[] = apiPlans.map((plan: Record<string, any>) => ({
        id: plan.id.toString(),
        name: plan.planName,
        code: plan.planCode || plan.planName.toLowerCase(),
        price: { 
          monthly: plan.monthlyPrice || 0, 
          yearly: plan.yearlyPrice || 0 
        },
        monthlyOriginalPrice: plan.monthlyOriginalPrice || 0,
        monthlySavings: plan.monthlySavings || 0,
        yearlyOriginalPrice: plan.yearlyOriginalPrice || 0,
        yearlySavings: plan.yearlySavings || 0,
        yearlyMonthlySavings: Math.round((plan.yearlySavings || 0) / 12), // 年付月均优惠金额
        yearlyDiscountRate: globalDiscountRate,
        discountPercentageText: discountText,
        creditsAmount: plan.creditsAmount,
        isCurrent: plan.isCurrentPlan || false,
        buttonText: plan.isCurrentPlan ? '当前计划' : (plan.planCode === 'CREDIT_PACK' ? '立即购买' : '订阅'),
        buttonType: plan.isCurrentPlan ? 'default' as const : 'primary' as const,
        isCreditsOnly: plan.planCode === 'CREDIT_PACK',
        features: plan.features || []
      }));
      
      setPlans(formattedPlans);
    } catch (error) {
      console.error('加载套餐失败:', error);
      message.error('加载套餐失败，请重试');
      setPlans([]); // 出错时显示空列表，避免错误的硬编码数据
    } finally {
      setLoading(false);
    }
  };


  // 前端只负责展示，所有价格计算由后端完成


  const handleSubscribe = (planId: string) => {
    const plan = plans.find(p => p.id === planId);
    if (!plan) return;

    if (plan.isCurrent) {
      return; // 当前计划不需要处理
    }

    // 检查是否是积分购买套餐
    if (plan.isCreditsOnly || plan.code === 'CREDIT_PACK') {
      // 打开积分购买弹窗
      setCreditPurchaseVisible(true);
    } else {
      // 打开普通订阅支付弹窗
      // 月付使用原价，年付使用优惠后价格
      const amount = billingCycle === 'yearly' ? plan.price.yearly : plan.monthlyOriginalPrice;
      setSelectedPlan({
        id: planId,
        name: plan.name,
        amount: amount
      });
      setPaymentVisible(true);
    }
  };

  const handlePaymentSuccess = () => {
    setPaymentVisible(false);
    onClose();
    // TODO: 刷新用户套餐信息
  };

  const handleCreditPurchaseSuccess = () => {
    setCreditPurchaseVisible(false);
    onClose();
    // TODO: 刷新用户积分信息
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
              {(() => {
                const discountPlan = plans.find(p => p.yearlyDiscountRate > 0);
                return discountPlan && (
                  <Tag 
                    color="blue"
                    style={{ 
                      fontSize: '12px',
                      fontWeight: 500,
                      borderRadius: '4px'
                    }}
                  >
                    节省{discountPlan.discountPercentageText}
                  </Tag>
                );
              })()}
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
            gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', 
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

              {/* 价格 - 月付显示原价，年付显示优惠价 */}
              <div style={{ textAlign: 'center', marginBottom: '16px' }}>
                {billingCycle === 'monthly' ? (
                  /* 月付：直接显示月价原价，无优惠信息 */
                  <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'center' }}>
                    <span style={{ fontSize: '32px', fontWeight: 600, color: '#333' }}>
                      ¥{plan.monthlyOriginalPrice}
                    </span>
                    <span style={{ fontSize: '14px', color: '#666', marginLeft: '4px' }}>
                      {plan.isCreditsOnly ? ` / ${plan.creditsAmount}积分` : ' / 月'}
                    </span>
                  </div>
                ) : (
                  /* 年付：显示原价、优惠价和优惠信息 */
                  <>
                    {/* 显示原价和优惠标签 */}
                    {!plan.isCreditsOnly && plan.yearlySavings > 0 && (
                      <div style={{ marginBottom: '8px' }}>
                        <span style={{ 
                          fontSize: '16px', 
                          color: '#999', 
                          textDecoration: 'line-through',
                          marginRight: '8px'
                        }}>
                          ¥{plan.yearlyOriginalPrice}
                        </span>
                        <span style={{ 
                          fontSize: '12px', 
                          color: '#ff4d4f',
                          background: '#fff2f0',
                          padding: '2px 6px',
                          borderRadius: '3px'
                        }}>
                          省¥{plan.yearlySavings}
                        </span>
                      </div>
                    )}
                    
                    {/* 显示年付优惠后价格 */}
                    <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'center' }}>
                      <span style={{ fontSize: '32px', fontWeight: 600, color: '#333' }}>
                        ¥{plan.price.yearly}
                      </span>
                      <span style={{ fontSize: '14px', color: '#666', marginLeft: '4px' }}>
                        {plan.isCreditsOnly ? ` / ${plan.creditsAmount}积分` : ' / 年'}
                      </span>
                    </div>
                    
                    {/* 显示月均价格和优惠提示 */}
                    {!plan.isCreditsOnly && plan.price.yearly > 0 && (
                      <div style={{ fontSize: '12px', color: '#999', marginTop: '4px' }}>
                        <div>平均 {plan.price.monthly}元/月</div>
                        {plan.monthlySavings > 0 && (
                          <div style={{ color: '#ff4d4f', marginTop: '2px' }}>
                            每月省 {plan.monthlySavings}元
                          </div>
                        )}
                      </div>
                    )}
                  </>
                )}
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
                    <CheckOutlined style={{ 
                      color: feature.highlight ? '#52c41a' : '#1890ff', 
                      marginRight: '8px',
                      marginTop: '2px',
                      fontSize: '12px',
                      flexShrink: 0
                    }} />
                    <span style={{ 
                      color: '#333',
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
          currentSubscription={currentSubscription}
        />
      )}

      {/* 积分购买弹窗 */}
      <CreditPurchaseModal
        visible={creditPurchaseVisible}
        onClose={() => setCreditPurchaseVisible(false)}
        onSuccess={handleCreditPurchaseSuccess}
      />
    </Modal>
  );
};

export default SubscriptionModal;
