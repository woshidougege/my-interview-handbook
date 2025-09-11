import React, { useState, useEffect } from 'react';
import Head from 'next/head';
import { Form, Input, Button, Card, message, Spin, Row, Col } from 'antd';
import { UserOutlined, LockOutlined, PhoneOutlined } from '@ant-design/icons';
import { useRouter } from 'next/router';
import { authApi } from '../services/api';
import { encryptPassword } from '../utils/sm2Encrypt';
import { getPublicKey, getCachedPublicKey } from '../services/publicKeyService';

const RegisterPage: React.FC = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [publicKeyLoaded, setPublicKeyLoaded] = useState(false);
  const router = useRouter();

  useEffect(() => {
    // 获取SM2公钥
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
      } catch (error) {
        message.error('获取加密公钥失败');
      }
    };

    initPublicKey();
  }, []);

  const onFinish = async (values: any) => {
    try {
      setLoading(true);
      
      // 加密密码
      let encryptedPassword;
      try {
        encryptedPassword = encryptPassword(values.password);
      } catch (encryptError: any) {
        message.error('密码加密失败，请联系管理员');
        setLoading(false);
        return;
      }
      
      // 准备注册数据
      const registerData = {
        username: values.username,
        password: encryptedPassword,
        phone: values.phone,
      };

      // 调用注册接口
      const response = await authApi.register(registerData);
      
      if (response.data.success) {
        message.success('注册成功');
        router.push('/login');
      } else {
        message.error(response.data.message || '注册失败');
      }
    } catch (error: any) {
      message.error(error.response?.data?.message || '注册失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <Head>
        <title>用户注册 - Super Agent</title>
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
          title="用户注册" 
          style={{ width: 400, boxShadow: '0 4px 12px rgba(0,0,0,0.15)' }}
        >
          <Spin spinning={loading}>
            <Form
              form={form}
              name="register"
              onFinish={onFinish}
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

              <Form.Item
                name="confirmPassword"
                label="确认密码"
                dependencies={['password']}
                rules={[
                  { required: true, message: '请确认密码' },
                  ({ getFieldValue }) => ({
                    validator(_, value) {
                      if (!value || getFieldValue('password') === value) {
                        return Promise.resolve();
                      }
                      return Promise.reject(new Error('两次输入的密码不一致'));
                    },
                  }),
                ]}
              >
                <Input.Password prefix={<LockOutlined />} placeholder="请确认密码" />
              </Form.Item>

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

              <Form.Item>
                <Button 
                  type="primary" 
                  htmlType="submit" 
                  style={{ width: '100%' }}
                  disabled={!publicKeyLoaded}
                >
                  {publicKeyLoaded ? '注册' : '正在加载加密组件...'}
                </Button>
              </Form.Item>

              <Row justify="center">
                <Col>
                  <Button type="link" onClick={() => router.push('/login')}>
                    已有账号？立即登录
                  </Button>
                </Col>
              </Row>
            </Form>
          </Spin>
        </Card>
      </div>
    </>
  );
};

export default RegisterPage;