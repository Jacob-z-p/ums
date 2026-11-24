import type { Settings as LayoutSettings } from '@ant-design/pro-components';
import { SettingDrawer } from '@ant-design/pro-components';
import type { RequestConfig, RunTimeLayoutConfig } from '@umijs/max';
import { history, Link } from '@umijs/max';
import { Button, Result } from 'antd';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import React from 'react';
import { AvatarDropdown, ErrorBoundary, OfflineBanner } from '@/components';
import Forbidden from '@/pages/exception/403';
import { apiStatus, currentUser, type SessionUser } from '@/services/ums';
import { isAuthPage, loginRedirect } from '@/utils/authNavigation';
import defaultSettings from '../config/defaultSettings';
import { errorConfig } from './requestErrorConfig';

dayjs.extend(relativeTime);

type InitialState = {
  settings?: Partial<LayoutSettings>;
  currentUser?: SessionUser;
  sessionError?: string;
  settingDrawerOpen?: boolean;
};

export async function getInitialState(): Promise<InitialState> {
  const state: InitialState = {
    settings: defaultSettings as Partial<LayoutSettings>,
    settingDrawerOpen: false,
  };
  if (isAuthPage(history.location.pathname)) return state;

  try {
    state.currentUser = await currentUser();
  } catch (error) {
    if (apiStatus(error) === 401) {
      loginRedirect();
    } else {
      state.sessionError = '无法确认登录状态，请重试';
    }
  }
  return state;
}

export const layout: RunTimeLayoutConfig = ({
  initialState,
  setInitialState,
}) => ({
  menuItemRender: (item, dom) =>
    item.path ? <Link to={item.path}>{dom}</Link> : dom,
  avatarProps: {
    src: initialState?.currentUser?.avatarUrl ?? undefined,
    title: initialState?.currentUser?.nickname,
    render: (_, children) => <AvatarDropdown>{children}</AvatarDropdown>,
  },
  onPageChange: () => {
    if (!initialState?.currentUser && !initialState?.sessionError)
      loginRedirect();
  },
  unAccessible: <Forbidden />,
  ErrorBoundary,
  childrenRender: (children) => {
    if (initialState?.sessionError) {
      return (
        <Result
          status="error"
          title={initialState.sessionError}
          extra={
            <Button type="primary" onClick={() => window.location.reload()}>
              重试
            </Button>
          }
        />
      );
    }
    if (!initialState?.currentUser) return null;
    return (
      <>
        {children}
        <SettingDrawer
          disableUrlParams
          enableDarkTheme
          collapse={initialState.settingDrawerOpen}
          onCollapseChange={(open) => {
            setInitialState((state) => ({ ...state, settingDrawerOpen: open }));
          }}
          settings={initialState.settings}
          onSettingChange={(settings) => {
            setInitialState((state) => ({ ...state, settings }));
          }}
        />
      </>
    );
  },
  ...initialState?.settings,
});

export const request: RequestConfig = {
  timeout: 10_000,
  ...errorConfig,
};

export function rootContainer(container: React.ReactNode) {
  return (
    <>
      <OfflineBanner />
      <ErrorBoundary>{container}</ErrorBoundary>
    </>
  );
}
