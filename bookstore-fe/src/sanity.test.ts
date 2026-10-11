import { describe, it, expect } from 'vitest';

describe('Sanity Check', () => {
  it('runs tests and verifies MSW integration', async () => {
    const res = await fetch('/api/books');
    const data = await res.json();
    expect(res.status).toBe(200);
    expect(data).toHaveLength(2);
    expect(data[0].title).toBe('Clean Code');
  });
});