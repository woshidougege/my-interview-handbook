import type { NextConfig } from "next";

const isProd = process.env.NODE_ENV === 'production';

const nextConfig: NextConfig = {
  // 静态导出配置
  output: isProd ? 'export' : undefined,
  
  // 图片优化配置（静态导出时需要关闭）
  images: {
    unoptimized: isProd,
  },
  
  // 添加尾部斜杠
  trailingSlash: true,
  
  // Base path（如果部署到子路径，如 GitHub Pages）
  // basePath: isProd ? '/my-interview-handbook' : '',
  
  // Turbopack 配置（Next.js 16+ 默认）
  turbopack: {},
};

export default nextConfig;
