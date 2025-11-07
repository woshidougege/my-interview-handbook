'use client';

import { useState, useEffect, useMemo } from 'react';
import PersonSelector from '@/components/PersonSelector';
import TabNavigation from '@/components/TabNavigation';
import SearchBox from '@/components/SearchBox';
import MarkdownRenderer from '@/components/MarkdownRenderer';
import { PersonProfile, MarkdownContent, SearchResult } from '@/types/content';
import { createSearchIndex, search, SearchableContent } from '@/lib/search';
import Fuse from 'fuse.js';

export default function Home() {
  const [people, setPeople] = useState<PersonProfile[]>([]);
  const [selectedPerson, setSelectedPerson] = useState<string>('');
  const [activeTab, setActiveTab] = useState<string>('');
  const [contents, setContents] = useState<Record<string, Record<string, MarkdownContent>>>({});
  const [searchIndex, setSearchIndex] = useState<Fuse<SearchableContent> | null>(null);
  const [loading, setLoading] = useState(true);

  // 加载人员列表和内容
  useEffect(() => {
    async function loadData() {
      try {
        // 加载人员列表
        const peopleRes = await fetch('/api/people');
        const peopleData: PersonProfile[] = await peopleRes.json();
        setPeople(peopleData);

        if (peopleData.length > 0) {
          // 默认选择第一个人
          const firstPerson = peopleData[0];
          setSelectedPerson(firstPerson.id);
          setActiveTab(firstPerson.tabs[0]?.id || '');

          // 加载所有人的所有内容（用于搜索）
          const allContents: Record<string, Record<string, MarkdownContent>> = {};
          const searchableContents: SearchableContent[] = [];

          for (const person of peopleData) {
            allContents[person.id] = {};
            
            for (const tab of person.tabs) {
              const contentRes = await fetch(
                `/api/content?person=${person.id}&file=${tab.file}`
              );
              const content: MarkdownContent = await contentRes.json();
              allContents[person.id][tab.id] = content;

              // 添加到搜索索引
              searchableContents.push({
                person: person.id,
                personName: person.name,
                tab: tab.id,
                tabTitle: tab.title,
                content: content.content,
              });
            }
          }

          setContents(allContents);

          // 创建搜索索引
          const index = createSearchIndex(searchableContents);
          setSearchIndex(index);
        }

        setLoading(false);
      } catch (error) {
        console.error('Error loading data:', error);
        setLoading(false);
      }
    }

    loadData();
  }, []);

  // 当前人员配置
  const currentPerson = people.find(p => p.id === selectedPerson);
  const currentTabs = currentPerson?.tabs || [];
  
  // 当前内容
  const currentContent = contents[selectedPerson]?.[activeTab];

  // 搜索处理
  const handleSearch = useMemo(() => {
    return (query: string): SearchResult[] => {
      if (!searchIndex || !query.trim()) return [];
      return search(searchIndex, query);
    };
  }, [searchIndex]);

  // 搜索结果点击
  const handleSearchResultClick = (result: SearchResult) => {
    setSelectedPerson(result.person);
    setActiveTab(result.tab);
  };

  // 切换人员
  const handlePersonChange = (personId: string) => {
    setSelectedPerson(personId);
    const person = people.find(p => p.id === personId);
    if (person && person.tabs.length > 0) {
      setActiveTab(person.tabs[0].id);
    }
  };

  // 返回顶部
  const scrollToTop = () => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-purple-500 to-pink-500">
        <div className="text-white text-2xl font-bold">加载中...</div>
      </div>
    );
  }

  if (people.length === 0) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-purple-500 to-pink-500">
        <div className="bg-white p-8 rounded-lg shadow-xl text-center">
          <div className="text-6xl mb-4">📂</div>
          <h2 className="text-2xl font-bold text-gray-800 mb-2">暂无内容</h2>
          <p className="text-gray-600">请在 content/ 目录添加人员文件夹和内容</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-purple-500 to-pink-500 py-10 px-4">
      <div className="max-w-6xl mx-auto">
        {/* 主容器 */}
        <div className="bg-white rounded-2xl shadow-2xl overflow-hidden">
          {/* 顶部栏 */}
          <header className="bg-gradient-to-r from-purple-500 to-pink-500 text-white p-6">
            <div className="flex flex-col md:flex-row items-center justify-between gap-4">
              <div className="flex items-center gap-4">
                <h1 className="text-3xl font-bold">📚 面试手册</h1>
                <PersonSelector
                  people={people}
                  selectedPerson={selectedPerson}
                  onPersonChange={handlePersonChange}
                />
              </div>
              
              <div className="w-full md:w-96">
                <SearchBox
                  onSearch={handleSearch}
                  onResultClick={handleSearchResultClick}
                />
              </div>
            </div>
          </header>

          {/* 标签页导航 */}
          <TabNavigation
            tabs={currentTabs}
            activeTab={activeTab}
            onTabChange={setActiveTab}
          />

          {/* 内容区域 */}
          <main className="p-8">
            {currentContent ? (
              <MarkdownRenderer content={currentContent.content} />
            ) : (
              <div className="text-center py-12 text-gray-500">
                <div className="text-6xl mb-4">📄</div>
                <p>内容加载中...</p>
              </div>
            )}
          </main>
        </div>

        {/* 返回顶部按钮 */}
        <button
          onClick={scrollToTop}
          className="fixed bottom-6 right-6 bg-purple-500 text-white w-12 h-12 rounded-full shadow-lg hover:bg-purple-600 transition-all flex items-center justify-center z-50"
          title="返回顶部"
        >
          <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 10l7-7m0 0l7 7m-7-7v18" />
          </svg>
        </button>
        </div>
    </div>
  );
}
