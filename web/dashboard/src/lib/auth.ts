import { getToken, setToken, removeToken } from './cookies';
import { JWT_TOKEN_KEY } from './constants';

export const isAuthenticated = (): boolean => {
  return !!getToken(JWT_TOKEN_KEY);
};

export const login = (token: string) => {
  setToken(JWT_TOKEN_KEY, token);
};

export const logout = () => {
  removeToken(JWT_TOKEN_KEY);
};

export const getAuthToken = (): string | null => {
  return getToken(JWT_TOKEN_KEY);
};
