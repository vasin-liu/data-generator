import { expect, test } from '@playwright/test';
import {
  apiGetWithRole,
  expectApiSuccess,
  fetchTemplateEditor,
  unwrapApiData,
} from '../helpers/api';
import { openScenarioFromCatalog, saveTemplateFromReview } from '../helpers/editor';
import { gotoConsoleHome, navigateViaTopNav } from '../helpers/navigation';
import { TestIds } from '../helpers/test-ids';

test.describe('AI source authoring (P1)', () => {
  test('AI catalog API lists providers parsers and prompt templates', async ({ request }) => {
    const { res, body } = await apiGetWithRole(request, '/api/console/ai/catalog');
    expect(res.ok()).toBeTruthy();
    expectApiSuccess(body);
    const catalog = unwrapApiData<{
      providers?: Array<{ type?: string }>;
      parsers?: Array<{ id?: string }>;
      promptTemplates?: Array<{ id?: string }>;
    }>(body);
    expect(catalog?.providers?.some((row) => row.type === 'INLINE')).toBe(true);
    expect(catalog?.parsers?.length).toBeGreaterThan(0);
    expect(catalog?.promptTemplates?.length).toBeGreaterThan(0);
  });

  test('GF-AI scenario seeds INLINE AI source and persists after save', async ({ page, request }) => {
    test.setTimeout(120_000);

    await gotoConsoleHome(page);
    await navigateViaTopNav(page, TestIds.nav.templates);
    await openScenarioFromCatalog(page, 'GF-AI');
    await saveTemplateFromReview(page);

    const templateId = page.url().match(/\/templates\/(\d+)/)?.[1];
    expect(templateId).toBeTruthy();

    const { res, body } = await fetchTemplateEditor(request, templateId!);
    expect(res.ok()).toBeTruthy();
    expectApiSuccess(body);
    const data = unwrapApiData<{
      draft?: {
        sources?: Record<
          string,
          {
            type?: string;
            provider?: { type?: string; options?: { rows?: Array<{ name?: string }> } };
          }
        >;
      };
    }>(body);
    const aiSource = Object.values(data?.draft?.sources ?? {}).find((row) => row.type === 'ai');
    expect(aiSource?.provider?.type).toBe('INLINE');
    expect(aiSource?.provider?.options?.rows?.length).toBeGreaterThan(0);
  });
});
