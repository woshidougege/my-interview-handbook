// @ts-ignore
import { sm2 } from 'sm-crypto';

// SM2公钥（将从后端API获取）
let ARK_PUBLIC_KEY = '';

/**
 * 设置SM2公钥
 * @param publicKey 从后端获取的公钥
 */
export const setSm2PublicKey = (publicKey: string): void => {
  ARK_PUBLIC_KEY = publicKey;
};

/**
 * SM2加密函数
 * @param data 待加密的数据
 * @returns 加密后的字符串
 */
export const encryptWithSM2 = (data: string): string => {
  try {
    // 验证公钥是否有效
    if (!ARK_PUBLIC_KEY || typeof ARK_PUBLIC_KEY !== 'string') {
      throw new Error('无效的SM2公钥，请先调用setSm2PublicKey设置公钥');
    }
    
    // 验证数据是否有效
    if (!data || typeof data !== 'string') {
      throw new Error('无效的加密数据');
    }
    
    // 使用SM2公钥加密数据
    const encrypted = sm2.doEncrypt(data, ARK_PUBLIC_KEY, 0);
    
    // 验证加密结果
    if (!encrypted) {
      throw new Error('加密失败，返回结果为空');
    }
    
    return encrypted;
  } catch (error: any) {
    console.error('SM2加密失败:', error);
    throw new Error(`数据加密失败: ${error.message || '未知错误'}`);
  }
};

/**
 * 加密密码字段（方舟认证要求）
 * @param password 明文密码
 * @returns 加密后的密码
 */
export const encryptPassword = (password: string): string => {
  return encryptWithSM2(password);
};

/**
 * 加密手机验证码（方舟认证要求）
 * @param smsCode 短信验证码
 * @returns 加密后的验证码
 */
export const encryptSmsCode = (smsCode: string): string => {
  return encryptWithSM2(smsCode);
};