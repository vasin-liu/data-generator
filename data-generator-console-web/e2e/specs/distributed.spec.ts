import { expect, test } from '@playwright/test';
import { apiBaseUrl, expectApiSuccess, unwrapApiData } from '../helpers/api';

type DistributedQueueMetrics = {
  distributedEnabled: boolean;
  workerEnabled: boolean;
  coordinatorPollEnabled: boolean;
  jobsByStatus?: Record<string, number>;
};

test.describe('Distributed staging (C2)', () => {
  test.beforeEach(() => {
    test.skip(
      process.env.DG_E2E_DISTRIBUTED !== 'true',
      'requires Podman container with e2e-distributed profile',
    );
  });

  test('GET /api/console/distributed/metrics reports enabled worker', async ({ request }) => {
    const res = await request.get(`${apiBaseUrl()}/api/console/distributed/metrics`);
    expect(res.ok()).toBeTruthy();
    const body = await res.json();
    expectApiSuccess(body);
    const metrics = unwrapApiData<DistributedQueueMetrics>(body);
    expect(metrics?.distributedEnabled).toBe(true);
    expect(metrics?.workerEnabled).toBe(true);
    expect(metrics?.jobsByStatus).toBeTruthy();
  });
});
