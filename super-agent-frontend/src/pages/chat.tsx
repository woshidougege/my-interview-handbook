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
  Modal,
  Tooltip,
  Empty,
  Switch
} from 'antd';
import { 
  SendOutlined, 
  PlusOutlined, 
  StarOutlined, 
  StarFilled,
  EditOutlined,
  DeleteOutlined,
  MessageOutlined,
  AudioOutlined,
  AudioMutedOutlined,
  ApiOutlined
} from '@ant-design/icons';
import AppLayout from '@/components/Layout/AppLayout';
import { chatTaskApi, workspaceApi, userApi } from '@/services/api';
import { ChatMessage, ChatTask, ChatSession } from '@/types/chat';
import * as a2aService from '@/services/a2aService';

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
  const [editingSession, setEditingSession] = useState<ChatTask | null>(null);
  const [currentWorkspace, setCurrentWorkspace] = useState<{ id: string; name: string; description?: string } | null>(null);
  const [workspaceLoading, setWorkspaceLoading] = useState(true);
  const [generatingTitleForSession, setGeneratingTitleForSession] = useState<string | null>(null);
  const [isStreaming, setIsStreaming] = useState(false);
  const [currentStreamMessage, setCurrentStreamMessage] = useState<string>('');
  const [supplementInfo, setSupplementInfo] = useState<{
    id: string;
    text: string;
    agentEntityCode: string;
    sessionId: string;
    taskId?: string;
  } | null>(null);
  
  // 添加用于取消请求的引用
  const cancelRequestRef = useRef<(() => void) | null>(null);
  
  // 语音输入相关状态
  const [isRecording, setIsRecording] = useState(false);
  const [voiceConnectionStatus, setVoiceConnectionStatus] = useState<'disconnected' | 'connecting' | 'connected' | 'error'>('disconnected');
  
  const inputRef = useRef<HTMLTextAreaElement>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const speechServiceRef = useRef<any>(null); // TODO: 添加正确的类型定义

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
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


  // 页面加载时尝试获取现有工作空间和会话列表
  useEffect(() => {
    loadExistingWorkspace();
    
    // 组件卸载时的清理函数
    return () => {
      // 取消正在进行的请求
      if (cancelRequestRef.current) {
        cancelRequestRef.current();
      }
    };
  }, []);

  // 当工作空间准备好后加载会话列表
  useEffect(() => {
    if (currentWorkspace?.id) {
      loadSessions();
    }
  }, [currentWorkspace, loadSessions]);

  // 滚动到消息底部
  useEffect(() => {
    scrollToBottom();
  }, [messages, currentStreamMessage]);

  // 初始化简单语音识别服务
  useEffect(() => {
    const initSpeechService = async () => {
      try {
        const { SimpleSpeechService } = await import('@/services/simpleSpeechService');
        speechServiceRef.current = new SimpleSpeechService();
        setVoiceConnectionStatus('connected');
      } catch (error) {
        console.error('语音识别服务初始化失败:', error);
        setVoiceConnectionStatus('error');
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

  // 加载现有工作空间（如果有的话）
  const loadExistingWorkspace = async () => {
    setWorkspaceLoading(true);
    try {
      // 尝试获取现有工作空间
      const response = await workspaceApi.getWorkspaces();
      
      if (response.data.data && response.data.data.length > 0) {
        // 使用第一个工作空间
        setCurrentWorkspace(response.data.data[0]);
      } else {
        // 没有工作空间，但不在这里创建，等用户提问时再创建
        setCurrentWorkspace(null);
      }
    } catch (error) {
      console.error('获取工作空间失败:', error);
      // 不显示错误消息，因为可能是第一次使用
      setCurrentWorkspace(null);
    } finally {
      setWorkspaceLoading(false);
    }
  };

  // 确保工作空间存在（在用户提问时调用）
  const ensureWorkspace = async () => {
    if (currentWorkspace?.id) {
      return currentWorkspace;
    }

    try {
      // 先尝试获取现有工作空间
      const response = await workspaceApi.getWorkspaces();
      
      if (response.data.data && response.data.data.length > 0) {
        // 使用第一个工作空间
        const workspace = response.data.data[0];
        setCurrentWorkspace(workspace);
        return workspace;
      } else {
        // 获取当前用户信息
        const userResponse = await userApi.getCurrentUser();
        const currentUser = userResponse.data.data;
        
        // 创建新的工作空间
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
      // 先用默认标题创建会话任务，立即显示
      const defaultTitle = `新对话 - ${new Date().toLocaleTimeString()}`;
      const sessionResponse = await chatTaskApi.createChatTask(workspace.id, {
        workspaceId: workspace.id,
        title: defaultTitle,
        description: `会话开始于: ${firstQuestion.substring(0, 50)}...`
      });
      
      const newSession = sessionResponse.data.data;
      
      // 立即更新UI显示新会话
      setSessions(prev => [newSession, ...prev]);
      
      // 设置当前会话
      const chatSession: ChatSession = {
        id: newSession.id,
        title: newSession.title,
        messages: [],
        workspaceId: newSession.workspaceId,
        createdAt: newSession.createdAt,
        updatedAt: newSession.updatedAt,
        contextId: newSession.contextId // 添加contextId字段
      };
      
      setCurrentSession(chatSession);
      setMessages([]);
      
      // 异步生成真实标题并更新
      generateAndUpdateTitle(workspace.id, newSession.id, firstQuestion);
      
      return chatSession;
    } catch (err) {
      console.error('创建会话失败:', err);
      message.error('创建会话失败');
      throw err;
    }
  };

  // 异步生成并更新标题
  const generateAndUpdateTitle = async (workspaceId: string, sessionId: string, question: string) => {
    try {
      setTitleGenerating(true);
      setGeneratingTitleForSession(sessionId);
      
      // 生成标题
      const titleResponse = await chatTaskApi.generateChatTitle(workspaceId, {
        question: question,
        async: false
      });
      
      const generatedTitle = titleResponse.data.data.title;
      
      // 更新会话标题
      await chatTaskApi.updateChatTask(workspaceId, sessionId, {
        title: generatedTitle
      });
      
      // 更新UI中的标题
      setSessions(prev => prev.map(s => 
        s.id === sessionId ? { ...s, title: generatedTitle } : s
      ));
      
      // 如果是当前会话，也更新当前会话的标题
      setCurrentSession(prev => 
        prev && prev.id === sessionId 
          ? { ...prev, title: generatedTitle }
          : prev
      );
      
    } catch (err) {
      console.error('生成标题失败:', err);
      // 标题生成失败不影响正常对话，只是用默认标题
    } finally {
      setTitleGenerating(false);
      setGeneratingTitleForSession(null);
    }
  };

  // 发送消息
  const sendMessage = async () => {
    if (!inputValue.trim() || loading) return;
    
    // 如果有待处理的补充信息，优先处理
    if (supplementInfo) {
      await handleSupplementInfo();
      return;
    }

    try {
      // 创建用户消息
      const userMessage: ChatMessage = {
        id: Date.now().toString(),
        role: 'user',
        content: inputValue.trim(),
        timestamp: new Date().toISOString()
      };

      // 更新消息列表
      const newMessages = [...messages, userMessage];
      setMessages(newMessages);
      setInputValue('');
      
      // 滚动到底部
      setTimeout(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
      }, 100);

      // 使用A2A协议发送消息
      setLoading(true);
      
      // 获取当前用户信息
      let currentUser = null;
      try {
        const userResponse = await userApi.getCurrentUser();
        currentUser = userResponse.data.data;
      } catch (error) {
        console.error('获取用户信息失败:', error);
        message.error('获取用户信息失败');
        setLoading(false);
        setIsStreaming(false);
        return;
      }
      
      // 正确处理上下文ID：
      // 如果当前有会话，则使用会话的contextId作为上下文ID（历史对话）
      // 如果没有会话，则创建新会话并使用新会话的contextId作为上下文ID（新对话）
      let contextId = '';
      if (!currentSession) {
        // 创建新会话
        const workspace = await ensureWorkspace();
        const newSession = await createNewSession(inputValue.trim(), workspace);
        contextId = newSession.contextId ?? ''; // 修复：使用空值合并操作符处理可能的undefined
      } else {
        // 使用现有会话的contextId作为上下文ID
        contextId = currentSession.contextId ?? ''; // 修复：使用空值合并操作符处理可能的undefined
      }
      
      // 生成任务ID
      const taskId = `task_${Date.now()}`;
      
      // 使用SSE格式发送JSON-RPC请求
      setLoading(true);
      setIsStreaming(true);
      
      console.log('准备发送A2A请求，上下文ID:', contextId);
      
      // 准备A2A请求参数
      const a2aRequest = {
        userId: currentUser.id,
        message: inputValue.trim(),
        sessionId: contextId,   // sessionId保持与contextId一致
        contextId: contextId    // 正确传递contextId参数
      };

      console.log('准备发送A2A请求，参数:', a2aRequest);

      // 使用流式方式发送请求并处理响应
      // 用户消息已经在上面的代码块中创建并更新了消息列表

      // 在闭包中维护已处理的消息ID集合
      const processedMessageIds = new Set<string>();
      
      // 存储取消函数的引用
      cancelRequestRef.current = a2aService.streamMessage(
        a2aRequest,
        (chunk: string) => {
          // 检查是否是需要补充信息的情况
          if (chunk.startsWith('【需要补充信息】')) {
            try {
              const supplementData = JSON.parse(chunk.substring(9)); // 去掉"【需要补充信息】"前缀
              setSupplementInfo({
                ...supplementData,
                sessionId: currentSession?.id || ''
              });
              message.info('需要补充信息: ' + supplementData.text);
              setCurrentStreamMessage(prev => prev + `\n需要补充信息: ${supplementData.text}`);
            } catch (e) {
              console.error('解析补充信息失败:', e);
            }
          } else {
            // 检查消息ID是否已处理（去重）
            try {
              const jsonData = JSON.parse(chunk);
              if (jsonData.id && processedMessageIds.has(jsonData.id)) {
                console.log('跳过重复消息:', jsonData.id);
                return;
              }
              
              // 添加消息ID到已处理集合
              if (jsonData.id) {
                processedMessageIds.add(jsonData.id);
              }
              
              // 处理普通消息
              if (jsonData.data && jsonData.data.parts && jsonData.data.parts[0]) {
                const content = jsonData.data.parts[0].text || '';
                if (content) {
                  setCurrentStreamMessage(prev => prev + content);
                }
              }
            } catch (e) {
              // 如果不是JSON格式，直接追加到当前流消息中
              setCurrentStreamMessage(prev => prev + chunk);
            }
          }
        },
        (error: any) => {
          console.error('A2A流式传输错误:', error);
          message.error('消息发送失败: ' + (error.message || '未知错误'));
          setIsStreaming(false);
          setLoading(false);
          // 重置状态
          if (cancelRequestRef.current) {
            cancelRequestRef.current();
            cancelRequestRef.current = null;
          }
          setCurrentStreamMessage('');
        }
      );
      
      // 可以在需要时调用cancelRequest()来取消请求
    } catch (err: any) {
      console.error('发送消息失败:', err);
      message.error('发送消息失败: ' + (err.message || '未知错误'));
      
      // 显示错误信息
      const errorMessage: ChatMessage = {
        id: Date.now().toString(),
        role: 'assistant',
        content: '抱歉，处理您的请求时出现错误: ' + (err.message || '未知错误'),
        timestamp: new Date().toISOString()
      };
      setMessages(prev => [...prev, errorMessage]);
    } finally {
      if (!useA2A || !useSSE) {
        setLoading(false);
        setIsStreaming(false);
        setCurrentStreamMessage('');
      }
    }
  };

  // 处理补充信息
  const handleSupplementInfo = async () => {
    if (!supplementInfo || !inputValue.trim()) {
      message.warning('请输入补充信息');
      return;
    }

    try {
      setLoading(true);
      setIsStreaming(true);
      
      // 发送补充信息到A2A平台
      const response = await a2aService.handleSupplementInfo({
        userId: userId,
        taskId: supplementInfo.taskId || supplementInfo.id,
        userInput: inputValue.trim(),
        sessionId: supplementInfo.sessionId
      });

      if (response.data) {
        // 显示A2A平台的响应
        const aiMessage: ChatMessage = {
          id: Date.now().toString(),
          role: 'assistant',
          content: response.data,
          timestamp: new Date().toISOString()
        };
        setMessages(prev => [...prev, aiMessage]);
        
        // 清除补充信息状态
        setSupplementInfo(null);
        setInputValue('');
      }
    } catch (err: any) {
      console.error('处理补充信息失败:', err);
      message.error('处理补充信息失败: ' + (err.message || '未知错误'));
    } finally {
      setLoading(false);
      setIsStreaming(false);
    }
  };

  // 处理回车发送
  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      if (supplementInfo) {
        handleSupplementInfo();
      } else {
        sendMessage();
      }
    }
  };

  // 切换语音录音状态
  const toggleVoiceRecording = async () => {
    if (isRecording) {
      stopVoiceRecording();
    } else {
      await startVoiceRecording();
    }
  };

  // 开始语音录音
  const startVoiceRecording = async () => {
    try {
      if (!speechServiceRef.current) {
        message.error('语音识别服务未初始化');
        return;
      }

      const success = await speechServiceRef.current.startRecording();
      if (success) {
        setIsRecording(true);
        setVoiceConnectionStatus('connected');
        message.success('开始录音，说完后点击停止进行识别');
      } else {
        message.error('无法开始录音，请检查麦克风权限');
        setVoiceConnectionStatus('error');
      }
    } catch (error: any) {
      console.error('启动语音录音失败:', error);
      message.error(`无法启动语音输入: ${error.message}`);
      setVoiceConnectionStatus('error');
    }
  };

  // 停止语音录音并识别
  const stopVoiceRecording = async () => {
    try {
      if (!speechServiceRef.current || !isRecording) {
        return;
      }

      setVoiceConnectionStatus('connecting'); // 显示处理中状态
      message.loading('正在识别语音...');

      // 停止录音并获取音频文件
      const audioBlob = await speechServiceRef.current.stopRecording();
      if (audioBlob) {
        // 识别音频
        const result = await speechServiceRef.current.recognizeAudio(audioBlob);
        if (result.success && result.text) {
          // 将识别结果添加到输入框
          setInputValue(prev => prev + result.text + ' ');
          message.success('语音识别成功');
        } else {
          message.error(result.error || '语音识别失败');
        }
      } else {
        message.error('录音失败，请重试');
      }

      setIsRecording(false);
      setVoiceConnectionStatus('connected');
    } catch (error: any) {
      console.error('停止语音录音失败:', error);
      message.error(`语音识别失败: ${error.message}`);
      setIsRecording(false);
      setVoiceConnectionStatus('error');
    }
  };

  // 选择会话
  const selectSession = async (session: ChatTask) => {
    const chatSession: ChatSession = {
      id: session.id,
      title: session.title,
      messages: [], // 实际应用中应该加载历史消息
      workspaceId: session.workspaceId || currentWorkspace?.id || '',
      createdAt: session.createdAt,
      updatedAt: session.updatedAt,
      contextId: session.contextId // 添加contextId字段
    };
    
    setCurrentSession(chatSession);
    setMessages([]); // 实际应用中应该加载历史消息
  };

  // 收藏/取消收藏会话
  const toggleFavorite = async (session: ChatTask, e: React.MouseEvent) => {
    e.stopPropagation();
    if (!currentWorkspace?.id) return;

    try {
      if (session.favorite) {
        await chatTaskApi.unfavoriteChatTask(currentWorkspace.id, session.id);
      } else {
        await chatTaskApi.favoriteChatTask(currentWorkspace.id, session.id);
      }
      
      setSessions(prev => prev.map(s => 
        s.id === session.id ? { ...s, favorite: !s.favorite } : s
      ));
      
      message.success(session.favorite ? '已取消收藏' : '已收藏');
    } catch (error) {
      console.error('操作失败:', error);
      message.error('操作失败');
    }
  };

  // 删除会话
  const deleteSession = async (session: ChatTask, e: React.MouseEvent) => {
    e.stopPropagation();
    if (!currentWorkspace?.id) return;
    
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除会话"${session.title}"吗？`,
      onOk: async () => {
        try {
          await chatTaskApi.deleteChatTask(currentWorkspace.id, session.id);
          setSessions(prev => prev.filter(s => s.id !== session.id));
          
          if (currentSession?.id === session.id) {
            setCurrentSession(null);
            setMessages([]);
          }
          
          message.success('会话已删除');
        } catch (error) {
          console.error('删除失败:', error);
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
    if (!editingSession || !newTitle.trim() || !currentWorkspace?.id) return;
    
    try {
      await chatTaskApi.updateChatTask(currentWorkspace.id, editingSession.id, {
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
      console.error('更新失败:', error);
      message.error('更新失败');
    }
  };

  // A2A协议相关状态 - 默认启用A2A和SSE
  const [userId, setUserId] = useState('');
  const useA2A = true;
  const useSSE = true;
  
  // 在组件加载时获取当前用户信息并设置默认用户ID
  useEffect(() => {
    const fetchCurrentUser = async () => {
      try {
        const response = await userApi.getCurrentUser();
        const currentUser = response.data.data;
        // 设置默认用户ID为当前登录用户ID
        setUserId(currentUser.id);
      } catch (error) {
        console.error('获取当前用户信息失败:', error);
        message.error('获取用户信息失败');
      }
    };
    
    fetchCurrentUser();
  }, []);

  return (
    <>
      <Head>
        <title>AI对话 - Super Agent</title>
        <meta name="description" content="Super Agent AI对话功能" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <link rel="icon" href="/favicon.ico" />
        <style jsx>{`
          @keyframes blink {
            0%, 50% { opacity: 1; }
            51%, 100% { opacity: 0; }
          }
        `}</style>
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
                onClick={() => {
                  // 重置会话状态，回到大输入框界面
                  setCurrentSession(null);
                  setMessages([]);
                  setInputValue('');
                  message.success('已创建新对话，请在下方输入框中开始对话');
                }}
                loading={titleGenerating}
              >
                新建对话
              </Button>
            </div>
            
            <div style={{ padding: '0 16px 16px' }}>
              <div style={{ marginBottom: '16px' }}>
                <Title level={5} style={{ margin: '0 0 8px 0', color: '#333' }}>
                  工作空间
                </Title>
                <div style={{ display: 'flex', gap: '8px', marginBottom: '16px' }}>
                  <Button size="small" type="primary">全部</Button>
                  <Button size="small">收藏</Button>
                </div>
              </div>
              
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
                              <div style={{ display: 'flex', alignItems: 'center', width: '100%' }}>
                                <Text 
                                  strong 
                                  style={{ 
                                    fontSize: '14px',
                                    lineHeight: '20px',
                                    color: currentSession?.id === session.id ? '#1890ff' : '#333',
                                    opacity: generatingTitleForSession === session.id ? 0.6 : 1,
                                    flex: 1
                                  }}
                                  ellipsis={{ tooltip: session.title }}
                                >
                                  {session.title}
                                </Text>
                                {generatingTitleForSession === session.id && (
                                  <Spin 
                                    size="small" 
                                    style={{ 
                                      marginLeft: '8px',
                                      fontSize: '12px'
                                    }}
                                  />
                                )}
                              </div>
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
                  background: '#fff',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center'
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
                              styles={{ body: { padding: '12px 16px' } }}
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
                      {isStreaming && currentStreamMessage && (
                        <div style={{ display: 'flex', justifyContent: 'flex-start' }}>
                          <div style={{ display: 'flex', alignItems: 'flex-start', maxWidth: '70%' }}>
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
                                {currentStreamMessage}
                                <span style={{ 
                                  display: 'inline-block',
                                  width: '2px',
                                  height: '16px',
                                  backgroundColor: '#1890ff',
                                  marginLeft: '2px',
                                  animation: 'blink 1s infinite'
                                }} />
                              </Paragraph>
                            </Card>
                          </div>
                        </div>
                      )}
                      
                      {/* 加载状态 */}
                      {loading && !isStreaming && (
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
                                正在连接AI服务...
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
                  {/* A2A协议设置 */}
                  <div style={{ 
                    marginBottom: '12px', 
                    padding: '12px', 
                    background: '#f5f5f5', 
                    borderRadius: '6px' 
                  }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                      <span>
                        <ApiOutlined /> A2A协议 (默认启用)
                      </span>
                    </div>
                    <div style={{ marginTop: '8px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
                        <span>SSE格式 (默认启用)</span>
                      </div>
                      <Text strong>用户ID:</Text>
                      <Input 
                        size="small"
                        value={userId}
                        onChange={(e) => setUserId(e.target.value)}
                        style={{ marginTop: '4px' }}
                      />
                    </div>
                  </div>

                  {/* 语音输入和聊天输入区域 */}
                  <div style={{ display: 'flex', alignItems: 'flex-end', gap: '8px' }}>
                    {/* 语音输入按钮 */}
                    <Tooltip title={isRecording ? '点击停止语音输入' : voiceConnectionStatus === 'connected' ? '点击开始语音输入' : '语音服务连接中...'}>
                      <Button
                        type={isRecording ? "primary" : "default"}
                        icon={isRecording ? <AudioMutedOutlined /> : <AudioOutlined />}
                        onClick={toggleVoiceRecording}
                        disabled={loading || voiceConnectionStatus === 'connecting'}
                        style={{
                          height: '32px',
                          width: '32px',
                          minWidth: '32px',
                          borderRadius: '6px',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          backgroundColor: isRecording ? '#ff4d4f' : undefined,
                          borderColor: isRecording ? '#ff4d4f' : undefined,
                          color: isRecording ? '#fff' : undefined
                        }}
                        loading={voiceConnectionStatus === 'connecting'}
                      />
                    </Tooltip>

                    {/* 输入框和发送按钮 */}
                    <Space.Compact style={{ flex: 1 }}>
                      <TextArea
                        ref={inputRef}
                        value={inputValue}
                        onChange={(e) => setInputValue(e.target.value)}
                        onKeyPress={handleKeyPress}
                        placeholder={supplementInfo ? `请输入补充信息: ${supplementInfo?.text}` : "输入您的问题..."}
                        autoSize={{ minRows: 1, maxRows: 4 }}
                        style={{ resize: 'none' }}
                        disabled={loading || isRecording}
                      />
                      <Button
                        type="primary"
                        icon={<SendOutlined />}
                        onClick={supplementInfo ? handleSupplementInfo : sendMessage}
                        loading={loading}
                        disabled={!inputValue.trim() || isRecording}
                        style={{ height: 'auto' }}
                      >
                        {supplementInfo ? '提交' : '发送'}
                      </Button>
                    </Space.Compact>
                  </div>
                </div>
              </>
            ) : (
              <>
                {/* 欢迎页面 - 显示输入框 */}
                <div style={{ 
                  flex: 1,
                  display: 'flex', 
                  flexDirection: 'column',
                  background: '#fafafa'
                }}>
                  {/* 欢迎内容区域 */}
                  <div style={{
                    flex: 1,
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'center',
                    alignItems: 'center',
                    padding: '40px'
                  }}>
                    <MessageOutlined style={{ fontSize: '72px', color: '#bfbfbf', marginBottom: '24px' }} />
                    <Title level={2} style={{ color: '#666', marginBottom: '16px' }}>
                      Super Agent
                    </Title>
                    <Paragraph style={{ fontSize: '16px', color: '#999', textAlign: 'center', marginBottom: '40px' }}>
                      您的数字分身，按照您的目标自主规划、执行并交付<br />
                      开始新对话，体验AI助手的强能力
                    </Paragraph>
                    
                    {/* 大输入框 */}
                    <div style={{ 
                      width: '100%', 
                      maxWidth: '600px',
                      marginBottom: '40px'
                    }}>
                      <div style={{
                        position: 'relative',
                        border: '2px solid #d9d9d9',
                        borderRadius: '12px',
                        padding: '16px',
                        background: '#fff',
                        boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
                        transition: 'all 0.3s ease'
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
                          disabled={loading || isRecording}
                        />
                        <div style={{
                          display: 'flex',
                          justifyContent: 'space-between',
                          alignItems: 'center',
                          marginTop: '12px'
                        }}>
                          {/* 语音输入按钮 */}
                          <Tooltip title={isRecording ? '点击停止语音输入' : voiceConnectionStatus === 'connected' ? '点击开始语音输入' : '语音服务连接中...'}>
                            <Button
                              type={isRecording ? "primary" : "default"}
                              size="large"
                              icon={isRecording ? <AudioMutedOutlined /> : <AudioOutlined />}
                              onClick={toggleVoiceRecording}
                              disabled={loading || voiceConnectionStatus === 'connecting'}
                              style={{
                                borderRadius: '8px',
                                backgroundColor: isRecording ? '#ff4d4f' : undefined,
                                borderColor: isRecording ? '#ff4d4f' : undefined,
                                color: isRecording ? '#fff' : undefined
                              }}
                              loading={voiceConnectionStatus === 'connecting'}
                            >
                              {isRecording ? '停止录音' : '语音输入'}
                            </Button>
                          </Tooltip>

                          {/* 发送按钮 */}
                          <Button
                            type="primary"
                            size="large"
                            icon={<SendOutlined />}
                            onClick={sendMessage}
                            loading={loading}
                            disabled={!inputValue.trim() || isRecording}
                            style={{ 
                              borderRadius: '8px',
                              fontWeight: '500'
                            }}
                          >
                            发送
                          </Button>
                        </div>
                        
                        {/* 录音状态提示 */}
                        {isRecording && (
                          <div style={{
                            marginTop: '8px',
                            textAlign: 'center',
                            color: '#ff4d4f',
                            fontSize: '14px',
                            fontWeight: 'bold'
                          }}>
                            🎤 正在录音中，再次点击&quot;停止录音&quot;结束语音输入
                          </div>
                        )}
                        
                      </div>
                    </div>
                    
                    {/* 功能提示 */}
                    <div style={{ textAlign: 'center', marginBottom: '24px' }}>
                      <Text style={{ fontSize: '16px', color: '#333' }}>
                        需要Super Agent帮您做哪些事？
                      </Text>
                    </div>
                    
                    {/* 功能标签 */}
                    <div style={{ 
                      display: 'flex', 
                      gap: '16px', 
                      flexWrap: 'wrap',
                      justifyContent: 'center'
                    }}>
                      <div style={{ textAlign: 'center', minWidth: '80px' }}>
                        <div style={{ fontSize: '24px', marginBottom: '4px' }}>🔧</div>
                        <Text style={{ fontSize: '12px', color: '#666' }}>软件操作</Text>
                      </div>
                      <div style={{ textAlign: 'center', minWidth: '80px' }}>
                        <div style={{ fontSize: '24px', marginBottom: '4px' }}>📊</div>
                        <Text style={{ fontSize: '12px', color: '#666' }}>深度推理</Text>
                      </div>
                      <div style={{ textAlign: 'center', minWidth: '80px' }}>
                        <div style={{ fontSize: '24px', marginBottom: '4px' }}>✂️</div>
                        <Text style={{ fontSize: '12px', color: '#666' }}>幻灯片制作</Text>
                      </div>
                      <div style={{ textAlign: 'center', minWidth: '80px' }}>
                        <div style={{ fontSize: '24px', marginBottom: '4px' }}>📈</div>
                        <Text style={{ fontSize: '12px', color: '#666' }}>数据分析</Text>
                      </div>
                      <div style={{ textAlign: 'center', minWidth: '80px' }}>
                        <div style={{ fontSize: '24px', marginBottom: '4px' }}>💻</div>
                        <Text style={{ fontSize: '12px', color: '#666' }}>网站开发</Text>
                      </div>
                      <div style={{ textAlign: 'center', minWidth: '80px' }}>
                        <Text style={{ fontSize: '12px', color: '#666' }}>更多</Text>
                      </div>
                    </div>
                  </div>
                </div>

              </>
            )}
          </Content>
        </Layout>
      </AppLayout>
    </>
  );
};

export default ChatPage;
