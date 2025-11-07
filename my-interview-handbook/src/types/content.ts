// 内容类型定义

export interface PersonProfile {
  id: string;
  name: string;
  title: string;
  avatar?: string;
  tabs: TabConfig[];
}

export interface TabConfig {
  id: string;
  title: string;
  icon: string;
  file: string;
  order: number;
}

export interface MarkdownContent {
  frontmatter: ContentFrontmatter;
  content: string;
  rawContent: string;
}

export interface ContentFrontmatter {
  title: string;
  icon?: string;
  order?: number;
  description?: string;
  [key: string]: any;
}

export interface Term {
  text: string;
  pronunciation?: string;
  explanation?: string;
}

export interface SearchResult {
  person: string;
  tab: string;
  content: string;
  score: number;
  matches: SearchMatch[];
}

export interface SearchMatch {
  key: string;
  value: string;
  indices: number[][];
}

