import { describe, expect, it, vi } from 'vitest';
import { ApiError, apiFetch } from './api';

describe('apiFetch', () => {
  it('returns successful responses', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(new Response('{"ok":true}', { status: 200 })),
    );

    const response = await apiFetch('/api/books');

    expect(await response.json()).toEqual({ ok: true });
    expect(fetch).toHaveBeenCalledWith('/api/books', {
      credentials: 'include',
    });
  });

  it('raises a typed error and globally reports server error details', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response('{"message":"Service unavailable"}', { status: 503 }),
      ),
    );
    const errorEvent = vi.fn();
    const unauthorizedEvent = vi.fn();
    window.addEventListener('bookstore:api-error', errorEvent);
    window.addEventListener('bookstore:unauthorized', unauthorizedEvent);

    await expect(apiFetch('/api/books')).rejects.toMatchObject({
      name: 'ApiError',
      status: 503,
      message: 'Service unavailable',
    } satisfies Partial<ApiError>);

    expect(errorEvent).toHaveBeenCalledOnce();
    expect(unauthorizedEvent).not.toHaveBeenCalled();
    window.removeEventListener('bookstore:api-error', errorEvent);
    window.removeEventListener('bookstore:unauthorized', unauthorizedEvent);
  });

  it('notifies the global error handler and expires the session on 401', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(new Response(null, { status: 401 })),
    );
    const unauthorizedEvent = vi.fn();
    window.addEventListener('bookstore:unauthorized', unauthorizedEvent);

    await expect(apiFetch('/api/cart')).rejects.toMatchObject({
      status: 401,
      message: 'Your session has expired. Please sign in again.',
    });

    expect(unauthorizedEvent).toHaveBeenCalledOnce();
    window.removeEventListener('bookstore:unauthorized', unauthorizedEvent);
  });

  it('preserves login validation errors without expiring the current session', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response('{"message":"Invalid username or password"}', {
          status: 401,
        }),
      ),
    );
    const unauthorizedEvent = vi.fn();
    window.addEventListener('bookstore:unauthorized', unauthorizedEvent);

    await expect(
      apiFetch('http://localhost:8080/api/users/login'),
    ).rejects.toMatchObject({
      status: 401,
      message: 'Invalid username or password',
    });
    expect(unauthorizedEvent).not.toHaveBeenCalled();

    window.removeEventListener('bookstore:unauthorized', unauthorizedEvent);
  });

  it('normalizes network failures into globally reported API errors', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('offline')));
    const errorEvent = vi.fn();
    window.addEventListener('bookstore:api-error', errorEvent);

    await expect(apiFetch('/api/books')).rejects.toMatchObject({
      status: null,
      message: 'Unable to reach the bookstore. Check your connection and try again.',
    });

    expect(errorEvent).toHaveBeenCalledOnce();
    window.removeEventListener('bookstore:api-error', errorEvent);
  });
});
