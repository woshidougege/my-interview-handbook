import { NextRequest, NextResponse } from 'next/server';
import { getPersonContentPath } from '@/lib/people';
import { parseMarkdown, saveMarkdown } from '@/lib/markdown';
import { saveFile, createBackup } from '@/lib/fileSystem';

// GET: 读取内容
export async function GET(request: NextRequest) {
  try {
    const searchParams = request.nextUrl.searchParams;
    const person = searchParams.get('person');
    const file = searchParams.get('file');

    if (!person || !file) {
      return NextResponse.json(
        { error: 'Missing person or file parameter' },
        { status: 400 }
      );
    }

    const filePath = getPersonContentPath(person, file);
    const content = parseMarkdown(filePath);

    if (!content) {
      return NextResponse.json(
        { error: 'File not found' },
        { status: 404 }
      );
    }

    return NextResponse.json(content);
  } catch (error) {
    console.error('Error reading content:', error);
    return NextResponse.json(
      { error: 'Failed to read content' },
      { status: 500 }
    );
  }
}

// POST: 保存内容（仅开发环境）
export async function POST(request: NextRequest) {
  // 仅在开发环境可用
  if (process.env.NODE_ENV === 'production') {
    return NextResponse.json(
      { error: 'Editing is only available in development mode' },
      { status: 403 }
    );
  }

  try {
    const body = await request.json();
    const { person, file, frontmatter, content } = body;

    if (!person || !file || !content) {
      return NextResponse.json(
        { error: 'Missing required fields' },
        { status: 400 }
      );
    }

    // 保存文件
    const filePath = getPersonContentPath(person, file);
    const saved = saveMarkdown(filePath, frontmatter || {}, content);

    if (!saved) {
      return NextResponse.json(
        { error: 'Failed to save file' },
        { status: 500 }
      );
    }

    // 创建备份
    const backup = createBackup(person, file, content);

    return NextResponse.json({
      success: true,
      backupPath: backup.backupPath,
    });
  } catch (error) {
    console.error('Error saving content:', error);
    return NextResponse.json(
      { error: 'Failed to save content' },
      { status: 500 }
    );
  }
}

