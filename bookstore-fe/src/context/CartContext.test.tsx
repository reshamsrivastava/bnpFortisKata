import { act, renderHook, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { server } from '../mocks/server';
import { useCart } from '../hooks/useCart';
import type { Book } from '../types/bookstore';
import { CartProvider } from './CartContext';

const apiUrl = 'http://localhost:8080/api/cart';
const book: Book = {
  id: '1',
  title: 'Clean Code',
  author: 'Robert C. Martin',
  price: 34.99,
  stock: 10,
};

function responseItem(quantity: number, id = 10) {
  return {
    id,
    bookId: Number(book.id),
    title: book.title,
    quantity,
    unitPrice: book.price,
    subtotal: book.price * quantity,
  };
}

function wrapper({ children }: { children: React.ReactNode }) {
  return <CartProvider>{children}</CartProvider>;
}

describe('CartContext', () => {
  it('loads an empty cart from the API', async () => {
    server.use(
      http.get(apiUrl, () =>
        HttpResponse.json({ id: 1, items: [], total: 0 }),
      ),
    );

    const { result } = renderHook(() => useCart(), { wrapper });

    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.items).toEqual([]);
    expect(result.current.totalItems).toBe(0);
    expect(result.current.totalPrice).toBe(0);
  });

  it('shows a recoverable error when the cart cannot be loaded', async () => {
    server.use(
      http.get(apiUrl, () =>
        HttpResponse.json({ message: 'Unavailable' }, { status: 503 }),
      ),
    );

    const { result } = renderHook(() => useCart(), { wrapper });

    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.items).toEqual([]);
    expect(result.current.error).toMatch(/load your cart/i);
  });

  it('adds a book with an initial quantity of one', async () => {
    server.use(
      http.get(apiUrl, () =>
        HttpResponse.json({ id: 1, items: [], total: 0 }),
      ),
      http.post(`${apiUrl}/items`, async ({ request }) => {
        expect(await request.json()).toEqual({ bookId: 1, quantity: 1 });
        return HttpResponse.json(
          { id: 1, items: [responseItem(1)], total: book.price },
          { status: 201 },
        );
      }),
    );

    const { result } = renderHook(() => useCart(), { wrapper });
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    await act(() => result.current.addToCart(book));

    expect(result.current.items).toHaveLength(1);
    expect(result.current.items[0]).toMatchObject({
      book: { id: book.id, title: book.title, price: book.price },
      quantity: 1,
    });
  });

  it('updates cart state optimistically while the server mutation is pending', async () => {
    let requestStarted!: () => void;
    let finishRequest!: () => void;
    const started = new Promise<void>((resolve) => {
      requestStarted = resolve;
    });
    const requestCanFinish = new Promise<void>((resolve) => {
      finishRequest = resolve;
    });
    server.use(
      http.get(apiUrl, () =>
        HttpResponse.json({ id: 1, items: [], total: 0 }),
      ),
      http.post(`${apiUrl}/items`, async () => {
        requestStarted();
        await requestCanFinish;
        return HttpResponse.json(
          { id: 1, items: [responseItem(1)], total: book.price },
          { status: 201 },
        );
      }),
    );

    const { result } = renderHook(() => useCart(), { wrapper });
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    let mutation!: Promise<void>;
    act(() => {
      mutation = result.current.addToCart(book);
    });
    await started;
    expect(result.current.totalItems).toBe(1);
    expect(result.current.items[0].quantity).toBe(1);

    finishRequest();
    await act(() => mutation);
  });

  it('increments quantity when the same book is added again', async () => {
    server.use(
      http.get(apiUrl, () =>
        HttpResponse.json({
          id: 1,
          items: [responseItem(1)],
          total: book.price,
        }),
      ),
      http.post(`${apiUrl}/items`, () =>
        HttpResponse.json(
          { id: 1, items: [responseItem(2)], total: book.price * 2 },
          { status: 201 },
        ),
      ),
    );

    const { result } = renderHook(() => useCart(), { wrapper });
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    await act(() => result.current.addToCart(book));

    expect(result.current.items).toHaveLength(1);
    expect(result.current.items[0].quantity).toBe(2);
  });

  it('updates quantity and removes an item when quantity reaches zero', async () => {
    server.use(
      http.get(apiUrl, () =>
        HttpResponse.json({
          id: 1,
          items: [responseItem(2)],
          total: book.price * 2,
        }),
      ),
      http.put(`${apiUrl}/items/10`, async ({ request }) => {
        expect(await request.json()).toEqual({ quantity: 3 });
        return HttpResponse.json({
          id: 1,
          items: [responseItem(3)],
          total: book.price * 3,
        });
      }),
      http.delete(`${apiUrl}/items/10`, () =>
        HttpResponse.json({ id: 1, items: [], total: 0 }),
      ),
    );

    const { result } = renderHook(() => useCart(), { wrapper });
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    await act(() => result.current.updateQuantity(book.id, 3));
    expect(result.current.items[0].quantity).toBe(3);

    await act(() => result.current.updateQuantity(book.id, 0));
    expect(result.current.items).toEqual([]);
  });

  it('deletes an item by book identifier', async () => {
    server.use(
      http.get(apiUrl, () =>
        HttpResponse.json({
          id: 1,
          items: [responseItem(2)],
          total: book.price * 2,
        }),
      ),
      http.delete(`${apiUrl}/items/10`, () =>
        HttpResponse.json({ id: 1, items: [], total: 0 }),
      ),
    );

    const { result } = renderHook(() => useCart(), { wrapper });
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    await act(() => result.current.removeFromCart(book.id));

    expect(result.current.items).toEqual([]);
  });

  it('calculates aggregate quantity and price from all cart lines', async () => {
    const secondBookItem = {
      id: 11,
      bookId: 2,
      title: 'TDD by Example',
      quantity: 2,
      unitPrice: 20,
      subtotal: 40,
    };
    server.use(
      http.get(apiUrl, () =>
        HttpResponse.json({
          id: 1,
          items: [responseItem(2), secondBookItem],
          total: book.price * 2 + 40,
        }),
      ),
    );

    const { result } = renderHook(() => useCart(), { wrapper });

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(result.current.totalItems).toBe(4);
    expect(result.current.totalPrice).toBeCloseTo(book.price * 2 + 40);
  });

  it('keeps existing cart state and exposes an error when a mutation fails', async () => {
    server.use(
      http.get(apiUrl, () =>
        HttpResponse.json({
          id: 1,
          items: [responseItem(1)],
          total: book.price,
        }),
      ),
      http.post(`${apiUrl}/items`, () =>
        HttpResponse.json({ message: 'Unavailable' }, { status: 503 }),
      ),
    );

    const { result } = renderHook(() => useCart(), { wrapper });
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    await act(() => result.current.addToCart(book));

    expect(result.current.items[0].quantity).toBe(1);
    expect(result.current.error).toMatch(/cart/i);
  });
});
