import { LogoutOutlined, SkinOutlined } from '@ant-design/icons';
import { useModel } from '@umijs/max';
import { App } from 'antd';
import React, { useState } from 'react';
import { flushSync } from 'react-dom';
import { apiMessage, logout } from '@/services/ums';
import { loginRedirect } from '@/utils/authNavigation';
import HeaderDropdown from '../HeaderDropdown';

export const AvatarDropdown: React.FC<{ children?: React.ReactNode }> = ({
  children,
}) => {
  const { initialState, setInitialState } = useModel('@@initialState');
  const { message } = App.useApp();
  const [pending, setPending] = useState(false);

  async function signOut() {
    setPending(true);
    try {
      await logout();
      flushSync(() =>
        setInitialState((state) => ({ ...state, currentUser: undefined })),
      );
      loginRedirect();
    } catch (error) {
      message.error(apiMessage(error, '退出登录失败，请重试'));
    } finally {
      setPending(false);
    }
  }

  if (!initialState?.currentUser) return null;

  return (
    <HeaderDropdown
      placement="bottomRight"
      trigger={['click']}
      menu={{
        items: [
          {
            key: 'theme',
            label: '主题设置',
            icon: <SkinOutlined />,
            onClick: () =>
              setInitialState((state) => ({
                ...state,
                settingDrawerOpen: true,
              })),
          },
          {
            key: 'logout',
            label: '退出登录',
            icon: <LogoutOutlined />,
            disabled: pending,
            onClick: signOut,
          },
        ],
      }}
    >
      <button
        type="button"
        aria-label="账号菜单"
        className="cursor-pointer rounded border-0 bg-transparent px-2 py-1 text-inherit focus-visible:outline-2"
      >
        {children}
      </button>
    </HeaderDropdown>
  );
};
