import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, act, cleanup } from '@testing-library/react';
import { AuthProvider, useAuth } from '../contexts/AuthContext';

const mockFetch = vi.fn();
global.fetch = mockFetch;

beforeEach(() => {
  mockFetch.mockReset();
  localStorage.clear();
});

afterEach(() => {
  cleanup();
});

function TestConsumer() {
  const auth = useAuth();
  return (
    <div>
      <span data-testid="status">{auth.isAuthenticated ? 'logged-in' : 'logged-out'}</span>
      <span data-testid="loading">{auth.loading ? 'loading' : 'ready'}</span>
      <span data-testid="user">{auth.user?.email || 'none'}</span>
      <button data-testid="login-btn" onClick={() => auth.login('test@example.com', 'pass')}>
        Login
      </button>
      <button data-testid="logout-btn" onClick={() => auth.logout()}>
        Logout
      </button>
      <button
        data-testid="register-btn"
        onClick={() => auth.register('new@example.com', 'pass', 'New', 'User')}
      >
        Register
      </button>
    </div>
  );
}

function renderWithProvider() {
  return render(
    <AuthProvider>
      <TestConsumer />
    </AuthProvider>
  );
}

describe('AuthContext', () => {
  it('starts unauthenticated', () => {
    renderWithProvider();
    expect(screen.getByTestId('status').textContent).toBe('logged-out');
  });

  it('shows ready after loading', () => {
    renderWithProvider();
    expect(screen.getByTestId('loading').textContent).toBe('ready');
  });

  it('restores auth from localStorage', () => {
    const authData = {
      token: 'saved-token',
      refreshToken: 'saved-refresh',
      user: { id: '1', email: 'saved@example.com', firstName: 'Saved', lastName: 'User', role: 'MERCHANT' },
      expiresAt: Date.now() + 3600000,
    };
    localStorage.setItem('payflow-merchant-auth', JSON.stringify(authData));

    renderWithProvider();
    expect(screen.getByTestId('status').textContent).toBe('logged-in');
    expect(screen.getByTestId('user').textContent).toBe('saved@example.com');
  });

  it('ignores expired auth in localStorage', () => {
    const expiredAuth = {
      token: 'expired-token',
      refreshToken: '',
      user: { id: '1', email: 'expired@example.com', firstName: '', lastName: '', role: 'MERCHANT' },
      expiresAt: Date.now() - 1000,
    };
    localStorage.setItem('payflow-merchant-auth', JSON.stringify(expiredAuth));

    renderWithProvider();
    expect(screen.getByTestId('status').textContent).toBe('logged-out');
  });

  it('logs in successfully', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () =>
        Promise.resolve({
          success: true,
          data: {
            accessToken: 'test-token',
            refreshToken: 'refresh-token',
            expiresIn: 3600,
            user: {
              id: '1',
              email: 'test@example.com',
              firstName: 'Test',
              lastName: 'User',
              role: 'MERCHANT',
            },
          },
        }),
    });

    renderWithProvider();

    await act(async () => {
      screen.getByTestId('login-btn').click();
    });

    expect(screen.getByTestId('status').textContent).toBe('logged-in');
    expect(screen.getByTestId('user').textContent).toBe('test@example.com');
  });

  it('handles login failure', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 401,
      json: () => Promise.resolve({ success: false, message: 'Invalid credentials' }),
    });

    renderWithProvider();

    await act(async () => {
      screen.getByTestId('login-btn').click();
    });

    expect(screen.getByTestId('status').textContent).toBe('logged-out');
  });

  it('logs out', () => {
    const authData = {
      token: 'test-token',
      refreshToken: '',
      user: { id: '1', email: 'test@example.com', firstName: 'Test', lastName: 'User', role: 'MERCHANT' },
      expiresAt: Date.now() + 3600000,
    };
    localStorage.setItem('payflow-merchant-auth', JSON.stringify(authData));

    renderWithProvider();
    expect(screen.getByTestId('status').textContent).toBe('logged-in');

    act(() => {
      screen.getByTestId('logout-btn').click();
    });

    expect(screen.getByTestId('status').textContent).toBe('logged-out');
    expect(localStorage.getItem('payflow-merchant-auth')).toBeNull();
  });

  it('throws when useAuth is used outside provider', () => {
    expect(() => render(<TestConsumer />)).toThrow('useAuth must be used within AuthProvider');
  });
});
