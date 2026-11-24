import { SafetyCertificateOutlined } from '@ant-design/icons';
import { Helmet } from '@umijs/max';
import { Alert, Button, Card, Form, Input, Typography } from 'antd';
import { createStyles } from 'antd-style';
import React from 'react';
import { accountPattern, type Credentials } from '@/services/ums';

const useStyles = createStyles(({ token }) => ({
  page: {
    minHeight: '100dvh',
    display: 'grid',
    placeItems: 'center',
    padding: '24px 16px',
    background: token.colorBgLayout,
  },
  card: { width: '100%', maxWidth: 440 },
  brand: {
    display: 'flex',
    alignItems: 'center',
    gap: 8,
    color: token.colorPrimaryText,
    fontWeight: 600,
    marginBottom: 16,
  },
  heading: { margin: '0 0 24px' },
  alert: { marginBottom: 20 },
  alternate: { display: 'block', textAlign: 'center', marginTop: 20 },
}));

type AuthValues = Credentials & { confirmPassword?: string };

type AuthFormProps = {
  mode: 'login' | 'register';
  initialAccount?: string;
  notice?: string;
  error?: string;
  pending: boolean;
  alternate: React.ReactNode;
  onSubmit: (values: AuthValues) => Promise<void>;
};

export default function AuthForm({
  mode,
  initialAccount,
  notice,
  error,
  pending,
  alternate,
  onSubmit,
}: AuthFormProps) {
  const { styles } = useStyles();
  const registering = mode === 'register';
  const title = registering ? '注册账号' : '登录';

  return (
    <main className={styles.page}>
      <Helmet>
        <title>{title} - 用户中心</title>
      </Helmet>
      <Card className={styles.card}>
        <div className={styles.brand}>
          <SafetyCertificateOutlined /> 用户中心
        </div>
        <Typography.Title level={1} className={styles.heading}>
          {title}
        </Typography.Title>
        <Form<AuthValues>
          name={mode}
          layout="vertical"
          size="large"
          requiredMark={false}
          variant="outlined"
          disabled={pending}
          initialValues={{ account: initialAccount }}
          scrollToFirstError={{ focus: true }}
          onFinish={onSubmit}
        >
          {error ? (
            <Alert
              title={error}
              type="error"
              showIcon
              className={styles.alert}
            />
          ) : (
            notice && (
              <Alert
                title={notice}
                type="success"
                showIcon
                className={styles.alert}
              />
            )
          )}
          <Form.Item
            label="账号"
            name="account"
            rules={[
              { required: true, message: '请输入账号' },
              {
                pattern: accountPattern,
                message: '账号须为 1–64 位英文字母或数字',
              },
            ]}
          >
            <Input autoComplete="username" maxLength={64} />
          </Form.Item>
          <Form.Item
            label="密码"
            name="password"
            rules={[
              { required: true, message: '请输入密码' },
              {
                validator: (_, value?: string) =>
                  !value || Array.from(value).length >= 8
                    ? Promise.resolve()
                    : Promise.reject(new Error('密码至少需要 8 个字符')),
              },
            ]}
          >
            <Input.Password
              autoComplete={registering ? 'new-password' : 'current-password'}
            />
          </Form.Item>
          {registering && (
            <Form.Item
              label="确认密码"
              name="confirmPassword"
              dependencies={['password']}
              rules={[
                { required: true, message: '请再次输入密码' },
                ({ getFieldValue }) => ({
                  validator: (_, value?: string) =>
                    !value || value === getFieldValue('password')
                      ? Promise.resolve()
                      : Promise.reject(new Error('两次输入的密码不一致')),
                }),
              ]}
            >
              <Input.Password autoComplete="new-password" />
            </Form.Item>
          )}
          <Button type="primary" htmlType="submit" block loading={pending}>
            {title}
          </Button>
        </Form>
        <Typography.Text className={styles.alternate}>
          {alternate}
        </Typography.Text>
      </Card>
    </main>
  );
}
