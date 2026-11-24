import { history, Link, useLocation, useModel } from '@umijs/max';
import React, { useState } from 'react';
import { flushSync } from 'react-dom';
import {
  accountPattern,
  apiMessage,
  type Credentials,
  login,
} from '@/services/ums';
import { registerPath, safeDestination } from '@/utils/authNavigation';
import AuthForm from '../AuthForm';

export default function Login() {
  const { search } = useLocation();
  const { setInitialState } = useModel('@@initialState');
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string>();
  const params = new URLSearchParams(search);
  const account = params.get('account') || '';
  const redirect = params.get('redirect');
  const signup = redirect
    ? `${registerPath}?redirect=${encodeURIComponent(redirect)}`
    : registerPath;

  async function submit({ account, password }: Credentials) {
    setPending(true);
    setError(undefined);
    try {
      const currentUser = await login({ account, password });
      flushSync(() =>
        setInitialState((state) => ({
          ...state,
          currentUser,
          sessionError: undefined,
        })),
      );
      history.replace(safeDestination(redirect));
    } catch (cause) {
      setError(apiMessage(cause, '登录失败，请稍后重试'));
    } finally {
      setPending(false);
    }
  }

  return (
    <AuthForm
      mode="login"
      initialAccount={accountPattern.test(account) ? account : undefined}
      notice={params.get('registered') === '1' ? '注册成功，请登录' : undefined}
      error={error}
      pending={pending}
      alternate={
        <>
          还没有账号？ <Link to={signup}>注册账号</Link>
        </>
      }
      onSubmit={submit}
    />
  );
}
