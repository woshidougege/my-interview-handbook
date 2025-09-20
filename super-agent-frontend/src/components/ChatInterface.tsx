import React, { useState, useRef, useEffect } from 'react';
import { 
  Card, 
  Input, 
  Button, 
  Space, 
  Typography, 
  Avatar, 
  Spin, 
  message,
  Tooltip
} from 'antd';
import { 
  SendOutlined, 
  RobotOutlined, 
  UserOutlined,
  ThunderboltOutlined
} from '@ant-design/icons';
import { ChatMessage } from '@/types/chat';
import aliCloudAiService from '@/services/aliCloudAiService';

const { TextArea } = Input;
const { Text, Paragraph } = Typography;

interface ChatInterfaceProps {
  workspaceId: string;
  sessionId?: string;
  userId: string;
  onTitleGenerated?: (title: string) => void;
  className?: string;
}

const ChatInterface: React.FC<ChatInterfaceProps> = ({
  workspaceId,
  sessionId,
  userId,
  onTitleGenerated,
  className
}) => {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputValue, setInputValue] = useState('');
  const [loading, setLoading] = useState(false);
  const [titleGenerating, setTitleGenerating] = useState(false);
  const [currentStreamMessage, setCurrentStreamMessage] = useState<string>('');
  const [isStreaming, setIsStreaming] = useState(false);
  const [aiServiceAvailable, setAiServiceAvailable] = useState(true);
  
  const inputRef = useRef<HTMLTextAreaElement>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // 滚动到底部
  useEffect(() => {
    scrollToBottom();
  }, [messages, currentStreamMessage]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  // 检查AI服务可用性
  useEffect(() => {
    const checkAiService = () => {
      const available = aliCloudAiService.isAvailable();
      setAiServiceAvailable(available);
      if (!available) {
        message.warning('AI服务配置不完整，请检查API密钥设置');
      }
    };
    
    checkAiService();
  }, []);

  // 处理流式消息片段
  const handleStreamChunk = (chunk: string) => {
    setIsStreaming(true);
    setCurrentStreamMessage(prev => prev + chunk);
  };

  // 处理流式消息结束
  const handleStreamEnd = () => {
    setCurrentStreamMessage(prevStreamMessage => {
      if (prevStreamMessage.trim()) {
        const aiMessage: ChatMessage = {
          id: Date.now().toString(),
          role: 'assistant',
          content: prevStreamMessage,
          timestamp: new Date().toISOString()
        };
        setMessages(prev => [...prev, aiMessage]);
      }
      return ''; // 清空流式消息
    });
    
    // 明确重置所有流相关状态
    setIsStreaming(false);
    setLoading(false);
    
    // 确保输入框可以继续使用
    setTimeout(() => {
      if (inputRef.current) {
        inputRef.current.focus();
      }
    }, 100);
  };

  // 生成标题
  const generateTitle = async (firstMessage: string) => {
    if (!onTitleGenerated || !aiServiceAvailable) return;
    
    setTitleGenerating(true);
    try {
      const title = await aliCloudAiService.generateTitle(firstMessage);
      onTitleGenerated(title);
      message.success(`标题已生成: ${title}`);
    } catch (error) {
      console.error('标题生成失败:', error);
      message.error('标题生成失败');
    } finally {
      setTitleGenerating(false);
    }
  };

  // 发送消息
  const sendMessage = async () => {
    if (!inputValue.trim() || !aiServiceAvailable) return;

    const userMessage: ChatMessage = {
      id: Date.now().toString(),
      role: 'user',
      content: inputValue.trim(),
      timestamp: new Date().toISOString()
    };

    // 立即显示用户消息
    setMessages(prev => [...prev, userMessage]);
    
    // 如果是第一条消息，生成标题
    if (messages.length === 0) {
      generateTitle(userMessage.content);
    }
    
    const messageContent = inputValue.trim();
    setInputValue('');
    setLoading(true);
    setCurrentStreamMessage('');

    // 构建聊天历史，包括当前消息
    const chatHistory = [...messages, userMessage].map(msg => ({
      role: msg.role as 'user' | 'assistant',
      content: msg.content
    }));

    try {
      // 使用阿里云AI服务进行流式聊天
      await aliCloudAiService.streamChat(
        chatHistory,
        {
          onChunk: (chunk: string) => {
            handleStreamChunk(chunk);
          },
          onComplete: () => {
            handleStreamEnd();
          },
          onError: (error: string) => {
            console.error('AI聊天失败:', error);
            message.error(`AI聊天失败: ${error}`);
            setLoading(false);
            setIsStreaming(false);
            setCurrentStreamMessage('');
          }
        }
      );
    } catch (error) {
      console.error('发送消息失败:', error);
      message.error('发送消息失败');
      setLoading(false);
      setIsStreaming(false);
      setCurrentStreamMessage('');
    }
  };

  // 处理回车发送
  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  return (
    <div className={className} style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* AI服务状态提示 */}
      {!aiServiceAvailable && (
        <div style={{ 
          padding: '8px 16px', 
          background: '#fff1f0', 
          border: '1px solid #ffccc7',
          borderRadius: '6px',
          margin: '8px 16px',
          color: '#cf1322'
        }}>
          ⚠️ AI服务配置不完整，请设置环境变量 NEXT_PUBLIC_DASHSCOPE_API_KEY
        </div>
      )}

      {/* 消息区域 */}
      <div style={{ 
        flex: 1, 
        padding: '16px', 
        overflowY: 'auto',
        background: '#f8f9fa'
      }}>
        {messages.length === 0 ? (
          <div style={{ 
            display: 'flex', 
            justifyContent: 'center', 
            alignItems: 'center', 
            height: '100%',
            flexDirection: 'column'
          }}>
            <RobotOutlined style={{ fontSize: '48px', color: '#bfbfbf', marginBottom: '16px' }} />
            <Text type="secondary" style={{ fontSize: '16px' }}>
              开始与Super Agent对话，享受智能助手服务
            </Text>
          </div>
        ) : (
          <Space direction="vertical" size="large" style={{ width: '100%' }}>
            {messages.map((message) => (
              <div
                key={message.id}
                style={{
                  display: 'flex',
                  justifyContent: message.role === 'user' ? 'flex-end' : 'flex-start',
                  width: '100%'
                }}
              >
                <div style={{ 
                  display: 'flex', 
                  alignItems: 'flex-start', 
                  maxWidth: '75%',
                  flexDirection: message.role === 'user' ? 'row-reverse' : 'row'
                }}>
                  <Avatar 
                    style={{ 
                      backgroundColor: message.role === 'user' ? '#52c41a' : '#1890ff',
                      margin: message.role === 'user' ? '0 0 0 12px' : '0 12px 0 0',
                      flexShrink: 0
                    }}
                    icon={message.role === 'user' ? <UserOutlined /> : <RobotOutlined />}
                  >
                    {message.role === 'user' ? null : 'AI'}
                  </Avatar>
                  
                  <Card
                    size="small"
                    style={{
                      backgroundColor: message.role === 'user' ? '#1890ff' : '#fff',
                      borderRadius: '12px',
                      maxWidth: '100%',
                      wordBreak: 'break-word',
                      border: message.role === 'user' ? 'none' : '1px solid #f0f0f0'
                    }}
                    bodyStyle={{ padding: '12px 16px' }}
                  >
                    <Paragraph 
                      style={{ 
                        margin: 0, 
                        color: message.role === 'user' ? '#fff' : '#333',
                        fontSize: '14px',
                        lineHeight: '1.6'
                      }}
                    >
                      {message.content}
                    </Paragraph>
                    <Text 
                      style={{ 
                        fontSize: '12px', 
                        opacity: 0.7,
                        color: message.role === 'user' ? '#fff' : '#999',
                        display: 'block',
                        marginTop: '4px'
                      }}
                    >
                      {new Date(message.timestamp).toLocaleTimeString()}
                    </Text>
                  </Card>
                </div>
              </div>
            )            )}
            
            {/* 流式AI回复 */}
            {(isStreaming || loading) && (
              <div style={{ display: 'flex', justifyContent: 'flex-start' }}>
                <div style={{ display: 'flex', alignItems: 'flex-start' }}>
                  <Avatar 
                    style={{ backgroundColor: '#1890ff', marginRight: '12px' }}
                    icon={<RobotOutlined />}
                  >
                    AI
                  </Avatar>
                  <Card
                    size="small"
                    style={{ borderRadius: '12px', minWidth: currentStreamMessage ? '200px' : 'auto' }}
                    bodyStyle={{ padding: '12px 16px' }}
                  >
                    {currentStreamMessage ? (
                      <>
                        <Paragraph 
                          style={{ 
                            margin: 0, 
                            color: '#333',
                            fontSize: '14px',
                            lineHeight: '1.6',
                            whiteSpace: 'pre-wrap'
                          }}
                        >
                          {currentStreamMessage}
                        </Paragraph>
                        <div style={{ display: 'flex', alignItems: 'center', marginTop: '4px' }}>
                          <Spin size="small" />
                          <Text style={{ marginLeft: '8px', color: '#666', fontSize: '12px' }}>
                            正在生成...
                          </Text>
                        </div>
                      </>
                    ) : (
                      <>
                        <Spin size="small" />
                        <Text style={{ marginLeft: '8px', color: '#666' }}>
                          Super Agent 正在思考中...
                        </Text>
                      </>
                    )}
                  </Card>
                </div>
              </div>
            )}
            <div ref={messagesEndRef} />
          </Space>
        )}
      </div>

      {/* 输入区域 */}
      <div style={{ 
        padding: '16px', 
        borderTop: '1px solid #e8e8e8',
        background: '#fff'
      }}>
        <div style={{ position: 'relative' }}>
          <TextArea
            ref={inputRef}
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            onKeyPress={handleKeyPress}
            placeholder="输入您的问题，Super Agent 将为您提供智能解答..."
            autoSize={{ minRows: 1, maxRows: 4 }}
            style={{ 
              resize: 'none',
              paddingRight: '120px'
            }}
            disabled={loading || titleGenerating || !aiServiceAvailable}
          />
          
          <div style={{
            position: 'absolute',
            right: '8px',
            bottom: '8px',
            display: 'flex',
            gap: '8px'
          }}>
            {titleGenerating && (
              <Tooltip title="正在生成标题">
                <Button
                  type="text"
                  icon={<ThunderboltOutlined />}
                  loading={titleGenerating}
                  size="small"
                />
              </Tooltip>
            )}
            
            <Button
              type="primary"
              icon={<SendOutlined />}
              onClick={sendMessage}
              loading={loading}
              disabled={!inputValue.trim() || titleGenerating || !aiServiceAvailable}
              size="small"
            >
              发送
            </Button>
          </div>
        </div>
        
        <div style={{ 
          marginTop: '8px', 
          fontSize: '12px', 
          color: '#999',
          textAlign: 'center'
        }}>
          按 Enter 发送，Shift + Enter 换行
        </div>
      </div>
    </div>
  );
};

export default ChatInterface;
