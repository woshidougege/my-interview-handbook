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
}
