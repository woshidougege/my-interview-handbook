import Fuse from 'fuse.js';
import { SearchResult } from '@/types/content';

export interface SearchableContent {
  person: string;
  personName: string;
  tab: string;
  tabTitle: string;
  content: string;
}

/**
 * 创建搜索索引
 */
export function createSearchIndex(contents: SearchableContent[]) {
  return new Fuse(contents, {
    keys: [
      { name: 'personName', weight: 0.3 },
      { name: 'tabTitle', weight: 0.2 },
      { name: 'content', weight: 0.5 },
    ],
    threshold: 0.4,
    includeScore: true,
    includeMatches: true,
    minMatchCharLength: 2,
  });
}

/**
 * 执行搜索
 */
export function search(
  fuse: Fuse<SearchableContent>,
  query: string
): SearchResult[] {
  if (!query.trim()) {
    return [];
  }

  const results = fuse.search(query);
  
  return results.map(result => ({
    person: result.item.person,
    tab: result.item.tab,
    content: result.item.content,
    score: result.score || 0,
    matches: (result.matches || []).map(match => ({
      key: match.key || '',
      value: match.value || '',
      indices: match.indices || [],
    })),
  }));
}

/**
 * 高亮搜索结果
 */
export function highlightMatches(text: string, indices: number[][]): string {
  if (!indices || indices.length === 0) {
    return text;
  }

  let result = '';
  let lastIndex = 0;

  // 合并重叠的索引范围
  const merged = mergeIndices(indices);

  for (const [start, end] of merged) {
    result += text.slice(lastIndex, start);
    result += `<mark class="bg-yellow-200 text-gray-900">${text.slice(start, end + 1)}</mark>`;
    lastIndex = end + 1;
  }

  result += text.slice(lastIndex);
  return result;
}

/**
 * 合并重叠的索引范围
 */
function mergeIndices(indices: number[][]): number[][] {
  if (indices.length === 0) return [];

  const sorted = [...indices].sort((a, b) => a[0] - b[0]);
  const merged: number[][] = [sorted[0]];

  for (let i = 1; i < sorted.length; i++) {
    const current = sorted[i];
    const last = merged[merged.length - 1];

    if (current[0] <= last[1] + 1) {
      // 重叠或相邻，合并
      last[1] = Math.max(last[1], current[1]);
    } else {
      // 不重叠，添加新范围
      merged.push(current);
    }
  }

  return merged;
}

