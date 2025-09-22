// 事件回调函数类型定义
type EventCallback = (...args: unknown[]) => void;

// 语音识别结果接口
export interface SpeechRecognitionResult {
  type: string;
  sessionId: string;
  status: string;
  text: string;
  isFinal: boolean;
  confidence?: number;
  timestamp: number;
}

/**
 * 语音识别服务
 * 负责处理与后端WebSocket的通信，实现实时语音识别
 */
export class SpeechRecognitionService {
  private ws: WebSocket | null = null;
  private eventListeners: Map<string, EventCallback[]> = new Map();
  private isConnecting = false;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 3;
  private reconnectDelay = 1000;

  constructor() {
    this.initEventListeners();
  }

  private initEventListeners() {
    this.eventListeners.set('connectionOpen', []);
    this.eventListeners.set('connectionClose', []);
    this.eventListeners.set('connectionError', []);
    this.eventListeners.set('recognition', []);
    this.eventListeners.set('error', []);
  }

  /**
   * 添加事件监听器
   */
  on(event: string, callback: EventCallback) {
    if (!this.eventListeners.has(event)) {
      this.eventListeners.set(event, []);
    }
    this.eventListeners.get(event)?.push(callback);
  }

  /**
   * 移除事件监听器
   */
  off(event: string, callback: EventCallback) {
    const listeners = this.eventListeners.get(event);
    if (listeners) {
      const index = listeners.indexOf(callback);
      if (index > -1) {
        listeners.splice(index, 1);
      }
    }
  }

  /**
   * 触发事件
   */
  private emit(event: string, data?: unknown) {
    const listeners = this.eventListeners.get(event) || [];
    listeners.forEach(callback => {
      try {
        callback(data);
      } catch (error) {
        console.error(`Event callback error for ${event}:`, error);
      }
    });
  }

  /**
   * 连接WebSocket
   */
  async connect(): Promise<void> {
    if (this.isConnecting || (this.ws && this.ws.readyState === WebSocket.OPEN)) {
      return;
    }

    this.isConnecting = true;

    return new Promise((resolve, reject) => {
      try {
        // 构建WebSocket URL - 与SSE服务保持一致的baseURL逻辑
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        
        // 获取后端基础URL - 使用环境变量配置
        let baseUrl: string;
        if (process.env.NODE_ENV === 'development') {
          // 开发环境：使用环境变量（已包含完整的context-path）
          baseUrl = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8081/super-agent';
        } else {
          // 生产环境：使用当前域名和context-path
          baseUrl = `${window.location.protocol}//${window.location.host}/super-agent`;
        }
        
        // 直接构建完整的WebSocket URL（保留context-path）
        const wsUrl = baseUrl.replace(/^https?:/, protocol) + '/api/v1/ws/speech-recognition';

        console.log('Connecting to WebSocket:', wsUrl);
        this.ws = new WebSocket(wsUrl);

        // 设置二进制数据类型
        this.ws.binaryType = 'arraybuffer';

        this.ws.onopen = (event) => {
          console.log('WebSocket connected:', event);
          this.isConnecting = false;
          this.reconnectAttempts = 0;
          this.emit('connectionOpen');
          resolve();
        };

        this.ws.onclose = (event) => {
          console.log('WebSocket closed:', event.code, event.reason);
          this.isConnecting = false;
          this.ws = null;
          this.emit('connectionClose', event.reason);

          // 检查关闭原因，如果是服务初始化失败，不要重连
          const reason = event.reason || '';
          if (reason.includes('启动识别服务失败') || 
              reason.includes('语音识别会话失效') ||
              reason.includes('服务异常')) {
            console.warn('WebSocket closed due to service failure, stopping reconnection:', reason);
            this.maxReconnectAttempts = 0; // 禁用重连
            this.reconnectAttempts = 999; // 确保不再重连
            return;
          }

          // 如果是异常关闭，尝试重连
          if (event.code !== 1000 && this.reconnectAttempts < this.maxReconnectAttempts) {
            this.scheduleReconnect();
          }
        };

        this.ws.onerror = (event) => {
          console.error('WebSocket error:', event);
          this.isConnecting = false;
          this.emit('connectionError', 'WebSocket连接错误');
          reject(new Error('WebSocket连接失败'));
        };

        this.ws.onmessage = (event) => {
          this.handleMessage(event.data);
        };

        // 设置连接超时
        setTimeout(() => {
          if (this.isConnecting) {
            this.isConnecting = false;
            if (this.ws) {
              this.ws.close();
            }
            reject(new Error('WebSocket连接超时'));
          }
        }, 10000);

      } catch (error) {
        this.isConnecting = false;
        console.error('WebSocket connection setup error:', error);
        reject(error);
      }
    });
  }

  /**
   * 计划重连
   */
  private scheduleReconnect() {
    this.reconnectAttempts++;
    const delay = this.reconnectDelay * this.reconnectAttempts;
    
    console.log(`Scheduling reconnect attempt ${this.reconnectAttempts} in ${delay}ms`);
    
    setTimeout(() => {
      if (this.reconnectAttempts <= this.maxReconnectAttempts) {
        console.log(`Attempting to reconnect... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
        this.connect().catch(error => {
          console.error('Reconnect failed:', error);
        });
      }
    }, delay);
  }

  /**
   * 处理WebSocket消息
   */
  private handleMessage(data: string) {
    try {
      const message = JSON.parse(data);
      console.log('Received message:', message);

      switch (message.type) {
        case 'connection':
          // 连接成功消息
          if (message.status === 'connected') {
            console.log('Speech recognition service ready:', message);
          }
          break;

        case 'recognition':
          // 识别结果
          this.emit('recognition', {
            type: message.type,
            sessionId: message.sessionId,
            status: message.status,
            text: message.text,
            isFinal: message.isFinal,
            confidence: message.confidence,
            timestamp: message.timestamp
          });
          break;

        case 'error':
          // 错误消息
          const errorMsg = message.message || '语音识别服务错误';
          console.error('Speech recognition error:', errorMsg);
          
          // 如果是服务初始化失败的错误，停止重连
          if (errorMsg.includes('启动识别服务失败') || 
              errorMsg.includes('语音识别服务不可用') ||
              errorMsg.includes('API Key') ||
              errorMsg.includes('初始化失败')) {
            console.warn('Speech recognition service initialization failed, stopping reconnection attempts');
            this.maxReconnectAttempts = 0; // 禁用重连
            this.reconnectAttempts = 999; // 确保不再重连
          }
          
          this.emit('error', errorMsg);
          break;

        case 'started':
          console.log('Recording started');
          break;

        case 'stopped':
          console.log('Recording stopped');
          break;

        case 'pong':
          // 心跳响应
          console.log('Heartbeat pong received');
          break;

        default:
          console.warn('Unknown message type:', message.type);
      }

    } catch (error) {
      console.error('Message parsing error:', error, data);
      this.emit('error', '消息解析失败');
    }
  }

  /**
   * 重置重连状态（用于手动重新启用语音识别）
   */
  resetReconnection() {
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5; // 重置为默认值
    console.log('Speech recognition reconnection has been reset and re-enabled');
  }

  /**
   * 断开连接
   */
  disconnect() {
    if (this.ws) {
      this.ws.close(1000, 'Client disconnect');
      this.ws = null;
    }
    this.isConnecting = false;
    this.reconnectAttempts = 0;
  }

  /**
   * 检查连接状态
   */
  isConnected(): boolean {
    return this.ws !== null && this.ws.readyState === WebSocket.OPEN;
  }

  /**
   * 发送开始录音命令
   */
  start() {
    if (!this.isConnected()) {
      throw new Error('WebSocket未连接');
    }

    this.sendMessage({
      type: 'start',
      timestamp: Date.now()
    });
  }

  /**
   * 发送停止录音命令
   */
  stop() {
    if (!this.isConnected()) {
      return; // 静默忽略，因为连接可能已关闭
    }

    this.sendMessage({
      type: 'stop',
      timestamp: Date.now()
    });
  }

  /**
   * 发送音频数据
   */
  sendAudioData(audioData: ArrayBuffer) {
    if (!this.isConnected()) {
      console.warn('WebSocket not connected, cannot send audio data');
      return;
    }

    try {
      // 直接发送二进制音频数据
      this.ws?.send(audioData);
    } catch (error) {
      console.error('Send audio data error:', error);
      this.emit('error', '发送音频数据失败');
    }
  }

  /**
   * 发送文本消息
   */
  private sendMessage(message: Record<string, unknown>) {
    if (!this.isConnected()) {
      throw new Error('WebSocket未连接');
    }

    try {
      const messageStr = JSON.stringify(message);
      console.log('Sending message:', messageStr);
      this.ws?.send(messageStr);
    } catch (error) {
      console.error('Send message error:', error);
      this.emit('error', '发送消息失败');
    }
  }

  /**
   * 发送心跳包
   */
  ping() {
    if (this.isConnected()) {
      this.sendMessage({
        type: 'ping',
        timestamp: Date.now()
      });
    }
  }

  /**
   * 获取连接状态
   */
  getConnectionState(): string {
    if (!this.ws) return 'disconnected';
    
    switch (this.ws.readyState) {
      case WebSocket.CONNECTING: return 'connecting';
      case WebSocket.OPEN: return 'connected';
      case WebSocket.CLOSING: return 'closing';
      case WebSocket.CLOSED: return 'closed';
      default: return 'unknown';
    }
  }
}
