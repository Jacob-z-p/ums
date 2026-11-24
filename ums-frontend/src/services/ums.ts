import { request } from '@umijs/max';

export const accountPattern = /^[A-Za-z0-9]{1,64}$/;

export type Credentials = { account: string; password: string };

export type SessionUser = {
  id: number;
  account: string;
  nickname: string;
  avatarUrl: string | null;
  role: 0 | 1;
};

export type ManagedUser = SessionUser & {
  status: 0 | 1;
  createdAt: string;
  updatedAt: string;
};

type UserQuery = { account?: string; page: number; size: number };

type UserPage = {
  items: ManagedUser[];
  total: number;
  page: number;
  size: number;
};

const options = { skipErrorHandler: true };

export const register = (data: Credentials) =>
  request<{ id: number }>('/api/user/register', {
    ...options,
    method: 'POST',
    data,
  });

export const login = (data: Credentials) =>
  request<SessionUser>('/api/user/login', { ...options, method: 'POST', data });

export const currentUser = () =>
  request<SessionUser>('/api/user/current', options);

export const logout = () =>
  request<void>('/api/user/logout', { ...options, method: 'POST' });

export const listUsers = (params: UserQuery, signal: AbortSignal) =>
  request<UserPage>('/api/admin/users', { ...options, params, signal });

export const deleteUser = (id: number) =>
  request<void>(`/api/admin/users/${id}`, { ...options, method: 'DELETE' });

type ApiError = {
  response?: { status?: number; data?: { message?: unknown } };
};

export function apiStatus(error: unknown) {
  return (error as ApiError | null)?.response?.status;
}

export function apiMessage(error: unknown, fallback: string) {
  const message = (error as ApiError | null)?.response?.data?.message;
  return typeof message === 'string' && message ? message : fallback;
}
