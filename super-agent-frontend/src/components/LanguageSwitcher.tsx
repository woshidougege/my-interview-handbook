import React from 'react';
import { Select } from 'antd';
import { GlobalOutlined } from '@ant-design/icons';
import { getCurrentLanguage, setLanguage, getLanguageDisplayName, Language } from '@/utils/i18n';

/**
 * 语言切换组件
 * 用于在页面顶部切换中英文
 */
const LanguageSwitcher: React.FC = () => {
  const currentLanguage = getCurrentLanguage();

  const handleLanguageChange = (value: Language) => {
    setLanguage(value);
  };

  const options = [
    { value: 'zh-CN' as Language, label: '简体中文' },
    { value: 'en-US' as Language, label: 'English' },
  ];

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

