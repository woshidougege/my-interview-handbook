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
  LogoutOutlined
} from '@ant-design/icons';
import { UserInfo, UserCredit } from '@/types/user';
import { userApi, creditApi } from '@/services/api';
import UserSettings from './UserSettings';


const UserProfile: React.FC = () => {
  const [userInfo, setUserInfo] = useState<UserInfo | null>(null);
  const [creditInfo, setCreditInfo] = useState<UserCredit | null>(null);
  const [settingsVisible, setSettingsVisible] = useState(false);
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
    message.info('升级功能开发中');
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
            lineHeight: 1
          }}>
            ¥ {(creditInfo?.totalBalance || 1094).toLocaleString()}
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
    </>
  );
};

export default UserProfile;
