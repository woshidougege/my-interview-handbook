import React, { useState, useEffect } from 'react';
import { Select } from 'antd';
import { GlobalOutlined } from '@ant-design/icons';
import { getCurrentLanguage, setLanguage, Language } from '@/utils/i18n';

/**
 * 语言切换组件
 * 用于在页面顶部切换中英文
 */
const LanguageSwitcher: React.FC = () => {
  const [currentLanguage, setCurrentLanguage] = useState<Language>('zh-CN');
  const [mounted, setMounted] = useState(false);

  // 只在客户端获取语言设置
  useEffect(() => {
    setCurrentLanguage(getCurrentLanguage());
    setMounted(true);
  }, []);

  const handleLanguageChange = (value: Language) => {
    setLanguage(value);
  };

  const options = [
    { value: 'zh-CN' as Language, label: '简体中文' },
    { value: 'en-US' as Language, label: 'English' },
  ];

  // 避免SSR和客户端渲染不一致的问题
  if (!mounted) {
    return (
      <Select
        value="zh-CN"
        style={{ width: 130 }}
        suffixIcon={<GlobalOutlined />}
        options={options}
        disabled
      />
    );
  }

  return (
    <Select
      value={currentLanguage}
      onChange={handleLanguageChange}
      style={{ width: 130 }}
      suffixIcon={<GlobalOutlined />}
      options={options}
    />
  );
};

export default LanguageSwitcher;

