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
  
  // 静态导出配置（用于nginx部署）
  // 注意：开发环境下禁用静态导出以支持API代理
  ...(process.env.NODE_ENV === 'production' && {
    output: 'export',
    distDir: 'out',
    trailingSlash: true,
  }),
  
  // HTTP代理配置
  experimental: {
    proxyTimeout: 300000, // 5分钟超时
  },
  
  // API代理配置 - 开发环境下启用，生产环境由nginx处理
  async rewrites() {
    if (process.env.NODE_ENV === 'development') {
      const backendUrl = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8081/super-agent';
      return [
        // API接口代理
        {
          source: '/super-agent/api/v1/:path*',
          destination: `${backendUrl}/api/v1/:path*`,
        },
        // SSO接口代理
        {
          source: '/super-agent/sso/:path*',
          destination: `${backendUrl}/sso/:path*`,
        },
      ];
    }
    return [];
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