import React, { useState, useEffect } from 'react';
import Head from 'next/head';
import { Form, Input, Button, Card, Tabs, message, Spin, Row, Col, Alert } from 'antd';
import { UserOutlined, LockOutlined, PhoneOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import { useRouter } from 'next/router';
import { authApi } from '../services/api';
import { encryptPassword, encryptSmsCode } from '../utils/sm2Encrypt';
import { getPublicKey, getCachedPublicKey, preloadPublicKey } from '../services/publicKeyService';

const LoginPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [smsCountdown, setSmsCountdown] = useState(0);
  const [publicKeyLoaded, setPublicKeyLoaded] = useState(false);
  const [publicKeyError, setPublicKeyError] = useState('');
  const router = useRouter();

  // 获取SM2公钥
  useEffect(() => {
    const initPublicKey = async () => {
      try {
        // 如果缓存中有公钥，直接使用
        if (getCachedPublicKey()) {
          setPublicKeyLoaded(true);
          return;
        }
        
        // 否则重新获取公钥
        await getPublicKey();
        setPublicKeyLoaded(true);
      } catch (error: any) {
        setPublicKeyError('获取公钥失败: ' + (error.message || '网络错误'));
      }
    };

    initPublicKey();
  }, []);

  // 密码登录
  const onPasswordLogin = async (values: any) => {
    setLoading(true);
    try {
      // 加密密码
      const encryptedPassword = encryptPassword(values.password);
      
      // 调用密码登录接口
      const response = await authApi.passwordLogin({
        username: values.username,
        password: encryptedPassword,
      });

      if (response.data.success) {
        message.success('登录成功');
        localStorage.setItem('auth_ticket', response.data.data.ticket);
        router.push('/');
      } else {
        message.error(response.data.message || '登录失败');
      }
    } catch (error: any) {
      message.error(error.response?.data?.message || '登录失败');
    } finally {
      setLoading(false);
    }
  };

  // 手机验证码登录
  const onPhoneLogin = async (values: any) => {
    setLoading(true);
    try {
      // 加密验证码
      const encryptedSmsCode = encryptSmsCode(values.phoneCode);
      
      // 调用手机登录接口
      const response = await authApi.phoneLogin({
        phone: values.phone,
        smsCode: encryptedSmsCode,
      });

      if (response.data.success) {
        message.success('登录成功');
        localStorage.setItem('auth_ticket', response.data.data.ticket);
        router.push('/');
      } else {
        message.error(response.data.message || '登录失败');
      }
    } catch (error: any) {
      message.error(error.response?.data?.message || '登录失败');
    } finally {
      setLoading(false);
    }
  };

  // 发送短信验证码
  const sendSmsCode = async (phone: string) => {
    try {
      // 调用发送短信接口
      const response = await authApi.sendSms({ phone });
      
      if (response.data.success) {
        message.success('验证码发送成功');
        // 开始倒计时
        let countdown = 60;
        setSmsCountdown(countdown);
        const timer = setInterval(() => {
          countdown--;
          setSmsCountdown(countdown);
          if (countdown <= 0) {
            clearInterval(timer);
          }
        }, 1000);
      } else {
        message.error(response.data.message || '验证码发送失败');
      }
    } catch (error: any) {
      message.error(error.response?.data?.message || '验证码发送失败');
    }
  };

  const items = [
    {
      key: 'password',
      label: '密码登录',
      children: (
        <Form
          name="password-login"
          onFinish={onPasswordLogin}
          autoComplete="off"
          layout="vertical"
        >
          <Form.Item
            name="username"
            label="用户名"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input prefix={<UserOutlined />} placeholder="请输入用户名" />
          </Form.Item>

          <Form.Item
            name="password"
            label="密码"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="请输入密码" />
          </Form.Item>

          <Form.Item>
            <Button 
              type="primary" 
              htmlType="submit" 
              style={{ width: '100%' }}
              disabled={!publicKeyLoaded || loading}
            >
              {publicKeyLoaded ? '登录' : '正在加载加密密钥...'}
            </Button>
          </Form.Item>
        </Form>
      ),
    },
    {
      key: 'phone',
      label: '手机验证码登录',
      children: (
        <Form
          name="phone-login"
          onFinish={onPhoneLogin}
          autoComplete="off"
          layout="vertical"
        >
          <Form.Item
            name="phone"
            label="手机号"
            rules={[
              { required: true, message: '请输入手机号' },
              { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号' }
            ]}
          >
            <Input prefix={<PhoneOutlined />} placeholder="请输入手机号" />
          </Form.Item>

          <Form.Item
            name="phoneCode"
            label="验证码"
            rules={[{ required: true, message: '请输入验证码' }]}
          >
            <Row gutter={8}>
              <Col span={16}>
                <Input prefix={<SafetyCertificateOutlined />} placeholder="请输入验证码" />
              </Col>
              <Col span={8}>
                <Button 
                  disabled={smsCountdown > 0}
                  onClick={() => {
                    const form = Form.useForm()[0];
                    const phone = form.getFieldValue('phone');
                    if (phone && /^1[3-9]\d{9}$/.test(phone)) {
                      sendSmsCode(phone);
                    } else {
                      message.error('请输入有效的手机号');
                    }
                  }}
                  style={{ width: '100%' }}
                >
                  {smsCountdown > 0 ? `${smsCountdown}s后重试` : '获取验证码'}
                </Button>
              </Col>
            </Row>
          </Form.Item>

          <Form.Item>
            <Button type="primary" htmlType="submit" style={{ width: '100%' }}>
              登录
            </Button>
          </Form.Item>
        </Form>
      ),
    },
  ];

  return (
    <>
      <Head>
        <title>登录 - Super Agent</title>
      </Head>
      
      <div style={{ 
        minHeight: '100vh', 
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '20px'
      }}>
        <Card 
          title="用户登录" 
          style={{ width: 400, boxShadow: '0 4px 12px rgba(0,0,0,0.15)' }}
        >
          <Spin spinning={loading}>
            {publicKeyError && (
              <Alert
                message="加密密钥错误"
                description={publicKeyError}
                type="error"
                style={{ marginBottom: 16 }}
                showIcon
              />
            )}
            <Tabs items={items} defaultActiveKey="password" />
            
            <Row justify="center" style={{ marginTop: 16 }}>
              <Col>
                <Button type="link" onClick={() => router.push('/register')}>
                  没有账号？立即注册
                </Button>
              </Col>
            </Row>
          </Spin>
        </Card>
      </div>
    </>
  );
};

export default LoginPage;