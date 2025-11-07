import { NextResponse } from 'next/server';
import { getAllPeople } from '@/lib/people';

export async function GET() {
  try {
    const people = getAllPeople();
    return NextResponse.json(people);
  } catch (error) {
    console.error('Error getting people:', error);
    return NextResponse.json({ error: 'Failed to load people' }, { status: 500 });
  }
}

