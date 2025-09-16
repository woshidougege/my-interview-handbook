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
  WalletOutlined,
  EyeInvisibleOutlined,
  EyeTwoTone
} from '@ant-design/icons';
import { UserInfo, UserCredit } from '@/types/user';
import { userApi, subscriptionApi, authApi } from '@/services/api';
import { formatNumber } from '@/utils/format';
import CreditTransactionList from './CreditTransactionList';
import { encryptWithSM2 } from '@/utils/sm2Encrypt';
import { getCachedPublicKey } from '@/services/publicKeyService';


interface UserSettingsProps {
  visible: boolean;
  onClose: () => void;
}

const UserSettings: React.FC<UserSettingsProps> = ({ visible, onClose }) => {
  const [form] = Form.useForm();
  const [passwordForm] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [userInfo, setUserInfo] = useState<UserInfo | null>(null);
  const [creditInfo, setCreditInfo] = useState<UserCredit | null>(null);
  const [activeTab, setActiveTab] = useState('profile');
  const [isEditingPassword, setIsEditingPassword] = useState(false);
  const [passwordLoading, setPasswordLoading] = useState(false);


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
        
        const creditInfo: UserCredit = {
          userId: user.id || '',
          availableCredits: data.availableCredits || 150000,
          planName: data.planName || '基础版',
          limitedCredits: data.limitedCredits || 800,
          dailyRefreshCredits: data.dailyRefreshCredits || 300,
          hasCreditAccount: data.hasCreditAccount || false,
          totalBalance: data.totalBalance || 0,
          freeBalance: data.freeBalance || 0,
          subscriptionBalance: data.subscriptionBalance || 0,
          permanentBalance: data.permanentBalance || 0,
          totalEarned: data.totalEarned || 0,
          totalSpent: data.totalSpent || 0,
          createTime: data.createTime || new Date().toISOString(),
          updateTime: data.updateTime || new Date().toISOString()
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

  // 处理密码修改
  const handleChangePassword = async (values: any) => {
    try {
      setPasswordLoading(true);
      
      // SM2加密新密码
      let encryptedPassword = values.newPassword;
      
      if (getCachedPublicKey()) {
        try {
          encryptedPassword = encryptWithSM2(values.newPassword);
        } catch (encryptError) {
          console.warn('SM2加密失败，使用明文密码:', encryptError);
        }
      } else {
        console.warn('未找到缓存公钥，使用明文密码');
      }

      // 调用后端API修改密码
      const response = await authApi.changePasswordSSO({
        newPassword: encryptedPassword
      });
      message.success('密码修改成功');
      setIsEditingPassword(false);
      passwordForm.resetFields();
    } catch (error: any) {
      console.error('修改密码失败:', error);
      message.error('密码修改失败: ' + (error.message || '未知错误'));
    } finally {
      setPasswordLoading(false);
    }
  };

  // 取消密码修改
  const handleCancelPasswordEdit = () => {
    setIsEditingPassword(false);
    passwordForm.resetFields();
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
      width={800}
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
        <div style={{ display: 'flex', height: '600px' }}>
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

                {/* 密码修改区域 - 独立于主表单 */}
                <div style={{ marginBottom: '12px', marginTop: '16px' }}>
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
                      onClick={() => {
                        setIsEditingPassword(true);
                      }}
                    />
                  </div>
                  
                  {!isEditingPassword ? (
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
                  ) : (
                    <Form
                      form={passwordForm}
                      onFinish={(values) => {
                        handleChangePassword(values);
                      }}
                      layout="vertical"
                      style={{ margin: 0 }}
                    >
                      <Form.Item
                        name="newPassword"
                        rules={[
                          { required: true, message: '请输入新密码' },
                          { min: 6, message: '密码长度至少6位' }
                        ]}
                        style={{ marginBottom: '12px' }}
                      >
                        <Input.Password
                          placeholder="请输入新密码"
                          iconRender={(visible) => (visible ? <EyeTwoTone /> : <EyeInvisibleOutlined />)}
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
                      
                      <Form.Item
                        name="confirmPassword"
                        dependencies={['newPassword']}
                        rules={[
                          { required: true, message: '请确认新密码' },
                          ({ getFieldValue }) => ({
                            validator(_, value) {
                              if (!value || getFieldValue('newPassword') === value) {
                                return Promise.resolve();
                              }
                              return Promise.reject(new Error('两次输入的密码不一致'));
                            },
                          }),
                        ]}
                        style={{ marginBottom: '12px' }}
                      >
                        <Input.Password
                          placeholder="请确认新密码"
                          iconRender={(visible) => (visible ? <EyeTwoTone /> : <EyeInvisibleOutlined />)}
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
                      
                      <div style={{ display: 'flex', gap: '8px' }}>
                        <Button 
                          type="primary" 
                          htmlType="submit" 
                          loading={passwordLoading}
                          size="small"
                          style={{ 
                            background: '#1890ff',
                            borderColor: '#1890ff',
                            height: '32px',
                            fontSize: '12px'
                          }}
                        >
                          确认
                        </Button>
                        <Button 
                          onClick={() => {
                            handleCancelPasswordEdit();
                          }}
                          size="small"
                          style={{ 
                            background: '#fff',
                            borderColor: '#d9d9d9',
                            color: '#333',
                            height: '32px',
                            fontSize: '12px'
                          }}
                        >
                          取消
                        </Button>
                      </div>
                    </Form>
                  )}
                </div>
              </div>
            )}

            {activeTab === 'billing' && (
              <div style={{ 
                background: 'rgba(51, 51, 51, 0.9)',
                color: '#fff',
                padding: '20px',
                borderRadius: '8px',
                marginTop: '-16px',
                marginLeft: '-24px',
                marginRight: '-24px',
                marginBottom: '-16px',
                minHeight: '400px'
              }}>
                {/* 套餐用户信息 */}
                <div style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  marginBottom: '20px',
                  padding: '16px 20px',
                  background: 'rgba(255, 255, 255, 0.1)',
                  borderRadius: '8px'
                }}>
                  <span style={{ 
                    fontSize: '16px', 
                    fontWeight: 500,
                    color: '#fff'
                  }}>
                    {creditInfo?.planName || '基础版'}用户
                  </span>
                  <Button
                    type="primary"
                    size="small"
                    style={{
                      background: '#1890ff',
                      borderColor: '#1890ff',
                      fontSize: '12px',
                      padding: '4px 16px',
                      height: '28px',
                      borderRadius: '14px',
                      fontWeight: 500
                    }}
                  >
                    升级
                  </Button>
                </div>

                {/* 积分信息区域 */}
                <div style={{ marginBottom: '20px' }}>
                  {/* 全部积分 */}
                  <div style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    marginBottom: '12px',
                    padding: '0 4px'
                  }}>
                    <div>
                      <span style={{ 
                        color: '#ccc', 
                        fontSize: '14px',
                        marginRight: '20px' 
                      }}>
                        全部积分
                      </span>
                      <span style={{ 
                        color: '#fff', 
                        fontSize: '16px',
                        fontWeight: 600 
                      }}>
                        {formatNumber(creditInfo?.availableCredits || 150000)}
                      </span>
                    </div>
                    <Button
                      size="small"
                      style={{
                        background: 'rgba(255, 255, 255, 0.2)',
                        borderColor: 'rgba(255, 255, 255, 0.3)',
                        color: '#fff',
                        fontSize: '12px',
                        padding: '4px 12px',
                        height: '24px',
                        borderRadius: '12px'
                      }}
                    >
                      赠充
                    </Button>
                  </div>

                  {/* 限时积分 */}
                  <div style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    marginBottom: '12px',
                    padding: '0 4px'
                  }}>
                    <div>
                      <span style={{ 
                        color: '#ccc', 
                        fontSize: '14px',
                        marginRight: '20px' 
                      }}>
                        限时积分
                      </span>
                      <span style={{ 
                        color: '#fff', 
                        fontSize: '16px',
                        fontWeight: 600 
                      }}>
                        {formatNumber(creditInfo?.limitedCredits || 800)}
                      </span>
                    </div>
                    <Button
                      size="small"
                      style={{
                        background: 'rgba(255, 255, 255, 0.2)',
                        borderColor: 'rgba(255, 255, 255, 0.3)',
                        color: '#fff',
                        fontSize: '12px',
                        padding: '4px 12px',
                        height: '24px',
                        borderRadius: '12px'
                      }}
                    >
                      分享链接
                    </Button>
                  </div>

                  {/* 当日刷新积分 */}
                  <div style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    marginBottom: '20px',
                    padding: '0 4px'
                  }}>
                    <div>
                      <span style={{ 
                        color: '#ccc', 
                        fontSize: '14px',
                        marginRight: '20px'
                      }}>
                        当日刷新积分
                      </span>
                      <span style={{ 
                        color: '#fff', 
                        fontSize: '16px',
                        fontWeight: 600 
                      }}>
                        {formatNumber(creditInfo?.dailyRefreshCredits || 300)}
                      </span>
                    </div>
                  </div>
                </div>

                {/* 积分详情列表 */}
                <div style={{
                  background: 'rgba(255, 255, 255, 0.05)',
                  borderRadius: '8px',
                  padding: '16px',
                  marginTop: '16px'
                }}>
                  <CreditTransactionList />
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
