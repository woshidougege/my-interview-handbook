import React, { useState } from 'react';
import { Button } from 'antd';
import CreditPurchaseModal from './CreditPurchaseModal';

/**
 * 积分购买使用示例组件
 * 展示如何在页面中集成积分购买功能
 */
const CreditPurchaseExample: React.FC = () => {
  const [creditPurchaseVisible, setCreditPurchaseVisible] = useState(false);

  const handlePurchaseSuccess = () => {
    // 购买成功后的处理逻辑
    console.log('积分购买成功！');
    // 可以在这里刷新用户积分信息、更新页面状态等
  };

  return (
    <div style={{ padding: '20px' }}>
      <h2>积分购买示例</h2>
      <p>点击下面的按钮打开积分购买窗口：</p>
      
      <Button
        type="primary"
        size="large"
        onClick={() => setCreditPurchaseVisible(true)}
      >
        购买积分
      </Button>

      <CreditPurchaseModal
        visible={creditPurchaseVisible}
        onClose={() => setCreditPurchaseVisible(false)}
        onSuccess={handlePurchaseSuccess}
      />
      
      <div style={{ marginTop: '20px', color: '#666' }}>
        <h3>功能说明：</h3>
        <ul>
          <li>点击"购买积分"按钮会打开积分购买模态框</li>
          <li>模态框会自动获取当前用户的套餐信息和积分配置</li>
          <li>用户可以选择不同的积分套餐（10000积分/59元、20000积分/99元、50000积分/199元）</li>
          <li>购买的积分永久有效，无过期时间限制</li>
          <li>支持微信扫码支付</li>
          <li>实时监听支付状态变化</li>
          <li>购买成功后会触发onSuccess回调</li>
        </ul>
      </div>
    </div>
  );
};

export default CreditPurchaseExample;
