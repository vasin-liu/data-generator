/** Mirrors {@code org.gensokyo.data.model.vo.R} JSON shape. */
export interface ApiResult<T> {
  code: number;
  message: string;
  success: boolean;
  data: T;
}

/** Parses JSON API envelope; throws on {@code success === false}. */
export async function parseApiResult<T>(res: Response): Promise<T> {
  const body = (await res.json()) as ApiResult<T>;
  if (!body.success) {
    throw new Error(body.message || `Request failed (${res.status})`);
  }
  return body.data;
}

export interface TemplateSummary {
  id: number;
  name: string;
  archived: boolean | null;
}

export interface ConsoleRuntime {
  v1ExecutionEnabled: boolean;
}

export interface TaskExecutionSummary {
  id: number;
  templateId: number;
  templateName: string;
  instanceId: number;
  definitionKind: string;
  status: string;
  queuedAt: string | null;
  startedAt: string | null;
  finishedAt: string | null;
  rowCount: number | null;
  errorMessage: string | null;
  metricsJson: string | null;
}

export interface RunStartResult {
  templateId: number;
  instanceId: number;
}

export type TemplateDefinitionKind = 'V1' | 'V2';

export interface TemplateEditorPayload {
  templateId: number | null;
  kind: TemplateDefinitionKind;
  draft: TemplateV2Draft;
  v1Yaml: string | null;
  archived: boolean;
}

/** Mirrors {@code TemplateV2DraftVO} — nested objects stay JSON-polymorphic. */
export interface TemplateV2Draft {
  id?: number | null;
  instanceId?: number | null;
  name?: string;
  generator?: { batchSize?: number };
  sources?: Record<string, SourceDraft>;
  transform?: TransformDraft;
  sink?: SinkDraft;
  executionPolicy?: ExecutionPolicyDraft;
  [key: string]: unknown;
}

export interface SourceDraft {
  type?: string;
  dataSourceId?: string;
  sql?: string;
  iterator?: { type?: string; from?: number; to?: number; step?: number };
}

export interface TransformDraft {
  type?: string;
  sql?: string;
  columns?: { name?: string; expression?: string }[];
}

export interface SinkDraft {
  writers?: WriterDraft[];
}

export interface WriterDraft {
  type?: string;
  dataSourceId?: string;
  target?: string;
}

export interface ExecutionPolicyDraft {
  mode?: string;
  sourceChunkSize?: number;
  sinkBatchSize?: number;
  previewRowLimit?: number;
}

export interface ValidationResult {
  valid: boolean;
  errors: string[];
  warnings: string[];
}

export interface PreviewResult {
  templateId: number;
  preview: {
    schema?: unknown;
    rows?: unknown[];
    warnings?: string[];
  };
}

export interface DataSourceSummary {
  name: string;
  url: string;
  username: string | null;
  driverClassName: string;
  driverJarPath: string | null;
  enabled: boolean;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface DataSourcesOverview {
  persisted: DataSourceSummary[];
  runtimeKeys: string[];
}

export interface DataSourceTestRequest {
  url: string;
  username: string;
  password: string;
  driverClassName: string;
  driverJarPath?: string | null;
}

export interface MigrationInventorySummary {
  totalTemplates: number;
  databaseTemplates: number;
  readyToPromote: number;
  compatibilityOnly: number;
  blocked: number;
  withCompareReport: number;
}

export interface MigrationInventoryEntry {
  id: string;
  name: string;
  scenarioFamily: string | null;
  migrationClass: string | null;
  wave: number | null;
  businessSignoffApproved: boolean;
  businessSignoffAt?: string | null;
  lastCompareReportPath: string | null;
  dbTemplateId: number | null;
}

export interface MigrationAnalysis {
  suggestedClass: string;
  recommendedPath: string | null;
  scenarioFamily: string | null;
  wave: number | null;
  blockers: string[];
  warnings: string[];
}

export interface MigrationCompareReport {
  classification: string;
  recommendation: string | null;
  v1RowCount: number;
  v2RowCount: number;
  sampleMatchRate: number;
  reportPath: string | null;
  warnings: string[];
}

export interface MigrationSignoffRequest {
  approved: boolean;
  approvedBy?: string;
  notes?: string;
}
