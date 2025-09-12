/**
 * 支付状态SSE监听工具类
 */

export interface PaymentStatusEvent {
  orderNo: string;
  status: string;
  amount?: number;
  paymentMethod?: string;
  message: string;
  eventTime: string;
  extra?: string;
}

export type PaymentStatusCallback = (event: PaymentStatusEvent) => void;

export class PaymentSSEListener {
  private eventSource: EventSource | null = null;
  private orderNo: string;
  private baseUrl: string;
  private isListening: boolean = false;
  private eventCount: number = 0;

  constructor(orderNo: string) {
    this.orderNo = orderNo;
    // 使用专门的SSE API
    this.baseUrl = '/api/sse-direct';
  }

  /**
   * 开始监听支付状态
   */
  startListening(callbacks: {
    onPaymentStatus?: PaymentStatusCallback;
    onConnection?: PaymentStatusCallback;
    onHeartbeat?: () => void;
    onError?: (error: Event) => void;
    onClose?: () => void;
  }): void {
    if (this.isListening) {
      return;
    }

    const url = `${this.baseUrl}?orderNo=${this.orderNo}`;

    this.eventSource = new EventSource(url);
    this.isListening = true;
    this.eventCount = 0; // 重置事件计数器

    // 监听所有事件（包括默认message事件）
    this.eventSource.onmessage = (event) => {
      this.eventCount++;
    };

    // 监听连接建立事件
    this.eventSource.addEventListener('connection', (event) => {
      this.eventCount++;
      try {
        const data: PaymentStatusEvent = JSON.parse(event.data);
        callbacks.onConnection?.(data);
      } catch (e) {
        console.error('解析connection事件数据失败:', e);
      }
    });

    // 监听支付状态变化事件
    this.eventSource.addEventListener('payment_status', (event) => {
      this.eventCount++;
      try {
        const data: PaymentStatusEvent = JSON.parse(event.data);
        callbacks.onPaymentStatus?.(data);
      } catch (e) {
        console.error('解析payment_status事件数据失败:', e);
      }
    });

    // 监听心跳事件
    this.eventSource.addEventListener('heartbeat', () => {
      callbacks.onHeartbeat?.();
    });

    // 监听错误事件
    this.eventSource.onerror = (error) => {
      this.isListening = false;
      callbacks.onError?.(error);
    };

    // 监听连接关闭事件
    this.eventSource.addEventListener('close', () => {
      this.isListening = false;
      callbacks.onClose?.();
    });

    // 监听连接打开事件
    this.eventSource.onopen = () => {
      // 设置超时检测，5秒后检查是否收到任何事件
      setTimeout(() => {
        if (this.eventCount === 0) {
          // 手动触发一个测试请求
          fetch(`/api/payment/test/sse/${this.orderNo}`, { method: 'POST' })
            .catch(() => {}); // 静默处理错误
        }
      }, 5000);
    };
  }

  /**
   * 停止监听
   */
  stopListening(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
      this.isListening = false;
    }
  }

  /**
   * 获取监听状态
   */
  getListeningStatus(): boolean {
    return this.isListening;
  }

  /**
   * 查询监听统计信息
   */
  async getListenStats(): Promise<{
    orderNo: string;
    connectionCount: number;
    totalActiveOrders: number;
    isListening: boolean;
  }> {
    const response = await fetch(`${this.baseUrl}/listen/status/${this.orderNo}`);
    const result = await response.json();
    return result.data;
  }
}

// 使用示例：
/*
const paymentListener = new PaymentSSEListener('ORDER123456789');

paymentListener.startListening({
  onConnection: (event) => {
    console.log('连接成功:', event.message);
  },
  
  onPaymentStatus: (event) => {
    switch (event.status) {
      case 'paid':
        console.log('支付成功！', event);
        // 更新UI，显示支付成功状态
        showPaymentSuccess(event);
        // 停止监听
        paymentListener.stopListening();
        break;
      case 'failed':
        console.log('支付失败！', event);
        showPaymentFailed(event);
        break;
      case 'cancelled':
        console.log('订单已取消！', event);
        showPaymentCancelled(event);
        break;
      default:
        console.log('支付状态更新:', event);
    }
  },
  
  onError: (error) => {
    console.error('监听失败:', error);
    // 可以实现重连逻辑
  },
  
  onClose: () => {
    console.log('监听连接已关闭');
  }
});

// 在组件卸载时记得停止监听
// paymentListener.stopListening();
*/

// 同时支持命名导出和默认导出
export default PaymentSSEListener;