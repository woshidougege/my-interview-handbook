'use client';

import { TabConfig } from '@/types/content';

interface TabNavigationProps {
  tabs: TabConfig[];
  activeTab: string;
  onTabChange: (tabId: string) => void;
}

export default function TabNavigation({
  tabs,
  activeTab,
  onTabChange,
}: TabNavigationProps) {
  return (
    <div className="sticky top-0 z-40 bg-white border-b-2 border-gray-200 shadow-sm">
      {/* 桌面端：横向滚动 */}
      <div className="overflow-x-auto scrollbar-hide">
        <div className="flex min-w-max">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => onTabChange(tab.id)}
              className={`
                flex items-center gap-2 px-6 py-4 font-medium transition-all
                border-b-4 whitespace-nowrap
                ${
                  activeTab === tab.id
                    ? 'border-purple-500 text-purple-600 bg-purple-50'
                    : 'border-transparent text-gray-600 hover:text-purple-600 hover:bg-gray-50'
                }
              `}
            >
              <span className="text-2xl">{tab.icon}</span>
              <span className="font-semibold">{tab.title}</span>
            </button>
          ))}
        </div>
      </div>

      {/* 移动端：网格布局 */}
      <div className="md:hidden grid grid-cols-2 sm:grid-cols-4 gap-2 p-2 bg-gray-50">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => onTabChange(tab.id)}
            className={`
              flex flex-col items-center gap-1 p-3 rounded-lg font-medium transition-all
              ${
                activeTab === tab.id
                  ? 'bg-purple-500 text-white shadow-lg'
                  : 'bg-white text-gray-600 hover:bg-purple-50'
              }
            `}
          >
            <span className="text-2xl">{tab.icon}</span>
            <span className="text-xs">{tab.title}</span>
          </button>
        ))}
      </div>
    </div>
  );
}

