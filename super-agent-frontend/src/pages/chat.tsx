import React, { useState, useRef, useEffect } from 'react';
import Head from 'next/head';
import { 
  Layout, 
  Input, 
  Button, 
  Card, 
  List, 
  Avatar, 
  Typography, 
  Space, 
  Spin, 
  message,
  Modal,
  Tooltip,
  Empty
} from 'antd';
import { 
  SendOutlined, 
  PlusOutlined, 
  StarOutlined, 
  StarFilled,
  EditOutlined,
  DeleteOutlined,
  MessageOutlined
} from '@ant-design/icons';
import AppLayout from '@/components/Layout/AppLayout';
import { aiApi, chatTaskApi } from '@/services/api';
import { ChatMessage, ChatTask, ChatSession } from '@/types/chat';

const { Sider, Content } = Layout;
const { TextArea } = Input;
const { Title, Text, Paragraph } = Typography;

const ChatPage: React.FC = () => {
  const [currentSession, setCurrentSession] = useState<ChatSession | null>(null);
  const [sessions, setSessions] = useState<ChatTask[]>([]);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputValue, setInputValue] = useState('');
  const [loading, setLoading] = useState(false);
  const [sessionsLoading, setSessionsLoading] = useState(false);
  const [titleGenerating, setTitleGenerating] = useState(false);
  const [newSessionModalVisible, setNewSessionModalVisible] = useState(false);
  const [editingSession, setEditingSession] = useState<ChatTask | null>(null);
  
  const inputRef = useRef<any>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  
  // 模拟工作空间ID（实际应用中应该从上下文或路由获取）
  const workspaceId = '1234567890123456789';

  // 页面加载时获取会话列表
  useEffect(() => {
    loadSessions();
  }, []);

  // 滚动到消息底部
  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  // 加载会话列表
  const loadSessions = async () => {
    setSessionsLoading(true);
    try {
      const response = await chatTaskApi.getChatTasks(workspaceId, {
        pageNum: 1,
        pageSize: 50
      });
      setSessions(response.data.data.records || []);
    } catch (error) {
      message.error('加载会话列表失败');
    } finally {
      setSessionsLoading(false);
    }
  };

  // 创建新会话
  const createNewSession = async (firstQuestion?: string) => {
    if (firstQuestion) {
      setTitleGenerating(true);
      try {
        // 先生成标题
        const titleResponse = await aiApi.generateChatTitle(workspaceId, {
          question: firstQuestion,
          async: false
        });
        
        const generatedTitle = titleResponse.data.data.title;
        
        // 创建会话任务
        const sessionResponse = await chatTaskApi.createChatTask(workspaceId, {
          title: generatedTitle,
          description: `会话开始于: ${firstQuestion.substring(0, 50)}...`
        });
        
        const newSession = sessionResponse.data.data;
        setSessions(prev => [newSession, ...prev]);
        
        // 设置当前会话
        const chatSession: ChatSession = {
          id: newSession.id,
          title: newSession.title,
          messages: [],
          workspaceId: newSession.workspaceId,
          createdAt: newSession.createdAt,
          updatedAt: newSession.updatedAt
        };
        
        setCurrentSession(chatSession);
        setMessages([]);
        
        message.success('新会话创建成功');
        return chatSession;
      } catch (error) {
        message.error('创建会话失败');
        throw error;
      } finally {
        setTitleGenerating(false);
      }
    } else {
      setNewSessionModalVisible(true);
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

    // 如果没有当前会话，先创建一个
    let session = currentSession;
    if (!session) {
      try {
        session = await createNewSession(userMessage.content);
      } catch (error) {
        return;
      }
    }

    // 添加用户消息
    const newMessages = [...messages, userMessage];
    setMessages(newMessages);
    setInputValue('');
    setLoading(true);

    try {
      // 模拟AI回复（实际应用中应该调用真实的AI接口）
      await new Promise(resolve => setTimeout(resolve, 1000 + Math.random() * 2000));
      
      const aiMessage: ChatMessage = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: `这是对"${userMessage.content}"的回复。在实际应用中，这里会调用真实的AI服务来生成回复。`,
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

  // 选择会话
  const selectSession = async (session: ChatTask) => {
    const chatSession: ChatSession = {
      id: session.id,
      title: session.title,
      messages: [], // 实际应用中应该加载历史消息
      workspaceId: session.workspaceId,
      createdAt: session.createdAt,
      updatedAt: session.updatedAt
    };
    
    setCurrentSession(chatSession);
    setMessages([]); // 实际应用中应该加载历史消息
  };

  // 收藏/取消收藏会话
  const toggleFavorite = async (session: ChatTask, e: React.MouseEvent) => {
    e.stopPropagation();
    try {
      if (session.favorite) {
        await chatTaskApi.unfavoriteChatTask(workspaceId, session.id);
      } else {
        await chatTaskApi.favoriteChatTask(workspaceId, session.id);
      }
      
      setSessions(prev => prev.map(s => 
        s.id === session.id ? { ...s, favorite: !s.favorite } : s
      ));
      
      message.success(session.favorite ? '已取消收藏' : '已收藏');
    } catch (error) {
      message.error('操作失败');
    }
  };

  // 删除会话
  const deleteSession = async (session: ChatTask, e: React.MouseEvent) => {
    e.stopPropagation();
    
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除会话"${session.title}"吗？`,
      onOk: async () => {
        try {
          await chatTaskApi.deleteChatTask(workspaceId, session.id);
          setSessions(prev => prev.filter(s => s.id !== session.id));
          
          if (currentSession?.id === session.id) {
            setCurrentSession(null);
            setMessages([]);
          }
          
          message.success('会话已删除');
        } catch (error) {
          message.error('删除失败');
        }
      }
    });
  };

  // 编辑会话标题
  const editSessionTitle = (session: ChatTask, e: React.MouseEvent) => {
    e.stopPropagation();
    setEditingSession(session);
  };

  // 保存编辑的标题
  const saveSessionTitle = async (newTitle: string) => {
    if (!editingSession || !newTitle.trim()) return;
    
    try {
      await chatTaskApi.updateChatTask(workspaceId, editingSession.id, {
        title: newTitle.trim()
      });
      
      setSessions(prev => prev.map(s => 
        s.id === editingSession.id ? { ...s, title: newTitle.trim() } : s
      ));
      
      if (currentSession?.id === editingSession.id) {
        setCurrentSession(prev => prev ? { ...prev, title: newTitle.trim() } : null);
      }
      
      setEditingSession(null);
      message.success('标题已更新');
    } catch (error) {
      message.error('更新失败');
    }
  };

  return (
    <>
      <Head>
        <title>AI对话 - Super Agent</title>
        <meta name="description" content="Super Agent AI对话功能" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <link rel="icon" href="/favicon.ico" />
      </Head>
      
      <AppLayout>
        <Layout style={{ height: 'calc(100vh - 64px)', background: '#fff' }}>
          {/* 侧边栏 - 会话列表 */}
          <Sider 
            width={300} 
            style={{ 
              background: '#f8f9fa', 
              borderRight: '1px solid #e8e8e8',
              height: '100%',
              overflow: 'auto'
            }}
          >
            <div style={{ padding: '16px' }}>
              <Button 
                type="primary" 
                icon={<PlusOutlined />} 
                block 
                size="large"
                onClick={() => createNewSession()}
                loading={titleGenerating}
              >
                新建对话
              </Button>
            </div>
            
            <div style={{ padding: '0 16px 16px' }}>
              <Title level={5} style={{ margin: '0 0 12px 0', color: '#666' }}>
                对话历史
              </Title>
              
              <Spin spinning={sessionsLoading}>
                {sessions.length === 0 ? (
                  <Empty 
                    description="暂无对话记录" 
                    style={{ margin: '40px 0' }}
                  />
                ) : (
                  <List
                    dataSource={sessions}
                    renderItem={(session) => (
                      <List.Item
                        style={{
                          padding: '12px',
                          marginBottom: '8px',
                          background: currentSession?.id === session.id ? '#e6f7ff' : '#fff',
                          borderRadius: '6px',
                          cursor: 'pointer',
                          border: currentSession?.id === session.id ? '1px solid #1890ff' : '1px solid #f0f0f0'
                        }}
                        onClick={() => selectSession(session)}
                      >
                        <div style={{ width: '100%' }}>
                          <div style={{ 
                            display: 'flex', 
                            justifyContent: 'space-between', 
                            alignItems: 'flex-start',
                            marginBottom: '4px'
                          }}>
                            {editingSession?.id === session.id ? (
                              <Input
                                size="small"
                                defaultValue={session.title}
                                onPressEnter={(e) => saveSessionTitle((e.target as HTMLInputElement).value)}
                                onBlur={(e) => saveSessionTitle(e.target.value)}
                                onClick={(e) => e.stopPropagation()}
                                autoFocus
                              />
                            ) : (
                              <Text 
                                strong 
                                style={{ 
                                  fontSize: '14px',
                                  lineHeight: '20px',
                                  color: currentSession?.id === session.id ? '#1890ff' : '#333'
                                }}
                                ellipsis={{ tooltip: session.title }}
                              >
                                {session.title}
                              </Text>
                            )}
                            
                            <Space size="small">
                              <Tooltip title={session.favorite ? '取消收藏' : '收藏'}>
                                <Button
                                  type="text"
                                  size="small"
                                  icon={session.favorite ? <StarFilled style={{ color: '#faad14' }} /> : <StarOutlined />}
                                  onClick={(e) => toggleFavorite(session, e)}
                                />
                              </Tooltip>
                              <Tooltip title="编辑标题">
                                <Button
                                  type="text"
                                  size="small"
                                  icon={<EditOutlined />}
                                  onClick={(e) => editSessionTitle(session, e)}
                                />
                              </Tooltip>
                              <Tooltip title="删除">
                                <Button
                                  type="text"
                                  size="small"
                                  danger
                                  icon={<DeleteOutlined />}
                                  onClick={(e) => deleteSession(session, e)}
                                />
                              </Tooltip>
                            </Space>
                          </div>
                          
                          <Text 
                            type="secondary" 
                            style={{ fontSize: '12px' }}
                          >
                            {new Date(session.updatedAt).toLocaleString()}
                          </Text>
                        </div>
                      </List.Item>
                    )}
                  />
                )}
              </Spin>
            </div>
          </Sider>

          {/* 主要内容区域 */}
          <Content style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
            {currentSession ? (
              <>
                {/* 对话头部 */}
                <div style={{ 
                  padding: '16px 24px', 
                  borderBottom: '1px solid #e8e8e8',
                  background: '#fff'
                }}>
                  <Title level={4} style={{ margin: 0 }}>
                    {currentSession.title}
                  </Title>
                </div>

                {/* 消息列表 */}
                <div style={{ 
                  flex: 1, 
                  padding: '16px 24px', 
                  overflowY: 'auto',
                  background: '#fafafa'
                }}>
                  {messages.length === 0 ? (
                    <div style={{ 
                      display: 'flex', 
                      justifyContent: 'center', 
                      alignItems: 'center', 
                      height: '100%',
                      flexDirection: 'column'
                    }}>
                      <MessageOutlined style={{ fontSize: '48px', color: '#bfbfbf', marginBottom: '16px' }} />
                      <Text type="secondary" style={{ fontSize: '16px' }}>
                        开始你的AI对话之旅
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
                          <div style={{ display: 'flex', alignItems: 'flex-start', maxWidth: '70%' }}>
                            {message.role === 'assistant' && (
                              <Avatar 
                                style={{ 
                                  backgroundColor: '#1890ff', 
                                  marginRight: '12px',
                                  flexShrink: 0
                                }}
                              >
                                AI
                              </Avatar>
                            )}
                            
                            <Card
                              size="small"
                              style={{
                                backgroundColor: message.role === 'user' ? '#1890ff' : '#fff',
                                color: message.role === 'user' ? '#fff' : '#333',
                                borderRadius: '12px',
                                maxWidth: '100%',
                                wordBreak: 'break-word'
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
                            
                            {message.role === 'user' && (
                              <Avatar 
                                style={{ 
                                  backgroundColor: '#52c41a', 
                                  marginLeft: '12px',
                                  flexShrink: 0
                                }}
                              >
                                我
                              </Avatar>
                            )}
                          </div>
                        </div>
                      ))}
                      
                      {loading && (
                        <div style={{ display: 'flex', justifyContent: 'flex-start' }}>
                          <div style={{ display: 'flex', alignItems: 'flex-start' }}>
                            <Avatar style={{ backgroundColor: '#1890ff', marginRight: '12px' }}>
                              AI
                            </Avatar>
                            <Card
                              size="small"
                              style={{ borderRadius: '12px' }}
                              bodyStyle={{ padding: '12px 16px' }}
                            >
                              <Spin size="small" />
                              <Text style={{ marginLeft: '8px', color: '#666' }}>
                                正在思考中...
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
                  padding: '16px 24px', 
                  borderTop: '1px solid #e8e8e8',
                  background: '#fff'
                }}>
                  <Space.Compact style={{ width: '100%' }}>
                    <TextArea
                      ref={inputRef}
                      value={inputValue}
                      onChange={(e) => setInputValue(e.target.value)}
                      onKeyPress={handleKeyPress}
                      placeholder="输入您的问题..."
                      autoSize={{ minRows: 1, maxRows: 4 }}
                      style={{ resize: 'none' }}
                      disabled={loading}
                    />
                    <Button
                      type="primary"
                      icon={<SendOutlined />}
                      onClick={sendMessage}
                      loading={loading}
                      disabled={!inputValue.trim()}
                      style={{ height: 'auto' }}
                    >
                      发送
                    </Button>
                  </Space.Compact>
                </div>
              </>
            ) : (
              // 欢迎页面
              <div style={{ 
                display: 'flex', 
                justifyContent: 'center', 
                alignItems: 'center', 
                height: '100%',
                flexDirection: 'column',
                background: '#fafafa'
              }}>
                <MessageOutlined style={{ fontSize: '72px', color: '#bfbfbf', marginBottom: '24px' }} />
                <Title level={2} style={{ color: '#666', marginBottom: '16px' }}>
                  欢迎使用 Super Agent
                </Title>
                <Paragraph style={{ fontSize: '16px', color: '#999', textAlign: 'center', marginBottom: '32px' }}>
                  您的数字分身，按照您的目标自主规划、执行并交付<br />
                  开始新对话，体验AI助手的强大能力
                </Paragraph>
                <Button 
                  type="primary" 
                  size="large"
                  icon={<PlusOutlined />}
                  onClick={() => createNewSession()}
                  loading={titleGenerating}
                >
                  开始新对话
                </Button>
              </div>
            )}
          </Content>
        </Layout>
      </AppLayout>
    </>
  );
};

export default ChatPage;
