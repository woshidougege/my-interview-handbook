import fs from 'fs';
import path from 'path';
import { PersonProfile } from '@/types/content';

const contentDir = path.join(process.cwd(), 'content');

/**
 * 获取所有人员列表
 */
export function getAllPeople(): PersonProfile[] {
  // 如果在浏览器环境，返回空数组（客户端会通过API获取）
  if (typeof window !== 'undefined') {
    return [];
  }

  if (!fs.existsSync(contentDir)) {
    return [];
  }

  const peopleDirectories = fs.readdirSync(contentDir, { withFileTypes: true })
    .filter(dirent => dirent.isDirectory())
    .map(dirent => dirent.name);

  const people: PersonProfile[] = [];

  for (const personDir of peopleDirectories) {
    const profilePath = path.join(contentDir, personDir, 'profile.json');
    
    if (fs.existsSync(profilePath)) {
      try {
        const profileData = JSON.parse(fs.readFileSync(profilePath, 'utf-8'));
        people.push({
          id: personDir,
          ...profileData,
        });
      } catch (error) {
        console.error(`Error reading profile for ${personDir}:`, error);
      }
    }
  }

  // 按名字排序
  return people.sort((a, b) => a.name.localeCompare(b.name, 'zh-CN'));
}

/**
 * 获取指定人员的配置
 */
export function getPersonProfile(personId: string): PersonProfile | null {
  const people = getAllPeople();
  return people.find(p => p.id === personId) || null;
}

/**
 * 获取人员的内容文件路径
 */
export function getPersonContentPath(personId: string, filename: string): string {
  return path.join(contentDir, personId, filename);
}

