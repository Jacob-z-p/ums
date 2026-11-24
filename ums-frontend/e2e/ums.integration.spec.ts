import { randomUUID } from 'node:crypto';
import {
  type APIRequestContext,
  expect,
  type Page,
  test,
} from '@playwright/test';

const password = 'TestPassword123';

const newAccount = () => `E2E${randomUUID().replaceAll('-', '')}`;

async function submitLogin(page: Page, account: string, secret: string) {
  await page.getByLabel('账号', { exact: true }).fill(account);
  await page.getByLabel('密码', { exact: true }).fill(secret);
  await page.getByRole('button', { name: '登录', exact: true }).click();
}

async function submitRegistration(page: Page, account: string) {
  await page.getByLabel('账号', { exact: true }).fill(account);
  await page.getByLabel('密码', { exact: true }).fill(password);
  await page.getByLabel('确认密码').fill(password);
  await page.getByRole('button', { name: '注册账号' }).click();
}

async function registerAccount(request: APIRequestContext, account: string) {
  const result = await request.post('/api/user/register', {
    data: { account, password },
  });
  expect(result.status()).toBe(201);
  return (await result.json()).id as number;
}

test('registers and rejects duplicates, validates credentials, and restores a regular session', async ({
  page,
}) => {
  const account = newAccount();
  await page.goto('/user/register');
  await submitRegistration(page, account);
  await expect(page.getByText('注册成功，请登录')).toBeVisible();
  await expect(page.getByLabel('账号', { exact: true })).toHaveValue(account);
  await page.getByRole('link', { name: '注册账号' }).click();
  await submitRegistration(page, account.toLowerCase());
  await expect(page.getByText('账号已存在')).toBeVisible();

  await page.getByRole('link', { name: '返回登录' }).click();
  await submitLogin(page, account, 'Incorrect123');
  await expect(page.getByText('账号或密码错误')).toBeVisible();
  await submitLogin(page, account.toLowerCase(), password);
  await expect(page).toHaveURL(/\/welcome$/);
  await expect(page.getByTestId('welcome-title')).toBeVisible();

  const cookie = (await page.context().cookies()).find(
    ({ name }) => name === 'JSESSIONID',
  );
  expect(cookie?.httpOnly).toBe(true);
  await page.reload();
  await expect(page.getByTestId('welcome-title')).toBeVisible();
  const current = await page.request.get('/api/user/current');
  expect(current.status()).toBe(200);
  await expect(current.json()).resolves.toEqual({
    id: expect.any(Number),
    account,
    nickname: account,
    avatarUrl: null,
    role: 0,
  });
  expect((await page.request.get('/api/admin/users')).status()).toBe(403);

  await page.goto('/admin/users');
  await expect(page.getByText('需要管理员权限')).toBeVisible();
  await expect(
    page.getByRole('link', { name: '用户管理', exact: true }),
  ).toHaveCount(0);
});

test('paginates, searches, deletes, and invalidates sessions through the same-origin proxy', async ({
  page,
  request,
}) => {
  test.setTimeout(90_000);
  const admin = process.env.UMS_E2E_ADMIN_ACCOUNT;
  const adminPassword = process.env.UMS_E2E_ADMIN_PASSWORD;
  if (!admin || !adminPassword) {
    throw new Error(
      '请设置 UMS_E2E_ADMIN_ACCOUNT 和 UMS_E2E_ADMIN_PASSWORD，并连接独立的测试数据库',
    );
  }

  const victim = newAccount();
  const victimId = await registerAccount(request, victim);
  expect(
    (
      await request.post('/api/user/login', {
        data: { account: victim, password },
      })
    ).status(),
  ).toBe(200);
  for (let index = 0; index < 20; index++) {
    await registerAccount(request, newAccount());
  }

  await page.goto('/admin/sub-page');
  await expect(page).toHaveURL(/\/user\/login\?redirect=/);
  await submitLogin(page, admin, adminPassword);
  await expect(page).toHaveURL(/\/admin\/users$/);
  await expect(page.getByTitle('2', { exact: true })).toBeVisible();
  const secondPage = page.waitForResponse(
    (response) =>
      new URL(response.url()).pathname === '/api/admin/users' &&
      new URL(response.url()).searchParams.get('page') === '2',
  );
  await page.getByTitle('2', { exact: true }).click();
  const secondPageResult = await secondPage;
  expect(secondPageResult.status()).toBe(200);
  await expect(secondPageResult.json()).resolves.toMatchObject({
    page: 2,
    size: 20,
  });

  const accountFilter = page.getByLabel('登录账号');
  await accountFilter.fill(victim.slice(0, -1));
  await page.getByRole('button', { name: '查询' }).click();
  await expect(page.getByText('未找到用户')).toBeVisible();
  await accountFilter.fill(victim.toLowerCase());
  await page.getByRole('button', { name: '查询' }).click();
  await expect(page.getByRole('rowheader', { name: victim })).toBeVisible();

  await page.getByRole('button', { name: `删除 ${victim}` }).click();
  await page.getByRole('button', { name: '取消' }).click();
  await expect(page.getByRole('rowheader', { name: victim })).toBeVisible();
  const removal = page.waitForResponse(
    (response) => response.request().method() === 'DELETE',
  );
  await page.getByRole('button', { name: `删除 ${victim}` }).click();
  await page.getByRole('button', { name: '确认删除' }).click();
  expect((await removal).status()).toBe(204);
  await expect(page.getByText('未找到用户')).toBeVisible();

  expect((await request.get('/api/user/current')).status()).toBe(401);
  expect(
    (
      await request.post('/api/user/login', {
        data: { account: victim, password },
      })
    ).status(),
  ).toBe(401);
  expect(
    (await page.request.delete(`/api/admin/users/${victimId}`)).status(),
  ).toBe(404);
  const current = await (await page.request.get('/api/user/current')).json();
  expect(current.role).toBe(1);
  expect(
    (await page.request.delete(`/api/admin/users/${current.id}`)).status(),
  ).toBe(403);

  await page.getByRole('button', { name: '重置' }).click();
  await expect(accountFilter).toHaveValue('');
  const cookie = (await page.context().cookies()).find(
    ({ name }) => name === 'JSESSIONID',
  );
  if (!cookie) throw new Error('登录后未收到 JSESSIONID');
  await page.getByRole('button', { name: '账号菜单' }).click();
  await page.getByRole('menuitem', { name: '退出登录' }).click();
  await expect(page).toHaveURL(/\/user\/login/);
  const headers = { Cookie: `${cookie.name}=${cookie.value}` };
  expect((await request.get('/api/admin/users', { headers })).status()).toBe(
    401,
  );
  expect((await request.post('/api/user/logout', { headers })).status()).toBe(
    204,
  );
});
