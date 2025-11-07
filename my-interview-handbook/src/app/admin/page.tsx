'use client';

import { useState, useEffect } from 'react';
import { PersonProfile, MarkdownContent } from '@/types/content';
import Link from 'next/link';

export default function AdminPage() {
  const [people, setPeople] = useState<PersonProfile[]>([]);
  const [selectedPerson, setSelectedPerson] = useState<string>('');
  const [selectedFile, setSelectedFile] = useState<string>('');
  const [content, setContent] = useState<string>('');
  const [originalContent, setOriginalContent] = useState<string>('');
  const [isSaving, setIsSaving] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const [isDev, setIsDev] = useState(false);

  // 检查是否为开发环境
  useEffect(() => {
    setIsDev(process.env.NODE_ENV === 'development');
  }, []);

  // 加载人员列表
  useEffect(() => {
    async function loadPeople() {
      try {
        const res = await fetch('/api/people');
        const data = await res.json();
        setPeople(data);
        if (data.length > 0) {
          setSelectedPerson(data[0].id);
          if (data[0].tabs.length > 0) {
            setSelectedFile(data[0].tabs[0].file);
          }
        }
      } catch (error) {
        console.error('Error loading people:', error);
      }
    }
    loadPeople();
  }, []);

  // 加载文件内容
  useEffect(() => {
    if (!selectedPerson || !selectedFile) return;

    async function loadContent() {
      try {
        const res = await fetch(`/api/content?person=${selectedPerson}&file=${selectedFile}`);
        const data: MarkdownContent = await res.json();
        setContent(data.rawContent);
        setOriginalContent(data.rawContent);
      } catch (error) {
        console.error('Error loading content:', error);
        setMessage({ type: 'error', text: '加载文件失败' });
      }
    }

    loadContent();
  }, [selectedPerson, selectedFile]);

  // 保存内容
  const handleSave = async () => {
    if (!isDev) {
      setMessage({ type: 'error', text: '仅在开发环境可编辑' });
      return;
    }

    setIsSaving(true);
    setMessage(null);

    try {
      const res = await fetch('/api/content', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          person: selectedPerson,
          file: selectedFile,
          content,
        }),
      });

      const result = await res.json();

      if (res.ok) {
        setMessage({ type: 'success', text: '保存成功！已创建备份' });
        setOriginalContent(content);
      } else {
        setMessage({ type: 'error', text: result.error || '保存失败' });
      }
    } catch (error) {
      console.error('Error saving:', error);
      setMessage({ type: 'error', text: '保存失败' });
    } finally {
      setIsSaving(false);
    }
  };

  // 当前人员
  const currentPerson = people.find(p => p.id === selectedPerson);
  const hasChanges = content !== originalContent;

  if (!isDev) {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center p-4">
        <div className="bg-white rounded-lg shadow-xl p-8 max-w-md text-center">
          <div className="text-6xl mb-4">🔒</div>
          <h1 className="text-2xl font-bold text-gray-800 mb-2">仅开发环境可用</h1>
          <p className="text-gray-600 mb-6">
            后台管理页面仅在开发环境（npm run dev）可用。
          </p>
          <Link
            href="/"
            className="inline-block bg-purple-500 text-white px-6 py-3 rounded-lg hover:bg-purple-600 transition"
          >
            返回首页
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-100">
      {/* 顶部栏 */}
      <header className="bg-purple-600 text-white p-4 shadow-lg">
        <div className="max-w-7xl mx-auto flex items-center justify-between">
          <h1 className="text-2xl font-bold">🛠️ 后台管理</h1>
          <Link
            href="/"
            className="bg-white text-purple-600 px-4 py-2 rounded-lg hover:bg-gray-100 transition"
          >
            返回首页
          </Link>
        </div>
      </header>

      <div className="max-w-7xl mx-auto p-6">
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
          {/* 左侧：文件列表 */}
          <div className="lg:col-span-1 bg-white rounded-lg shadow p-4">
            <h2 className="text-lg font-bold mb-4 text-gray-800">文件列表</h2>
            
            {/* 人员选择 */}
            <select
              value={selectedPerson}
              onChange={(e) => setSelectedPerson(e.target.value)}
              className="w-full p-2 border rounded-lg mb-4"
            >
              {people.map((person) => (
                <option key={person.id} value={person.id}>
                  {person.name}
                </option>
              ))}
            </select>

            {/* 文件列表 */}
            <div className="space-y-2">
              {currentPerson?.tabs.map((tab) => (
                <button
                  key={tab.file}
                  onClick={() => setSelectedFile(tab.file)}
                  className={`w-full text-left p-3 rounded-lg transition ${
                    selectedFile === tab.file
                      ? 'bg-purple-500 text-white'
                      : 'bg-gray-100 hover:bg-gray-200 text-gray-800'
                  }`}
                >
                  <div className="font-semibold">{tab.icon} {tab.title}</div>
                  <div className="text-xs opacity-75">{tab.file}</div>
                </button>
              ))}
            </div>
          </div>

          {/* 右侧：编辑器 */}
          <div className="lg:col-span-3 bg-white rounded-lg shadow p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-bold text-gray-800">
                编辑：{currentPerson?.name} - {currentPerson?.tabs.find(t => t.file === selectedFile)?.title}
              </h2>
              
              <div className="flex gap-2">
                {hasChanges && (
                  <span className="text-sm text-orange-600 px-3 py-1 bg-orange-100 rounded">
                    未保存
                  </span>
                )}
                <button
                  onClick={handleSave}
                  disabled={!hasChanges || isSaving}
                  className={`px-6 py-2 rounded-lg transition ${
                    hasChanges && !isSaving
                      ? 'bg-purple-500 text-white hover:bg-purple-600'
                      : 'bg-gray-300 text-gray-500 cursor-not-allowed'
                  }`}
                >
                  {isSaving ? '保存中...' : '保存'}
                </button>
              </div>
            </div>

            {/* 消息提示 */}
            {message && (
              <div
                className={`mb-4 p-4 rounded-lg ${
                  message.type === 'success'
                    ? 'bg-green-100 text-green-800'
                    : 'bg-red-100 text-red-800'
                }`}
              >
                {message.text}
              </div>
            )}

            {/* Markdown 编辑器 */}
            <div className="border-2 border-gray-300 rounded-lg overflow-hidden">
              <textarea
                value={content}
                onChange={(e) => setContent(e.target.value)}
                className="w-full h-[70vh] p-4 font-mono text-sm resize-none focus:outline-none"
                placeholder="在此编辑 Markdown 内容..."
                spellCheck={false}
              />
            </div>

            {/* 提示 */}
            <div className="mt-4 text-sm text-gray-600 space-y-1">
              <p>💡 <strong>术语语法：</strong> [[术语|发音|解释]]</p>
              <p>📝 <strong>Frontmatter：</strong> 文件顶部以 --- 包裹的 YAML 配置</p>
              <p>💾 <strong>自动备份：</strong> 每次保存会在 backups/ 目录创建带时间戳的备份</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

