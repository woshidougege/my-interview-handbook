import { authApi } from './api';
import { setSm2PublicKey } from '../utils/sm2Encrypt';

// 全局公钥缓存
let cachedPublicKey: string | null = null;
let isPreloading = false;

/**
 * 预加载公钥（应用启动时调用）
 */
export const preloadPublicKey = async (): Promise<void> => {
  if (isPreloading) return;
  
  isPreloading = true;
  try {
    const response = await authApi.getPublicKey();
    if (response.data.code === 200 && response.data.data?.publicKey) {
      cachedPublicKey = response.data.data.publicKey;
      // 设置到SM2加密工具中
      setSm2PublicKey(cachedPublicKey!);
    }
  } catch (error) {
    console.warn('预加载公钥失败:', error);
  } finally {
    isPreloading = false;
  }
};

/**
 * 获取公钥（如果缓存为空则重新请求）
 */
export const getPublicKey = async (): Promise<string> => {
  if (cachedPublicKey) {
    return cachedPublicKey;
  }

  try {
    const response = await authApi.getPublicKey();
    if (response.data.code === 200 && response.data.data?.publicKey) {
      cachedPublicKey = response.data.data.publicKey;
      // 设置到SM2加密工具中
      setSm2PublicKey(cachedPublicKey!);
      return cachedPublicKey!;
    }
    throw new Error('获取公钥失败: 响应数据格式错误');
  } catch (error) {
    console.error('获取公钥失败:', error);
    throw new Error('获取公钥失败，请检查网络连接');
  }
};

/**
 * 清除公钥缓存（用于重新加载）
 */
export const clearPublicKeyCache = (): void => {
  cachedPublicKey = null;
};

/**
 * 获取当前缓存的公钥（不发起网络请求）
 */
export const getCachedPublicKey = (): string | null => {
  return cachedPublicKey;
};