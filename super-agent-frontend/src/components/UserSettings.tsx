import React, { useState, useEffect } from 'react';
import {
  Modal,
  Avatar,
  Form,
  Input,
  Button,
  message
} from 'antd';
import {
  UserOutlined,
  EditOutlined,
  WalletOutlined
} from '@ant-design/icons';
import { UserInfo, UserCredit } from '@/types/user';
import { userApi, subscriptionApi } from '@/services/api';
import { formatNumber } from '@/utils/format';


interface UserSettingsProps {
  visible: boolean;
  onClose: () => void;
}

const UserSettings: React.FC<UserSettingsProps> = ({ visible, onClose }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [userInfo, setUserInfo] = useState<UserInfo | null>(null);
  const [creditInfo, setCreditInfo] = useState<UserCredit | null>(null);
  const [activeTab, setActiveTab] = useState('profile');

  useEffect(() => {
    if (visible) {
      loadUserData();
    }
  }, [visible]);

  const loadUserData = async () => {
    try {
      setLoading(true);
      
      // 获取用户信息
      const userResponse = await userApi.getCurrentUser();
      const user = userResponse.data.data;
      setUserInfo(user);
      
      // 获取积分信息（从订阅接口）
      try {
        const subscriptionResponse = await subscriptionApi.getCurrentSubscription();
        const data = subscriptionResponse.data.data;
        
        const creditInfo = {
          availableCredits: data.availableCredits || 0,
          hasCreditAccount: data.hasCreditAccount || false
        };
        setCreditInfo(creditInfo);
      } catch (error) {
        console.log('获取积分信息失败:', error);
      }
      
      // 填充表单
      form.setFieldsValue({
        username: user.username,
        phone: user.phone,
      });
      
    } catch (error: any) {
      message.error('加载用户数据失败: ' + (error.message || '未知错误'));
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async (values: any) => {
    try {
      setLoading(true);
      // TODO: 调用更新用户信息的API
      message.success('保存成功');
      onClose();
    } catch (error: any) {
      message.error('保存失败: ' + (error.message || '未知错误'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title={
        <span style={{ 
          color: '#fff', 
          fontSize: '18px', 
          fontWeight: 600 
        }}>
          设置
        </span>
      }
      open={visible}
      onCancel={onClose}
      footer={null}
      width={700}
      centered
      styles={{
        content: { 
          background: '#fff',
          borderRadius: '12px',
          overflow: 'hidden',
          boxShadow: '0 4px 16px rgba(0, 0, 0, 0.08)'
        },
        header: {
          background: '#fff',
          borderBottom: '1px solid #f0f0f0',
          color: '#333',
          padding: '20px 24px'
        },
        body: { 
          padding: '0',
          background: '#fff'
        }
      }}
    >
      <div style={{ background: 'transparent', color: '#333' }}>
        {/* 侧边导航栏 */}
        <div style={{ display: 'flex', height: '400px' }}>
          <div style={{ 
            width: '140px', 
            background: '#fafafa',
            padding: '16px 0',
            borderRight: '1px solid #f0f0f0'
          }}>
            <div 
              style={{ 
                padding: '12px 16px',
                cursor: 'pointer',
                background: activeTab === 'profile' ? '#1890ff' : 'transparent',
                color: activeTab === 'profile' ? '#fff' : '#666',
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                fontSize: '14px',
                fontWeight: activeTab === 'profile' ? 500 : 400,
                transition: 'all 0.2s ease',
                borderRadius: '6px',
                margin: '0 12px 6px 12px'
              }}
              onClick={() => setActiveTab('profile')}
            >
              <UserOutlined style={{ fontSize: '16px' }} />
              个人资料
            </div>
            <div 
              style={{ 
                padding: '12px 16px',
                cursor: 'pointer',
                background: activeTab === 'billing' ? '#1890ff' : 'transparent',
                color: activeTab === 'billing' ? '#fff' : '#666',
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                fontSize: '14px',
                fontWeight: activeTab === 'billing' ? 500 : 400,
                transition: 'all 0.2s ease',
                borderRadius: '6px',
                margin: '0 12px 6px 12px'
              }}
              onClick={() => setActiveTab('billing')}
            >
              <WalletOutlined style={{ fontSize: '16px' }} />
              计费
            </div>
          </div>

          {/* 内容区域 */}
          <div style={{ flex: 1, padding: '16px 24px', background: '#fff' }}>
            {activeTab === 'profile' && (
              <div>
                {/* 用户头像区域 */}
                <div style={{ textAlign: 'center', marginBottom: '12px' }}>
                  <Avatar 
                    size={48} 
                    style={{ 
                      backgroundColor: '#1890ff',
                      color: '#fff',
                      fontSize: '16px'
                    }}
                  >
                    {userInfo?.username?.substring(0, 2) || 'SU'}
                  </Avatar>
                </div>

                {/* 用户信息表单 */}
                <Form
                  form={form}
                  layout="vertical"
                  onFinish={handleSave}
                >
                  <Form.Item
                    label={<span style={{ color: '#333', fontSize: '14px', fontWeight: 500 }}>用户名</span>}
                    name="username"
                    rules={[
                      { required: true, message: '请输入用户名' },
                      { min: 2, max: 20, message: '用户名长度为2-20个字符' }
                    ]}
                    style={{ marginBottom: '12px' }}
                  >
                    <Input 
                      placeholder="请输入用户名"
                      style={{
                        background: '#fff',
                        border: '1px solid #d9d9d9',
                        color: '#333',
                        fontSize: '14px',
                        padding: '8px 12px',
                        borderRadius: '6px',
                        height: '40px'
                      }}
                    />
                  </Form.Item>

                  <div style={{ marginBottom: '12px' }}>
                    <div style={{ marginBottom: '8px', color: '#333', fontSize: '14px', fontWeight: 500 }}>手机号</div>
                    <div style={{ 
                      background: '#f5f5f5',
                      color: '#666',
                      fontSize: '14px',
                      padding: '8px 12px',
                      borderRadius: '6px',
                      height: '40px',
                      display: 'flex',
                      alignItems: 'center',
                      border: '1px solid #e8e8e8'
                    }}>
                      {userInfo?.phone || '18888888800'}
                    </div>
                  </div>

                  <div style={{ marginBottom: '12px' }}>
                    <div style={{ 
                      display: 'flex', 
                      alignItems: 'center', 
                      justifyContent: 'space-between',
                      marginBottom: '8px'
                    }}>
                      <span style={{ color: '#333', fontSize: '14px', fontWeight: 500 }}>密码</span>
                      <EditOutlined 
                        style={{ 
                          color: '#1890ff', 
                          cursor: 'pointer',
                          fontSize: '14px'
                        }}
                        onClick={() => message.info('密码修改功能开发中')}
                      />
                    </div>
                    <div style={{ 
                      color: '#666', 
                      fontSize: '14px',
                      background: '#f5f5f5',
                      padding: '8px 12px',
                      borderRadius: '6px',
                      height: '40px',
                      display: 'flex',
                      alignItems: 'center',
                      border: '1px solid #e8e8e8'
                    }}>
                      ******
                    </div>
                  </div>

                  <Form.Item style={{ marginTop: '16px', marginBottom: '0' }}>
                    <div style={{ display: 'flex', gap: '12px' }}>
                      <Button 
                        type="primary" 
                        htmlType="submit" 
                        loading={loading}
                        size="large"
                        style={{ 
                          background: '#1890ff',
                          borderColor: '#1890ff',
                          flex: 1,
                          height: '40px',
                          borderRadius: '6px',
                          fontWeight: 500,
                          fontSize: '14px'
                        }}
                      >
                        保存修改
                      </Button>
                      <Button 
                        onClick={onClose}
                        size="large"
                        style={{ 
                          background: '#fff',
                          borderColor: '#d9d9d9',
                          color: '#333',
                          flex: 1,
                          height: '40px',
                          borderRadius: '6px',
                          fontWeight: 500,
                          fontSize: '14px'
                        }}
                      >
                        取消
                      </Button>
                    </div>
                  </Form.Item>
                </Form>
              </div>
            )}

            {activeTab === 'billing' && (
              <div style={{ textAlign: 'center' }}>
                <div style={{ 
                  background: 'linear-gradient(135deg, #1890ff 0%, #40a9ff 100%)',
                  padding: '20px', 
                  borderRadius: '8px',
                  marginBottom: '16px',
                  boxShadow: '0 4px 16px rgba(24, 144, 255, 0.15)'
                }}>
                  <div style={{ 
                    fontSize: '32px', 
                    color: '#fff', 
                    marginBottom: '4px',
                    fontWeight: 600
                  }}>
                    {formatNumber(creditInfo?.totalBalance || 1094)}
                  </div>
                  <div style={{ 
                    color: '#fff', 
                    fontSize: '16px',
                    opacity: 0.9
                  }}>
                    当前积分余额
                  </div>
                </div>

                <div style={{ 
                  display: 'flex', 
                  justifyContent: 'space-between',
                  marginBottom: '16px',
                  gap: '12px'
                }}>
                  <div style={{ 
                    flex: 1,
                    background: '#fff',
                    padding: '12px',
                    borderRadius: '8px',
                    border: '1px solid #f0f0f0',
                    boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)'
                  }}>
                    <div style={{ color: '#666', fontSize: '12px', marginBottom: '6px' }}>
                      累计获得
                    </div>
                    <div style={{ 
                      color: '#52c41a', 
                      fontSize: '18px',
                      fontWeight: 600
                    }}>
                      +{formatNumber(creditInfo?.totalEarned || 0)}
                    </div>
                  </div>
                  <div style={{ 
                    flex: 1,
                    background: '#fff',
                    padding: '12px',
                    borderRadius: '8px',
                    border: '1px solid #f0f0f0',
                    boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)'
                  }}>
                    <div style={{ color: '#666', fontSize: '12px', marginBottom: '6px' }}>
                      累计消费
                    </div>
                    <div style={{ 
                      color: '#ff4d4f', 
                      fontSize: '18px',
                      fontWeight: 600
                    }}>
                      -{formatNumber(creditInfo?.totalSpent || 0)}
                    </div>
                  </div>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  <Button 
                    type="primary" 
                    block 
                    size="large"
                    style={{ 
                      background: '#1890ff',
                      borderColor: '#1890ff',
                      height: '40px',
                      borderRadius: '8px',
                      fontSize: '14px',
                      fontWeight: 500
                    }}
                  >
                    充值积分
                  </Button>
                  <Button 
                    block 
                    size="large"
                    style={{ 
                      background: '#fff',
                      borderColor: '#d9d9d9',
                      color: '#333',
                      height: '40px',
                      borderRadius: '8px',
                      fontSize: '14px',
                      fontWeight: 500
                    }}
                  >
                    查看交易记录
                  </Button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </Modal>
  );
};

export default UserSettings;
