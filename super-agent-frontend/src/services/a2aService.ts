import axios from 'axios';
import { API_CONFIG } from '@/config/apiEndpoints';

// 创建axios实例
const a2aApi = axios.create({
  baseURL: API_CONFIG.BASE_URL,
  timeout: API_CONFIG.TIMEOUT,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器
a2aApi.interceptors.request.use(
  (config) => {
    // 添加认证token
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器
a2aApi.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    console.error('A2A服务错误:', error);
    return Promise.reject(error);
  }
);

export interface A2AMessageRequest {
  userId: string;
  message: string;
  sessionId: string;
  contextId?: string;
}

export interface A2ASupplementInfoRequest {
  userId: string;
  taskId: string;
  userInput: string;
  sessionId: string;
}

interface A2AJsonRpcRequest {
  jsonrpc: string;
  method: string;
  id: string;
  params: {
    message: {
      role: string;
      parts: Array<{
        kind: string;
        text: string;
      }>;
      kind: string;
      taskId: string;
      contextId: string;
    };
  };
}

// 发送消息到A2A平台的标准格式 - 流式响应版本
export const sendJsonRpcMessage = (
  userId: string,
  message: string,
  taskId: string,
  contextId: string,
  onMessage: (data: string) => void,
  onError: (error: any) => void,
  onClose?: () => void
): (() => void) => {
  // 使用默认的能力中心编码和实体编码
  const abilityCode = API_CONFIG.A2A_PLATFORM.DEFAULT_ABILITY_CODE;
  const entityCode = API_CONFIG.A2A_PLATFORM.DEFAULT_ENTITY_CODE;
  const url = `${API_CONFIG.BASE_URL}/a2a/kunlun/a2a/api/${abilityCode}/entity/${entityCode}/userid/${userId}`;
  
  const requestBody = {
    jsonrpc: '2.0' as const,
    method: 'message/stream' as const,
    id: `requestId_${Date.now()}`,
    params: {
      message: {
        role: 'user' as const,
        parts: [
          {
            kind: 'text' as const,
            text: message,
          },
        ],
        kind: 'message' as const,
        taskId,
        contextId,
      },
    },
  };

  // 打印发送的请求体
  console.log('发送SSE请求时的请求体:', JSON.stringify(requestBody, null, 2));
  
  const abortController = new AbortController();
  
  // 获取token
  const token = localStorage.getItem('token');
  const headers: Record<string, string> = {
    'Content-Type': 'application/json; charset=utf-8',
    'Accept': 'text/event-stream',
    'Accept-Charset': 'utf-8',
    'Connection': 'keep-alive',
  };
  
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  // 创建一个Promise来包装fetch请求
  const fetchPromise = fetch(url, {
    method: 'POST',
    headers,
    body: JSON.stringify(requestBody),
    signal: abortController.signal,
  });

  // 创建一个超时Promise
  const timeoutPromise = new Promise((_, reject) => {
    setTimeout(() => {
      reject(new Error('Request timeout'));
    }, API_CONFIG.TIMEOUT); // 使用配置的超时时间
  });

  // 竞争fetch请求和超时Promise
  Promise.race([fetchPromise, timeoutPromise])
    .then((response: any) => {
      // 如果是超时Promise获胜，response将是undefined
      if (!response || !(response instanceof Response)) {
        throw new Error('Request timeout');
      }

      // 检查响应状态
      if (!response.ok) {
        return response.text().then((errorText: string) => {
          throw new Error(`HTTP ${response.status}: ${errorText || response.statusText}`);
        });
      }

      // 对于流式响应，我们需要逐步读取并解析SSE
      if (!response.body) {
        throw new Error('ReadableStream not supported');
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder('utf-8');
      let buffer = ''; // 用于累积接收到的文本
      const processedMessageIds = new Set<string>(); // 用于跟踪已处理的消息ID

      function parseSSEData(data: string): void {
        // 将新数据追加到缓冲区
        buffer += data;
        
        // 按行分割
        const lines = buffer.split('\n');
        // 保留最后一个可能不完整的行在buffer中
        buffer = lines.pop() || '';

        for (let i = 0; i < lines.length; i++) {
          const line = lines[i].trim();
          // 只处理以 "data:" 开头的行
          if (line.startsWith('data:')) {
            const eventData = line.slice(5).trim(); // 移除 "data:" 前缀
            // 如果是心跳消息 "ping" 或空数据，则忽略
            if (eventData && eventData !== 'ping') {
              try {
                // 尝试解析JSON数据以检查messageId
                const jsonData = JSON.parse(eventData);
                if (jsonData.result && 
                    jsonData.result.status && 
                    jsonData.result.status.message && 
                    jsonData.result.status.message.messageId) {
                  const messageId = jsonData.result.status.message.messageId;
                  
                  // 如果消息已处理过，则跳过
                  if (processedMessageIds.has(messageId)) {
                    console.log('检测到重复消息，ID:', messageId, '已跳过');
                    continue;
                  }
                  
                  // 标记消息为已处理
                  processedMessageIds.add(messageId);
                  console.log('处理新消息，ID:', messageId);
                }
              } catch (e) {
                // JSON解析失败，不影响数据传输
                console.warn('JSON解析失败:', e);
              }
              
              onMessage(eventData);
            }
          }
          // 忽略其他类型的SSE字段，如 id:, event:, retry:
        }
      }

      // 逐步读取流
      function readStream() {
        reader.read().then(({ done, value }) => {
          if (done) {
            // 流已完成，处理缓冲区中的剩余数据
            console.log('前端接收数据完成');
            if (buffer.trim()) {
              parseSSEData('\n'); // 添加换行符以触发最后一条消息的解析
            }
            if (onClose) onClose();
            return;
          }

          // 解码数据块
          const chunk = decoder.decode(value, { stream: true });
          
          // 添加详细日志记录
          console.log('前端接收到数据块:', {
            size: value?.length || 0,
            content: chunk.substring(0, 100) + (chunk.length > 100 ? '...' : '')
          });
          
          // 解析SSE数据
          parseSSEData(chunk);

          // 继续读取
          readStream();
        }).catch(error => {
          // 处理读取错误
          if (error.name !== 'AbortError') {
            console.error('数据读取错误:', error);
            // 在处理最终缓冲区前先报告错误
            onError(error);
            // 尝试发送缓冲区中已有的部分数据
            if (buffer.trim()) {
              parseSSEData('\n');
            }
          } else if (onClose) {
            onClose();
          }
        });
      }

      // 开始读取流
      readStream();
    })
    .catch(error => {
      // 处理连接错误
      if (error.name !== 'AbortError') {
        onError(error);
      } else if (onClose) {
        onClose();
      }
    });
  
  // 返回取消函数
  return () => {
    abortController.abort();
  };
};

// 流式发送消息到A2A平台
export const streamMessage = (
  request: A2AMessageRequest,
  onMessage: (chunk: string) => void,
  onError?: (error: any) => void
): (() => void) => {
  // 记录基础配置信息
  console.log('API配置信息:', API_CONFIG);
  
  // 使用正确的URL路径
  const url = `${API_CONFIG.BASE_URL}/a2a/stream-message`;
  
  console.log('构造的A2A请求URL:', url);
  console.log('完整的API基础URL:', API_CONFIG.BASE_URL);
  console.log('A2A请求参数:', request);
  
  const abortController = new AbortController();
  
  // 获取token
  const token = localStorage.getItem('token');
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'Accept': 'text/event-stream',
  };
  
  // 添加认证头
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
    console.log('使用认证token');
  } else {
    console.warn('未找到认证token，请求可能被拦截');
  }
  
  console.log('请求头信息:', headers);

  // 创建一个简化版的请求对象，只包含必要的字段
  // 所有其他参数由后端生成
  const simplifiedRequest = {
    userId: request.userId,
    message: request.message,
    sessionId: request.sessionId,
    contextId: request.contextId
  };

  // 创建一个Promise来包装fetch请求
  const fetchPromise = fetch(url, {
    method: 'POST',
    headers,
    body: JSON.stringify(simplifiedRequest),
    signal: abortController.signal,
  });
  
  console.log('发起fetch请求到:', url);

  // 创建一个超时Promise
  const timeoutPromise = new Promise((_, reject) => {
    setTimeout(() => {
      reject(new Error('Request timeout'));
    }, API_CONFIG.TIMEOUT);
  });

  // 竞争fetch请求和超时Promise
  Promise.race([fetchPromise, timeoutPromise])
    .then((response: any) => {
      console.log('A2A响应对象:', response);
      if (!response || !(response instanceof Response)) {
        throw new Error('Request timeout');
      }
      
      console.log('响应URL:', response.url);
      console.log('响应状态:', response.status);
      console.log('响应状态文本:', response.statusText);
      console.log('响应头:', Array.from(response.headers.entries()));

      // 特殊处理认证失败的情况
      if (response.status === 401) {
        const error = new Error('认证失败，请重新登录');
        error.name = 'AuthenticationError';
        throw error;
      }

      if (!response.ok) {
        return response.text().then((errorText: string) => {
          console.error('A2A错误响应文本:', errorText);
          console.error('完整响应对象:', response);
          throw new Error(`HTTP ${response.status}: ${errorText || response.statusText}`);
        });
      }

      if (!response.body) {
        throw new Error('ReadableStream not supported');
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder('utf-8');
      let buffer = '';

      function parseSSEData(data: string): void {
        buffer += data;
        const lines = buffer.split('\n');
        // 保留最后一个可能不完整的行在buffer中
        buffer = lines.pop() || '';

        for (let i = 0; i < lines.length; i++) {
          const line = lines[i].trim();
          if (line.startsWith('data:')) {
            const eventData = line.slice(5).trim();
            if (eventData && eventData !== 'ping') {
              onMessage(eventData);
            }
          }
        }
      }

      function readStream() {
        reader.read().then(({ done, value }) => {
          if (done) {
            // 处理最后剩余的缓冲数据（如果有的话且不是空行）
            if (buffer.trim()) {
              // 创建一个临时的结束标记来触发处理剩余数据
              parseSSEData('\n');
            }
            return;
          }

          const chunk = decoder.decode(value, { stream: true });
          parseSSEData(chunk);
          readStream();
        }).catch(error => {
          if (error.name !== 'AbortError') {
            console.error('Stream reading error:', error);
            if (onError) {
              onError(error);
            }
          }
        });
      }

      readStream();
    })
    .catch(error => {
      if (error.name !== 'AbortError') {
        console.error('A2A service error:', error);
        if (onError) {
          onError(error);
        }
      }
    });
  
  return () => {
    abortController.abort();
  };
};

// 处理补充信息请求
export const handleSupplementInfo = async (request: A2ASupplementInfoRequest) => {
  try {
    const response = await a2aApi.post('/a2a/supplement-info', request);
    return response;
  } catch (error) {
    console.error('处理补充信息失败:', error);
    throw error;
  }
};
