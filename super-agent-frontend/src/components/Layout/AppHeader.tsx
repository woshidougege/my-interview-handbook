import React from 'react';
import { Layout, Typography, Space } from 'antd';
import UserProfile from '../UserProfile';

const { Header } = Layout;
const { Title } = Typography;

const AppHeader: React.FC = () => {
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

      {/* 右侧用户信息 */}
      <UserProfile />
    </Header>
  );
};

export default AppHeader;
