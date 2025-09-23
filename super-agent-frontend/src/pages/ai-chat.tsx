import React, { useState, useRef, useEffect, useCallback } from 'react';
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
  Empty,
  Tooltip
} from 'antd';
import { 
  SendOutlined, 
  PlusOutlined, 
  MessageOutlined,
  ApiOutlined,
  AudioOutlined,
  AudioMutedOutlined
} from '@ant-design/icons';
import AppLayout from '@/components/Layout/AppLayout';
import { chatTaskApi, workspaceApi, userApi } from '@/services/api';
import { ChatMessage, ChatTask, ChatSession } from '@/types/chat';
import { API_ENDPOINTS, buildApiUrl } from '@/config/apiEndpoints';

const { Sider, Content } = Layout;
const { TextArea } = Input;
const { Title, Text, Paragraph } = Typography;

const AiChatPage: React.FC = () => {
  // 基础状态
  const [currentSession, setCurrentSession] = useState<ChatSession | null>(null);
  const [sessions, setSessions] = useState<ChatTask[]>([]);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputValue, setInputValue] = useState('');
  const [loading, setLoading] = useState(false);
  const [sessionsLoading, setSessionsLoading] = useState(false);
  const [currentWorkspace, setCurrentWorkspace] = useState<{ id: string; name: string; description?: string } | null>(null);
  const [workspaceLoading, setWorkspaceLoading] = useState(true);
  
  // 流式聊天状态
  const [isStreaming, setIsStreaming] = useState(false);
  const [streamingMessage, setStreamingMessage] = useState('');
  
  // 语音输入状态
  const [isRecording, setIsRecording] = useState(false);
  const [voiceStatus, setVoiceStatus] = useState<'idle' | 'recording' | 'processing' | 'error'>('idle');
  const [voiceMessage, setVoiceMessage] = useState<string>('');
  
  // 流式内容状态  
  const streamContentRef = useRef('');
  const streamTimestampRef = useRef<string>(''); // 保存流式消息的时间戳
  
  // 其他引用
  const inputRef = useRef<HTMLTextAreaElement>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const speechServiceRef = useRef<any>(null);

  // 滚动到底部
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  // 清理流式状态
  const clearTypewriterState = () => {
    streamContentRef.current = '';
    streamTimestampRef.current = '';
    setStreamingMessage('');
  };

  // 添加新的流式内容（HTTP流式版本）
  const addStreamContent = (newContent: string) => {
    streamContentRef.current += newContent;
    // HTTP streaming版本：使用第一次接收数据的时间作为消息时间戳
    if (!streamTimestampRef.current) {
      streamTimestampRef.current = new Date().toISOString();
    }
    setStreamingMessage(streamContentRef.current);
    scrollToBottom();
  };

  // 完成流式显示（HTTP流式版本）
  const finishStreaming = () => {
    const fullContent = streamContentRef.current;
    
    if (fullContent.trim()) {
      // HTTP流式版本：直接使用流开始时的时间戳
      const messageTimestamp = streamTimestampRef.current || new Date().toISOString();
      
      // 保存完整消息到历史
      const aiMessage: ChatMessage = {
        id: Date.now().toString(),
        role: 'assistant',
        content: fullContent,
        timestamp: messageTimestamp
      };
      
      console.log('💾 HTTP流式消息保存完成，内容长度:', fullContent.length, '时间戳:', messageTimestamp);
      setMessages(prev => [...prev, aiMessage]);
    }
    
    // 清理状态
    clearTypewriterState();
    setIsStreaming(false);
    setLoading(false);
  };

  // 加载会话列表
  const loadSessions = useCallback(async () => {
    if (!currentWorkspace?.id) return;
    
    setSessionsLoading(true);
    try {
      const response = await chatTaskApi.getChatTasks(currentWorkspace.id, {
        pageNum: 1,
        pageSize: 50
      });
      setSessions(response.data.data.records || []);
    } catch (error) {
      console.error('加载会话列表失败:', error);
      message.error('加载会话列表失败');
    } finally {
      setSessionsLoading(false);
    }
  }, [currentWorkspace?.id]);

  // 加载现有工作空间
  const loadExistingWorkspace = async () => {
    setWorkspaceLoading(true);
    try {
      const response = await workspaceApi.getWorkspaces();
      
      if (response.data.data && response.data.data.length > 0) {
        setCurrentWorkspace(response.data.data[0]);
      } else {
        setCurrentWorkspace(null);
      }
    } catch (error) {
      console.error('获取工作空间失败:', error);
      setCurrentWorkspace(null);
    } finally {
      setWorkspaceLoading(false);
    }
  };

  // 确保工作空间存在
  const ensureWorkspace = async () => {
    if (currentWorkspace?.id) {
      return currentWorkspace;
    }

    try {
      const response = await workspaceApi.getWorkspaces();
      
      if (response.data.data && response.data.data.length > 0) {
        const workspace = response.data.data[0];
        setCurrentWorkspace(workspace);
        return workspace;
      } else {
        const userResponse = await userApi.getCurrentUser();
        const currentUser = userResponse.data.data;
        
        const createResponse = await workspaceApi.createWorkspace({
          userId: currentUser.userId,
          name: '我的工作空间',
          description: 'Super Agent 默认工作空间'
        });
        const workspace = createResponse.data.data;
        setCurrentWorkspace(workspace);
        return workspace;
      }
    } catch (error) {
      console.error('创建工作空间失败:', error);
      message.error('创建工作空间失败');
      throw error;
    }
  };

  // 创建新会话
  const createNewSession = async (firstQuestion: string, workspace: { id: string; name: string; description?: string }) => {
    try {
      const defaultTitle = `新对话 - ${new Date().toLocaleTimeString()}`;
      const sessionResponse = await chatTaskApi.createChatTask(workspace.id, {
        workspaceId: workspace.id,
        title: defaultTitle,
        description: `会话开始于: ${firstQuestion.substring(0, 50)}...`
      });
      
      const newSession = sessionResponse.data.data;
      setSessions(prev => [newSession, ...prev]);
      
      const chatSession: ChatSession = {
        id: newSession.id,
        title: newSession.title,
        messages: [],
        workspaceId: newSession.workspaceId,
        createdAt: newSession.createdAt,
        updatedAt: newSession.updatedAt,
        contextId: newSession.contextId
      };
      
      setCurrentSession(chatSession);
      
      // 异步生成标题
      generateAndUpdateTitle(workspace.id, newSession.id, firstQuestion);
      
      return chatSession;
    } catch (err) {
      console.error('创建会话失败:', err);
      message.error('创建会话失败');
      throw err;
    }
  };

  // 生成并更新标题
  const generateAndUpdateTitle = async (workspaceId: string, sessionId: string, question: string) => {
    try {
      const titleResponse = await chatTaskApi.generateChatTitle(workspaceId, {
        question: question,
        async: false
      });
      
      const generatedTitle = titleResponse.data.data.title;
      
      await chatTaskApi.updateChatTask(workspaceId, sessionId, {
        title: generatedTitle
      });
      
      setSessions(prev => prev.map(s => 
        s.id === sessionId ? { ...s, title: generatedTitle } : s
      ));
      
      setCurrentSession(prev => 
        prev && prev.id === sessionId 
          ? { ...prev, title: generatedTitle }
          : prev
      );
      
    } catch (err) {
      console.error('生成标题失败:', err);
    }
  };

  // 流式聊天核心逻辑
  const startStreamChat = async (chatMessages: ChatMessage[]) => {
    setLoading(true);
    setIsStreaming(true);
    clearTypewriterState();

    try {
      const chatHistory = chatMessages.map(msg => ({
        role: msg.role,
        content: msg.content
      }));

      console.log('🚀 开始流式聊天，消息历史:', chatHistory);

      const requestUrl = buildApiUrl(API_ENDPOINTS.CHAT.STREAM);
      console.log('🌐 请求URL:', requestUrl);

      const response = await fetch(requestUrl, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'text/plain',
          'Cache-Control': 'no-cache',
        },
        body: JSON.stringify({
          messages: chatHistory,
          workspaceId: currentWorkspace?.id || 'ai-chat'
        }),
        // 重要：确保支持流式响应
        mode: 'cors',
        credentials: 'same-origin',
      });

      console.log('📡 响应状态:', response.status, response.statusText);
      console.log('📋 响应头:', Object.fromEntries(response.headers.entries()));

      if (!response.ok) {
        const errorText = await response.text();
        console.error('❌ HTTP错误响应:', errorText);
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      console.log('🔄 开始处理流式响应...');
      await handleStreamResponse(response);

    } catch (error) {
      console.error('流式聊天失败:', error);
      setLoading(false);
      setIsStreaming(false);
      clearTypewriterState();
      throw error;
    }
  };

  // 处理HTTP流式响应（更简单高效）
  const handleStreamResponse = async (response: Response) => {
    console.log('🔍 检查响应体:', response.body ? '存在' : '不存在');
    
    const reader = response.body?.getReader();
    const decoder = new TextDecoder();
    
    if (!reader) {
      console.error('❌ 无法获取响应流读取器');
      throw new Error('无法获取响应流');
    }
    
    console.log('✅ 成功获取流读取器，开始读取数据...');

    let buffer = '';
    let hasStarted = false;
    let chunkCount = 0;

    try {
      while (true) {
        console.log(`🔄 正在读取第${chunkCount + 1}个数据块...`);
        const { done, value } = await reader.read();
        
        if (done) {
          console.log('🏁 HTTP流式响应完成，总共接收', chunkCount, '个数据块');
          finishStreaming();
          break;
        }
        
        chunkCount++;
        console.log(`📦 接收到第${chunkCount}个数据块，大小:`, value?.length || 0, '字节');
        
        // 🚀 直接处理文本流，无需复杂的SSE解析
        const chunk = decoder.decode(value, { stream: true });
        console.log('🔤 解码后的文本块:', chunk.length > 100 ? chunk.substring(0, 100) + '...' : chunk);
        buffer += chunk;
        
        // 按行分割处理
        const lines = buffer.split('\n');
        buffer = lines.pop() || ''; // 保留最后可能不完整的行
        
        console.log('📄 分割出', lines.length, '行数据，剩余缓冲:', buffer.length, '字符');
        
        for (const line of lines) {
          const trimmedLine = line.trim();
          if (!trimmedLine) continue;
          
          console.log('📝 处理行数据:', trimmedLine);
          
          // 检查结束标记
          if (trimmedLine === '[DONE]') {
            console.log('🎯 收到完成信号');
            finishStreaming();
            return;
          }
          
          // 检查错误消息
          if (trimmedLine.startsWith('ERROR:')) {
            console.error('后端返回错误:', trimmedLine);
            // 可以选择显示错误信息或者停止流式处理
            continue;
          }
          
          // 🔥 直接处理文本内容，无JSON解析开销
          if (!hasStarted) {
            hasStarted = true;
            setLoading(false); // 第一个数据到达时停止loading
            console.log('🚀 开始接收HTTP流式数据');
          }
          
          console.log('📝 实时添加内容:', trimmedLine.length > 50 ? trimmedLine.substring(0, 50) + '...' : trimmedLine);
          addStreamContent(trimmedLine);
        }
      }
    } catch (streamError) {
      console.error('❌ 流处理出错:', streamError);
      throw streamError;
    } finally {
      console.log('🔚 释放流读取器');
      reader.releaseLock();
    }
  };

  // 发送消息
  const sendMessage = async () => {
    if (!inputValue.trim() || loading || isStreaming) {
      return;
    }

    try {
      const userMessage: ChatMessage = {
        id: Date.now().toString(),
        role: 'user',
        content: inputValue.trim(),
        timestamp: new Date().toISOString()
      };

      setMessages(prev => [...prev, userMessage]);
      setInputValue('');
      
      if (!currentSession) {
        const workspace = await ensureWorkspace();
        await createNewSession(userMessage.content, workspace);
      }
      
      await startStreamChat([...messages, userMessage]);

    } catch (error) {
      console.error('发送消息失败:', error);
      message.error('发送消息失败');
      
      const errorMessage: ChatMessage = {
        id: Date.now().toString(),
        role: 'assistant',
        content: '抱歉，发送消息失败，请重试。',
        timestamp: new Date().toISOString()
      };
      setMessages(prev => [...prev, errorMessage]);
      
      setLoading(false);
      setIsStreaming(false);
      clearTypewriterState();
    }
  };

  // 处理回车发送
  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  // 获取语音按钮图标
  const getVoiceIcon = () => {
    if (isRecording) {
      return <AudioMutedOutlined />;
    }
    return <AudioOutlined />;
  };

  // 获取语音按钮提示文字
  const getVoiceTooltip = () => {
    switch (voiceStatus) {
      case 'recording':
        return '点击停止录音';
      case 'processing':
        return '正在识别中...';
      case 'error':
        return voiceMessage || '语音功能不可用';
      default:
        return '点击开始语音输入';
    }
  };

  // 获取语音按钮文字
  const getVoiceButtonText = () => {
    switch (voiceStatus) {
      case 'recording':
        return '停止录音';
      case 'processing':
        return '识别中...';
      case 'error':
        return '不可用';
      default:
        return '语音输入';
    }
  };

  // 切换语音录音状态
  const toggleVoiceRecording = async () => {
    if (isRecording) {
      await stopVoiceRecording();
    } else {
      await startVoiceRecording();
    }
  };

  // 开始语音录音
  const startVoiceRecording = async () => {
    if (!speechServiceRef.current) {
      message.error('语音服务未初始化');
      return;
    }

    try {
      setVoiceStatus('recording');
      setVoiceMessage('正在录音中...');
      
      const success = await speechServiceRef.current.startRecording();
      if (success) {
        setIsRecording(true);
        message.success('开始语音输入，再次点击结束录音');
      } else {
        setVoiceStatus('error');
        setVoiceMessage('无法启动录音');
        message.error('无法启动语音输入，请检查麦克风权限');
      }
    } catch (error: any) {
      console.error('启动语音录音失败:', error);
      setVoiceStatus('error');
      setVoiceMessage('启动录音失败');
      message.error(`无法启动语音输入: ${error.message}`);
    }
  };

  // 停止语音录音并识别
  const stopVoiceRecording = async () => {
    if (!speechServiceRef.current || !isRecording) {
      return;
    }

    try {
      setVoiceStatus('processing');
      setVoiceMessage('录音完成，正在识别...');
      setIsRecording(false);
      
      // 获取录音文件
      const audioBlob = await speechServiceRef.current.stopRecording();
      if (!audioBlob) {
        throw new Error('未获取到录音文件');
      }

      // 上传并识别
      const result = await speechServiceRef.current.recognizeAudio(audioBlob, 'zh');
      
      if (result.success && result.text) {
        // 将识别结果添加到输入框
        setInputValue(prev => prev + result.text + ' ');
        setVoiceStatus('idle');
        setVoiceMessage('');
        message.success(`语音识别成功：${result.text}`);
        
        // 自动聚焦到输入框
        setTimeout(() => {
          inputRef.current?.focus();
        }, 100);
      } else {
        setVoiceStatus('error');
        setVoiceMessage(result.error || '识别失败');
        message.error(`语音识别失败: ${result.error || '未知错误'}`);
      }
      
    } catch (error: any) {
      console.error('语音识别失败:', error);
      setVoiceStatus('error');
      setVoiceMessage('识别失败');
      message.error(`语音识别失败: ${error.message}`);
    }
  };

  // 选择会话
  const selectSession = async (session: ChatTask) => {
    const chatSession: ChatSession = {
      id: session.id,
      title: session.title,
      messages: [],
      workspaceId: session.workspaceId || currentWorkspace?.id || '',
      createdAt: session.createdAt,
      updatedAt: session.updatedAt,
      contextId: session.contextId
    };
    
    setCurrentSession(chatSession);
    setMessages([]);
    clearTypewriterState();
  };

  // 初始化
  useEffect(() => {
    loadExistingWorkspace();
  }, []);

  useEffect(() => {
    if (currentWorkspace?.id) {
      loadSessions();
    }
  }, [currentWorkspace, loadSessions]);

  useEffect(() => {
    scrollToBottom();
  }, [messages, streamingMessage]);

  // 初始化语音识别服务
  useEffect(() => {
    const initSpeechService = async () => {
      try {
        const { SimpleSpeechService } = await import('@/services/simpleSpeechService');
        
        // 检查浏览器支持
        if (!SimpleSpeechService.isSupported()) {
          console.warn('浏览器不支持语音录音功能');
          setVoiceStatus('error');
          setVoiceMessage('浏览器不支持语音录音');
          return;
        }

        speechServiceRef.current = new SimpleSpeechService();
        setVoiceStatus('idle');
        setVoiceMessage('');
        console.log('语音服务初始化成功');

      } catch (error) {
        console.error('语音识别服务初始化失败:', error);
        setVoiceStatus('error');
        setVoiceMessage('语音服务初始化失败');
      }
    };

    initSpeechService();

    return () => {
      // 清理资源
      if (speechServiceRef.current) {
        speechServiceRef.current.cleanup();
      }
    };
  }, []);

  useEffect(() => {
    return () => {
      clearTypewriterState();
    };
  }, []);

  return (
    <>
      <Head>
        <title>AI对话（百炼） - Super Agent</title>
        <meta name="description" content="Super Agent 百炼AI对话功能" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <link rel="icon" href="/favicon.ico" />
        <style jsx>{`
          @keyframes blink {
            0%, 50% { opacity: 1; }
            51%, 100% { opacity: 0; }
          }
          
          /* 移动端适配样式 */
          @media (max-width: 768px) {
            :global(.ai-chat-sidebar) {
              position: fixed !important;
              z-index: 1000;
              left: -300px;
              transition: left 0.3s ease;
            }
            
            :global(.ai-chat-sidebar.ant-layout-sider-collapsed) {
              left: -300px !important;
            }
            
            :global(.message-container) {
              max-width: 95% !important;
            }
            
            :global(.input-area) {
              padding: 8px 12px !important;
            }
            
            :global(.input-controls) {
              gap: 6px !important;
            }
            
            :global(.voice-btn) {
              height: 36px !important;
              width: 36px !important;
              min-width: 36px !important;
            }
            
            :global(.input-textarea) {
              font-size: 16px !important;
              padding: 8px 12px !important;
            }
            
            :global(.send-btn) {
              min-width: 50px !important;
              padding: 0 8px !important;
            }
            
            :global(.send-btn-text) {
              font-size: 14px;
            }
            
            /* 消息卡片移动端优化 */
            :global(.ant-card-body) {
              padding: 8px 12px !important;
            }
            
            /* 欢迎页面移动端优化 */
            :global(.welcome-input-area) {
              margin: 0 12px !important;
              max-width: none !important;
            }
            
            :global(.welcome-input-controls) {
              flex-direction: column !important;
              gap: 12px !important;
              align-items: stretch !important;
            }
            
            :global(.welcome-voice-btn) {
              align-self: center !important;
            }
            
            /* 隐藏侧边栏在移动端 */
            :global(.ant-layout-sider) {
              display: none !important;
            }
          }
          
          @media (max-width: 480px) {
            :global(.message-container) {
              max-width: 100% !important;
              margin: 0 !important;
            }
            
            :global(.input-area) {
              padding: 6px 8px !important;
            }
            
            :global(.voice-btn) {
              height: 32px !important;
              width: 32px !important;
              min-width: 32px !important;
            }
            
            :global(.send-btn-text) {
              display: none;
            }
            
            :global(.send-btn) {
              min-width: 40px !important;
            }
          }
        `}</style>
      </Head>
      
      <AppLayout>
        <Layout style={{ height: 'calc(100vh - 64px)', background: '#fff' }}>
          {/* 侧边栏 - 会话列表 */}
          <Sider 
            width={300} 
            collapsedWidth={0}
            breakpoint="lg"
            collapsible
            trigger={null}
            style={{ 
              background: '#f8f9fa', 
              borderRight: '1px solid #e8e8e8',
              height: '100%',
              overflow: 'auto'
            }}
            className="ai-chat-sidebar"
          >
            <div style={{ padding: '16px' }}>
              <Button 
                type="primary" 
                icon={<PlusOutlined />} 
                block 
                size="large"
                onClick={() => {
                  setCurrentSession(null);
                  setMessages([]);
                  setInputValue('');
                  clearTypewriterState();
                  message.success('已创建新对话，请在下方输入框中开始对话');
                }}
              >
                新建对话
              </Button>
            </div>
            
            <div style={{ padding: '0 16px 16px' }}>
              <Title level={5} style={{ margin: '0 0 12px 0', color: '#666' }}>
                对话历史
              </Title>
              
              <Spin spinning={sessionsLoading || workspaceLoading}>
                {workspaceLoading ? (
                  <div style={{ textAlign: 'center', padding: '40px 0' }}>
                    <Spin />
                    <div style={{ marginTop: '16px', color: '#666' }}>
                      正在初始化工作空间...
                    </div>
                  </div>
                ) : sessions.length === 0 ? (
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
                          <Text 
                            strong 
                            style={{ 
                              fontSize: '14px',
                              lineHeight: '20px',
                              color: currentSession?.id === session.id ? '#1890ff' : '#333',
                              display: 'block'
                            }}
                            ellipsis={{ tooltip: session.title }}
                          >
                            {session.title}
                          </Text>
                          
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
                  {messages.length === 0 && !isStreaming ? (
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
                          <div style={{ display: 'flex', alignItems: 'flex-start', maxWidth: '85%' }} className="message-container">
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
                      
                      {/* 流式消息显示 */}
                      {isStreaming && (
                        <div style={{ display: 'flex', justifyContent: 'flex-start' }}>
                          <div style={{ display: 'flex', alignItems: 'flex-start', maxWidth: '85%' }} className="message-container">
                            <Avatar style={{ backgroundColor: '#1890ff', marginRight: '12px', flexShrink: 0 }}>
                              AI
                            </Avatar>
                            <Card
                              size="small"
                              style={{
                                backgroundColor: '#fff',
                                borderRadius: '12px',
                                maxWidth: '100%',
                                wordBreak: 'break-word'
                              }}
                              bodyStyle={{ padding: '12px 16px' }}
                            >
                              <Paragraph 
                                style={{ 
                                  margin: 0, 
                                  color: '#333',
                                  fontSize: '14px',
                                  lineHeight: '1.6'
                                }}
                              >
                                {streamingMessage || (loading && '正在思考...')}
                                {streamingMessage && (
                                  <span style={{ 
                                    display: 'inline-block',
                                    width: '2px',
                                    height: '16px',
                                    backgroundColor: '#1890ff',
                                    marginLeft: '2px',
                                    animation: 'blink 1s infinite'
                                  }} />
                                )}
                              </Paragraph>
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
                  padding: '12px 16px', 
                  borderTop: '1px solid #e8e8e8',
                  background: '#fff'
                }} className="input-area">
                  <div style={{ 
                    marginBottom: '8px', 
                    padding: '8px 12px', 
                    background: '#f0f8ff', 
                    borderRadius: '6px',
                    fontSize: '14px'
                  }}>
                    <span>
                      <ApiOutlined /> 百炼AI对话 (已启用)
                    </span>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'flex-end', gap: '8px' }} className="input-controls">
                    {/* 语音输入按钮 */}
                    <Tooltip title={getVoiceTooltip()}>
                      <Button
                        type={isRecording ? "primary" : "default"}
                        icon={getVoiceIcon()}
                        onClick={toggleVoiceRecording}
                        disabled={loading || voiceStatus === 'processing' || voiceStatus === 'error'}
                        style={{
                          height: '40px',
                          width: '40px',
                          minWidth: '40px',
                          borderRadius: '8px',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          backgroundColor: isRecording ? '#ff4d4f' : voiceStatus === 'processing' ? '#1890ff' : undefined,
                          borderColor: isRecording ? '#ff4d4f' : voiceStatus === 'processing' ? '#1890ff' : undefined,
                          color: (isRecording || voiceStatus === 'processing') ? '#fff' : undefined
                        }}
                        loading={voiceStatus === 'processing'}
                        className="voice-btn"
                      />
                    </Tooltip>

                    {/* 输入框和发送按钮 */}
                    <Space.Compact style={{ flex: 1 }} className="input-compact">
                      <TextArea
                        ref={inputRef}
                        value={inputValue}
                        onChange={(e) => setInputValue(e.target.value)}
                        onKeyPress={handleKeyPress}
                        placeholder="输入您的问题..."
                        autoSize={{ minRows: 1, maxRows: 4 }}
                        style={{ resize: 'none', fontSize: '16px' }}
                        disabled={loading || isRecording || isStreaming}
                        className="input-textarea"
                      />
                      <Button
                        type="primary"
                        icon={<SendOutlined />}
                        onClick={sendMessage}
                        loading={loading}
                        disabled={!inputValue.trim() || isRecording || isStreaming}
                        style={{ height: 'auto', minWidth: '60px' }}
                        className="send-btn"
                      >
                        <span className="send-btn-text">
                          {isStreaming ? '回答中...' : '发送'}
                        </span>
                      </Button>
                    </Space.Compact>
                  </div>
                  
                  {/* 语音状态提示 */}
                  {(voiceStatus === 'recording' || voiceStatus === 'processing' || (voiceStatus === 'error' && voiceMessage)) && (
                    <div style={{
                      marginTop: '8px',
                      padding: '8px 12px',
                      background: voiceStatus === 'error' ? '#fff1f0' : voiceStatus === 'processing' ? '#e6f7ff' : '#fff7e6',
                      border: `1px solid ${voiceStatus === 'error' ? '#ffccc7' : voiceStatus === 'processing' ? '#91d5ff' : '#ffd666'}`,
                      borderRadius: '6px',
                      fontSize: '14px',
                      color: voiceStatus === 'error' ? '#cf1322' : voiceStatus === 'processing' ? '#1890ff' : '#d48806',
                      textAlign: 'center'
                    }}>
                      {voiceStatus === 'recording' && '🎤 正在录音中，再次点击结束录音...'}
                      {voiceStatus === 'processing' && '🔄 录音完成，正在识别中...'}
                      {voiceStatus === 'error' && voiceMessage && `❌ ${voiceMessage}`}
                    </div>
                  )}
                </div>
              </>
            ) : (
              /* 欢迎页面 */
              <div style={{ 
                flex: 1,
                display: 'flex', 
                flexDirection: 'column',
                justifyContent: 'center',
                alignItems: 'center',
                background: '#fafafa',
                padding: '40px'
              }}>
                <MessageOutlined style={{ fontSize: '72px', color: '#bfbfbf', marginBottom: '24px' }} />
                <Title level={2} style={{ color: '#666', marginBottom: '16px' }}>
                  Super Agent
                </Title>
                <Paragraph style={{ fontSize: '16px', color: '#999', textAlign: 'center', marginBottom: '40px' }}>
                  阿里云百炼AI助手，智能对话，实时回复<br />
                  开始新对话，体验百炼AI的强大能力
                </Paragraph>
                
                <div style={{ 
                  width: '100%', 
                  maxWidth: '600px',
                  marginBottom: '40px'
                }} className="welcome-input-area">
                  <div style={{
                    position: 'relative',
                    border: '2px solid #d9d9d9',
                    borderRadius: '12px',
                    padding: '16px',
                    background: '#fff',
                    boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
                  }}>
                    <TextArea
                      value={inputValue}
                      onChange={(e) => setInputValue(e.target.value)}
                      onKeyPress={handleKeyPress}
                      placeholder="输入您的问题，开始与AI助手对话..."
                      autoSize={{ minRows: 3, maxRows: 8 }}
                      style={{ 
                        border: 'none',
                        resize: 'none',
                        fontSize: '16px',
                        lineHeight: '1.6'
                      }}
                      disabled={loading || isStreaming}
                    />
                    <div style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      marginTop: '12px'
                    }} className="welcome-input-controls">
                      {/* 语音输入按钮 */}
                      <Tooltip title={getVoiceTooltip()}>
                        <Button
                          type={isRecording ? "primary" : "default"}
                          size="large"
                          icon={getVoiceIcon()}
                          onClick={toggleVoiceRecording}
                          disabled={loading || voiceStatus === 'processing' || voiceStatus === 'error'}
                          style={{
                            borderRadius: '8px',
                            backgroundColor: isRecording ? '#ff4d4f' : voiceStatus === 'processing' ? '#1890ff' : undefined,
                            borderColor: isRecording ? '#ff4d4f' : voiceStatus === 'processing' ? '#1890ff' : undefined,
                            color: (isRecording || voiceStatus === 'processing') ? '#fff' : undefined
                          }}
                          loading={voiceStatus === 'processing'}
                          className="welcome-voice-btn"
                        >
                          {getVoiceButtonText()}
                        </Button>
                      </Tooltip>

                      {/* 发送按钮 */}
                      <Button
                        type="primary"
                        size="large"
                        icon={<SendOutlined />}
                        onClick={sendMessage}
                        loading={loading}
                        disabled={!inputValue.trim() || isRecording || isStreaming}
                        style={{ 
                          borderRadius: '8px',
                          fontWeight: '500'
                        }}
                      >
                        {isStreaming ? '回答中...' : '发送'}
                      </Button>
                    </div>
                    
                    {/* 语音状态提示 */}
                    {(voiceStatus === 'recording' || voiceStatus === 'processing' || (voiceStatus === 'error' && voiceMessage)) && (
                      <div style={{
                        marginTop: '12px',
                        padding: '12px',
                        background: voiceStatus === 'error' ? '#fff1f0' : voiceStatus === 'processing' ? '#e6f7ff' : '#fff7e6',
                        border: `1px solid ${voiceStatus === 'error' ? '#ffccc7' : voiceStatus === 'processing' ? '#91d5ff' : '#ffd666'}`,
                        borderRadius: '8px',
                        fontSize: '15px',
                        color: voiceStatus === 'error' ? '#cf1322' : voiceStatus === 'processing' ? '#1890ff' : '#d48806',
                        textAlign: 'center',
                        fontWeight: 'bold'
                      }}>
                        {voiceStatus === 'recording' && '🎤 正在录音中，再次点击结束录音...'}
                        {voiceStatus === 'processing' && '🔄 录音完成，正在识别中...'}
                        {voiceStatus === 'error' && voiceMessage && `❌ ${voiceMessage}`}
                      </div>
                    )}
                  </div>
                </div>
              </div>
            )}
          </Content>
        </Layout>
      </AppLayout>
    </>
  );
};

export default AiChatPage;
