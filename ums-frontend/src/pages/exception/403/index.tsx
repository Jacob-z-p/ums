import { Link } from '@umijs/max';
import { Button, Card, Result } from 'antd';
import React from 'react';

export default () => (
  <Card variant="borderless">
    <Result
      status="403"
      title="403"
      subTitle="需要管理员权限"
      extra={
        <Link to="/welcome">
          <Button type="primary">返回首页</Button>
        </Link>
      }
    />
  </Card>
);
