'use client';

import { PersonProfile } from '@/types/content';
import { useState, useEffect, useRef } from 'react';

interface PersonSelectorProps {
  people: PersonProfile[];
  selectedPerson: string;
  onPersonChange: (personId: string) => void;
}

export default function PersonSelector({
  people,
  selectedPerson,
  onPersonChange,
}: PersonSelectorProps) {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const currentPerson = people.find(p => p.id === selectedPerson);

  // 点击外部关闭下拉框
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  return (
    <div className="relative" ref={dropdownRef}>
      {/* 选择器按钮 */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center gap-3 px-4 py-2 bg-white border-2 border-purple-500 rounded-lg hover:bg-purple-50 transition-colors"
      >
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-full bg-gradient-to-br from-purple-500 to-pink-500 flex items-center justify-center text-white font-bold">
            {currentPerson?.name[0] || '?'}
          </div>
          <div className="text-left">
            <div className="font-bold text-gray-900">{currentPerson?.name || '选择人员'}</div>
            <div className="text-xs text-gray-500">{currentPerson?.title || ''}</div>
          </div>
        </div>
        <svg
          className={`w-4 h-4 transition-transform ${isOpen ? 'rotate-180' : ''}`}
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
        </svg>
      </button>

      {/* 下拉列表 */}
      {isOpen && (
        <div className="absolute top-full left-0 mt-2 w-64 bg-white border-2 border-purple-200 rounded-lg shadow-xl z-50 max-h-96 overflow-y-auto">
          {people.map((person) => (
            <button
              key={person.id}
              onClick={() => {
                onPersonChange(person.id);
                setIsOpen(false);
              }}
              className={`w-full flex items-center gap-3 px-4 py-3 hover:bg-purple-50 transition-colors ${
                person.id === selectedPerson ? 'bg-purple-100' : ''
              }`}
            >
              <div className="w-10 h-10 rounded-full bg-gradient-to-br from-purple-500 to-pink-500 flex items-center justify-center text-white font-bold text-lg">
                {person.name[0]}
              </div>
              <div className="text-left flex-1">
                <div className="font-bold text-gray-900">{person.name}</div>
                <div className="text-sm text-gray-500">{person.title}</div>
              </div>
              {person.id === selectedPerson && (
                <svg
                  className="w-5 h-5 text-purple-500"
                  fill="currentColor"
                  viewBox="0 0 20 20"
                >
                  <path
                    fillRule="evenodd"
                    d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                    clipRule="evenodd"
                  />
                </svg>
              )}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

