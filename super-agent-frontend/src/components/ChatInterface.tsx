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
  ThunderboltOutlined,
  AudioOutlined,
  AudioMutedOutlined
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
  
  // 语音输入相关状态
  const [isRecording, setIsRecording] = useState(false);
  const [voiceConnectionStatus, setVoiceConnectionStatus] = useState<'disconnected' | 'connecting' | 'connected' | 'error'>('disconnected');
  
  const inputRef = useRef<HTMLTextAreaElement>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const speechServiceRef = useRef<any>(null); // TODO: 添加正确的类型定义
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);

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

  // 初始化语音识别服务
  useEffect(() => {
    const initSpeechService = async () => {
      try {
        const { SpeechRecognitionService } = await import('@/services/speechRecognitionService');
        speechServiceRef.current = new SpeechRecognitionService();

        // 设置事件监听
        speechServiceRef.current.on('connectionOpen', () => {
          setVoiceConnectionStatus('connected');
        });

        speechServiceRef.current.on('connectionClose', () => {
          setVoiceConnectionStatus('disconnected');
          setIsRecording(false);
        });

        speechServiceRef.current.on('connectionError', (error: string) => {
          setVoiceConnectionStatus('error');
          setIsRecording(false);
          message.error(`语音识别连接失败: ${error}`);
        });

        speechServiceRef.current.on('recognition', (result: any) => {
          if (result.text) {
            if (result.isFinal) {
              // 最终结果，追加到输入框
              setInputValue(prev => prev + result.text + ' ');
            } else {
              // 临时结果，显示预览（可选实现）
              console.log('临时识别结果:', result.text);
            }
          }
        });

        speechServiceRef.current.on('error', (error: string) => {
          message.error(`语音识别错误: ${error}`);
          setIsRecording(false);
        });

      } catch (error) {
        console.error('语音识别服务初始化失败:', error);
      }
    };

    initSpeechService();

    return () => {
      // 清理资源
      if (speechServiceRef.current) {
        speechServiceRef.current.disconnect();
      }
      if (mediaRecorderRef.current) {
        mediaRecorderRef.current.stop();
      }
    };
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
      setVoiceConnectionStatus('connecting');
      
      // 连接WebSocket
      await speechServiceRef.current?.connect();
      
      // 请求麦克风权限并开始录音
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: {
          sampleRate: 16000,
          channelCount: 1,
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true
        }
      });

      mediaRecorderRef.current = new MediaRecorder(stream, {
        mimeType: 'audio/webm;codecs=opus'
      });

      mediaRecorderRef.current.ondataavailable = (event) => {
        if (event.data.size > 0) {
          const reader = new FileReader();
          reader.onload = () => {
            const arrayBuffer = reader.result as ArrayBuffer;
            speechServiceRef.current?.sendAudioData(arrayBuffer);
          };
          reader.readAsArrayBuffer(event.data);
        }
      };

      mediaRecorderRef.current.start(100); // 每100ms发送一次数据
      setIsRecording(true);

      // 启动语音识别会话
      speechServiceRef.current?.start();

      message.success('开始语音输入');
    } catch (error: any) {
      console.error('启动语音录音失败:', error);
      message.error(`无法启动语音输入: ${error.message}`);
      setVoiceConnectionStatus('error');
    }
  };

  // 停止语音录音
  const stopVoiceRecording = () => {
    try {
      if (mediaRecorderRef.current && isRecording) {
        mediaRecorderRef.current.stop();
        const tracks = mediaRecorderRef.current.stream.getTracks();
        tracks.forEach(track => track.stop());
      }

      speechServiceRef.current?.stop();
      setIsRecording(false);
      
      message.info('语音输入已结束');
    } catch (error) {
      console.error('停止语音录音失败:', error);
      message.error('停止语音输入失败');
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
        <div style={{ position: 'relative', display: 'flex', alignItems: 'flex-end', gap: '8px' }}>
          {/* 语音输入按钮 */}
          <Tooltip title={isRecording ? '点击停止语音输入' : voiceConnectionStatus === 'connected' ? '点击开始语音输入' : '语音服务连接中...'}>
            <Button
              type={isRecording ? "primary" : "default"}
              icon={isRecording ? <AudioMutedOutlined /> : <AudioOutlined />}
              onClick={toggleVoiceRecording}
              disabled={loading || titleGenerating || !aiServiceAvailable || voiceConnectionStatus === 'connecting'}
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

          {/* 输入框容器 */}
          <div style={{ position: 'relative', flex: 1 }}>
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
              disabled={loading || titleGenerating || !aiServiceAvailable || isRecording}
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
                disabled={!inputValue.trim() || titleGenerating || !aiServiceAvailable || isRecording}
                size="small"
              >
                发送
              </Button>
            </div>
          </div>
        </div>
        
        <div style={{ 
          marginTop: '8px', 
          fontSize: '12px', 
          color: '#999',
          textAlign: 'center'
        }}>
          {isRecording ? (
            <span style={{ color: '#ff4d4f', fontWeight: 'bold' }}>
              🎤 正在录音中，再次点击麦克风结束录音
            </span>
          ) : (
            '按 Enter 发送，Shift + Enter 换行，点击 🎤 语音输入'
          )}
        </div>
      </div>
    </div>
  );
};

export default ChatInterface;
