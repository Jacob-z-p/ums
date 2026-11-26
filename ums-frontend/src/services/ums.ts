import { request } from '@umijs/max';
import { appBasePath } from '@/utils/appBasePath';

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

type Result<T> = {
  code: number;
  data: T;
  message: string;
  description: string;
};

const options = { skipErrorHandler: true };
const apiPath = `${appBasePath}/api`;

function unwrap<T>({ data }: Result<T>): T {
  return data;
}

export const register = (data: Credentials) =>
  request<Result<{ id: number }>>(`${apiPath}/user/register`, {
    ...options,
    method: 'POST',
    data,
  }).then(unwrap);

export const login = (data: Credentials) =>
  request<Result<SessionUser>>(`${apiPath}/user/login`, {
    ...options,
    method: 'POST',
    data,
  }).then(unwrap);

export const currentUser = () =>
  request<Result<SessionUser>>(`${apiPath}/user/current`, options).then(unwrap);

export const logout = async () => {
  await request<Result<null>>(`${apiPath}/user/logout`, {
    ...options,
    method: 'POST',
  });
};

export const listUsers = (params: UserQuery, signal: AbortSignal) =>
  request<Result<UserPage>>(`${apiPath}/admin/users`, {
    ...options,
    params,
    signal,
  }).then(unwrap);

export const deleteUser = async (id: number) => {
  await request<Result<null>>(`${apiPath}/admin/users/${id}`, {
    ...options,
    method: 'DELETE',
  });
};

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
