import React, { useState } from 'react';
import Head from 'next/head';
import AppLayout from '@/components/Layout/AppLayout';
import UserSettings from '@/components/UserSettings';

const SettingsPage: React.FC = () => {
  const [settingsVisible, setSettingsVisible] = useState(true);

  const handleClose = () => {
    setSettingsVisible(false);
    // 可以在这里添加路由跳转逻辑，返回首页或其他页面
    window.history.back();
  };

  return (
    <>
      <Head>
        <title>用户设置 - Super Agent</title>
        <meta name="description" content="Super Agent 用户设置页面" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <link rel="icon" href="/favicon.ico" />
      </Head>
      
      <AppLayout>
        <div style={{ 
          minHeight: 'calc(100vh - 64px)', 
          display: 'flex', 
          alignItems: 'center', 
          justifyContent: 'center',
          background: 'rgba(0, 0, 0, 0.3)'
        }}>
          <UserSettings
            visible={settingsVisible}
            onClose={handleClose}
          />
        </div>
      </AppLayout>
    </>
  );
};

export default SettingsPage;
