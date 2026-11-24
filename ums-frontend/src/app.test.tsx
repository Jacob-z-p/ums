import { beforeEach, describe, expect, it, vi } from 'vitest';

const { history, currentUser } = vi.hoisted(() => ({
  history: {
    location: { pathname: '/welcome', search: '', hash: '' },
    replace: vi.fn(),
  },
  currentUser: vi.fn(),
}));

vi.mock('@umijs/max', () => ({ history, Link: () => null }));
vi.mock('@/services/ums', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/services/ums')>()),
  currentUser,
}));
vi.mock('@/components', () => ({
  AvatarDropdown: () => null,
  ErrorBoundary: () => null,
  OfflineBanner: () => null,
}));
vi.mock('@ant-design/pro-components', () => ({ SettingDrawer: () => null }));
vi.mock('./requestErrorConfig', () => ({ errorConfig: {} }));
vi.mock('../config/defaultSettings', () => ({
  default: { navTheme: 'light' },
}));

import { getInitialState } from './app';
import { safeDestination } from './utils/authNavigation';

describe('app getInitialState', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    history.location = { pathname: '/welcome', search: '', hash: '' };
  });

  it('restores the user from the unwrapped session response', async () => {
    const user = {
      id: 1,
      account: 'Alice',
      nickname: 'Alice',
      avatarUrl: null,
      role: 1,
    };
    currentUser.mockResolvedValue(user);
    expect(await getInitialState()).toMatchObject({
      currentUser: user,
      settings: { navTheme: 'light' },
      settingDrawerOpen: false,
    });
    expect(currentUser).toHaveBeenCalledOnce();
  });

  it('redirects an expired session and preserves the requested path', async () => {
    history.location = {
      pathname: '/admin/users',
      search: '?page=2',
      hash: '#section',
    };
    currentUser.mockRejectedValue({ response: { status: 401 } });

    expect((await getInitialState()).currentUser).toBeUndefined();
    expect(history.replace).toHaveBeenCalledWith(
      '/user/login?redirect=' +
        encodeURIComponent('/admin/users?page=2#section'),
    );
  });

  it.each(['/user/login', '/user/register'])(
    'leaves %s public',
    async (pathname) => {
      history.location.pathname = pathname;
      expect((await getInitialState()).currentUser).toBeUndefined();
      expect(currentUser).not.toHaveBeenCalled();
      expect(history.replace).not.toHaveBeenCalled();
    },
  );

  it('allows retrying when the session check fails unexpectedly', async () => {
    currentUser.mockRejectedValue({ response: { status: 503 } });
    expect(await getInitialState()).toHaveProperty('sessionError');
    expect(history.replace).not.toHaveBeenCalled();
  });
});

describe('login destination', () => {
  it('keeps the query and fragment of a local destination', () => {
    expect(safeDestination('/admin/users?account=Alice#list')).toBe(
      '/admin/users?account=Alice#list',
    );
  });

  it.each([
    null,
    'https://example.com',
    '//example.com',
    '/path/..//example.com',
    '/\\example.com',
    '/user/login',
    '/user/%72egister',
  ])('falls back to the welcome page for %s', (destination) =>
    expect(safeDestination(destination)).toBe('/welcome'),
  );
});
