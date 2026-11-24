import type { SessionUser } from '@/services/ums';

export default function access(
  initialState: { currentUser?: SessionUser } | undefined,
) {
  return { canAdmin: initialState?.currentUser?.role === 1 };
}
