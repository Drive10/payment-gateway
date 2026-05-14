import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from "react";

interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
}

interface AuthState {
  token: string;
  refreshToken: string;
  user: User;
  expiresAt: number;
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, firstName: string, lastName: string) => Promise<void>;
  logout: () => void;
  isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextType | null>(null);

const STORAGE_KEY = "payflow-merchant-auth";

function persistAuth(state: AuthState) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

function getStoredAuth(): AuthState | null {
  try {
    const value = localStorage.getItem(STORAGE_KEY);
    return value ? JSON.parse(value) : null;
  } catch {
    return null;
  }
}

function clearStoredAuth() {
  localStorage.removeItem(STORAGE_KEY);
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState<AuthState | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const stored = getStoredAuth();
    if (stored && stored.expiresAt > Date.now()) {
      setAuth(stored);
    } else {
      clearStoredAuth();
    }
    setLoading(false);
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const res = await fetch("/api/v1/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password }),
    });
    const data = await res.json();
    if (!res.ok || !data.success) {
      throw new Error(data?.error?.message || data?.message || "Login failed");
    }
    const d = data.data;
    const state: AuthState = {
      token: d.accessToken,
      refreshToken: d.refreshToken || "",
      user: {
        id: d.user?.id || d.id || "",
        email: d.user?.email || email,
        firstName: d.user?.firstName || d.firstName || "",
        lastName: d.user?.lastName || d.lastName || "",
        role: d.user?.role || d.role || "MERCHANT",
      },
      expiresAt: Date.now() + (d.expiresIn || 3600) * 1000,
    };
    persistAuth(state);
    setAuth(state);
  }, []);

  const register = useCallback(async (email: string, password: string, firstName: string, lastName: string) => {
    const res = await fetch("/api/v1/auth/register", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password, firstName, lastName }),
    });
    if (!res.ok) {
      const data = await res.json().catch(() => ({}));
      throw new Error(data?.error?.message || data?.message || "Registration failed");
    }
    await login(email, password);
  }, [login]);

  const logout = useCallback(() => {
    clearStoredAuth();
    setAuth(null);
  }, []);

  return (
    <AuthContext.Provider
      value={{
        user: auth?.user || null,
        token: auth?.token || null,
        loading,
        login,
        register,
        logout,
        isAuthenticated: !!auth,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
