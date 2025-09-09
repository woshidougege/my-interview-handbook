import React, { useState, useEffect } from 'react';
import {
  Card,
  Row,
  Col,
  Statistic,
  Table,
  Tag,
  Avatar,
  Space,
  Typography,
  Spin,
  message,
} from 'antd';
import {
  ThunderboltOutlined,
  ExclamationCircleOutlined,
  TrophyOutlined,
  UserOutlined
} from '@ant-design/icons';
import { UserInfo, UserCredit, CreditTransaction } from '@/types/user';
import { userApi, creditApi } from '@/services/api';
import { formatDate, formatNumber } from '@/utils/format';

const { Title, Text } = Typography;

interface AccountOverviewProps {
  userId?: string;
}

const AccountOverview: React.FC<AccountOverviewProps> = ({ userId = '1001' }) => {
  const [loading, setLoading] = useState(true);
  const [userInfo, setUserInfo] = useState<UserInfo | null>(null);
  const [creditInfo, setCreditInfo] = useState<UserCredit | null>(null);
  const [transactions, setTransactions] = useState<CreditTransaction[]>([]);

  useEffect(() => {
    loadData();
  }, [userId]);

  const loadData = async () => {
    try {
      setLoading(true);
      
      // 获取当前用户信息
      const userResponse = await userApi.getCurrentUser();
      const user = userResponse.data.data;
      setUserInfo(user);
      
      // 获取用户积分信息
      const creditResponse = await creditApi.getUserCredit(user.id);
      const credit = creditResponse.data.data;
      setCreditInfo(credit);
      
      // 获取积分交易记录
      const transactionResponse = await creditApi.getCreditTransactions(user.id, 1, 10);
      const transactionList = transactionResponse.data.data.records || [];
      setTransactions(transactionList);
      
    } catch (error: any) {
      message.error('加载数据失败: ' + (error.message || '未知错误'));
      console.error('Failed to load data:', error);
    } finally {
      setLoading(false);
    }
  };

  // 积分交易记录表格列定义
  const transactionColumns = [
    {
      title: '交易时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 150,
      render: (time: string) => (
        <Text type="secondary">{formatDate(time)}</Text>
      ),
    },
    {
      title: '交易类型',
      dataIndex: 'transactionTypeDesc',
      key: 'transactionTypeDesc',
      width: 120,
      render: (desc: string, record: CreditTransaction) => {
        const isIncome = record.amount > 0;
        return (
          <Tag color={isIncome ? 'green' : 'red'}>
            {desc}
          </Tag>
        );
      },
    },
    {
      title: '积分变动',
      dataIndex: 'amount',
      key: 'amount',
      width: 100,
      render: (amount: number) => (
        <Text strong style={{ color: amount > 0 ? '#52c41a' : '#ff4d4f' }}>
          {amount > 0 ? '+' : ''}{formatNumber(amount)}
        </Text>
      ),
    },
    {
      title: '余额',
      dataIndex: 'balanceAfter',
      key: 'balanceAfter',
      width: 100,
      render: (balance: number) => (
        <Text>{formatNumber(balance)}</Text>
      ),
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
    },
  ];

  if (loading) {
    return (
      <div style={{ padding: '50px', textAlign: 'center' }}>
        <Spin size="large" />
      </div>
    );
  }

  return (
    <div style={{ padding: '24px' }}>
      {/* 页面标题 */}
      <div style={{ marginBottom: '24px' }}>
        <Title level={2} style={{ margin: 0 }}>
          账户总览
        </Title>
        <Text type="secondary">查看您的账户信息和积分使用情况</Text>
      </div>

      {/* 用户信息卡片 */}
      <Row gutter={[16, 16]} style={{ marginBottom: '24px' }}>
        <Col span={24}>
          <Card>
            <Row align="middle">
              <Col>
                <Avatar 
                  size={64} 
                  icon={<UserOutlined />}
                />
              </Col>
              <Col style={{ marginLeft: '16px' }}>
                <Title level={4} style={{ margin: 0 }}>
                  {userInfo?.username || '用户'}
                </Title>
                <Space direction="vertical" size={4}>
                  <Text type="secondary">手机号: {userInfo?.phone}</Text>
                  <Text type="secondary">状态: {userInfo?.statusDesc}</Text>
                </Space>
              </Col>
            </Row>
          </Card>
        </Col>
      </Row>

      {/* 积分统计卡片 */}
      <Row gutter={[16, 16]} style={{ marginBottom: '24px' }}>
        <Col xs={24} sm={8}>
          <Card>
            <Statistic
              title="积分余额"
              value={creditInfo?.totalBalance || 0}
              prefix={<ThunderboltOutlined style={{ color: '#faad14' }} />}
              valueStyle={{ color: '#faad14' }}
              formatter={(value) => formatNumber(Number(value))}
            />
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card>
            <Statistic
              title="累计充值（元）"
              value={(creditInfo?.totalEarned || 0) * 0.01}
              prefix={<TrophyOutlined style={{ color: '#52c41a' }} />}
              valueStyle={{ color: '#52c41a' }}
              precision={2}
              formatter={(value) => `¥${formatNumber(Number(value))}`}
            />
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card>
            <Statistic
              title="总消费积分"
              value={creditInfo?.totalSpent || 0}
              prefix={<ThunderboltOutlined style={{ color: '#ff4d4f' }} />}
              valueStyle={{ color: '#ff4d4f' }}
              formatter={(value) => formatNumber(Number(value))}
            />
          </Card>
        </Col>
      </Row>


      {/* 积分交易记录 */}
      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Card 
            title="积分交易记录" 
            extra={
              <Space>
                <Text type="secondary">最近10条记录</Text>
              </Space>
            }
          >
            <Table
              columns={transactionColumns}
              dataSource={transactions}
              rowKey="id"
              pagination={{
                pageSize: 10,
                showSizeChanger: true,
                showQuickJumper: true,
                showTotal: (total) => `共 ${total} 条记录`,
              }}
              size="middle"
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default AccountOverview;
