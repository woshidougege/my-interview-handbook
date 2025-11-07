'use client';

import { useState, useEffect, useRef } from 'react';
import { SearchResult } from '@/types/content';

interface SearchBoxProps {
  onSearch: (query: string) => SearchResult[];
  onResultClick: (result: SearchResult) => void;
}

export default function SearchBox({ onSearch, onResultClick }: SearchBoxProps) {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<SearchResult[]>([]);
  const [isOpen, setIsOpen] = useState(false);
  const searchRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (query.trim()) {
      const searchResults = onSearch(query);
      setResults(searchResults);
      setIsOpen(true);
    } else {
      setResults([]);
      setIsOpen(false);
    }
  }, [query, onSearch]);

  // 点击外部关闭搜索结果
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (searchRef.current && !searchRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleClear = () => {
    setQuery('');
    setResults([]);
    setIsOpen(false);
  };

  const handleResultClick = (result: SearchResult) => {
    onResultClick(result);
    setIsOpen(false);
    setQuery('');
  };

  return (
    <div className="relative" ref={searchRef}>
      {/* 搜索输入框 */}
      <div className="relative">
        <div className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400">
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
        </div>
        
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="搜索技术术语、项目名称..."
          className="w-full pl-10 pr-10 py-3 border-2 border-gray-300 rounded-lg focus:border-purple-500 focus:outline-none focus:ring-2 focus:ring-purple-200 transition-all"
        />

        {query && (
          <button
            onClick={handleClear}
            className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        )}
      </div>

      {/* 搜索结果下拉框 */}
      {isOpen && results.length > 0 && (
        <div className="absolute top-full left-0 right-0 mt-2 bg-white border-2 border-purple-200 rounded-lg shadow-2xl z-50 max-h-96 overflow-y-auto">
          <div className="p-2">
            <div className="text-sm text-gray-500 px-3 py-2">
              找到 {results.length} 个结果
            </div>
            
            {results.map((result, index) => (
              <button
                key={index}
                onClick={() => handleResultClick(result)}
                className="w-full text-left px-3 py-3 hover:bg-purple-50 rounded-lg transition-colors"
              >
                <div className="flex items-center gap-2 mb-1">
                  <span className="text-xs font-semibold text-purple-600 bg-purple-100 px-2 py-1 rounded">
                    {result.person}
                  </span>
                  <span className="text-xs text-gray-500">{result.tab}</span>
                </div>
                
                <div className="text-sm text-gray-700 line-clamp-2">
                  {result.content.substring(0, 150)}...
                </div>
              </button>
            ))}
          </div>
        </div>
      )}

      {/* 无结果提示 */}
      {isOpen && query && results.length === 0 && (
        <div className="absolute top-full left-0 right-0 mt-2 bg-white border-2 border-gray-200 rounded-lg shadow-xl z-50 p-6 text-center">
          <div className="text-gray-400 text-lg mb-2">😕</div>
          <div className="text-gray-600">未找到匹配的结果</div>
          <div className="text-sm text-gray-400 mt-1">试试其他关键词</div>
        </div>
      )}
    </div>
  );
}

