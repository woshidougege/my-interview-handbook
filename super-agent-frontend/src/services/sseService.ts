// SSE服务不需要ChatMessage类型

/**
 * SSE连接状态
 */
export enum ConnectionStatus {
  DISCONNECTED = 'DISCONNECTED',
  CONNECTING = 'CONNECTING',
  CONNECTED = 'CONNECTED',
  RECONNECTING = 'RECONNECTING',
  ERROR = 'ERROR'
}

/**
 * SSE事件类型（与后端SseEventType对应）
 */
export enum SseEventType {
  CONNECTED = 'connected',
  AI_THINKING = 'ai_thinking',
  AI_CHUNK = 'ai_chunk', 
  AI_COMPLETE = 'ai_complete',
  ERROR = 'error',
  HEARTBEAT = 'heartbeat'
}

/**
 * SSE消息结构（与后端SseMessageDto对应）
 */
export interface SseMessage {
  messageId: string;
  eventType: SseEventType;
  content: string;
  workspaceId?: string;
  sessionId?: string;
  userId?: string;
  timestamp: string;
  errorMessage?: string;
  data?: unknown;
}

/**
 * SSE事件回调类型
 */
export interface SseCallbacks {
  onConnectionChange?: (status: ConnectionStatus) => void;
  onMessage?: (message: SseMessage) => void;
  onStreamChunk?: (chunk: string, messageId: string) => void;
  onStreamEnd?: (messageId: string) => void;
  onError?: (error: string) => void;
}

/**
 * SSE聊天服务类
 * 基于Server-Sent Events实现实时AI对话
 */
class SseService {
  private eventSource: EventSource | null = null;
  private status: ConnectionStatus = ConnectionStatus.DISCONNECTED;
  private callbacks: SseCallbacks = {};
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectInterval = 3000;
  private heartbeatTimeout: NodeJS.Timeout | null = null;
  private currentUserId: string | null = null;
  private currentWorkspaceId: string | null = null;
  private currentSessionId: string | null = null;
  
  /**
   * 获取SSE连接URL
   */
  private getSseUrl(sessionId: string, workspaceId: string, userId?: string): string {
    const baseUrl = this.getBaseUrl();
    const params = new URLSearchParams({
      workspaceId,
      ...(userId && { userId })
    });
    return `${baseUrl}/api/v1/chat/sse/${sessionId}?${params.toString()}`;
  }
  
  /**
   * 获取基础URL
   */
  private getBaseUrl(): string {
    if (process.env.NODE_ENV === 'development') {
      return process.env.NEXT_PUBLIC_BACKEND_URL || 'http://localhost:8081/super-agent';
    }
    const protocol = window.location.protocol;
    const host = window.location.host;
    return `${protocol}//${host}/super-agent`;
  }
  
  /**
   * 建立SSE连接
   */
  connect(userId: string, workspaceId: string, sessionId: string, callbacks: SseCallbacks = {}) {
    console.log(`[SSE] 开始连接 - 用户: ${userId}, 工作空间: ${workspaceId}, 会话: ${sessionId}`);
    
    this.currentUserId = userId;
    this.currentWorkspaceId = workspaceId;
    this.currentSessionId = sessionId;
    this.callbacks = callbacks;
    
    // 如果已有连接，先关闭
    if (this.eventSource) {
      this.disconnect();
    }
    
    this.setConnectionStatus(ConnectionStatus.CONNECTING);
    
    try {
      const sseUrl = this.getSseUrl(sessionId, workspaceId, userId);
      console.log(`[SSE] 连接到: ${sseUrl}`);
      
      this.eventSource = new EventSource(sseUrl);
      
      // 连接成功
      this.eventSource.onopen = () => {
        console.log('[SSE] 连接已建立');
        this.setConnectionStatus(ConnectionStatus.CONNECTED);
        this.reconnectAttempts = 0;
        this.startHeartbeatMonitor();
      };
      
      // 连接错误
      this.eventSource.onerror = (event) => {
        console.error('[SSE] 连接错误:', event);
        this.setConnectionStatus(ConnectionStatus.ERROR);
        this.handleConnectionError();
      };
      
      // 监听各种SSE事件
      this.setupEventListeners();
      
    } catch (error) {
      console.error('[SSE] 连接失败:', error);
      this.setConnectionStatus(ConnectionStatus.ERROR);
      this.callbacks.onError?.(`连接失败: ${error}`);
    }
  }
  
  /**
   * 设置事件监听器
   */
  private setupEventListeners() {
    if (!this.eventSource) return;
    
    // 连接成功事件
    this.eventSource.addEventListener(SseEventType.CONNECTED, (event: Event) => {
      const messageEvent = event as MessageEvent;
      console.log('[SSE] 收到连接确认:', messageEvent.data);
      const message = this.parseEventData(messageEvent.data);
      this.callbacks.onMessage?.(message);
    });
    
    // AI思考中事件
    this.eventSource.addEventListener(SseEventType.AI_THINKING, (event: Event) => {
      const messageEvent = event as MessageEvent;
      console.log('[SSE] AI开始思考');
      const message = this.parseEventData(messageEvent.data);
      this.callbacks.onMessage?.(message);
    });
    
    // AI回复片段事件
    this.eventSource.addEventListener(SseEventType.AI_CHUNK, (event: Event) => {
      const messageEvent = event as MessageEvent;
      const message = this.parseEventData(messageEvent.data);
      console.log('[SSE] 收到AI片段:', message.content);
      this.callbacks.onStreamChunk?.(message.content, message.messageId);
    });
    
    // AI回复完成事件
    this.eventSource.addEventListener(SseEventType.AI_COMPLETE, (event: Event) => {
      const messageEvent = event as MessageEvent;
      console.log('[SSE] AI回复完成');
      const message = this.parseEventData(messageEvent.data);
      this.callbacks.onStreamEnd?.(message.messageId);
    });
    
    // 错误事件
    this.eventSource.addEventListener(SseEventType.ERROR, (event: Event) => {
      const messageEvent = event as MessageEvent;
      const message = this.parseEventData(messageEvent.data);
      console.error('[SSE] 收到错误消息:', message.errorMessage);
      this.callbacks.onError?.(message.errorMessage || message.content);
    });
    
    // 心跳事件
    this.eventSource.addEventListener(SseEventType.HEARTBEAT, () => {
      console.log('[SSE] 收到心跳');
      this.resetHeartbeatMonitor();
    });
  }
  
  /**
   * 解析事件数据
   */
  private parseEventData(data: string): SseMessage {
    try {
      return JSON.parse(data);
    } catch (error) {
      console.error('[SSE] 解析消息失败:', error);
      return {
        messageId: 'error',
        eventType: SseEventType.ERROR,
        content: data,
        timestamp: new Date().toISOString()
      };
    }
  }
  
  /**
   * 发送消息（通过HTTP POST）
   */
  async sendMessage(content: string): Promise<void> {
    if (!this.currentSessionId || !this.currentWorkspaceId) {
      throw new Error('未建立连接或缺少必要参数');
    }
    
    try {
      const url = `${this.getBaseUrl()}/api/v1/chat/stream/${this.currentSessionId}`;
      const params = new URLSearchParams({
        message: content,
        workspaceId: this.currentWorkspaceId,
        ...(this.currentUserId && { userId: this.currentUserId })
      });
      
      console.log(`[SSE] 发送消息: ${content}`);
      
      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: params.toString()
      });
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }
      
      console.log('[SSE] 消息发送成功');
      
    } catch (error) {
      console.error('[SSE] 发送消息失败:', error);
      this.callbacks.onError?.(`发送消息失败: ${error}`);
      throw error;
    }
  }
  
  /**
   * 断开连接
   */
  disconnect() {
    console.log('[SSE] 断开连接');
    
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
    
    this.stopHeartbeatMonitor();
    this.setConnectionStatus(ConnectionStatus.DISCONNECTED);
    this.currentUserId = null;
    this.currentWorkspaceId = null;
    this.currentSessionId = null;
  }
  
  /**
   * 处理连接错误
   */
  private handleConnectionError() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(`[SSE] 尝试重连 (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
      
      this.setConnectionStatus(ConnectionStatus.RECONNECTING);
      
      setTimeout(() => {
        if (this.currentUserId && this.currentWorkspaceId && this.currentSessionId) {
          this.connect(this.currentUserId, this.currentWorkspaceId, this.currentSessionId, this.callbacks);
        }
      }, this.reconnectInterval);
    } else {
      console.error('[SSE] 重连次数已达上限');
      this.setConnectionStatus(ConnectionStatus.ERROR);
      this.callbacks.onError?.('连接失败，请刷新页面重试');
    }
  }
  
  /**
   * 设置连接状态
   */
  private setConnectionStatus(status: ConnectionStatus) {
    if (this.status !== status) {
      this.status = status;
      console.log(`[SSE] 状态变更: ${status}`);
      this.callbacks.onConnectionChange?.(status);
    }
  }
  
  /**
   * 开始心跳监控
   */
  private startHeartbeatMonitor() {
    this.resetHeartbeatMonitor();
  }
  
  /**
   * 重置心跳监控
   */
  private resetHeartbeatMonitor() {
    if (this.heartbeatTimeout) {
      clearTimeout(this.heartbeatTimeout);
    }
    
    // 60秒未收到心跳则认为连接异常
    this.heartbeatTimeout = setTimeout(() => {
      console.warn('[SSE] 心跳超时，连接可能异常');
      this.setConnectionStatus(ConnectionStatus.ERROR);
      this.handleConnectionError();
    }, 60 * 1000);
  }
  
  /**
   * 停止心跳监控
   */
  private stopHeartbeatMonitor() {
    if (this.heartbeatTimeout) {
      clearTimeout(this.heartbeatTimeout);
      this.heartbeatTimeout = null;
    }
  }
  
  /**
   * 获取当前连接状态
   */
  getConnectionStatus(): ConnectionStatus {
    return this.status;
  }
  
  /**
   * 检查是否已连接
   */
  isConnected(): boolean {
    return this.status === ConnectionStatus.CONNECTED;
  }
}

// 导出单例实例
const sseService = new SseService();
export default sseService;
export { SseService };
