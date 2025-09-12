import React, { useState, useRef, useEffect, useCallback } from 'react';
import { 
  Card, 
  Input, 
  Button, 
  Space, 
  Typography, 
  Avatar, 
  Spin, 
  message,
  Tooltip,
  Alert
} from 'antd';
import { 
  SendOutlined, 
  RobotOutlined, 
  UserOutlined,
  ThunderboltOutlined,
  WifiOutlined,
  DisconnectOutlined
} from '@ant-design/icons';
import { aiApi } from '@/services/api';
import { ChatMessage } from '@/types/chat';
import sseService, { ConnectionStatus } from '@/services/sseService';

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
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>(ConnectionStatus.DISCONNECTED);
  const [currentStreamMessage, setCurrentStreamMessage] = useState<string>('');
  const [isStreaming, setIsStreaming] = useState(false);
  
  const inputRef = useRef<HTMLTextAreaElement>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // 滚动到底部
  useEffect(() => {
    scrollToBottom();
  }, [messages, currentStreamMessage]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  // 组件卸载时断开SSE连接
  useEffect(() => {
    return () => {
      sseService.disconnect();
    };
  }, []);

  // 处理SSE消息
  const handleSseMessage = useCallback((msg: { eventType: string; content: string }) => {
    console.log('收到SSE消息:', msg);
    // 根据事件类型处理消息
    switch (msg.eventType) {
      case 'connected':
        console.log('SSE连接已建立:', msg.content);
        break;
      case 'ai_thinking':
        console.log('AI正在思考:', msg.content);
        break;
      case 'error':
        message.error(`AI服务错误: ${msg.content}`);
        setLoading(false);
        setIsStreaming(false);
        setCurrentStreamMessage('');
        break;
    }
  }, []);

  // 处理流式消息片段
  const handleStreamChunk = useCallback((chunk: string) => {
    setIsStreaming(true);
    setCurrentStreamMessage(prev => prev + chunk);
  }, []);

  // 处理流式消息结束
  const handleStreamEnd = useCallback(() => {
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
    setIsStreaming(false);
    setLoading(false);
  }, []);

  // 生成标题
  const generateTitle = async (firstMessage: string) => {
    if (!onTitleGenerated) return;
    
    setTitleGenerating(true);
    try {
      const response = await aiApi.generateChatTitle(workspaceId, {
        question: firstMessage,
        async: false
      });
      
      const title = response.data.data.title;
      onTitleGenerated(title);
      message.success(`标题已生成: ${title}`);
    } catch {
      message.error('标题生成失败');
    } finally {
      setTitleGenerating(false);
    }
  };

  // 发送消息
  const sendMessage = async () => {
    if (!inputValue.trim()) return;

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

    try {
      // 如果SSE连接未建立，先建立连接
      if (!sseService.isConnected()) {
        console.log('建立SSE连接...');
        
        // 使用有效的sessionId
        const effectiveSessionId = sessionId || `temp_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
        
        await sseService.connect(userId, workspaceId, effectiveSessionId, {
          onConnectionChange: (status) => {
            setConnectionStatus(status);
          },
          onMessage: (msg) => {
            handleSseMessage(msg);
          },
          onStreamChunk: (chunk) => {
            handleStreamChunk(chunk);
          },
          onStreamEnd: () => {
            handleStreamEnd();
          },
          onError: (errorMsg) => {
            message.error(`连接错误: ${errorMsg}`);
          }
        });
        
        // 等待连接建立
        let retries = 0;
        while (!sseService.isConnected() && retries < 10) {
          await new Promise(resolve => setTimeout(resolve, 200));
          retries++;
        }
        
        if (!sseService.isConnected()) {
          message.error('无法建立实时连接，请稍后重试');
          setLoading(false);
          return;
        }
      }
      
      // 通过SSE服务发送消息
      await sseService.sendMessage(messageContent);
    } catch (error) {
      console.error('发送消息失败', error);
      message.error('发送消息失败');
      setLoading(false);
    }
  };

  // 处理回车发送
  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  // 获取连接状态显示
  const getConnectionStatusDisplay = () => {
    switch (connectionStatus) {
      case ConnectionStatus.CONNECTED:
        return { icon: <WifiOutlined style={{ color: '#52c41a' }} />, text: '已连接', color: '#52c41a' };
      case ConnectionStatus.CONNECTING:
        return { icon: <Spin size="small" />, text: '连接中...', color: '#1890ff' };
      case ConnectionStatus.RECONNECTING:
        return { icon: <Spin size="small" />, text: '重连中...', color: '#faad14' };
      case ConnectionStatus.ERROR:
        return { icon: <DisconnectOutlined style={{ color: '#ff4d4f' }} />, text: '连接失败', color: '#ff4d4f' };
      default:
        return { icon: <DisconnectOutlined style={{ color: '#d9d9d9' }} />, text: '未连接', color: '#d9d9d9' };
    }
  };

  const statusDisplay = getConnectionStatusDisplay();

  return (
    <div className={className} style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* 连接状态栏 */}
      {connectionStatus !== ConnectionStatus.CONNECTED && (
        <Alert
          message={
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              {statusDisplay.icon}
              <span style={{ color: statusDisplay.color }}>{statusDisplay.text}</span>
            </div>
          }
          type={connectionStatus === ConnectionStatus.ERROR ? 'error' : 'warning'}
          showIcon={false}
          style={{ margin: '8px 16px', borderRadius: '6px' }}
        />
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
            disabled={loading || titleGenerating || connectionStatus !== ConnectionStatus.CONNECTED}
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
              disabled={!inputValue.trim() || titleGenerating || connectionStatus !== ConnectionStatus.CONNECTED}
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
