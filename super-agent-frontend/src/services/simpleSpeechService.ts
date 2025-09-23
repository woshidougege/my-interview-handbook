/**
 * 简化版语音识别服务
 * 录音完成后上传文件进行识别，不再使用实时WebSocket
 */

import { API_CONFIG, API_ENDPOINTS } from '@/config/apiEndpoints';

export interface SpeechRecognitionResult {
  success: boolean;
  text?: string;
  error?: string;
}

export class SimpleSpeechService {
  private mediaRecorder: MediaRecorder | null = null;
  private audioChunks: Blob[] = [];
  private isRecording = false;

  /**
   * 检查浏览器支持
   */
  static isSupported(): boolean {
    return !!(
      typeof navigator !== 'undefined' &&
      navigator.mediaDevices &&
      typeof navigator.mediaDevices.getUserMedia === 'function' &&
      typeof MediaRecorder !== 'undefined'
    );
  }

  /**
   * 开始录音
   */
  async startRecording(): Promise<boolean> {
    if (this.isRecording) {
      console.warn('已在录音中');
      return false;
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: {
          sampleRate: 16000,
          channelCount: 1,
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true
        }
      });

      this.audioChunks = [];
      this.mediaRecorder = new MediaRecorder(stream, {
        mimeType: 'audio/webm;codecs=opus'
      });

      this.mediaRecorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          this.audioChunks.push(event.data);
        }
      };

      this.mediaRecorder.start();
      this.isRecording = true;

      console.log('开始录音');
      return true;

    } catch (error) {
      console.error('开始录音失败:', error);
      return false;
    }
  }

  /**
   * 停止录音并获取音频文件
   */
  async stopRecording(): Promise<Blob | null> {
    if (!this.isRecording || !this.mediaRecorder) {
      console.warn('未在录音中');
      return null;
    }

    return new Promise((resolve) => {
      if (!this.mediaRecorder) {
        resolve(null);
        return;
      }

      this.mediaRecorder.onstop = () => {
        const audioBlob = new Blob(this.audioChunks, { type: 'audio/webm' });
        
        // 停止所有音轨
        if (this.mediaRecorder?.stream) {
          this.mediaRecorder.stream.getTracks().forEach(track => track.stop());
        }
        
        this.isRecording = false;
        this.mediaRecorder = null;
        this.audioChunks = [];

        console.log('录音完成，文件大小:', audioBlob.size, 'bytes');
        resolve(audioBlob);
      };

      this.mediaRecorder.stop();
    });
  }

  /**
   * 上传音频文件进行识别
   */
  async recognizeAudio(audioBlob: Blob, language: string = 'zh'): Promise<SpeechRecognitionResult> {
    try {
      const formData = new FormData();
      formData.append('audioFile', audioBlob, `recording_${Date.now()}.webm`);
      formData.append('language', language);

      console.log('上传音频文件进行识别，大小:', audioBlob.size, 'bytes');

      const response = await fetch(`${API_CONFIG.BASE_URL}/${API_ENDPOINTS.SPEECH.RECOGNITION_UPLOAD}`, {
        method: 'POST',
        body: formData
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const result = await response.json();
      
      if (result.success && result.data) {
        return {
          success: true,
          text: result.data.text || ''
        };
      } else {
        return {
          success: false,
          error: result.message || '识别失败'
        };
      }

    } catch (error) {
      console.error('音频识别失败:', error);
      return {
        success: false,
        error: error instanceof Error ? error.message : '识别服务异常'
      };
    }
  }

  /**
   * 一键录音并识别
   */
  async recordAndRecognize(_language: string = 'zh'): Promise<SpeechRecognitionResult> {
    // 开始录音
    const startSuccess = await this.startRecording();
    if (!startSuccess) {
      return {
        success: false,
        error: '无法开始录音，请检查麦克风权限'
      };
    }

    // 等待用户停止录音（需要外部调用stopRecording）
    return {
      success: false,
      error: '请调用stopRecording()来完成录音'
    };
  }

  /**
   * 获取当前录音状态
   */
  getRecordingState(): boolean {
    return this.isRecording;
  }

  /**
   * 强制清理资源
   */
  cleanup(): void {
    if (this.mediaRecorder && this.isRecording) {
      this.mediaRecorder.stop();
      this.mediaRecorder.stream?.getTracks().forEach(track => track.stop());
    }
    
    this.mediaRecorder = null;
    this.audioChunks = [];
    this.isRecording = false;
    console.log('语音服务资源已清理');
  }
}
