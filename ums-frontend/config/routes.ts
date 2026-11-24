export default [
  {
    path: '/user',
    layout: false,
    routes: [
      { path: '/user/login', component: './user/login' },
      { path: '/user/register', component: './user/register' },
    ],
  },
  {
    path: '/welcome',
    name: 'welcome',
    icon: 'home',
    component: './Welcome',
  },
  { path: '/admin', redirect: '/admin/users' },
  { path: '/admin/sub-page', redirect: '/admin/users' },
  {
    path: '/admin/users',
    name: 'users',
    icon: 'team',
    access: 'canAdmin',
    component: './admin/users',
  },
  { path: '/', redirect: '/welcome' },
  { path: '*', component: './exception/404', layout: false },
];
