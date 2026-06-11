import { expect, test } from '@playwright/test';
import { apiBaseUrl, expectApiSuccess, unwrapApiData } from '../helpers/api';

type DistributedQueueMetrics = {
  distributedEnabled: boolean;
  workerEnabled: boolean;
  coordinatorPollEnabled: boolean;
  jobsByStatus?: Record<string, number>;
};

test.describe('Distributed staging (C2)', () => {
  const splitMode = process.env.DG_E2E_DISTRIBUTED_SPLIT === 'true';
  const embeddedMode = process.env.DG_E2E_DISTRIBUTED === 'true';

  test.beforeEach(() => {
    test.skip(
      !splitMode && !embeddedMode,
      'requires Podman e2e-distributed or split coordinator profile',
    );
  });

  test('GET /api/console/distributed/metrics reports enabled coordinator', async ({ request }) => {
    const res = await request.get(`${apiBaseUrl()}/api/console/distributed/metrics`);
    expect(res.ok()).toBeTruthy();
    const body = await res.json();
    expectApiSuccess(body);
    const metrics = unwrapApiData<DistributedQueueMetrics>(body);
    expect(metrics?.distributedEnabled).toBe(true);
    if (splitMode) {
      expect(metrics?.workerEnabled).toBe(false);
      expect(metrics?.coordinatorPollEnabled).toBe(false);
    } else {
      expect(metrics?.workerEnabled).toBe(true);
    }
    expect(metrics?.jobsByStatus).toBeTruthy();
  });
});
