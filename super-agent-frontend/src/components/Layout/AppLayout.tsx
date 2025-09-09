import React from 'react';
import { Layout } from 'antd';
import AppHeader from './AppHeader';

const { Content } = Layout;

interface AppLayoutProps {
  children: React.ReactNode;
}

const AppLayout: React.FC<AppLayoutProps> = ({ children }) => {
  return (
    <Layout style={{ minHeight: '100vh' }}>
      <AppHeader />
      <Content style={{ background: '#f5f5f5' }}>
        {children}
      </Content>
    </Layout>
  );
};

export default AppLayout;
