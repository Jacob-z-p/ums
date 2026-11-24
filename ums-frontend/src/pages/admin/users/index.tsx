import { PageContainer } from '@ant-design/pro-components';
import { useModel } from '@umijs/max';
import type { TableColumnsType } from 'antd';
import {
  Alert,
  App,
  Button,
  Card,
  Form,
  Input,
  Popconfirm,
  Space,
  Table,
  Tag,
} from 'antd';
import dayjs from 'dayjs';
import React, { useEffect, useState } from 'react';
import { flushSync } from 'react-dom';
import {
  accountPattern,
  apiMessage,
  apiStatus,
  deleteUser,
  listUsers,
  type ManagedUser,
} from '@/services/ums';
import { loginRedirect } from '@/utils/authNavigation';

const pageSize = 20;

export default function AdminUsers() {
  const [form] = Form.useForm<{ account?: string }>();
  const { message } = App.useApp();
  const { setInitialState } = useModel('@@initialState');
  const [query, setQuery] = useState({ account: '', page: 1 });
  const [rows, setRows] = useState<ManagedUser[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [deleting, setDeleting] = useState<number>();
  const [error, setError] = useState<string>();

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError(undefined);
    listUsers(
      { account: query.account || undefined, page: query.page, size: pageSize },
      controller.signal,
    )
      .then((result) => {
        if (controller.signal.aborted) return;
        const lastPage = Math.max(1, Math.ceil(result.total / pageSize));
        if (query.page > lastPage) {
          setQuery({ ...query, page: lastPage });
          return;
        }
        setRows(result.items);
        setTotal(result.total);
        setLoading(false);
      })
      .catch((cause) => {
        if (controller.signal.aborted) return;
        if (apiStatus(cause) === 401) {
          flushSync(() =>
            setInitialState((state) => ({ ...state, currentUser: undefined })),
          );
          loginRedirect();
          return;
        }
        setError(apiMessage(cause, '加载用户失败，请重试'));
        setRows([]);
        setTotal(0);
        setLoading(false);
      });
    return () => controller.abort();
  }, [query, setInitialState]);

  async function remove(user: ManagedUser) {
    setDeleting(user.id);
    try {
      await deleteUser(user.id);
      message.success(`已删除账号 ${user.account}`);
      setQuery((previous) => ({ ...previous }));
    } catch (cause) {
      const status = apiStatus(cause);
      if (status === 401) {
        flushSync(() =>
          setInitialState((state) => ({ ...state, currentUser: undefined })),
        );
        loginRedirect();
        return;
      }
      message.error(apiMessage(cause, '删除失败，请重试'));
      if (status === 404) setQuery((previous) => ({ ...previous }));
    } finally {
      setDeleting(undefined);
    }
  }

  const columns: TableColumnsType<ManagedUser> = [
    {
      title: '账号',
      dataIndex: 'account',
      rowScope: 'row',
      width: 160,
      ellipsis: true,
    },
    { title: '昵称', dataIndex: 'nickname', width: 160, ellipsis: true },
    {
      title: '角色',
      dataIndex: 'role',
      width: 100,
      render: (_, user) =>
        user.role === 1 ? <Tag color="blue">管理员</Tag> : <Tag>普通用户</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      render: (_, user) =>
        user.status === 0 ? (
          <Tag color="green">正常</Tag>
        ) : (
          <Tag color="red">禁用</Tag>
        ),
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      width: 180,
      render: (createdAt: string) =>
        dayjs(createdAt).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      title: '操作',
      key: 'actions',
      width: 100,
      render: (_, user) =>
        user.role === 0 &&
        Number.isSafeInteger(user.id) && (
          <Popconfirm
            title={`删除账号 ${user.account}？`}
            description="删除后，该账号将无法登录。"
            okText="确认删除"
            cancelText="取消"
            disabled={loading || deleting !== undefined}
            onConfirm={() => remove(user)}
          >
            <Button
              type="link"
              danger
              aria-label={`删除 ${user.account}`}
              disabled={loading || deleting !== undefined}
            >
              删除
            </Button>
          </Popconfirm>
        ),
    },
  ];

  return (
    <PageContainer title="用户管理">
      <Card>
        <Form
          form={form}
          layout="vertical"
          className="mb-4 flex flex-wrap items-start gap-x-3"
          onFinish={({ account }) =>
            setQuery({ account: account || '', page: 1 })
          }
        >
          <Form.Item
            className="mb-0 w-full sm:w-64"
            label="登录账号"
            name="account"
            rules={[
              {
                pattern: accountPattern,
                message: '请输入 1–64 位英文字母或数字',
              },
            ]}
          >
            <Input placeholder="按完整账号查询" allowClear maxLength={64} />
          </Form.Item>
          <Space className="mt-2 sm:mt-[30px]">
            <Button type="primary" htmlType="submit">
              查询
            </Button>
            <Button
              onClick={() => {
                form.resetFields();
                setQuery({ account: '', page: 1 });
              }}
            >
              重置
            </Button>
          </Space>
        </Form>
        {error && (
          <Alert
            className="mb-4"
            type="error"
            title={error}
            showIcon
            action={
              <Button onClick={() => setQuery((previous) => ({ ...previous }))}>
                重试
              </Button>
            }
          />
        )}
        <Table<ManagedUser>
          rowKey="account"
          columns={columns}
          dataSource={rows}
          loading={loading}
          scroll={{ x: 790 }}
          locale={{ emptyText: error ? '暂无可显示的用户' : '未找到用户' }}
          pagination={{
            current: query.page,
            pageSize,
            total,
            showSizeChanger: false,
            showTotal: (count) => `共 ${count} 位用户`,
            onChange: (page) => setQuery((previous) => ({ ...previous, page })),
          }}
        />
      </Card>
    </PageContainer>
  );
}
