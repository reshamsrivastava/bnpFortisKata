import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { GlobalErrorProvider } from './GlobalErrorContext';

describe('GlobalErrorProvider', () => {
  it('shows and dismisses errors reported by any API client', async () => {
    const user = userEvent.setup();
    render(
      <GlobalErrorProvider>
        <p>Application</p>
      </GlobalErrorProvider>,
    );

    window.dispatchEvent(
      new CustomEvent('bookstore:api-error', {
        detail: { message: 'The bookstore is unavailable.' },
      }),
    );
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'The bookstore is unavailable.',
    );

    await user.click(screen.getByRole('button', { name: /dismiss error/i }));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});
