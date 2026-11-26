import { history } from '@umijs/max';
import { appBasePath } from './appBasePath';

export const loginPath = '/user/login';
export const registerPath = '/user/register';
export const welcomePath = '/welcome';

function routePathname(pathname: string) {
  if (pathname === appBasePath) return '/';
  return pathname.startsWith(`${appBasePath}/`)
    ? pathname.slice(appBasePath.length)
    : pathname;
}

function isAuthRoute(pathname: string) {
  const path = pathname.replace(/\/+$/, '').toLowerCase();
  return path === loginPath || path === registerPath;
}

export function isAuthPage(pathname: string) {
  return isAuthRoute(routePathname(pathname));
}

export function loginRedirect() {
  const { pathname, search, hash } = history.location;
  const destination = routePathname(pathname);
  if (!isAuthRoute(destination)) {
    history.replace(
      `${loginPath}?redirect=${encodeURIComponent(destination + search + hash)}`,
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
      isAuthRoute(decodeURIComponent(url.pathname))
    ) {
      return welcomePath;
    }
    return url.pathname + url.search + url.hash;
  } catch {
    return welcomePath;
  }
}
