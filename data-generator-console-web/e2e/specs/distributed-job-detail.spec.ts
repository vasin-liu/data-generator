import { expect, test } from '@playwright/test';
import { expectApiSuccess, fetchJobDetail, unwrapApiData } from '../helpers/api';
import { gotoConsoleHome, navigateViaTopNav } from '../helpers/navigation';
import { TestIds } from '../helpers/test-ids';

test.describe('Distributed job detail (C2 split)', () => {
  test.beforeEach(() => {
    test.skip(
      process.env.DG_E2E_DISTRIBUTED_SPLIT !== 'true',
      'requires dual-container Podman staging (DG_E2E_DISTRIBUTED_SPLIT=true)',
    );
  });

  test('job detail shows distributed queue metadata after worker run', async ({ page, request }) => {
    const instanceId = process.env.DG_E2E_JOB_INSTANCE_ID;
    expect(instanceId).toBeTruthy();

    const { res, body } = await fetchJobDetail(request, instanceId!);
    expect(res.ok()).toBeTruthy();
    expectApiSuccess(body);
    const detail = unwrapApiData<{
      execution?: { status?: string };
      distributedJob?: { status?: string; workerId?: string };
    }>(body);
    expect(detail?.execution?.status).toBe('SUCCESS');
    expect(detail?.distributedJob?.status).toBe('SUCCESS');
    expect(detail?.distributedJob?.workerId).toBeTruthy();

    await gotoConsoleHome(page);
    await navigateViaTopNav(page, TestIds.nav.jobs);
    await page.goto(`./jobs/${instanceId}`);
    await expect(page.getByTestId('job-detail-page')).toBeVisible({ timeout: 30_000 });
    const distributed = page.getByTestId('job-detail-distributed');
    await expect(distributed).toBeVisible();
    await expect(distributed.getByText(detail!.distributedJob!.workerId!)).toBeVisible();
    await expect(distributed.getByRole('cell', { name: /成功|Succeeded/i })).toBeVisible();
  });
});
