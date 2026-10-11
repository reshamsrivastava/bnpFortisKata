import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { CartProvider } from '../../context/CartContext';
import { server } from '../../mocks/server';
import { CartLauncher } from './CartLauncher';

describe('CartLauncher', () => {
  it('opens the shopping cart dialog from the cart count trigger', async () => {
    server.use(
      http.get('http://localhost:8080/api/cart', () =>
        HttpResponse.json({
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
        }),
      ),
    );
    const user = userEvent.setup();
    render(
      <CartProvider>
        <CartLauncher />
      </CartProvider>,
    );

    const trigger = await screen.findByRole('button', {
      name: /open shopping cart, 2 items/i,
    });
    expect(trigger).toHaveTextContent('Cart (2)');
    await user.click(trigger);

    expect(await screen.findByRole('dialog')).toBeInTheDocument();
    expect(screen.getByText('Clean Code')).toBeInTheDocument();
  });
});
