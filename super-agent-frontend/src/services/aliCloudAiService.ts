/**
 * 阿里云百炼AI服务
 * 直接调用阿里云百炼OpenAI兼容接口，替代后端代理
 * 
 * @author AI Assistant
 * @since 1.0.0
 */

import OpenAI from 'openai';
import { AI_CONFIG, validateAiConfig } from '@/config/aiConfig';

/**
 * 流式聊天回调函数类型
 */
export interface StreamCallbacks {
  onChunk?: (chunk: string) => void;
  onComplete?: (finalMessage: string) => void;
  onError?: (error: string) => void;
}

/**
 * 聊天消息类型
 */
export interface ChatMessage {
  role: 'system' | 'user' | 'assistant';
  content: string;
}

/**
 * 阿里云AI服务类
 */
class AliCloudAiService {
  private openai: OpenAI | null = null;
  private initialized = false;

  /**
   * 初始化OpenAI客户端
   */
  private initialize(): boolean {
    if (this.initialized) {
      return true;
    }

    const validation = validateAiConfig();
    if (!validation.valid) {
      console.error('AI配置验证失败:', validation.errors);
      return false;
    }

    try {
      this.openai = new OpenAI({
        apiKey: AI_CONFIG.DASHSCOPE.API_KEY,
        baseURL: AI_CONFIG.DASHSCOPE.BASE_URL,
        dangerouslyAllowBrowser: true, // 允许在浏览器中使用
      });

      this.initialized = true;
      console.log('🤖 阿里云AI服务初始化成功');
      return true;
    } catch (error) {
      console.error('AI服务初始化失败:', error);
      return false;
    }
  }

  /**
   * 生成对话标题
   */
  async generateTitle(question: string): Promise<string> {
    if (!this.initialize() || !this.openai) {
      return AI_CONFIG.TITLE_GENERATION.prompt.replace('{{question}}', question.slice(0, 20));
    }

    try {
      const prompt = AI_CONFIG.TITLE_GENERATION.prompt.replace('{{question}}', question);
      
      const response = await this.openai.chat.completions.create({
        model: AI_CONFIG.TITLE_GENERATION.model,
        messages: [
          { role: 'user', content: prompt }
        ],
        temperature: AI_CONFIG.TITLE_GENERATION.temperature,
        max_tokens: 50,
        stream: false,
      });

      const title = response.choices[0]?.message?.content?.trim() || '新对话';
      
      // 确保标题长度不超过限制
      return title.length > AI_CONFIG.TITLE_GENERATION.maxLength 
        ? title.slice(0, AI_CONFIG.TITLE_GENERATION.maxLength) + '...'
        : title;
        
    } catch (error) {
      console.error('标题生成失败:', error);
      // 降级处理：返回问题的前几个字符作为标题
      return question.length > 10 ? question.slice(0, 10) + '...' : question || '新对话';
    }
  }

  /**
   * 同步聊天（非流式）
   */
  async chat(messages: ChatMessage[], model?: string): Promise<string> {
    if (!this.initialize() || !this.openai) {
      throw new Error('AI服务未初始化或配置错误');
    }

    try {
      const response = await this.openai.chat.completions.create({
        model: model || AI_CONFIG.DASHSCOPE.DEFAULT_MODEL,
        messages: [
          { role: 'system', content: AI_CONFIG.DASHSCOPE.SYSTEM_PROMPT },
          ...messages
        ],
        temperature: AI_CONFIG.DASHSCOPE.DEFAULT_PARAMS.temperature,
        max_tokens: AI_CONFIG.DASHSCOPE.DEFAULT_PARAMS.max_tokens,
        top_p: AI_CONFIG.DASHSCOPE.DEFAULT_PARAMS.top_p,
        stream: false,
      });

      return response.choices[0]?.message?.content || '抱歉，我没有理解您的问题。';
    } catch (error) {
      console.error('AI聊天失败:', error);
      throw new Error(AI_CONFIG.ERROR_HANDLING.defaultErrorMessage);
    }
  }

  /**
   * 流式聊天
   */
  async streamChat(
    messages: ChatMessage[], 
    callbacks: StreamCallbacks,
    model?: string
  ): Promise<void> {
    if (!this.initialize() || !this.openai) {
      callbacks.onError?.('AI服务未初始化或配置错误');
      return;
    }

    try {
      const stream = await this.openai.chat.completions.create({
        model: model || AI_CONFIG.DASHSCOPE.DEFAULT_MODEL,
        messages: [
          { role: 'system', content: AI_CONFIG.DASHSCOPE.SYSTEM_PROMPT },
          ...messages
        ],
        temperature: AI_CONFIG.DASHSCOPE.DEFAULT_PARAMS.temperature,
        max_tokens: AI_CONFIG.DASHSCOPE.DEFAULT_PARAMS.max_tokens,
        top_p: AI_CONFIG.DASHSCOPE.DEFAULT_PARAMS.top_p,
        stream: true,
      });

      let fullMessage = '';
      
      for await (const chunk of stream) {
        const content = chunk.choices[0]?.delta?.content || '';
        if (content) {
          fullMessage += content;
          callbacks.onChunk?.(content);
        }
        
        // 检查是否完成
        if (chunk.choices[0]?.finish_reason === 'stop') {
          callbacks.onComplete?.(fullMessage);
          break;
        }
      }
    } catch (error) {
      console.error('流式聊天失败:', error);
      callbacks.onError?.(AI_CONFIG.ERROR_HANDLING.defaultErrorMessage);
    }
  }

  /**
   * 检查服务是否可用
   */
  isAvailable(): boolean {
    return this.initialize();
  }

  /**
   * 获取可用模型列表
   */
  getAvailableModels(): string[] {
    return Object.values(AI_CONFIG.DASHSCOPE.MODELS);
  }
}

// 导出单例实例
export const aliCloudAiService = new AliCloudAiService();
export default aliCloudAiService;
