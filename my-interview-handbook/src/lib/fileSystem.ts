import fs from 'fs';
import path from 'path';

const contentDir = path.join(process.cwd(), 'content');
const backupDir = path.join(process.cwd(), 'backups');

/**
 * 确保目录存在
 */
function ensureDir(dir: string) {
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
  }
}

/**
 * 读取文件内容
 */
export function readFile(personId: string, filename: string): string | null {
  if (typeof window !== 'undefined') {
    return null;
  }

  try {
    const filePath = path.join(contentDir, personId, filename);
    if (!fs.existsSync(filePath)) {
      return null;
    }
    return fs.readFileSync(filePath, 'utf-8');
  } catch (error) {
    console.error(`Error reading file ${personId}/${filename}:`, error);
    return null;
  }
}

/**
 * 保存文件内容
 */
export function saveFile(
  personId: string,
  filename: string,
  content: string
): { success: boolean; error?: string } {
  if (typeof window !== 'undefined') {
    return { success: false, error: 'Cannot save file in browser' };
  }

  try {
    const personDir = path.join(contentDir, personId);
    ensureDir(personDir);

    const filePath = path.join(personDir, filename);
    fs.writeFileSync(filePath, content, 'utf-8');

    return { success: true };
  } catch (error) {
    console.error(`Error saving file ${personId}/${filename}:`, error);
    return { success: false, error: String(error) };
  }
}

/**
 * 创建备份
 */
export function createBackup(
  personId: string,
  filename: string,
  content: string
): { success: boolean; backupPath?: string; error?: string } {
  if (typeof window !== 'undefined') {
    return { success: false, error: 'Cannot create backup in browser' };
  }

  try {
    const personBackupDir = path.join(backupDir, personId);
    ensureDir(personBackupDir);

    const timestamp = new Date()
      .toISOString()
      .replace(/[:.]/g, '-')
      .slice(0, 19);
    
    const backupFilename = `${filename.replace('.md', '')}.${timestamp}.md`;
    const backupPath = path.join(personBackupDir, backupFilename);

    fs.writeFileSync(backupPath, content, 'utf-8');

    return { success: true, backupPath };
  } catch (error) {
    console.error(`Error creating backup for ${personId}/${filename}:`, error);
    return { success: false, error: String(error) };
  }
}

/**
 * 列出某人的所有文件
 */
export function listPersonFiles(personId: string): string[] {
  if (typeof window !== 'undefined') {
    return [];
  }

  try {
    const personDir = path.join(contentDir, personId);
    if (!fs.existsSync(personDir)) {
      return [];
    }

    return fs.readdirSync(personDir)
      .filter(file => file.endsWith('.md'))
      .sort();
  } catch (error) {
    console.error(`Error listing files for ${personId}:`, error);
    return [];
  }
}

/**
 * 列出备份文件
 */
export function listBackups(personId: string): string[] {
  if (typeof window !== 'undefined') {
    return [];
  }

  try {
    const personBackupDir = path.join(backupDir, personId);
    if (!fs.existsSync(personBackupDir)) {
      return [];
    }

    return fs.readdirSync(personBackupDir)
      .filter(file => file.endsWith('.md'))
      .sort()
      .reverse(); // 最新的在前
  } catch (error) {
    console.error(`Error listing backups for ${personId}:`, error);
    return [];
  }
}

