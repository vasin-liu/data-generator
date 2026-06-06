import { expect, test } from '@playwright/test';
import { fetchHealth } from '../helpers/api';

test.describe('API / health', () => {
  test('GET /healthz returns UP', async ({ request }) => {
    const { res, body } = await fetchHealth(request);
    expect(res.ok()).toBeTruthy();
    expect(body).toMatchObject({ opcode: 0, status: 'UP' });
  });
});
