import React, { useState, useEffect } from 'react';
import {
  Avatar,
  Dropdown,
  Badge,
  Button,
  message
} from 'antd';
import {
  UserOutlined,
  SettingOutlined,
  LogoutOutlined,
  CrownOutlined
} from '@ant-design/icons';
import { UserInfo, UserCredit } from '@/types/user';
import { userApi, creditApi, subscriptionApi } from '@/services/api';
import UserSettings from './UserSettings';
import SubscriptionModal from './SubscriptionModal';


const UserProfile: React.FC = () => {
  const [userInfo, setUserInfo] = useState<UserInfo | null>(null);
  const [creditInfo, setCreditInfo] = useState<UserCredit | null>(null);
  const [currentSubscription, setCurrentSubscription] = useState<Record<string, any> | null>(null);
  const [settingsVisible, setSettingsVisible] = useState(false);
  const [subscriptionVisible, setSubscriptionVisible] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadUserData();
  }, []);

  const loadUserData = async () => {
    try {
      setLoading(true);
      
      // 获取用户信息
      const userResponse = await userApi.getCurrentUser();
      const user = userResponse.data.data;
      setUserInfo(user);
      
      // 获取积分信息
      const creditResponse = await creditApi.getUserCredit(user.id);
      const credit = creditResponse.data.data;
      setCreditInfo(credit);
      
      // 获取当前订阅状态
      try {
        const subscriptionResponse = await subscriptionApi.getCurrentSubscription(user.id);
        const subscription = subscriptionResponse.data.data;
        setCurrentSubscription(subscription);
      } catch (error) {
        // 订阅信息获取失败不影响其他功能
        console.log('获取订阅信息失败:', error);
      }
      
    } catch (error) {
      message.error('加载用户数据失败: ' + (error instanceof Error ? error.message : '未知错误'));
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    message.info('退出登录功能开发中');
  };

  const menuItems = [
    {
      key: 'user-info',
      label: (
        <div style={{ 
          padding: '8px 0',
          borderBottom: '1px solid #f0f0f0',
          marginBottom: '4px'
        }}>
          <div style={{ 
            display: 'flex',
            alignItems: 'center',
            gap: '8px'
          }}>
            <Avatar 
              size={32}
              style={{ 
                backgroundColor: '#1890ff',
                color: '#fff',
                fontSize: '12px'
              }}
            >
              {userInfo?.username?.substring(0, 2) || 'SU'}
            </Avatar>
            <div>
              <div style={{ 
                fontSize: '14px',
                fontWeight: 500,
                color: '#333'
              }}>
                {userInfo?.username || '张三'}
              </div>
              <div style={{ 
                fontSize: '12px',
                color: '#666',
                marginTop: '2px'
              }}>
                {currentSubscription ? '高级会员' : '免费用户'}
              </div>
            </div>
          </div>
        </div>
      ),
      disabled: true
    },
    {
      key: 'settings',
      label: (
        <span style={{ 
          color: '#333',
          fontSize: '14px',
          display: 'flex',
          alignItems: 'center',
          gap: '6px'
        }}>
          <SettingOutlined style={{ 
            fontSize: '14px',
            color: '#666'
          }} />
          设置
        </span>
      ),
      onClick: () => setSettingsVisible(true)
    },
    {
      key: 'subscription',
      label: (
        <span style={{ 
          color: '#333',
          fontSize: '14px',
          display: 'flex',
          alignItems: 'center',
          gap: '6px'
        }}>
          <CrownOutlined style={{ 
            fontSize: '14px',
            color: currentSubscription ? '#faad14' : '#666'
          }} />
          套餐订阅
        </span>
      ),
      onClick: () => setSubscriptionVisible(true)
    },
    {
      type: 'divider' as const
    },
    {
      key: 'logout',
      label: (
        <span style={{ 
          color: '#333',
          fontSize: '14px',
          display: 'flex',
          alignItems: 'center',
          gap: '6px'
        }}>
          <LogoutOutlined style={{ 
            fontSize: '14px',
            color: '#ff4d4f'
          }} />
          退出登录
        </span>
      ),
      onClick: handleLogout
    }
  ];

  if (loading) {
    return (
      <div>
        <Badge count={0}>
          <Avatar icon={<UserOutlined />} />
        </Badge>
      </div>
    );
  }

  const handleUpgrade = () => {
    setSubscriptionVisible(true);
  };

  return (
    <>
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
        {/* 积分余额和升级按钮 */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span style={{ 
            color: '#333', 
            fontSize: '16px',
            fontWeight: 600,
            lineHeight: 1,
            display: 'flex',
            alignItems: 'center',
            gap: '4px'
          }}>
            <span style={{ color: '#faad14' }}>⚡</span>
            {(creditInfo?.totalBalance || 1094).toLocaleString()}
          </span>
          <Button
            type="text"
            size="small"
            onClick={handleUpgrade}
            style={{ 
              background: '#1890ff',
              color: '#fff',
              fontSize: '12px',
              padding: '4px 8px',
              height: '24px',
              lineHeight: '16px',
              fontWeight: 500,
              borderRadius: '12px',
              border: 'none'
            }}
          >
            升级
          </Button>
        </div>

        {/* 用户头像和下拉菜单 */}
        <Dropdown
          menu={{ items: menuItems }}
          placement="bottomRight"
          trigger={['click']}
          overlayClassName="user-dropdown"
        >
          <Avatar 
            size={32}
            style={{ 
              backgroundColor: '#1890ff',
              color: '#fff',
              fontSize: '14px',
              cursor: 'pointer'
            }}
          >
            {userInfo?.username?.substring(0, 2) || 'SU'}
          </Avatar>
        </Dropdown>
      </div>

      {/* 设置弹窗 */}
      <UserSettings
        visible={settingsVisible}
        onClose={() => setSettingsVisible(false)}
      />

      {/* 订阅弹窗 */}
      <SubscriptionModal
        visible={subscriptionVisible}
        onClose={() => setSubscriptionVisible(false)}
        currentSubscription={currentSubscription}
      />
    </>
  );
};

export default UserProfile;
