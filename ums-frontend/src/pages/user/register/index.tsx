import { history, Link, useLocation } from '@umijs/max';
import React, { useState } from 'react';
import { apiMessage, type Credentials, register } from '@/services/ums';
import { loginPath } from '@/utils/authNavigation';
import AuthForm from '../AuthForm';

export default function Register() {
  const { search } = useLocation();
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string>();
  const redirect = new URLSearchParams(search).get('redirect');
  const loginUrl = redirect
    ? `${loginPath}?redirect=${encodeURIComponent(redirect)}`
    : loginPath;

  async function submit({ account, password }: Credentials) {
    setPending(true);
    setError(undefined);
    try {
      await register({ account, password });
      const params = new URLSearchParams({ account, registered: '1' });
      if (redirect) params.set('redirect', redirect);
      history.replace(`${loginPath}?${params.toString()}`);
    } catch (cause) {
      setError(apiMessage(cause, '注册失败，请稍后重试'));
    } finally {
      setPending(false);
    }
  }

  return (
    <AuthForm
      mode="register"
      error={error}
      pending={pending}
      alternate={
        <>
          已有账号？ <Link to={loginUrl}>返回登录</Link>
        </>
      }
      onSubmit={submit}
    />
  );
}
