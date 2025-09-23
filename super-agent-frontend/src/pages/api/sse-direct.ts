import { NextApiRequest, NextApiResponse } from 'next';
import { API_CONFIG, API_ENDPOINTS } from '@/config/apiEndpoints';

export default async function handler(req: NextApiRequest, res: NextApiResponse) {
  if (req.method !== 'GET') {
    return res.status(405).json({ message: 'Method not allowed' });
  }

  const { orderNo } = req.query;
  
  if (!orderNo || typeof orderNo !== 'string') {
    return res.status(400).json({ message: 'Missing orderNo parameter' });
  }

  // 设置SSE响应头
  res.writeHead(200, {
    'Content-Type': 'text/event-stream',
    'Cache-Control': 'no-cache, no-transform',
    'Connection': 'keep-alive',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'Cache-Control',
    'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
  });

  // 发送连接成功消息
  res.write('data: {"message": "SSE连接建立成功", "orderNo": "' + orderNo + '", "time": "' + new Date().toISOString() + '"}\n\n');

  try {
    const backendBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8081/super-agent';
    const backendUrl = `${backendBaseUrl}/${API_CONFIG.VERSION}/${API_ENDPOINTS.PAYMENT.STATUS_LISTEN(orderNo)}`;

    const http = await import('http');
    const url = await import('url');
    
    const parsedUrl = url.parse(backendUrl);
    
    const options = {
      hostname: parsedUrl.hostname,
      port: parsedUrl.port,
      path: parsedUrl.path,
      method: 'GET',
      headers: {
        'Accept': 'text/event-stream',
        'Cache-Control': 'no-cache',
        'Connection': 'keep-alive',
        // 传递前端的Cookie到后端（包含satoken）
        'Cookie': req.headers.cookie || '',
      }
    };

    const proxyReq = http.request(options, (proxyRes) => {
      if (proxyRes.statusCode !== 200) {
        res.write(`data: {"error": "Backend returned ${proxyRes.statusCode}"}\n\n`);
        res.end();
        return;
      }
      
      // 直接管道传输数据流
      proxyRes.on('data', (chunk) => {
        res.write(chunk);
      });
      
      proxyRes.on('end', () => {
        res.end();
      });

      proxyRes.on('error', () => {
        res.end();
      });
    });

    proxyReq.on('error', (err) => {
      res.write(`data: {"error": "Proxy connection failed: ${(err as Error).message}"}\n\n`);
      res.end();
    });

    // 处理客户端断开连接
    req.on('close', () => {
      proxyReq.destroy();
    });

    req.on('error', () => {
      proxyReq.destroy();
    });

    proxyReq.end();

  } catch (error) {
    res.write(`data: {"error": "Proxy setup failed: ${error}"}\n\n`);
    res.end();
  }
}

// 配置API选项
export const config = {
  api: {
    externalResolver: true,
    bodyParser: false,
  },
};
