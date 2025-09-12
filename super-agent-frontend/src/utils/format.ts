import dayjs from 'dayjs';

/**
 * 格式化日期时间
 * @param date 日期字符串或Date对象
 * @param format 格式化模板，默认为 'YYYY-MM-DD HH:mm:ss'
 * @returns 格式化后的日期字符串
 */
export const formatDate = (
  date: string | Date | undefined | null, 
  format: string = 'YYYY-MM-DD HH:mm:ss'
): string => {
  if (!date) return '-';
  return dayjs(date).format(format);
};

/**
 * 格式化日期时间（别名方法）
 * @param date 日期字符串或Date对象
 * @param format 格式化模板，默认为 'YYYY-MM-DD HH:mm:ss'
 * @returns 格式化后的日期字符串
 */
export const formatDateTime = (
  date: string | Date | undefined | null, 
  format: string = 'YYYY-MM-DD HH:mm:ss'
): string => {
  return formatDate(date, format);
};

/**
 * 格式化数字，添加千分位分隔符
 * @param num 数字
 * @param precision 小数位数，默认为0
 * @returns 格式化后的数字字符串
 */
export const formatNumber = (num: number | undefined | null, precision: number = 0): string => {
  if (num === undefined || num === null) return '0';
  
  return new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: precision,
    maximumFractionDigits: precision,
  }).format(num);
};

/**
 * 格式化货币
 * @param amount 金额
 * @param currency 货币符号，默认为 '¥'
 * @param precision 小数位数，默认为2
 * @returns 格式化后的货币字符串
 */
export const formatCurrency = (
  amount: number | undefined | null, 
  currency: string = '¥', 
  precision: number = 2
): string => {
  if (amount === undefined || amount === null) return `${currency}0.00`;
  
  return `${currency}${formatNumber(amount, precision)}`;
};

/**
 * 格式化文件大小
 * @param bytes 字节数
 * @returns 格式化后的文件大小字符串
 */
export const formatFileSize = (bytes: number | undefined | null): string => {
  if (!bytes || bytes === 0) return '0 B';
  
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`;
};

/**
 * 格式化百分比
 * @param value 数值（0-1之间的小数或0-100之间的整数）
 * @param precision 小数位数，默认为1
 * @param isDecimal 是否为小数格式（true为0-1，false为0-100），默认为true
 * @returns 格式化后的百分比字符串
 */
export const formatPercentage = (
  value: number | undefined | null, 
  precision: number = 1, 
  isDecimal: boolean = true
): string => {
  if (value === undefined || value === null) return '0%';
  
  const percentage = isDecimal ? value * 100 : value;
  return `${formatNumber(percentage, precision)}%`;
};

/**
 * 格式化手机号码
 * @param phone 手机号码
 * @returns 格式化后的手机号码（中间4位用*替代）
 */
export const formatPhone = (phone: string | undefined | null): string => {
  if (!phone) return '-';
  
  if (phone.length === 11) {
    return `${phone.slice(0, 3)}****${phone.slice(7)}`;
  }
  
  return phone;
};

/**
 * 格式化邮箱地址
 * @param email 邮箱地址
 * @returns 格式化后的邮箱地址（用户名部分部分隐藏）
 */
export const formatEmail = (email: string | undefined | null): string => {
  if (!email) return '-';
  
  const [username, domain] = email.split('@');
  if (!username || !domain) return email;
  
  if (username.length <= 2) {
    return `${username[0]}*@${domain}`;
  }
  
  return `${username.slice(0, 2)}***@${domain}`;
};

/**
 * 格式化时间段（相对时间）
 * @param date 日期字符串或Date对象
 * @returns 相对时间字符串
 */
export const formatRelativeTime = (date: string | Date | undefined | null): string => {
  if (!date) return '-';
  
  const now = dayjs();
  const target = dayjs(date);
  const diffInMinutes = now.diff(target, 'minute');
  
  if (diffInMinutes < 1) return '刚刚';
  if (diffInMinutes < 60) return `${diffInMinutes}分钟前`;
  
  const diffInHours = now.diff(target, 'hour');
  if (diffInHours < 24) return `${diffInHours}小时前`;
  
  const diffInDays = now.diff(target, 'day');
  if (diffInDays < 7) return `${diffInDays}天前`;
  
  return formatDate(date, 'YYYY-MM-DD');
};

// 默认导出包含所有格式化方法的对象
export default {
  formatDate,
  formatDateTime,
  formatNumber,
  formatCurrency,
  formatFileSize,
  formatPercentage,
  formatPhone,
  formatEmail,
  formatRelativeTime,
};
