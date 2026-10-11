import { act, render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { AuthProvider, useAuth } from './AuthContext';

function AuthenticationState() {
  const { user, isAuthenticated } = useAuth();
  return (
    <div>
      <span>{user?.username ?? 'signed out'}</span>
      <span>{String(isAuthenticated)}</span>
    </div>
  );
}

describe('AuthProvider global session handling', () => {
  it('clears the current session when the API reports an expired session', () => {
    localStorage.setItem(
      'auth_user',
      JSON.stringify({ username: 'reader', token: 'session-token' }),
    );
    render(
      <AuthProvider>
        <AuthenticationState />
      </AuthProvider>,
    );

    act(() => {
      window.dispatchEvent(new Event('bookstore:unauthorized'));
    });

    expect(screen.getByText('signed out')).toBeInTheDocument();
    expect(screen.getByText('false')).toBeInTheDocument();
    expect(localStorage.getItem('auth_user')).toBeNull();
  });
});
