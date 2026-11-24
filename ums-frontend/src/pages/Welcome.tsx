import { PageContainer } from '@ant-design/pro-components';
import { Link, useModel } from '@umijs/max';
import { Card, Typography } from 'antd';
import React from 'react';

export default function Welcome() {
  const { initialState } = useModel('@@initialState');
  const user = initialState?.currentUser;
  return (
    <PageContainer title="首页">
      <Card>
        <Typography.Title level={2} data-testid="welcome-title">
          欢迎使用用户中心
        </Typography.Title>
        {user && (
          <Typography.Paragraph>
            当前登录账号 {user.account}
          </Typography.Paragraph>
        )}
        {user?.role === 1 && <Link to="/admin/users">前往用户管理</Link>}
      </Card>
    </PageContainer>
  );
}
