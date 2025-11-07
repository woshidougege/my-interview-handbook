'use client';

import { useState, useMemo } from 'react';
import dynamic from 'next/dynamic';

// 动态导入可视化组件
const RAGFlow = dynamic(() => import('./visualizations/RAGFlow'), { ssr: false });
const KafkaArchitecture = dynamic(() => import('./visualizations/KafkaArchitecture'), { ssr: false });
const HudiArchitecture = dynamic(() => import('./visualizations/HudiArchitecture'), { ssr: false });

interface MarkdownRendererProps {
  content: string;
}

interface TermData {
  text: string;
  pronunciation?: string;
  explanation?: string;
}

export default function MarkdownRenderer({ content }: MarkdownRendererProps) {
  const [selectedTerm, setSelectedTerm] = useState<TermData & { element: HTMLElement } | null>(null);

  // 首先分离可视化组件
  const contentParts = useMemo(() => {
    const parts: Array<{ type: 'text' | 'component'; content: string }> = [];
    const regex = /(\{[A-Za-z]+\})/g;
    let lastIndex = 0;
    let match;

    while ((match = regex.exec(content)) !== null) {
      if (match.index > lastIndex) {
        parts.push({ type: 'text', content: content.slice(lastIndex, match.index) });
      }
      parts.push({ type: 'component', content: match[1] });
      lastIndex = match.index + match[0].length;
    }

    if (lastIndex < content.length) {
      parts.push({ type: 'text', content: content.slice(lastIndex) });
    }

    return parts;
  }, [content]);

  // 渲染可视化组件
  const renderComponent = (componentName: string) => {
    switch (componentName) {
      case 'RAGFlow':
        return <RAGFlow />;
      case 'KafkaArchitecture':
        return <KafkaArchitecture />;
      case 'HudiArchitecture':
        return <HudiArchitecture />;
      default:
        return null;
    }
  };

  // 解析术语标记并转换为 HTML
  const parseTermsInText = (text: string): string => {
    return text.replace(
      /\[\[([^\]|]+)(?:\|([^\]|]+))?(?:\|([^\]]+))?\]\]/g,
      (_, term, pronunciation, explanation) => {
        const dataAttrs = [
          `data-term="${term}"`,
          pronunciation && `data-pronunciation="${pronunciation}"`,
          explanation && `data-explanation="${explanation.replace(/"/g, '&quot;')}"`,
        ].filter(Boolean).join(' ');
        
        return `<span class="term-marker" ${dataAttrs}>${term}</span>`;
      }
    );
  };

  // 处理行内Markdown（粗体、斜体、链接、代码），同时处理术语
  const processInlineMarkdown = (text: string): string => {
    // 首先处理术语
    let processed = parseTermsInText(text);
    
    // 然后处理其他行内Markdown
    processed = processed
      // 粗体
      .replace(/\*\*([^*]+)\*\*/g, '<strong class="font-bold text-gray-900">$1</strong>')
      // 行内代码
      .replace(/`([^`]+)`/g, '<code class="bg-pink-100 text-pink-800 px-2 py-1 rounded text-sm font-mono">$1</code>')
      // 链接
      .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" class="text-purple-600 hover:text-purple-800 underline" target="_blank" rel="noopener noreferrer">$1</a>')
      // 斜体
      .replace(/\*([^*]+)\*/g, '<em class="italic">$1</em>');
    
    return processed;
  };

  // 渲染Markdown文本
  const renderMarkdown = (text: string) => {
    const lines = text.split('\n');
    const elements: JSX.Element[] = [];
    
    let currentList: string[] = [];
    let currentOrderedList: string[] = [];
    let inCodeBlock = false;
    let codeContent: string[] = [];
    let currentTable: string[][] = [];
    let inTable = false;

    const flushList = () => {
      if (currentList.length > 0) {
        elements.push(
          <ul key={`ul-${elements.length}`} className="my-4 ml-6 list-disc space-y-2">
            {currentList.map((item, i) => (
              <li key={i} className="text-gray-700">
                <span dangerouslySetInnerHTML={{ __html: processInlineMarkdown(item) }} />
              </li>
            ))}
          </ul>
        );
        currentList = [];
      }
    };

    const flushOrderedList = () => {
      if (currentOrderedList.length > 0) {
        elements.push(
          <ol key={`ol-${elements.length}`} className="my-4 ml-6 list-decimal space-y-2">
            {currentOrderedList.map((item, i) => (
              <li key={i} className="text-gray-700">
                <span dangerouslySetInnerHTML={{ __html: processInlineMarkdown(item) }} />
              </li>
            ))}
          </ol>
        );
        currentOrderedList = [];
      }
    };

    const flushCodeBlock = () => {
      if (codeContent.length > 0) {
        elements.push(
          <pre key={`code-${elements.length}`} className="my-4 bg-gray-900 rounded-lg overflow-hidden">
            <code className="block text-gray-100 p-4 overflow-x-auto text-sm font-mono">
              {codeContent.join('\n')}
            </code>
          </pre>
        );
        codeContent = [];
      }
    };

    const flushTable = () => {
      if (currentTable.length > 0) {
        const headerRow = currentTable[0];
        const bodyRows = currentTable.slice(1).filter(row => 
          !row.every(cell => cell.match(/^[\-:|\s]+$/))
        );
        
        elements.push(
          <div key={`table-${elements.length}`} className="overflow-x-auto my-6">
            <table className="min-w-full border-collapse">
              <thead>
                <tr>
                  {headerRow.map((cell, i) => (
                    <th key={i} className="bg-purple-500 text-white font-bold px-4 py-2 border border-gray-300 text-left">
                      <span dangerouslySetInnerHTML={{ __html: processInlineMarkdown(cell.trim()) }} />
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {bodyRows.map((row, i) => (
                  <tr key={i} className={i % 2 === 0 ? 'bg-white' : 'bg-yellow-50'}>
                    {row.map((cell, j) => (
                      <td key={j} className="px-4 py-2 border border-gray-300">
                        <span dangerouslySetInnerHTML={{ __html: processInlineMarkdown(cell.trim()) }} />
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        );
        currentTable = [];
        inTable = false;
      }
    };

    lines.forEach((line, index) => {
      // 代码块
      if (line.startsWith('```')) {
        if (inCodeBlock) {
          inCodeBlock = false;
          flushCodeBlock();
        } else {
          flushList();
          flushOrderedList();
          flushTable();
          inCodeBlock = true;
        }
        return;
      }

      if (inCodeBlock) {
        codeContent.push(line);
        return;
      }

      // 表格
      if (line.startsWith('|')) {
        flushList();
        flushOrderedList();
        inTable = true;
        const cells = line.split('|').slice(1, -1);
        currentTable.push(cells);
        return;
      } else if (inTable) {
        flushTable();
      }

      // 标题
      if (line.startsWith('####')) {
        flushList();
        flushOrderedList();
        elements.push(
          <h4 key={`h4-${index}`} className="text-xl font-bold mt-3 mb-2 text-gray-700">
            <span dangerouslySetInnerHTML={{ __html: processInlineMarkdown(line.slice(4).trim()) }} />
          </h4>
        );
      } else if (line.startsWith('###')) {
        flushList();
        flushOrderedList();
        elements.push(
          <h3 key={`h3-${index}`} className="text-2xl font-bold mt-4 mb-2 text-gray-700">
            <span dangerouslySetInnerHTML={{ __html: processInlineMarkdown(line.slice(3).trim()) }} />
          </h3>
        );
      } else if (line.startsWith('##')) {
        flushList();
        flushOrderedList();
        elements.push(
          <h2 key={`h2-${index}`} className="text-3xl font-bold mt-6 mb-3 text-gray-800">
            <span dangerouslySetInnerHTML={{ __html: processInlineMarkdown(line.slice(2).trim()) }} />
          </h2>
        );
      } else if (line.startsWith('#')) {
        flushList();
        flushOrderedList();
        elements.push(
          <h1 key={`h1-${index}`} className="text-4xl font-bold mt-8 mb-4 text-gray-900 border-b-4 border-purple-500 pb-2">
            <span dangerouslySetInnerHTML={{ __html: processInlineMarkdown(line.slice(1).trim()) }} />
          </h1>
        );
      }
      // 分隔线
      else if (line.trim() === '---') {
        flushList();
        flushOrderedList();
        elements.push(<hr key={`hr-${index}`} className="my-8 border-t-2 border-gray-300" />);
      }
      // 无序列表
      else if (line.match(/^[\-\*]\s/)) {
        flushOrderedList();
        currentList.push(line.replace(/^[\-\*]\s/, ''));
      }
      // 有序列表
      else if (line.match(/^\d+\.\s/)) {
        flushList();
        currentOrderedList.push(line.replace(/^\d+\.\s/, ''));
      }
      // 空行
      else if (line.trim() === '') {
        flushList();
        flushOrderedList();
      }
      // 普通段落
      else {
        flushList();
        flushOrderedList();
        if (line.trim()) {
          elements.push(
            <p key={`p-${index}`} className="my-4 text-gray-700 leading-relaxed">
              <span dangerouslySetInnerHTML={{ __html: processInlineMarkdown(line) }} />
            </p>
          );
        }
      }
    });

    flushList();
    flushOrderedList();
    flushCodeBlock();
    flushTable();

    return elements;
  };

  // 处理术语点击
  const handleClick = (event: React.MouseEvent) => {
    const target = event.target as HTMLElement;
    const termElement = target.closest('.term-marker');
    
    if (termElement) {
      const term = termElement.getAttribute('data-term');
      const pronunciation = termElement.getAttribute('data-pronunciation');
      const explanation = termElement.getAttribute('data-explanation');
      
      if (term) {
        setSelectedTerm({
          text: term,
          pronunciation: pronunciation || undefined,
          explanation: explanation || undefined,
          element: termElement as HTMLElement,
        });
      }
    }
  };

  return (
    <div className="markdown-content" onClick={handleClick}>
      {/* 术语标记样式 */}
      <style jsx>{`
        :global(.term-marker) {
          color: #9333ea;
          font-weight: 600;
          text-decoration: underline;
          text-decoration-style: dotted;
          text-decoration-thickness: 2px;
          cursor: help;
          transition: all 0.2s;
          display: inline;
          padding: 0 2px;
        }
        :global(.term-marker:hover) {
          color: #7e22ce;
          text-decoration-style: solid;
          background-color: #faf5ff;
          border-radius: 3px;
        }
      `}</style>

      {/* 渲染内容 */}
      {contentParts.map((part, index) => {
        if (part.type === 'component') {
          const componentName = part.content.match(/\{([A-Za-z]+)\}/)?.[1];
          if (componentName) {
            return (
              <div key={`component-${index}`} className="my-6">
                {renderComponent(componentName)}
              </div>
            );
          }
        } else {
          return <div key={`text-${index}`}>{renderMarkdown(part.content)}</div>;
        }
        return null;
      })}

      {/* 术语提示卡片 */}
      {selectedTerm && (
        <>
          {/* 背景遮罩 */}
          <div
            className="fixed inset-0 bg-black bg-opacity-30 z-40"
            onClick={() => setSelectedTerm(null)}
          />

          {/* 提示卡片 */}
          <div
            className="fixed z-50 max-w-md bg-white rounded-lg shadow-2xl p-6 border-2 border-purple-500"
            style={{
              left: '50%',
              top: '50%',
              transform: 'translate(-50%, -50%)',
              maxWidth: '90vw',
              maxHeight: '80vh',
              overflow: 'auto',
            }}
          >
            {/* 关闭按钮 */}
            <button
              onClick={() => setSelectedTerm(null)}
              className="absolute top-2 right-2 text-gray-400 hover:text-gray-600 transition-colors"
              aria-label="关闭"
            >
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>

            {/* 术语名称 */}
            <h3 className="text-2xl font-bold text-purple-600 mb-4 pr-8">
              {selectedTerm.text}
            </h3>

            {/* 发音 */}
            {selectedTerm.pronunciation && (
              <div className="mb-4 p-3 bg-gradient-to-r from-purple-500 to-pink-500 rounded-lg">
                <div className="text-sm text-white opacity-90 mb-1">🔊 发音</div>
                <div className="text-xl font-bold text-white">
                  {selectedTerm.pronunciation}
                </div>
              </div>
            )}

            {/* 解释 */}
            {selectedTerm.explanation && (
              <div className="text-gray-700 leading-relaxed whitespace-pre-wrap">
                {selectedTerm.explanation}
              </div>
            )}

            {/* 如果没有发音和解释 */}
            {!selectedTerm.pronunciation && !selectedTerm.explanation && (
              <div className="text-gray-500 italic">
                暂无详细信息
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}
