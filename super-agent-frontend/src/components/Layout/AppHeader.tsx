import React from 'react';
import { Layout, Typography, Space, Menu } from 'antd';
import { useRouter } from 'next/router';
import { HomeOutlined, ShoppingOutlined, UnorderedListOutlined, MessageOutlined } from '@ant-design/icons';
import UserProfile from '../UserProfile';

const { Header } = Layout;
const { Title } = Typography;

const AppHeader: React.FC = () => {
  const router = useRouter();

  const menuItems = [
    {
      key: '/',
      icon: <HomeOutlined />,
      label: '首页',
    },
    {
      key: '/chat',
      icon: <MessageOutlined />,
      label: 'AI对话',
    },
    {
      key: '/orders',
      icon: <UnorderedListOutlined />,
      label: '我的订单',
    },
    {
      key: '/settings',
      icon: <ShoppingOutlined />,
      label: '账户设置',
    },
  ];

  const handleMenuClick = (e: any) => {
    router.push(e.key);
  };

  return (
    <Header 
      style={{ 
        background: '#fff',
        padding: '0 24px',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        borderBottom: '1px solid #f0f0f0',
        height: '64px'
      }}
    >
      {/* Logo和标题 */}
      <Space>
        <Title level={3} style={{ margin: 0, color: '#1890ff' }}>
          Super Agent
        </Title>
      </Space>

      {/* 中间导航菜单 */}
      <Menu
        mode="horizontal"
        selectedKeys={[router.pathname]}
        items={menuItems}
        onClick={handleMenuClick}
        style={{ 
          border: 'none',
          background: 'transparent',
          minWidth: '300px',
        }}
      />

      {/* 右侧用户信息 */}
      <UserProfile />
    </Header>
  );
};

export default AppHeader;
