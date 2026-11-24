import { expect, test } from '@playwright/test';

const ordinaryUser = {
  id: 1,
  account: 'Alice',
  nickname: 'Alice',
  avatarUrl: null,
  role: 0,
};

const success = (data: unknown) => ({
  code: 0,
  data,
  message: '操作成功',
  description: '',
});

const failure = (code: number, message: string, description: string) => ({
  code,
  data: null,
  message,
  description,
});

const unauthorized = {
  status: 401,
  json: failure(2003, '请先登录', '当前登录会话无效或账号不可用'),
};

test('validates registration and signs in with the prefilled account', async ({
  page,
}) => {
  let registered = false;
  let signedIn = false;
  await page.route('**/api/**', async (route) => {
    const path = new URL(route.request().url()).pathname;
    if (path === '/api/user/register') {
      expect(route.request().postDataJSON()).toEqual({
        account: 'Alice',
        password: 'password123',
      });
      if (!registered) {
        registered = true;
        return route.fulfill({
          status: 409,
          json: failure(2002, '账号已存在', '该登录账号已被占用'),
        });
      }
      return route.fulfill({ status: 201, json: success({ id: 1 }) });
    }
    if (path === '/api/user/login') {
      signedIn = true;
      return route.fulfill({ json: success(ordinaryUser) });
    }
    if (path === '/api/user/current' && signedIn)
      return route.fulfill({ json: success(ordinaryUser) });
    return route.fulfill(unauthorized);
  });

  await page.goto('/user/login');
  await page.getByRole('link', { name: '注册账号' }).click();
  await page.getByLabel('账号', { exact: true }).fill('Alice_');
  await page.getByLabel('密码', { exact: true }).fill('😀😀😀😀😀😀😀');
  await page.getByLabel('确认密码').fill('different123');
  await page.getByRole('button', { name: '注册账号' }).click();
  await expect(page.getByText('账号须为 1–64 位英文字母或数字')).toBeVisible();
  await expect(page.getByText('密码至少需要 8 个字符')).toBeVisible();
  await expect(page.getByText('两次输入的密码不一致')).toBeVisible();

  await page.getByLabel('账号', { exact: true }).fill('Alice');
  await page.getByLabel('密码', { exact: true }).fill('password123');
  await page.getByLabel('确认密码').fill('password123');
  await page.getByRole('button', { name: '注册账号' }).click();
  await expect(page.getByText('账号已存在')).toBeVisible();

  await page.getByRole('button', { name: '注册账号' }).click();
  await expect(page.getByText('注册成功，请登录')).toBeVisible();
  await expect(page.getByLabel('账号', { exact: true })).toHaveValue('Alice');
  await page.getByLabel('密码', { exact: true }).fill('password123');
  await page.getByRole('button', { name: '登录', exact: true }).click();
  await expect(page.getByTestId('welcome-title')).toBeVisible();
  await page.reload();
  await expect(page.getByTestId('welcome-title')).toBeVisible();
});

test('redirects a guest and shows 403 for a signed-in ordinary user', async ({
  page,
}) => {
  let signedIn = false;
  await page.route('**/api/**', (route) => {
    if (route.request().url().endsWith('/api/user/login')) signedIn = true;
    return signedIn
      ? route.fulfill({ json: success(ordinaryUser) })
      : route.fulfill(unauthorized);
  });
  await page.goto('/admin/users');
  await expect(page).toHaveURL(/\/user\/login\?redirect=/);
  await page.getByLabel('账号', { exact: true }).fill('Alice');
  await page.getByLabel('密码', { exact: true }).fill('password123');
  await page.getByRole('button', { name: '登录', exact: true }).click();
  await expect(page).toHaveURL(/\/admin\/users$/);
  await expect(page.getByText('需要管理员权限')).toBeVisible();
  await expect(
    page.getByRole('link', { name: '用户管理', exact: true }),
  ).toHaveCount(0);
});

test('falls back to the preceding page after deleting the last row and handles an expired session', async ({
  page,
}) => {
  const admin = {
    ...ordinaryUser,
    id: 2,
    account: 'Admin',
    nickname: 'Admin',
    role: 1,
  };
  const victim = {
    ...ordinaryUser,
    status: 0,
    createdAt: '2026-01-01T12:00:00',
  };
  let removed = false;
  let expired = false;
  await page.route('**/api/**', (route) => {
    const url = new URL(route.request().url());
    if (url.pathname === '/api/user/current')
      return route.fulfill({ json: success(admin) });
    if (url.pathname === '/api/admin/users/1') {
      removed = true;
      return route.fulfill({ json: success(null) });
    }
    if (expired) return route.fulfill(unauthorized);
    const pageNumber = Number(url.searchParams.get('page'));
    const items = pageNumber === 2 ? (removed ? [] : [victim]) : [admin];
    return route.fulfill({
      json: success({
        items,
        page: pageNumber,
        size: 20,
        total: removed ? 20 : 21,
      }),
    });
  });

  await page.goto('/admin/users');
  await expect(page.getByText('共 21 位用户')).toBeVisible();
  await page.getByTitle('2', { exact: true }).click();
  await page.getByRole('button', { name: '删除 Alice' }).click();
  await page.getByRole('button', { name: '确认删除' }).click();
  await expect(page.getByRole('rowheader', { name: 'Admin' })).toBeVisible();
  await expect(page.getByText('共 20 位用户')).toBeVisible();

  expired = true;
  await page.getByRole('button', { name: '查询' }).click();
  await expect(page).toHaveURL(/\/user\/login\?redirect=/);
});

test('keeps the user signed in when logout fails', async ({ page }) => {
  await page.route('**/api/user/current', (route) =>
    route.fulfill({ json: success(ordinaryUser) }),
  );
  await page.route('**/api/user/logout', (route) =>
    route.fulfill({ status: 503 }),
  );
  await page.goto('/welcome');
  await page.getByRole('button', { name: '账号菜单' }).click();
  await page.getByRole('menuitem', { name: '退出登录' }).click();
  await expect(page.getByText('退出登录失败，请重试')).toBeVisible();
  await expect(page.getByTestId('welcome-title')).toBeVisible();
});
