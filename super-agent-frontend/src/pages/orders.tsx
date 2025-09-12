import React, { useState, useEffect, useCallback } from 'react';
import { Table, Tag, Button, Card, message, Modal, Input, Form, Space, Tooltip } from 'antd';
import { ColumnsType } from 'antd/es/table';
import Layout from '@/components/Layout/AppLayout';
import { paymentApi, userApi } from '@/services/api';
import format from '@/utils/format';
import { PaymentSSEListener } from '@/utils/paymentSSE';
import { OrderRecord, PaymentStatusType, PaymentStatusEvent } from '@/types/user';

interface RefundModalProps {
  visible: boolean;
  order: OrderRecord | null;
  onClose: () => void;
  onSuccess: () => void;
}

const RefundModal: React.FC<RefundModalProps> = ({ visible, order, onClose, onSuccess }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  const handleRefund = async (values: { refundAmount: number; refundReason: string }) => {
    if (!order) return;

    setLoading(true);
    try {
      await paymentApi.applyRefund({
        orderNo: order.orderNo,
        refundAmount: values.refundAmount,
        refundReason: values.refundReason,
      });
      
      message.success('退款申请提交成功！');
      form.resetFields();
      onClose();
      onSuccess();
    } catch (error: any) {
      message.error(error.message || '退款申请失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title="申请退款"
      visible={visible}
      onCancel={onClose}
      footer={null}
      width={500}
    >
      {order && (
        <div>
          <div className="mb-4 p-4 bg-gray-50 rounded">
            <p><strong>订单号：</strong>{order.orderNo}</p>
            <p><strong>订单金额：</strong>¥{order.amount.toFixed(2)}</p>
            <p><strong>当前状态：</strong>{getStatusTag(order.status)}</p>
          </div>
          
          <Form
            form={form}
            layout="vertical"
            onFinish={handleRefund}
            initialValues={{
              refundAmount: order.amount,
              refundReason: '',
            }}
          >
            <Form.Item
              label="退款金额"
              name="refundAmount"
              rules={[
                { required: true, message: '请输入退款金额' },
                { type: 'number', min: 0.01, max: order.amount, message: `退款金额必须在 0.01 - ${order.amount} 之间` },
              ]}
            >
              <Input
                type="number"
                step="0.01"
                prefix="¥"
                placeholder="请输入退款金额"
              />
            </Form.Item>
            
            <Form.Item
              label="退款原因"
              name="refundReason"
              rules={[{ required: true, message: '请输入退款原因' }]}
            >
              <Input.TextArea
                rows={3}
                placeholder="请详细说明退款原因"
                maxLength={200}
                showCount
              />
            </Form.Item>
            
            <Form.Item className="mb-0 text-right">
              <Space>
                <Button onClick={onClose}>取消</Button>
                <Button type="primary" htmlType="submit" loading={loading}>
                  提交退款申请
                </Button>
              </Space>
            </Form.Item>
          </Form>
        </div>
      )}
    </Modal>
  );
};

const getStatusColor = (status: PaymentStatusType): string => {
  switch (status) {
    case 'waiting':
    case 'pending':
      return 'orange';
    case 'paid':
      return 'green';
    case 'failed':
    case 'cancelled':
      return 'red';
    case 'expired':
      return 'default';
    case 'refund_processing':
      return 'blue';
    case 'refund_success':
      return 'cyan';
    case 'refund_fail':
    case 'refund_abnormal':
      return 'red';
    case 'refund_closed':
      return 'default';
    default:
      return 'default';
  }
};

const getStatusTag = (status: PaymentStatusType): React.ReactNode => {
  const statusMap: Record<PaymentStatusType, string> = {
    waiting: '等待支付',
    pending: '等待支付',
    paid: '支付成功',
    failed: '支付失败',
    cancelled: '已取消',
    expired: '已过期',
    refund_processing: '退款处理中',
    refund_success: '退款成功',
    refund_fail: '退款失败',
    refund_closed: '退款关闭',
    refund_abnormal: '退款异常',
  };

  return (
    <Tag color={getStatusColor(status)}>
      {statusMap[status] || status}
    </Tag>
  );
};

const Orders: React.FC = () => {
  const [orders, setOrders] = useState<OrderRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [refundModalVisible, setRefundModalVisible] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState<OrderRecord | null>(null);
  const [currentUser, setCurrentUser] = useState<any>(null);
  const [sseListeners, setSseListeners] = useState<Map<string, PaymentSSEListener>>(new Map());

  // 获取当前用户信息
  const fetchCurrentUser = useCallback(async () => {
    try {
      const response = await userApi.getCurrentUser();
      setCurrentUser(response.data.data);
    } catch (error: any) {
      message.error('获取用户信息失败');
    }
  }, []);

  // 获取订单列表
  const fetchOrders = useCallback(async () => {
    if (!currentUser?.id) return;

    setLoading(true);
    try {
      const response = await paymentApi.getUserOrders(currentUser.id);
      setOrders(response.data.data || []);
    } catch (error: any) {
      message.error(error.message || '获取订单列表失败');
    } finally {
      setLoading(false);
    }
  }, [currentUser?.id]);

  // 启动SSE监听（用于退款状态更新）
  const startSSEListening = useCallback((orderNo: string) => {
    // 避免重复监听
    if (sseListeners.has(orderNo)) {
      return;
    }

    const listener = new PaymentSSEListener(orderNo);
    
    listener.startListening({
      onConnection: () => {
        // 连接建立
      },
      
      onPaymentStatus: (event: PaymentStatusEvent) => {
        // 更新对应订单的状态
        setOrders(prev => prev.map(order => 
          order.orderNo === event.orderNo 
            ? { ...order, status: event.status, message: event.message }
            : order
        ));
        
        // 如果是退款成功，显示提示
        if (event.status === 'refund_success') {
          message.success(`订单 ${event.orderNo} 退款成功！`);
        } else if (event.status === 'refund_fail') {
          message.error(`订单 ${event.orderNo} 退款失败：${event.message}`);
        }
      },
      
      onError: () => {
        // 连接错误，移除监听器
        sseListeners.delete(orderNo);
        setSseListeners(new Map(sseListeners));
      },
      
      onClose: () => {
        // 连接关闭，移除监听器
        sseListeners.delete(orderNo);
        setSseListeners(new Map(sseListeners));
      }
    });
    
    // 保存监听器引用
    sseListeners.set(orderNo, listener);
    setSseListeners(new Map(sseListeners));
  }, [sseListeners]);

  // 停止SSE监听
  const stopSSEListening = useCallback((orderNo: string) => {
    const listener = sseListeners.get(orderNo);
    if (listener) {
      listener.stopListening();
      sseListeners.delete(orderNo);
      setSseListeners(new Map(sseListeners));
    }
  }, [sseListeners]);

  // 处理退款申请
  const handleRefund = (order: OrderRecord) => {
    if (order.status !== 'paid') {
      message.warning('只有支付成功的订单才能申请退款');
      return;
    }
    setSelectedOrder(order);
    setRefundModalVisible(true);
  };

  // 退款成功后的处理
  const handleRefundSuccess = useCallback(() => {
    fetchOrders();
    // 为退款中的订单启动SSE监听
    if (selectedOrder) {
      startSSEListening(selectedOrder.orderNo);
    }
  }, [fetchOrders, selectedOrder, startSSEListening]);

  // 查看订单详情（可扩展）
  const handleViewDetail = (order: OrderRecord) => {
    Modal.info({
      title: '订单详情',
      width: 600,
      content: (
        <div className="space-y-3">
          <div><strong>订单号：</strong>{order.orderNo}</div>
          <div><strong>订单状态：</strong>{getStatusTag(order.status)}</div>
          <div><strong>订单金额：</strong>¥{order.amount.toFixed(2)}</div>
          <div><strong>支付方式：</strong>{order.paymentMethod}</div>
          <div><strong>创建时间：</strong>{format.formatDateTime(order.createdAt)}</div>
          <div><strong>过期时间：</strong>{format.formatDateTime(order.expiredAt)}</div>
          <div><strong>状态描述：</strong>{order.message}</div>
        </div>
      ),
    });
  };

  // 定义表格列
  const columns: ColumnsType<OrderRecord> = [
    {
      title: '订单号',
      dataIndex: 'orderNo',
      key: 'orderNo',
      width: 200,
      render: (orderNo: string) => (
        <Tooltip title={orderNo}>
          <span className="font-mono text-sm">
            {orderNo.substring(0, 16)}...
          </span>
        </Tooltip>
      ),
    },
    {
      title: '订单状态',
      dataIndex: 'status',
      key: 'status',
      width: 120,
      render: (status: string) => getStatusTag(status),
    },
    {
      title: '订单金额',
      dataIndex: 'amount',
      key: 'amount',
      width: 120,
      render: (amount: number) => <span className="font-mono">¥{amount.toFixed(2)}</span>,
    },
    {
      title: '支付方式',
      dataIndex: 'paymentMethod',
      key: 'paymentMethod',
      width: 100,
      render: (method: string) => method === 'wechat' ? '微信支付' : method,
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (time: string) => format.formatDateTime(time),
    },
    {
      title: '过期时间',
      dataIndex: 'expiredAt',
      key: 'expiredAt',
      width: 180,
      render: (time: string) => format.formatDateTime(time),
    },
    {
      title: '操作',
      key: 'actions',
      width: 200,
      render: (_, order: OrderRecord) => (
        <Space size="small">
          <Button size="small" onClick={() => handleViewDetail(order)}>
            详情
          </Button>
          {order.status === 'paid' && (
            <Button size="small" type="primary" onClick={() => handleRefund(order)}>
              申请退款
            </Button>
          )}
          {(order.status === 'refund_processing' || 
            order.status === 'refund_success' || 
            order.status === 'refund_fail') && (
            <Button size="small" onClick={() => message.info('退款状态查询功能开发中')}>
              退款详情
            </Button>
          )}
        </Space>
      ),
    },
  ];

  useEffect(() => {
    fetchCurrentUser();
  }, [fetchCurrentUser]);

  useEffect(() => {
    if (currentUser?.id) {
      fetchOrders();
    }
  }, [currentUser?.id, fetchOrders]);

  // 为退款处理中的订单启动SSE监听
  useEffect(() => {
    orders.forEach(order => {
      if (order.status === 'refund_processing' && !sseListeners.has(order.orderNo)) {
        startSSEListening(order.orderNo);
      }
    });
  }, [orders, sseListeners, startSSEListening]);

  // 组件卸载时清理所有SSE监听器
  useEffect(() => {
    return () => {
      sseListeners.forEach((listener) => {
        listener.stopListening();
      });
      sseListeners.clear();
    };
  }, [sseListeners]);

  return (
    <Layout>
      <div className="p-6">
        <Card title="我的订单" className="shadow-sm">
          <div className="mb-4 flex justify-between items-center">
            <div className="text-gray-600">
              共 {orders.length} 个订单
            </div>
            <Button onClick={fetchOrders} loading={loading}>
              刷新
            </Button>
          </div>

          <Table
            columns={columns}
            dataSource={orders}
            rowKey="orderNo"
            loading={loading}
            pagination={{
              pageSize: 10,
              showSizeChanger: true,
              showQuickJumper: true,
              showTotal: (total) => `共 ${total} 条记录`,
            }}
            scroll={{ x: 1000 }}
          />
        </Card>

        <RefundModal
          visible={refundModalVisible}
          order={selectedOrder}
          onClose={() => {
            setRefundModalVisible(false);
            setSelectedOrder(null);
          }}
          onSuccess={handleRefundSuccess}
        />
      </div>
    </Layout>
  );
};

export default Orders;
