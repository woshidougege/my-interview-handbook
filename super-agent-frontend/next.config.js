/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  swcMinify: true,
  
  // 解决antd相关依赖的ES模块问题
  transpilePackages: [
    'antd',
    '@ant-design/icons',
    '@rc-component/async-validator',
    '@rc-component/util',
    'rc-util',
    'rc-picker',
    'rc-tree',
    'rc-table',
    'rc-select',
    'rc-dropdown',
    'rc-menu',
    'rc-dialog',
    'rc-drawer',
    'rc-tooltip',
    'rc-input',
    'rc-input-number',
    'rc-pagination',
    'rc-progress',
    'rc-rate',
    'rc-resize-observer',
    'rc-segmented',
    'rc-slider',
    'rc-steps',
    'rc-switch',
    'rc-tabs',
    'rc-textarea',
    'rc-upload',
    'rc-virtual-list',
    '@rc-component/portal',
    '@rc-component/trigger',
    '@rc-component/color-picker',
    '@rc-component/mutate-observer',
    '@rc-component/qrcode',
    '@rc-component/tour',
    '@rc-component/mini-decimal'
  ],
  
  // 标准Next.js应用配置
  images: {
    unoptimized: true
  },
  
  // HTTP代理配置
  experimental: {
    proxyTimeout: 300000, // 5分钟超时
  },
  
  // API代理配置 - 开发环境
  async rewrites() {
    const backendUrl = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8081/super-agent';
    return process.env.NODE_ENV === 'development' ? [
      {
        source: '/api/:path*',
        destination: `${backendUrl}/api/v1/:path*`,
      },
    ] : [];
  },
  
  // 环境变量
  env: {
    CUSTOM_KEY: process.env.CUSTOM_KEY,
  },
  
  // 编译优化
  optimizeFonts: false,
  
  // 构建配置
  webpack(config) {
    // 支持 .svg 文件作为 React 组件导入
    config.module.rules.push({
      test: /\.svg$/,
      use: ["@svgr/webpack"],
    });
    
    return config;
  },
};

module.exports = nextConfig;