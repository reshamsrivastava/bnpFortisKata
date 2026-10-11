// src/components/BookList/BookList.test.tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import { http, HttpResponse } from 'msw';
import { Theme } from '@radix-ui/themes';
import { server } from '../../mocks/server';
import { BookList } from './BookList';

const renderWithTheme = (ui: React.ReactElement) => {
  return render(<Theme>{ui}</Theme>);
};

describe('BookList Component', () => {
  it('displays loading state initially', () => {
    renderWithTheme(<BookList onAddToCart={vi.fn()} />);
    expect(screen.getByRole('status')).toHaveTextContent(/loading books/i);
  });

  it('renders a list of books fetched from the API', async () => {
    const handleAddToCart = vi.fn();
    renderWithTheme(<BookList onAddToCart={handleAddToCart} />);

    const cleanCodeTitle = await screen.findByRole('heading', {
      name: /clean code/i,
      level: 3,
    });
    expect(cleanCodeTitle).toBeInTheDocument();
    expect(screen.getByText(/by robert c. martin/i)).toBeInTheDocument();
    expect(screen.getByText(/\$34.99/i)).toBeInTheDocument();

    const addToCartButtons = screen.getAllByRole('button', {
      name: /add to cart/i,
    });
    expect(addToCartButtons).toHaveLength(3);

    await userEvent.click(addToCartButtons[0]);
    expect(handleAddToCart).toHaveBeenCalledTimes(1);
    expect(handleAddToCart).toHaveBeenCalledWith(
      expect.objectContaining({
        id: '1',
        title: 'Clean Code',
        price: 34.99,
      })
    );
  });

  it('renders an error message when the API request fails', async () => {
    server.use(
      http.get('/api/books', () => {
        return new HttpResponse(null, { status: 500 });
      })
    );

    renderWithTheme(<BookList onAddToCart={vi.fn()} />);

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent(/failed to load books/i);
  });
});