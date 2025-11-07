import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "我的面试手册",
  description: "个人面试准备资料，技术栈知识库",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="zh-CN">
      <body className="antialiased">
        {children}
      </body>
    </html>
  );
}
