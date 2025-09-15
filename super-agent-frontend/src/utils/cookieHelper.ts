/**
 * Cookie操作工具函数
 */

/**
 * 设置Cookie
 * @param name Cookie名称
 * @param value Cookie值
 * @param days 过期天数，默认7天
 * @param path 路径，默认为根路径
 */
export const setCookie = (
  name: string, 
  value: string, 
  days: number = 7, 
  path: string = '/'
): void => {
  const expires = new Date();
  expires.setTime(expires.getTime() + days * 24 * 60 * 60 * 1000);
  document.cookie = `${name}=${value}; expires=${expires.toUTCString()}; path=${path}`;
};

/**
 * 获取Cookie
 * @param name Cookie名称
 * @returns Cookie值或null
 */
export const getCookie = (name: string): string | null => {
  const nameEQ = name + '=';
  const ca = document.cookie.split(';');
  for (let i = 0; i < ca.length; i++) {
    let c = ca[i];
    while (c.charAt(0) === ' ') {
      c = c.substring(1, c.length);
    }
    if (c.indexOf(nameEQ) === 0) {
      return c.substring(nameEQ.length, c.length);
    }
  }
  return null;
};

/**
 * 删除Cookie
 * @param name Cookie名称
 * @param path 路径，默认为根路径
 */
export const deleteCookie = (name: string, path: string = '/'): void => {
  document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=${path};`;
};

/**
 * 检查Cookie是否存在
 * @param name Cookie名称
 * @returns 是否存在
 */
export const hasCookie = (name: string): boolean => {
  return getCookie(name) !== null;
};
