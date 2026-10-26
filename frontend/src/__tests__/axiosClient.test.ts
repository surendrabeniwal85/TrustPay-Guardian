import { describe, it, expect, beforeEach } from 'vitest';
import { setAccessToken, getAccessToken, getErrorMessage } from '../api/axiosClient';

describe('axiosClient and In-Memory Token Security', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    setAccessToken(null);
  });

  it('stores and retrieves access token strictly in memory', () => {
    setAccessToken('sample.jwt.token');
    expect(getAccessToken()).toBe('sample.jwt.token');

    // STRICT PRD REQUIREMENT: Tokens must never leak into localStorage or sessionStorage
    expect(localStorage.getItem('tpg_access_token')).toBeNull();
    expect(sessionStorage.getItem('tpg_access_token')).toBeNull();
  });

  it('formats error messages correctly from RFC 7807 problem details or plain errors', () => {
    const errorWithDetail = {
      isAxiosError: true,
      response: {
        data: {
          detail: 'Source account has insufficient balance',
        },
      },
    };
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    expect(getErrorMessage(errorWithDetail as any)).toBe('Source account has insufficient balance');

    const standardError = new Error('Network error');
    expect(getErrorMessage(standardError)).toBe('Network error');

    expect(getErrorMessage('unknown string')).toBe('An unexpected error occurred. Please try again.');
  });
});
