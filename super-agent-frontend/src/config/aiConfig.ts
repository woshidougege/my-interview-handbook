/**
 * 阿里云百炼AI配置
 * 
 * @author AI Assistant  
 * @since 1.0.0
 */

export const AI_CONFIG = {
  // 阿里云百炼API配置
  DASHSCOPE: {
    // API Key - 从环境变量获取
    API_KEY: process.env.NEXT_PUBLIC_DASHSCOPE_API_KEY || '',
    
    // API基础URL - 从环境变量获取，可自定义配置
    BASE_URL: process.env.NEXT_PUBLIC_DASHSCOPE_BASE_URL || 'https://dashscope.aliyuncs.com/compatible-mode/v1',
    
    // 默认模型配置
    DEFAULT_MODEL: 'qwen-plus',
    
    // 模型列表
    MODELS: {
      QWEN_TURBO: 'qwen-turbo',      // 快速模型
      QWEN_PLUS: 'qwen-plus',        // 平衡模型
      QWEN_MAX: 'qwen-max',          // 最强模型
    },
    
    // 默认参数
    DEFAULT_PARAMS: {
      temperature: 0.7,
      max_tokens: 2000,
      top_p: 0.9,
    },
    
    // 流式输出配置
    STREAM_CONFIG: {
      enabled: true,
      timeout: 30000, // 30秒超时
    },
    
    // 系统提示词
    SYSTEM_PROMPT: '你是Super Agent，一个智能助手。请用中文回答用户的问题，提供有用、准确和友好的回复。',
  },
  
  // 标题生成配置
  TITLE_GENERATION: {
    model: 'qwen-turbo', // 使用快速模型生成标题
    maxLength: 20,
    temperature: 0.5,
    prompt: `请根据用户的问题生成一个简洁、准确的会话标题。标题要求：
1. 不超过20个字符
2. 准确概括问题的核心内容  
3. 简洁明了，易于理解
4. 不包含特殊符号

用户问题：{{question}}

请直接返回标题，不要包含其他内容：`
  },
  
  // 错误处理配置
  ERROR_HANDLING: {
    defaultErrorMessage: '抱歉，AI服务暂时不可用，请稍后再试。',
    retryAttempts: 3,
    retryDelay: 1000,
  }
} as const;

// 验证配置
export const validateAiConfig = (): { valid: boolean; errors: string[] } => {
  const errors: string[] = [];
  
  if (!AI_CONFIG.DASHSCOPE.API_KEY) {
    errors.push('NEXT_PUBLIC_DASHSCOPE_API_KEY 环境变量未设置');
  }
  
  if (!AI_CONFIG.DASHSCOPE.BASE_URL) {
    errors.push('BASE_URL 未配置');
  }
  
  return {
    valid: errors.length === 0,
    errors
  };
};

// 开发环境下打印配置信息
if (process.env.NODE_ENV === 'development') {
  const validation = validateAiConfig();
  console.group('🤖 AI Configuration');
  console.log('Base URL:', AI_CONFIG.DASHSCOPE.BASE_URL);
  console.log('Default Model:', AI_CONFIG.DASHSCOPE.DEFAULT_MODEL);
  console.log('API Key Set:', !!AI_CONFIG.DASHSCOPE.API_KEY);
  console.log('Config Valid:', validation.valid);
  console.log('Environment Variables:');
  console.log('  - NEXT_PUBLIC_DASHSCOPE_API_KEY:', !!process.env.NEXT_PUBLIC_DASHSCOPE_API_KEY);
  console.log('  - NEXT_PUBLIC_DASHSCOPE_BASE_URL:', process.env.NEXT_PUBLIC_DASHSCOPE_BASE_URL || '(using default)');
  if (!validation.valid) {
    console.warn('Config Errors:', validation.errors);
  }
  console.groupEnd();
}
