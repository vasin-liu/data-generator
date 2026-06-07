import { apiRequest } from './client';
import { getConsoleRole } from './consoleRole';
import type {
  PreviewResult,
  RunStartResult,
  TemplateEditorPayload,
  TemplateV2Draft,
  ValidationResult,
} from './types';

export type {
  ComputeBlockDraft,
  TransformGraphDraft,
  TransformNodeDraft,
  WorkflowSpecDraft,
  WorkflowStepDraft,
} from './types';

/**
 * @returns empty wizard scaffold
 */
export function fetchEditorScaffold(): Promise<TemplateEditorPayload> {
  return apiRequest<TemplateEditorPayload>('/templates/scaffold');
}

/**
 * @param templateId persisted id
 */
export function fetchEditor(templateId: string): Promise<TemplateEditorPayload> {
  return apiRequest<TemplateEditorPayload>(`/templates/${templateId}`);
}

/**
 * @param draft first save
 */
export function createTemplate(draft: TemplateV2Draft): Promise<TemplateEditorPayload> {
  return apiRequest<TemplateEditorPayload>('/templates', {
    method: 'POST',
    body: JSON.stringify(draft),
  });
}

/**
 * @param templateId target id
 * @param draft body
 */
export function saveTemplate(
  templateId: string,
  draft: TemplateV2Draft,
): Promise<TemplateEditorPayload> {
  return apiRequest<TemplateEditorPayload>(`/templates/${templateId}`, {
    method: 'PUT',
    body: JSON.stringify(draft),
  });
}

/**
 * @param draft draft to validate
 * @param templateId optional persisted id
 */
export function validateDraft(
  draft: TemplateV2Draft,
  templateId?: string | null,
): Promise<ValidationResult> {
  const path =
    templateId != null
      ? `/templates/${templateId}/draft/validate`
      : '/templates/draft/validate';
  return apiRequest<ValidationResult>(path, {
    method: 'POST',
    body: JSON.stringify(draft),
  });
}

/**
 * @param draft draft body
 * @param templateId optional id
 * @param maxRows optional preview cap
 * @param throughTransformIndex optional 0-based inclusive transformer index for staged preview
 */
export function previewDraft(
  draft: TemplateV2Draft,
  templateId?: string | null,
  maxRows?: number,
  throughTransformIndex?: number,
): Promise<PreviewResult> {
  const path =
    templateId != null ? `/templates/${templateId}/draft/preview` : '/templates/draft/preview';
  return apiRequest<PreviewResult>(path, {
    method: 'POST',
    body: JSON.stringify({ draft, maxRows, throughTransformIndex }),
  });
}

/**
 * @param draft draft to save and run
 * @param templateId optional id
 */
export function runDraft(
  draft: TemplateV2Draft,
  templateId?: string | null,
): Promise<RunStartResult> {
  const path = templateId != null ? `/templates/${templateId}/draft/run` : '/templates/draft/run';
  return apiRequest<RunStartResult>(path, {
    method: 'POST',
    body: JSON.stringify(draft),
  });
}

/**
 * @param templateId template id
 */
export function fetchTemplateYaml(templateId: string): Promise<string> {
  return apiRequest<string>(`/templates/${templateId}/yaml`);
}

/**
 * @param templateId template id
 * @param yaml YAML text
 */
export function applyTemplateYaml(
  templateId: string,
  yaml: string,
): Promise<TemplateEditorPayload> {
  return apiRequest<TemplateEditorPayload>(`/templates/${templateId}/yaml`, {
    method: 'PUT',
    body: JSON.stringify({ yaml }),
  });
}

/**
 * @param draft in-memory wizard draft
 */
export async function exportDraftYaml(draft: TemplateV2Draft): Promise<string> {
  const res = await fetch('/api/templates/draft/yaml', {
    method: 'POST',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      'X-Console-Role': getConsoleRole(),
    },
    body: JSON.stringify(draft),
  });
  const body = (await res.json()) as import('./types').ApiResult<string>;
  if (!body.success) {
    throw new Error(body.message || `Export failed (${res.status})`);
  }
  if (body.data != null && body.data !== '') {
    return body.data;
  }
  throw new Error('Empty YAML export from server');
}

/**
 * @param yaml YAML text
 */
export function parseDraftYaml(yaml: string): Promise<TemplateV2Draft> {
  return apiRequest<TemplateV2Draft>('/templates/draft/yaml/parse', {
    method: 'POST',
    body: JSON.stringify({ yaml }),
  });
}

/**
 * Validates and publishes a persisted template (DRAFT → PUBLISHED).
 *
 * @param templateId template id
 */
export function publishTemplate(templateId: string): Promise<string> {
  return apiRequest<string>(`/templates/${templateId}/publish`, { method: 'POST' });
}
