/**
 * 国际化工具类
 * 管理语言切换和语言参数传递
 */

export type Language = 'zh-CN' | 'en-US';

const LANGUAGE_KEY = 'app_language';

/**
 * 获取当前语言
 */
export function getCurrentLanguage(): Language {
  // 优先从 localStorage 读取用户选择的语言
  const savedLanguage = localStorage.getItem(LANGUAGE_KEY) as Language;
  if (savedLanguage) {
    return savedLanguage;
  }
  
  // 否则从浏览器语言自动检测
  const browserLanguage = navigator.language;
  if (browserLanguage.startsWith('zh')) {
    return 'zh-CN';
  }
  return 'en-US';
}

/**
 * 设置语言
 */
export function setLanguage(language: Language): void {
  localStorage.setItem(LANGUAGE_KEY, language);
  // 刷新页面以应用新语言
  window.location.reload();
}

/**
 * 获取语言显示名称
 */
export function getLanguageDisplayName(language: Language): string {
  const names: Record<Language, string> = {
    'zh-CN': '简体中文',
    'en-US': 'English',
  };
  return names[language] || names['zh-CN'];
}

