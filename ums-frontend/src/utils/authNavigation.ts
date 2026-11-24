import { history } from '@umijs/max';

export const loginPath = '/user/login';
export const registerPath = '/user/register';
export const welcomePath = '/welcome';

export function isAuthPage(pathname: string) {
  const path = pathname.replace(/\/+$/, '').toLowerCase();
  return path === loginPath || path === registerPath;
}

export function loginRedirect() {
  const { pathname, search, hash } = history.location;
  if (!isAuthPage(pathname)) {
    history.replace(
      `${loginPath}?redirect=${encodeURIComponent(pathname + search + hash)}`,
    );
  }
}

export function safeDestination(redirect: string | null) {
  if (!redirect?.startsWith('/') || redirect.startsWith('//'))
    return welcomePath;

  try {
    const url = new URL(redirect, window.location.origin);
    if (
      url.origin !== window.location.origin ||
      url.pathname.startsWith('//') ||
      isAuthPage(decodeURIComponent(url.pathname))
    ) {
      return welcomePath;
    }
    return url.pathname + url.search + url.hash;
  } catch {
    return welcomePath;
  }
}
