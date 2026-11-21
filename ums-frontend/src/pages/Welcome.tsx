import { PageContainer } from '@ant-design/pro-components';
import { FormattedMessage } from '@umijs/max';
import { Card } from 'antd';
import React from 'react';

const Welcome: React.FC = () => (
  <PageContainer>
    <Card>
      <h2 data-testid="welcome-title">
        <FormattedMessage id="pages.welcome.userCenter" defaultMessage="欢迎使用用户中心" />
      </h2>
      <p>
        <FormattedMessage id="pages.welcome.userCenterDescription" defaultMessage="在这里开始构建用户管理系统。" />
      </p>
    </Card>
  </PageContainer>
);

export default Welcome;
