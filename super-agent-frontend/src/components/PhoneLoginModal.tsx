import React, { useState, useEffect } from 'react';
import { Modal, Tabs, Form, Input, Button, message } from 'antd';
import { PhoneOutlined, SafetyCertificateOutlined, UserOutlined, LockOutlined } from '@ant-design/icons';
import { API_ENDPOINTS, buildApiUrl } from '../config/apiEndpoints';
import { encryptWithSM2 } from '../utils/sm2Encrypt';
import { getPublicKey, getCachedPublicKey } from '../services/publicKeyService';
import { setCookie } from '../utils/cookieHelper';
import styles from './PhoneLoginModal.module.css';

const { TabPane } = Tabs;

interface PhoneLoginModalProps {
  visible: boolean;
  onCancel: () => void;
  onSuccess: (ticket: string) => void;
}

const PhoneLoginModal: React.FC<PhoneLoginModalProps> = ({ visible, onCancel, onSuccess }) => {
  const [loading, setLoading] = useState(false);
  const [smsLoading, setSmsLoading] = useState(false);
  const [smsCountdown, setSmsCountdown] = useState(0);
  const [publicKeyLoaded, setPublicKeyLoaded] = useState(false);
  const [publicKeyError, setPublicKeyError] = useState('');
  const [form] = Form.useForm();
  const [passwordForm] = Form.useForm();

  // 初始化SM2公钥
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
        console.error('获取公钥失败:', error);
        setPublicKeyError('获取加密公钥失败');
        // 不显示警告，用户不需要知道这么多技术细节
      }
    };

    if (visible) {
      initPublicKey();
    }
  }, [visible]);

  // 发送短信验证码
  const handleSendSms = async () => {
    try {
      const phone = form.getFieldValue('phone');
      if (!phone) {
        message.error('请先输入手机号');
        return;
      }

      // 验证手机号格式
      const phoneRegex = /^1[3-9]\d{9}$/;
      if (!phoneRegex.test(phone)) {
        message.error('请输入正确的手机号');
        return;
      }

      setSmsLoading(true);
      
      // 调用发送短信接口 - 使用配置化端点
      const response = await fetch(buildApiUrl(`${API_ENDPOINTS.LEGACY.AUTH_SEND_SMS}?phoneNumber=${phone}`), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
      });
      const result = await response.json();
      
      if (result.code === 200) {
        message.success('验证码发送成功');
        
        // 开始倒计时
        let countdown = 5;
        setSmsCountdown(countdown);
        const timer = setInterval(() => {
          countdown--;
          setSmsCountdown(countdown);
          if (countdown === 0) {
            clearInterval(timer);
          }
        }, 1000);
      } else {
        message.error(result.message || '发送验证码失败');
      }
    } catch (error: any) {
      console.error('发送短信验证码失败:', error);
      message.error('发送验证码失败，请稍后重试');
    } finally {
      setSmsLoading(false);
    }
  };

  // 手机验证码登录
  const handlePhoneLogin = async (values: any) => {
    setLoading(true);
    try {
      // 第一步：调用手机验证码登录接口获取ticket
      const loginResponse = await fetch(buildApiUrl(API_ENDPOINTS.LEGACY.AUTH_PHONE_LOGIN), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          phone: values.phone,
          phoneCode: values.smsCode,
        }),
      });
      const loginResult = await loginResponse.json();

      if (loginResult.code === 200) {
        const ticket = loginResult.data?.ticket;
        if (ticket) {
          // 第二步：使用ticket获取satoken
          await handleTicketLogin(ticket);
        } else {
          message.error('登录成功但未获取到有效凭证');
        }
      } else {
        message.error(loginResult.message || '登录失败');
      }
    } catch (error: any) {
      console.error('手机验证码登录失败:', error);
      message.error('登录失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  // 处理ticket登录流程
  const handleTicketLogin = async (ticket: string) => {
    try {
      // 第二步：根据ticket获取satoken
      const tokenResponse = await fetch(API_ENDPOINTS.SSO.DO_LOGIN_BY_TICKET(ticket), {
        method: 'GET',
      });
      const tokenResult = await tokenResponse.json();

      if (tokenResult.code === 200 && tokenResult.data) {
        const satoken = tokenResult.data;
        
        // 第三步：保存satoken到cookie
        setCookie('satoken', satoken, 30); // 保存30天
        
        // 第四步：获取用户信息验证登录
        const userResponse = await fetch(API_ENDPOINTS.SSO.GET_USER, {
          method: 'GET',
          headers: {
            'satoken': satoken,
          },
        });
        const userResult = await userResponse.json();
        
        if (userResult.code === 200) {
          message.success('登录成功');
          onSuccess(ticket); // 调用成功回调
        } else {
          message.error('获取用户信息失败');
        }
      } else {
        message.error('获取访问令牌失败');
      }
    } catch (error: any) {
      console.error('处理ticket登录失败:', error);
      message.error('登录过程中发生错误');
    }
  };

  // 密码登录
  const handlePasswordLogin = async (values: any) => {
    setLoading(true);
    try {
      // 第一步：调用密码登录接口获取ticket
      const loginResponse = await fetch(buildApiUrl(API_ENDPOINTS.LEGACY.AUTH_PASSWORD_LOGIN), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          username: values.username,
          pwd: publicKeyLoaded ? encryptWithSM2(values.password) : values.password,
        }),
      });
      const loginResult = await loginResponse.json();

      if (loginResult.code === 200) {
        const ticket = loginResult.data?.ticket;
        if (ticket) {
          // 第二步：使用ticket获取satoken
          await handleTicketLogin(ticket);
        } else {
          message.error('登录成功但未获取到有效凭证');
        }
      } else {
        message.error(loginResult.message || '登录失败');
      }
    } catch (error: any) {
      console.error('密码登录失败:', error);
      message.error('登录失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    form.resetFields();
    passwordForm.resetFields();
    setSmsCountdown(0);
    onCancel();
  };

  // 处理用户注册
  const handleRegister = async () => {
    const phone = form.getFieldValue('phone');
    const smsCode = form.getFieldValue('smsCode');
    
    if (!phone) {
      message.error('请先输入手机号');
      return;
    }
    
    if (!smsCode) {
      message.error('请先输入验证码');
      return;
    }

    setLoading(true);
    try {
      const registerResponse = await fetch(buildApiUrl(API_ENDPOINTS.LEGACY.AUTH_REGISTER), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          username: phone, // 使用手机号作为用户名
          userPwd: phone.slice(-6), // 使用手机号后6位作为初始密码，用户可后续修改
          phone: phone,
          phoneCode: smsCode,
        }),
      });
      const registerResult = await registerResponse.json();

      if (registerResult.code === 200) {
        message.success('注册成功！');
        const ticket = registerResult.data?.ticket;
        if (ticket) {
          await handleTicketLogin(ticket);
        } else {
          message.error('注册成功但未获取到有效凭证');
        }
      } else {
        message.error(registerResult.message || '注册失败');
      }
    } catch (error: any) {
      console.error('用户注册失败:', error);
      message.error('注册失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title={null}
      open={visible}
      onCancel={handleCancel}
      footer={null}
      width={400}
      centered
      destroyOnClose
      className={styles.loginModal}
      styles={{
        content: {
          padding: 0,
          background: '#525252',
          borderRadius: '12px',
          border: 'none',
        },
        body: {
          padding: '40px 32px 32px',
        }
      }}
      closeIcon={
        <span style={{ 
          color: '#fff', 
          fontSize: '18px',
          fontWeight: 'bold',
          width: '24px',
          height: '24px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center'
        }}>×</span>
      }
    >
      <div style={{ textAlign: 'center', marginBottom: '32px' }}>
        <h2 style={{ 
          color: '#fff', 
          fontSize: '24px', 
          fontWeight: 'normal', 
          margin: '0 0 16px 0',
          fontFamily: 'PingFang SC, Microsoft YaHei, sans-serif'
        }}>
          欢迎来到
        </h2>
        <h1 style={{ 
          color: '#fff', 
          fontSize: '32px', 
          fontWeight: 'bold', 
          margin: '0',
          fontFamily: 'PingFang SC, Microsoft YaHei, sans-serif'
        }}>
          Super Agent
        </h1>
      </div>

      <Tabs 
        defaultActiveKey="sms" 
        centered
        style={{
          marginBottom: '24px'
        }}
        tabBarStyle={{
          border: 'none',
          marginBottom: 0,
        }}
      >
        <TabPane 
          tab={<span style={{ 
            color: '#fff', 
            fontSize: '16px',
            fontWeight: '500'
          }}>短信登录</span>} 
          key="sms"
        >
          <Form
            form={form}
            onFinish={handlePhoneLogin}
            layout="vertical"
            size="large"
            style={{ marginTop: '24px' }}
          >
            <Form.Item
              name="phone"
              rules={[
                { required: true, message: '请输入手机号' },
                { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号' },
              ]}
            >
              <Input
                prefix={<PhoneOutlined style={{ color: '#888' }} />}
                placeholder="请输入手机号"
                style={{
                  backgroundColor: '#404040',
                  border: '1px solid #555',
                  color: '#fff',
                  height: '48px',
                  fontSize: '16px',
                  borderRadius: '8px',
                }}
              />
            </Form.Item>

            <Form.Item
              name="smsCode"
              rules={[
                { required: true, message: '请输入验证码' },
                { len: 6, message: '验证码为6位数字' },
              ]}
            >
              <Input
                prefix={<SafetyCertificateOutlined style={{ color: '#888' }} />}
                placeholder="请输入验证码"
                suffix={
                  <Button
                    type="link"
                    onClick={handleSendSms}
                    disabled={smsCountdown > 0 || smsLoading}
                    loading={smsLoading}
                    style={{
                      color: smsCountdown > 0 || smsLoading ? '#888' : '#1890ff',
                      fontSize: '14px',
                      padding: '0',
                    }}
                  >
                    {smsCountdown > 0 ? `${smsCountdown}s后重发` : '获取验证码'}
                  </Button>
                }
                style={{
                  backgroundColor: '#404040',
                  border: '1px solid #555',
                  color: '#fff',
                  height: '48px',
                  fontSize: '16px',
                  borderRadius: '8px',
                }}
              />
            </Form.Item>

            <Form.Item style={{ marginBottom: '16px' }}>
              <Button
                type="primary"
                htmlType="submit"
                loading={loading}
                block
                style={{
                  height: '48px',
                  fontSize: '16px',
                  fontWeight: 'bold',
                  backgroundColor: '#1890ff',
                  border: 'none',
                  borderRadius: '8px',
                  boxShadow: '0 2px 8px rgba(24, 144, 255, 0.3)',
                }}
              >
                登录/注册
              </Button>
            </Form.Item>
          </Form>
        </TabPane>

        <TabPane 
          tab={<span style={{ 
            color: '#fff', 
            fontSize: '16px',
            fontWeight: '500'
          }}>账号登录</span>} 
          key="password"
        >
          <Form
            form={passwordForm}
            onFinish={handlePasswordLogin}
            layout="vertical"
            size="large"
            style={{ marginTop: '24px' }}
          >
            <Form.Item
              name="username"
              rules={[{ required: true, message: '请输入用户名' }]}
            >
              <Input
                prefix={<UserOutlined style={{ color: '#888' }} />}
                placeholder="请输入用户名"
                style={{
                  backgroundColor: '#404040',
                  border: '1px solid #555',
                  color: '#fff',
                  height: '48px',
                  fontSize: '16px',
                  borderRadius: '8px',
                }}
              />
            </Form.Item>

            <Form.Item
              name="password"
              rules={[{ required: true, message: '请输入密码' }]}
            >
              <Input.Password
                prefix={<LockOutlined style={{ color: '#888' }} />}
                placeholder="请输入密码"
                style={{
                  backgroundColor: '#404040',
                  border: '1px solid #555',
                  color: '#fff',
                  height: '48px',
                  fontSize: '16px',
                  borderRadius: '8px',
                }}
              />
            </Form.Item>

            {/* 密码登录提示 */}
            {publicKeyError && (
              <div style={{ 
                color: '#ff7875', 
                fontSize: '12px', 
                marginBottom: '16px',
                textAlign: 'center'
              }}>
                ⚠️ {publicKeyError}
              </div>
            )}

            {!publicKeyLoaded && !publicKeyError && (
              <div style={{ 
                color: '#1890ff', 
                fontSize: '12px', 
                marginBottom: '16px',
                textAlign: 'center'
              }}>
                🔐 正在初始化加密环境...
              </div>
            )}

            <Form.Item style={{ marginBottom: '16px' }}>
              <Button
                type="primary"
                htmlType="submit"
                loading={loading}
                block
                style={{
                  height: '48px',
                  fontSize: '16px',
                  fontWeight: 'bold',
                  backgroundColor: '#1890ff',
                  border: 'none',
                  borderRadius: '8px',
                  boxShadow: '0 2px 8px rgba(24, 144, 255, 0.3)',
                }}
              >
                登录
              </Button>
            </Form.Item>
          </Form>
        </TabPane>
      </Tabs>

      {/* 底部链接和注册按钮 */}
      <div style={{ 
        textAlign: 'center', 
        marginTop: '24px',
      }}>
        {/* 注册按钮 */}
        <Button 
          type="link" 
          onClick={handleRegister}
          style={{ 
            color: '#1890ff', 
            padding: '0',
            height: 'auto',
            marginBottom: '16px'
          }}
        >
          还没有账号？点击注册
        </Button>
        
        {/* 条款链接 */}
        <div style={{ 
          fontSize: '12px',
          color: '#aaa'
        }}>
          注册登录即代表同意{' '}
          <a href="#" style={{ color: '#1890ff', textDecoration: 'none' }}>
            《服务条款》
          </a>{' '}
          和{' '}
          <a href="#" style={{ color: '#1890ff', textDecoration: 'none' }}>
            《隐私政策》
          </a>
        </div>
      </div>
    </Modal>
  );
};

export default PhoneLoginModal;
