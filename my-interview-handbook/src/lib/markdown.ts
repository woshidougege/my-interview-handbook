import fs from 'fs';
import matter from 'gray-matter';
import { MarkdownContent, Term } from '@/types/content';

/**
 * 解析 Markdown 文件
 */
export function parseMarkdown(filePath: string): MarkdownContent | null {
  if (typeof window !== 'undefined') {
    return null;
  }

  try {
    if (!fs.existsSync(filePath)) {
      return null;
    }

    const fileContent = fs.readFileSync(filePath, 'utf-8');
    const { data, content } = matter(fileContent);

    return {
      frontmatter: data,
      content: content,
      rawContent: fileContent,
    };
  } catch (error) {
    console.error(`Error parsing markdown file ${filePath}:`, error);
    return null;
  }
}

/**
 * 从 Markdown 内容中提取术语
 * 语法：[[术语|发音|解释]]
 */
export function extractTerms(content: string): Term[] {
  const termRegex = /\[\[([^\]|]+)(?:\|([^\]|]+))?(?:\|([^\]]+))?\]\]/g;
  const terms: Term[] = [];
  let match;

  while ((match = termRegex.exec(content)) !== null) {
    terms.push({
      text: match[1].trim(),
      pronunciation: match[2]?.trim(),
      explanation: match[3]?.trim(),
    });
  }

  return terms;
}

/**
 * 解析术语标记并转换为 HTML
 * 将 [[术语|发音|解释]] 转换为带 data 属性的 span
 */
export function parseTermsInContent(content: string): string {
  const termRegex = /\[\[([^\]|]+)(?:\|([^\]|]+))?(?:\|([^\]]+))?\]\]/g;
  
  return content.replace(termRegex, (match, text, pronunciation, explanation) => {
    const attrs: string[] = [`data-term="${escapeHtml(text)}"`];
    
    if (pronunciation) {
      attrs.push(`data-pronunciation="${escapeHtml(pronunciation)}"`);
    }
    
    if (explanation) {
      attrs.push(`data-explanation="${escapeHtml(explanation)}"`);
    }
    
    return `<span class="term" ${attrs.join(' ')}>${escapeHtml(text)}</span>`;
  });
}

/**
 * HTML 转义
 */
function escapeHtml(text: string): string {
  const map: Record<string, string> = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#039;',
  };
  return text.replace(/[&<>"']/g, m => map[m]);
}

/**
 * 保存 Markdown 文件
 */
export function saveMarkdown(
  filePath: string,
  frontmatter: Record<string, any>,
  content: string
): boolean {
  if (typeof window !== 'undefined') {
    return false;
  }

  try {
    const fileContent = matter.stringify(content, frontmatter);
    fs.writeFileSync(filePath, fileContent, 'utf-8');
    return true;
  } catch (error) {
    console.error(`Error saving markdown file ${filePath}:`, error);
    return false;
  }
}

