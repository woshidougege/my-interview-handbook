import React, { useState, useEffect } from 'react';
import Head from 'next/head';
import { Button, message } from 'antd';
import { useRouter } from 'next/router';
import PhoneLoginModal from '../components/PhoneLoginModal';

const LoginPage: React.FC = () => {
  const [loginModalVisible, setLoginModalVisible] = useState(false);
  const [mounted, setMounted] = useState(false);
  const router = useRouter();

  // 修复水合错误 - 确保客户端挂载后再显示模态框
  useEffect(() => {
    setMounted(true);
    setLoginModalVisible(true);
  }, []);

  const handleLoginSuccess = (ticket: string) => {
    console.log('登录成功，ticket:', ticket);
    setLoginModalVisible(false);
    
    // 登录成功后，跳转到主页或聊天页面
    message.success('登录成功，即将跳转到聊天页面');
    setTimeout(() => {
      router.push('/chat'); // 跳转到聊天页面
    }, 1000);
  };

  const handleCancel = () => {
    setLoginModalVisible(false);
    // 如果用户取消登录，可以跳转到首页或显示提示
    message.info('请先登录后再使用系统功能');
  };

  // 避免水合错误 - 服务端不渲染模态框
  if (!mounted) {
    return (
      <>
        <Head>
          <title>Super Agent - 登录</title>
          <meta name="description" content="Super Agent 智能助手系统登录" />
        </Head>
        
        <div style={{ 
          height: '100vh', 
          display: 'flex', 
          alignItems: 'center', 
          justifyContent: 'center',
          background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        }}>
          <div style={{ 
            textAlign: 'center', 
            color: '#fff',
          }}>
            <h1 style={{ 
              fontSize: '48px', 
              fontWeight: 'bold', 
              marginBottom: '16px',
              textShadow: '2px 2px 4px rgba(0,0,0,0.3)'
            }}>
              Super Agent
            </h1>
            <p style={{ fontSize: '18px', opacity: 0.9 }}>
              正在加载登录界面...
            </p>
          </div>
        </div>
      </>
    );
  }

  return (
    <>
      <Head>
        <title>Super Agent - 登录</title>
        <meta name="description" content="Super Agent 智能助手系统登录" />
      </Head>
      
      <div style={{ 
        height: '100vh', 
        display: 'flex', 
        alignItems: 'center', 
        justifyContent: 'center',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        position: 'relative',
      }}>
        {/* 背景装饰 */}
        <div style={{
          position: 'absolute',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'url(/static/login-bg.jpg) center center / cover no-repeat',
          opacity: 0.1,
          zIndex: 0,
        }} />
        
        {/* 页面内容 */}
        <div style={{ 
          textAlign: 'center', 
          color: '#fff',
          zIndex: 1,
        }}>
          {!loginModalVisible && (
            <>
              <h1 style={{ 
                fontSize: '48px', 
                fontWeight: 'bold', 
                marginBottom: '16px',
                textShadow: '2px 2px 4px rgba(0,0,0,0.3)'
              }}>
                Super Agent
              </h1>
              <p style={{ 
                fontSize: '18px', 
                marginBottom: '32px',
                opacity: 0.9
              }}>
                您的AI智能助手
              </p>
              
              <Button 
                type="primary" 
                size="large"
                onClick={() => setLoginModalVisible(true)}
                style={{ 
                  height: '48px', 
                  width: '200px', 
                  fontSize: '16px',
                  borderRadius: '24px',
                  border: 'none',
                  background: 'rgba(255,255,255,0.2)',
                  backdropFilter: 'blur(10px)',
                  color: '#fff'
                }}
              >
                立即登录
              </Button>
            </>
          )}
        </div>
        
        {/* 登录弹窗 - 只在客户端挂载后显示 */}
        {mounted && (
          <PhoneLoginModal
            visible={loginModalVisible}
            onCancel={handleCancel}
            onSuccess={handleLoginSuccess}
          />
        )}
      </div>
    </>
  );
};

export default LoginPage;