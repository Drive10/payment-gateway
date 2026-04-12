import { CookieOptions } from 'next/dist/compiled/@edge-runtime/cookies';

export const COOKIE_OPTIONS: CookieOptions = {
  httpOnly: true,
  secure: process.env.NODE_ENV === 'production',
  sameSite: 'lax',
  path: '/',
};

export const getToken = (key: string): string | null => {
  if (typeof document === 'undefined') return null;
  const match = document.cookie.match(new RegExp('(^| )' + key + '=([^;]+)'));
  return match ? decodeURIComponent(match[2]) : null;
};

export const setToken = (key: string, value: string, options?: CookieOptions) => {
  if (typeof document === 'undefined') return;
  const cookieOptions = { ...COOKIE_OPTIONS, ...options };
  document.cookie = `${key}=${encodeURIComponent(value)}; ${Object.entries(cookieOptions)
    .map(([k, v]) => `${k}=${v}`)
    .join('; ')}`;
};

export const removeToken = (key: string, options?: CookieOptions) => {
  if (typeof document === 'undefined') return;
  const cookieOptions = { ...COOKIE_OPTIONS, ...options };
  document.cookie = `${key}=; Max-Age=0; ${Object.entries(cookieOptions)
    .map(([k, v]) => `${k}=${v}`)
    .join('; ')}`;
};
