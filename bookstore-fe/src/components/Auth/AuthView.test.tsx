import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import { Theme } from '@radix-ui/themes';
import { AuthView } from './AuthView';

const renderWithTheme = (ui: React.ReactElement) => {
  return render(<Theme>{ui}</Theme>);
};

describe('AuthView Component', () => {
  it('renders login form by default and allows switching to register', async () => {
    const onAuthSuccess = vi.fn();
    renderWithTheme(<AuthView onAuthSuccess={onAuthSuccess} />);

    expect(screen.getByRole('heading', { name: /login/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/username/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
    expect(screen.queryByLabelText(/email/i)).not.toBeInTheDocument();

    // Toggle to Register mode
    const toggleButton = screen.getByRole('button', { name: /need an account\? register/i });
    await userEvent.click(toggleButton);

    expect(screen.getByRole('heading', { name: /register/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
  });

  it('submits login payload and calls onAuthSuccess', async () => {
    const onAuthSuccess = vi.fn();
    renderWithTheme(<AuthView onAuthSuccess={onAuthSuccess} />);

    await userEvent.type(screen.getByLabelText(/username/i), 'john_doe');
    await userEvent.type(screen.getByLabelText(/password/i), 'secret123');

    const submitBtn = screen.getByRole('button', { name: /^login$/i });
    await userEvent.click(submitBtn);

    await waitFor(() => {
      expect(onAuthSuccess).toHaveBeenCalledWith(
        expect.objectContaining({
          username: 'john_doe',
          token: 'mock-jwt-token-12345',
        })
      );
    });
  });

  it('submits registration payload and handles successful sign up', async () => {
    const onAuthSuccess = vi.fn();
    renderWithTheme(<AuthView onAuthSuccess={onAuthSuccess} />);

    // Switch to register
    await userEvent.click(screen.getByRole('button', { name: /need an account\? register/i }));

    await userEvent.type(screen.getByLabelText(/username/i), 'new_user');
    await userEvent.type(screen.getByLabelText(/email/i), 'new@example.com');
    await userEvent.type(screen.getByLabelText(/password/i), 'password123');

    await userEvent.click(screen.getByRole('button', { name: /^register$/i }));

    await waitFor(() => {
      expect(onAuthSuccess).toHaveBeenCalledWith(
        expect.objectContaining({
          username: 'new_user',
          email: 'new@example.com',
          token: 'mock-jwt-token-67890',
        })
      );
    });
  });

  it('displays error message when login fails', async () => {
    renderWithTheme(<AuthView onAuthSuccess={vi.fn()} />);

    await userEvent.type(screen.getByLabelText(/username/i), 'john_doe');
    await userEvent.type(screen.getByLabelText(/password/i), 'wrongpassword');

    await userEvent.click(screen.getByRole('button', { name: /^login$/i }));

    const errorAlert = await screen.findByRole('alert');
    expect(errorAlert).toHaveTextContent(/invalid username or password/i);
  });
});