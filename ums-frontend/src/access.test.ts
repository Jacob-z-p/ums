import { describe, expect, it } from 'vitest';
import access from './access';

describe('access', () => {
  const user = { id: 1, account: 'Alice', nickname: 'Alice', avatarUrl: null };

  it('allows administrators', () => {
    expect(access({ currentUser: { ...user, role: 1 } }).canAdmin).toBe(true);
  });

  it('denies ordinary users and guests', () => {
    expect(access({ currentUser: { ...user, role: 0 } }).canAdmin).toBe(false);
    expect(access(undefined).canAdmin).toBe(false);
  });
});
