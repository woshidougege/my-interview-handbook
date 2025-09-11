import React, { useEffect } from 'react';
import type { AppProps } from 'next/app';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import '../styles/global.css';
import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';
import { preloadPublicKey } from '../services/publicKeyService';

// 设置 dayjs 中文
dayjs.locale('zh-cn');

// Ant Design 主题配置
const theme = {
  token: {
    colorPrimary: '#1890ff',
    colorSuccess: '#52c41a',
    colorWarning: '#faad14',
    colorError: '#ff4d4f',
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, "Noto Sans", sans-serif',
  },
  components: {
    Card: {
      borderRadiusLG: 8,
    },
    Button: {
      borderRadius: 6,
    },
  },
};

export default function App({ Component, pageProps }: AppProps) {
  // 应用启动时预加载公钥
  useEffect(() => {
    preloadPublicKey().catch(error => {
      console.warn('预加载公钥失败:', error);
    });
  }, []);

  return (
    <ConfigProvider locale={zhCN} theme={theme}>
      <Component {...pageProps} />
    </ConfigProvider>
  );
}
