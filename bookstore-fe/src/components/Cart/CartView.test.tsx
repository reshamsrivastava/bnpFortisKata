import { act, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';
import { CartProvider } from '../../context/CartContext';
import { server } from '../../mocks/server';
import { CartView } from './CartView';
import { useCart } from '../../hooks/useCart';

const cartUrl = 'http://localhost:8080/api/cart';
const checkoutUrl = 'http://localhost:8080/api/orders/checkout';

const cartWithBook = {
  id: 1,
  items: [
    {
      id: 10,
      bookId: 1,
      title: 'Clean Code',
      quantity: 2,
      unitPrice: 34.99,
      subtotal: 69.98,
    },
  ],
  total: 69.98,
};

function renderCart(open = true) {
  const onOpenChange = vi.fn();
  render(
    <CartProvider>
      <CartStateProbe />
      <CartView open={open} onOpenChange={onOpenChange} />
    </CartProvider>,
  );
  return { onOpenChange };
}

describe('CartView', () => {
  it('renders an empty cart message when there are no items', async () => {
    server.use(
      http.get(cartUrl, () =>
        HttpResponse.json({ id: 1, items: [], total: 0 }),
      ),
    );

    renderCart();

    expect(
      await screen.findByText(/your cart is empty/i),
    ).toBeInTheDocument();
  });

  it('renders cart line items, prices, and subtotals in a table', async () => {
    server.use(http.get(cartUrl, () => HttpResponse.json(cartWithBook)));

    renderCart();

    const table = await screen.findByRole('table', {
      name: /items in your cart/i,
    });
    expect(within(table).getByText('Clean Code')).toBeInTheDocument();
    expect(within(table).getByText('$34.99')).toBeInTheDocument();
    expect(within(table).getByText('$69.98')).toBeInTheDocument();
    expect(screen.getByText(/subtotal:/i, { selector: 'span' })).toBeInTheDocument();
  });

  it('increases, decreases, and deletes cart items through their controls', async () => {
    let quantity = 2;
    server.use(
      http.get(cartUrl, () =>
        HttpResponse.json({
          ...cartWithBook,
          items: [{ ...cartWithBook.items[0], quantity, subtotal: 69.98 }],
        }),
      ),
      http.put(`${cartUrl}/items/10`, async ({ request }) => {
        const body = (await request.json()) as { quantity: number };
        quantity = body.quantity;
        return HttpResponse.json({
          ...cartWithBook,
          items: [
            {
              ...cartWithBook.items[0],
              quantity,
              subtotal: 34.99 * quantity,
            },
          ],
          total: 34.99 * quantity,
        });
      }),
      http.delete(`${cartUrl}/items/10`, () => {
        quantity = 0;
        return HttpResponse.json({ id: 1, items: [], total: 0 });
      }),
    );
    const user = userEvent.setup();
    renderCart();

    expect(await screen.findByLabelText('Clean Code quantity')).toHaveTextContent('2');
    await user.click(screen.getByRole('button', { name: /increase clean code quantity/i }));
    expect(await screen.findByLabelText('Clean Code quantity')).toHaveTextContent('3');

    await user.click(screen.getByRole('button', { name: /decrease clean code quantity/i }));
    await waitFor(() =>
      expect(screen.getByLabelText('Clean Code quantity')).toHaveTextContent('2'),
    );

    await user.click(screen.getByRole('button', { name: /remove clean code/i }));
    await user.click(screen.getByRole('button', { name: /^remove$/i }));
    expect(await screen.findByText(/your cart is empty/i)).toBeInTheDocument();
    expect(quantity).toBe(0);
  });

  it('closes the dialog when the close action is selected', async () => {
    server.use(http.get(cartUrl, () => HttpResponse.json(cartWithBook)));
    const user = userEvent.setup();
    const { onOpenChange } = renderCart();

    await user.click(await screen.findByRole('button', { name: /close cart/i }));

    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  it('shows a loading state while the cart request is pending', async () => {
    let finishRequest!: () => void;
    const requestCanFinish = new Promise<void>((resolve) => {
      finishRequest = resolve;
    });
    server.use(
      http.get(cartUrl, async () => {
        await requestCanFinish;
        return HttpResponse.json({ id: 1, items: [], total: 0 });
      }),
    );

    renderCart();

    expect(screen.getByRole('status', { name: /loading cart/i })).toHaveTextContent(
      /loading your cart/i,
    );
    finishRequest();
    expect(await screen.findByText(/your cart is empty/i)).toBeInTheDocument();
  });

  it('shows cart request errors accessibly', async () => {
    server.use(
      http.get(cartUrl, () =>
        HttpResponse.json({ message: 'Unavailable' }, { status: 503 }),
      ),
    );

    renderCart();

    expect(await screen.findByRole('alert')).toHaveTextContent(/unable to load your cart/i);
  });

  it('shows the order summary immediately and sends checkout with a UUID-v4 idempotency key', async () => {
    let finishCheckout!: () => void;
    const checkoutCanFinish = new Promise<void>((resolve) => {
      finishCheckout = resolve;
    });
    let sentKey: string | null = null;
    server.use(
      http.get(cartUrl, () => HttpResponse.json(cartWithBook)),
      http.post(checkoutUrl, async ({ request }) => {
        sentKey = request.headers.get('Idempotency-Key');
        await checkoutCanFinish;
        return HttpResponse.json({
          id: 42,
          status: 'COMPLETED',
          items: [
            {
              id: 100,
              bookId: 1,
              title: 'Clean Code',
              author: 'Robert C. Martin',
              isbn: '9780132350884',
              quantity: 2,
              unitPrice: 34.99,
              subtotal: 69.98,
            },
          ],
          total: 69.98,
          createdAt: '2026-10-11T10:00:00Z',
        }, { status: 201 });
      }),
    );
    const user = userEvent.setup();
    renderCart();
    await screen.findByText('Clean Code');

    await user.click(screen.getByRole('button', { name: /checkout/i }));

    expect(await screen.findByRole('heading', { name: /order summary/i })).toBeInTheDocument();
    expect(screen.getByText('Clean Code')).toBeInTheDocument();
    expect(
      within(screen.getByRole('table', { name: /items in your order/i })).getByRole(
        'cell',
        { name: '2' },
      ),
    ).toBeInTheDocument();
    expect(screen.getByText(/total:\s*\$69\.98/i)).toBeInTheDocument();
    await waitFor(() => expect(sentKey).not.toBeNull());
    expect(sentKey).toMatch(
      /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i,
    );

    await act(async () => finishCheckout());
    expect(await screen.findByRole('heading', { name: /order confirmed/i })).toBeInTheDocument();
    expect(screen.getByText(/order #42/i)).toBeInTheDocument();
    expect(screen.getByLabelText('Cart item count')).toHaveTextContent('0');
  });

  it('preserves the cart and reports a recoverable error when checkout fails', async () => {
    const idempotencyKeys: string[] = [];
    let attempt = 0;
    server.use(
      http.get(cartUrl, () => HttpResponse.json(cartWithBook)),
      http.post(checkoutUrl, ({ request }) => {
        idempotencyKeys.push(request.headers.get('Idempotency-Key') ?? '');
        attempt += 1;
        return attempt === 1
          ? HttpResponse.json({ message: 'Out of stock' }, { status: 409 })
          : HttpResponse.json(
              {
                id: 43,
                status: 'COMPLETED',
                items: [],
                total: 69.98,
                createdAt: '2026-10-11T10:00:00Z',
              },
              { status: 201 },
            );
      }),
    );
    const user = userEvent.setup();
    renderCart();
    await screen.findByText('Clean Code');

    await user.click(screen.getByRole('button', { name: /checkout/i }));

    expect(
      await within(screen.getByRole('dialog')).findByRole('alert'),
    ).toHaveTextContent(/checkout failed/i);
    expect(screen.getByLabelText('Cart item count')).toHaveTextContent('2');
    expect(screen.getByRole('heading', { name: /order summary/i })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /retry checkout/i }));
    expect(await screen.findByRole('heading', { name: /order confirmed/i })).toBeInTheDocument();
    expect(idempotencyKeys).toHaveLength(2);
    expect(idempotencyKeys[0]).toMatch(
      /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i,
    );
    expect(idempotencyKeys[1]).toBe(idempotencyKeys[0]);
  });
});

function CartStateProbe() {
  const { totalItems } = useCart();
  return <output aria-label="Cart item count">{totalItems}</output>;
}
