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
import { aiApi } from '@/services/api';
import { ChatMessage } from '@/types/chat';

const { TextArea } = Input;
const { Text, Paragraph } = Typography;

interface ChatInterfaceProps {
  workspaceId: string;
  sessionId?: string;
  onTitleGenerated?: (title: string) => void;
  className?: string;
}

const ChatInterface: React.FC<ChatInterfaceProps> = ({
  workspaceId,
  sessionId,
  onTitleGenerated,
  className
}) => {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputValue, setInputValue] = useState('');
  const [loading, setLoading] = useState(false);
  const [titleGenerating, setTitleGenerating] = useState(false);
  
  const inputRef = useRef<any>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // 滚动到底部
  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

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
    } catch (error) {
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

    const newMessages = [...messages, userMessage];
    setMessages(newMessages);
    
    // 如果是第一条消息，生成标题
    if (messages.length === 0) {
      generateTitle(userMessage.content);
    }
    
    setInputValue('');
    setLoading(true);

    try {
      // 模拟AI回复（实际应用中调用真实AI接口）
      await new Promise(resolve => setTimeout(resolve, 1500 + Math.random() * 2000));
      
      const aiMessage: ChatMessage = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: `这是对"${userMessage.content}"的智能回复。Super Agent 正在根据您的需求提供专业的解答和建议。`,
        timestamp: new Date().toISOString()
      };

      setMessages(prev => [...prev, aiMessage]);
    } catch (error) {
      message.error('发送消息失败');
    } finally {
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

  return (
    <div className={className} style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
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
            ))}
            
            {loading && (
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
                    style={{ borderRadius: '12px' }}
                    bodyStyle={{ padding: '12px 16px' }}
                  >
                    <Spin size="small" />
                    <Text style={{ marginLeft: '8px', color: '#666' }}>
                      Super Agent 正在思考中...
                    </Text>
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
            disabled={loading || titleGenerating}
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
              disabled={!inputValue.trim() || titleGenerating}
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
