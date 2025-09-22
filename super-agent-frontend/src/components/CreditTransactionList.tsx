import React, { useState, useEffect } from 'react';
import {
  Table,
  Pagination,
  Spin,
  message
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { userApi } from '@/services/api';

interface CreditTransaction {
  id: string;
  description: string;
  amount: number;
  transactionType: string;
  createTime: string;
  remark?: string;
}

interface CreditTransactionListProps {
  // 预留扩展属性
  refreshTrigger?: number;
}

const CreditTransactionList: React.FC<CreditTransactionListProps> = () => {
  const [loading, setLoading] = useState(false);
  const [transactions, setTransactions] = useState<CreditTransaction[]>([]);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 5,
    total: 0
  });

  useEffect(() => {
    loadTransactions(1, 5);
  }, []);

  const loadTransactions = async (page: number, pageSize: number) => {
    try {
      setLoading(true);
      
      // 调用真实API
      const response = await userApi.getCreditTransactions(page, pageSize);
      const data = response.data.data;
      
      setTransactions(data.records || []);
      setPagination({
        current: data.pageNum || page,
        pageSize: data.pageSize || pageSize,
        total: data.totalRow || 0
      });
    } catch (error: any) {
      message.error('加载积分交易记录失败: ' + (error.message || '未知错误'));
    } finally {
      setLoading(false);
    }
  };

  const handlePageChange = (page: number, pageSize?: number) => {
    loadTransactions(page, pageSize || pagination.pageSize);
  };

  const columns: ColumnsType<CreditTransaction> = [
    {
      title: '详情',
      dataIndex: 'description',
      key: 'description',
      width: '50%',
      render: (text) => (
        <div style={{ 
          color: '#fff', 
          fontSize: '14px',
          maxWidth: '200px',
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          whiteSpace: 'nowrap'
        }}>
          {text}
        </div>
      )
    },
    {
      title: '时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: '30%',
      render: (time) => (
        <div style={{ color: '#ccc', fontSize: '14px' }}>
          {time}
        </div>
      )
    },
    {
      title: '积分变更',
      dataIndex: 'amount',
      key: 'amount',
      width: '20%',
      align: 'right',
      render: (amount: number) => {
        const isPositive = amount > 0;
        return (
          <div style={{ 
            color: isPositive ? '#52c41a' : '#ff4d4f',
            fontSize: '14px',
            fontWeight: 500
          }}>
            {isPositive ? '+' : ''}{amount}
          </div>
        );
      }
    }
  ];

  return (
    <div style={{ marginTop: '16px' }}>
      <Spin spinning={loading}>
        <Table
          columns={columns}
          dataSource={transactions}
          pagination={false}
          rowKey="id"
          size="small"
          style={{
            background: 'transparent'
          }}
          className="credit-transaction-table"
        />
      </Spin>
      
      {pagination.total > 0 && (
        <div style={{ 
          display: 'flex', 
          justifyContent: 'center', 
          marginTop: '16px'
        }}>
          <Pagination
            current={pagination.current}
            total={pagination.total}
            pageSize={pagination.pageSize}
            onChange={handlePageChange}
            showTotal={(total, range) => 
              `${range[0]}-${range[1]} of ${total} items`
            }
            size="small"
            showSizeChanger={false}
            className="credit-pagination"
          />
        </div>
      )}
      
      <style jsx global>{`
        .credit-transaction-table {
          background: transparent !important;
        }
        .credit-transaction-table .ant-table {
          background: transparent !important;
          color: #fff;
        }
        .credit-transaction-table .ant-table-thead > tr > th {
          background: rgba(255, 255, 255, 0.08) !important;
          color: #ccc !important;
          font-weight: 500;
          font-size: 12px;
          border-bottom: 1px solid rgba(255, 255, 255, 0.1) !important;
          border-left: none !important;
          border-right: none !important;
        }
        .credit-transaction-table .ant-table-tbody > tr > td {
          background: rgba(255, 255, 255, 0.02) !important;
          border-bottom: 1px solid rgba(255, 255, 255, 0.05) !important;
          border-left: none !important;
          border-right: none !important;
          padding: 12px 16px;
          color: #fff;
        }
        .credit-transaction-table .ant-table-tbody > tr:hover > td {
          background: rgba(255, 255, 255, 0.08) !important;
        }
        .credit-transaction-table .ant-table-container {
          background: transparent !important;
        }
        .credit-transaction-table .ant-table-content {
          background: transparent !important;
        }
        .credit-transaction-table .ant-spin-container {
          background: transparent !important;
        }
        
        /* Loading组件暗色主题 */
        .credit-transaction-table .ant-spin {
          color: #1890ff !important;
        }
        .credit-transaction-table .ant-spin-dot-item {
          background-color: #1890ff !important;
        }
        
        /* 分页组件暗色主题 */
        .credit-pagination .ant-pagination-item {
          background: rgba(255, 255, 255, 0.08) !important;
          border: 1px solid rgba(255, 255, 255, 0.1) !important;
        }
        .credit-pagination .ant-pagination-item a {
          color: #ccc !important;
        }
        .credit-pagination .ant-pagination-item:hover {
          background: rgba(255, 255, 255, 0.15) !important;
          border-color: rgba(255, 255, 255, 0.2) !important;
        }
        .credit-pagination .ant-pagination-item:hover a {
          color: #fff !important;
        }
        .credit-pagination .ant-pagination-item-active {
          background: #1890ff !important;
          border-color: #1890ff !important;
        }
        .credit-pagination .ant-pagination-item-active a {
          color: #fff !important;
        }
        .credit-pagination .ant-pagination-prev,
        .credit-pagination .ant-pagination-next {
          background: rgba(255, 255, 255, 0.08) !important;
          border: 1px solid rgba(255, 255, 255, 0.1) !important;
        }
        .credit-pagination .ant-pagination-prev .ant-pagination-item-link,
        .credit-pagination .ant-pagination-next .ant-pagination-item-link {
          color: #ccc !important;
        }
        .credit-pagination .ant-pagination-prev:hover,
        .credit-pagination .ant-pagination-next:hover {
          background: rgba(255, 255, 255, 0.15) !important;
          border-color: rgba(255, 255, 255, 0.2) !important;
        }
        .credit-pagination .ant-pagination-prev:hover .ant-pagination-item-link,
        .credit-pagination .ant-pagination-next:hover .ant-pagination-item-link {
          color: #fff !important;
        }
        .credit-pagination .ant-pagination-total-text {
          color: #ccc !important;
        }
      `}</style>
    </div>
  );
};

export default CreditTransactionList;
