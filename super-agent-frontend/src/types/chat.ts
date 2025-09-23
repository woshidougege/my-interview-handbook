// 对话任务相关类型定义

export interface ChatTask {
  id: string;
  title: string;
  description?: string;
  workspaceId: string;
  status: 'ACTIVE' | 'COMPLETED' | 'ARCHIVED';
  favorite: boolean;
  createdAt: string;
  updatedAt: string;
  contextId?: string; // 添加contextId字段
  chatHistory?: ChatHistoryItem[]; // 添加聊天历史字段
}

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: string;
}

export interface ChatTitleGenerateRequest {
  question: string;
  async?: boolean;
}

export interface ChatTitleGenerateResponse {
  title: string;
  async: boolean;
  duration?: number;
}

export interface ChatSession {
  id: string;
  title: string;
  messages: ChatMessage[];
  workspaceId: string;
  createdAt: string;
  updatedAt: string;
  contextId?: string; // 添加contextId字段
}

// 添加聊天历史详情相关类型
export interface ChatHistoryItem {
  contextId: string;
  finalResult: boolean;
  kind: string;
  status?: {
    message?: {
      contextId: string;
      kind: string;
      messageId: string;
      parts: any[];  // 修改为any[]类型以支持各种格式
      role: string;
    };
    state?: string;
    timestamp?: string;
  };
  taskId: string;
  message?: {
    contextId: string;
    kind: string;
    messageId: string;
    parts: any[];  // 修改为any[]类型以支持各种格式
    role: string;
    taskId: string;
  };
}

export interface ChatMessageItem {
  contextId: string;
  kind: string;
  messageId: string;
  parts: ChatMessagePart[];
  role: string;
  taskId?: string;
}

export interface ChatMessagePart {
  kind: string;
  text: string;
}

export interface ChatHistoryResponse {
  code: number;
  message: string;
  data: ChatHistoryItem[];
}