import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';
import { CartProvider } from '../../context/CartContext';
import { server } from '../../mocks/server';
import { CartView } from './CartView';

const cartUrl = 'http://localhost:8080/api/cart';

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

    expect(await screen.findByText('2')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /increase clean code quantity/i }));
    expect(await screen.findByText('3')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /decrease clean code quantity/i }));
    await waitFor(() => expect(screen.getByText('2')).toBeInTheDocument());

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
});
