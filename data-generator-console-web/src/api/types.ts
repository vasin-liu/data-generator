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
  id: string;
  name: string;
  status: string | null;
  archived: boolean | null;
  category?: string | null;
  tags?: string[];
}

export interface TemplateTaxonomy {
  categories: string[];
  tags: string[];
}

export interface ConsoleRuntime {
  v1ExecutionEnabled: boolean;
  scheduleEnabled: boolean;
  distributedEnabled: boolean;
  consoleSecurityEnabled: boolean;
  consoleRoleHeader: string;
  consoleRoles: string[];
}

/** Mirrors {@code StageMetricVO}. */
export interface StageMetric {
  name: string;
  rowsProcessed: number | null;
  durationMs: number | null;
  errorSample: string | null;
  rowsOk?: number | null;
  rowsFailed?: number | null;
}

/** Mirrors {@code AiCallMetricVO}. */
export interface AiCallMetric {
  sourceName: string | null;
  providerType: string | null;
  model: string | null;
  promptTokens: number | null;
  completionTokens: number | null;
  latencyMs: number | null;
  attempts: number | null;
  responseSample: string | null;
}

/** Mirrors {@code RunReportVO}. */
export interface RunReport {
  sources: StageMetric[];
  transformers: StageMetric[];
  sinks: StageMetric[];
  executionMode: string | null;
  durationMs: number | null;
  errorSamples: string[];
  aiCalls?: AiCallMetric[];
}

export interface TaskExecutionSummary {
  id: string;
  templateId: string;
  templateName: string;
  instanceId: string;
  definitionKind: string;
  triggerType: string;
  scheduleId: string | null;
  status: string;
  queuedAt: string | null;
  startedAt: string | null;
  finishedAt: string | null;
  rowCount: number | null;
  errorMessage: string | null;
  metricsJson: string | null;
  report: RunReport | null;
  pauseReason: string | null;
}

/** Mirrors {@code DistributedJobView}. */
export interface DistributedJobView {
  jobId: string;
  status: string;
  workerId: string | null;
  leaseUntil: string | null;
  attempts: number | null;
  queuedAt: string | null;
  finishedAt: string | null;
}

/** Mirrors {@code PartitionRunMetrics}. */
export interface PartitionRunMetrics {
  configuredPartitions: number;
  executedPartitions: number;
}

/** Mirrors {@code JobExecutionDetail}. */
export interface JobExecutionDetail {
  execution: TaskExecutionSummary;
  distributedJob: DistributedJobView | null;
  partitionMetrics: PartitionRunMetrics | null;
}

/** Mirrors {@code DistributedQueueMetricsDto}. */
export interface DistributedQueueMetrics {
  distributedEnabled: boolean;
  workerEnabled: boolean;
  coordinatorPollEnabled: boolean;
  jobsByStatus: Record<string, number>;
  activeWorkers: { workerId: string; activeJobs: number }[];
  collectedAt: string;
}

export interface RunStartResult {
  templateId: string;
  instanceId: string;
}

/** Mirrors {@code TaskScheduleView}. */
export interface TaskScheduleView {
  id: string | number;
  templateId: string | number;
  cronExpression: string;
  enabled: boolean;
  description: string | null;
  lastTriggeredAt: string | null;
  lastInstanceId: string | number | null;
  nextTriggerAt: string | null;
}

/** Mirrors {@code TaskScheduleUpsertRequest}. */
export interface TaskScheduleUpsertRequest {
  templateId: string | number;
  cronExpression: string;
  enabled?: boolean;
  description?: string | null;
}

/** Mirrors {@code AuditEventView}. */
export interface AuditEventView {
  id: string | number;
  occurredAt: string;
  actor: string | null;
  action: string;
  resourceType: string;
  resourceId: string | null;
  detail: Record<string, unknown>;
}

export type TemplateDefinitionKind = 'V1' | 'V2';

export interface TemplateEditorPayload {
  templateId: string | null;
  kind: TemplateDefinitionKind;
  draft: TemplateV2Draft;
  v1Yaml: string | null;
  archived: boolean;
  status: string | null;
}

/** Official V2 scenario catalog row for create-from-scenario wizard. */
export interface ScenarioCatalogEntry {
  scenarioId: string;
  family: string;
  name: string;
  catalogRef: string;
  resourceFile: string;
}

/** Mirrors {@code TemplateV2DraftVO} — nested objects stay JSON-polymorphic. */
export interface TemplateV2Draft {
  id?: number | null;
  instanceId?: number | null;
  name?: string;
  category?: string;
  tags?: string[];
  generator?: GeneratorDraft;
  sources?: Record<string, SourceDraft>;
  transform?: TransformDraft;
  transformers?: TransformDraft[];
  transformerCapabilities?: unknown[];
  sink?: SinkDraft;
  executionPolicy?: ExecutionPolicyDraft;
  sinkExecutionPolicy?: SinkExecutionPolicyDraft;
  workflow?: WorkflowSpecDraft;
  computeBlocks?: ComputeBlockDraft[];
  [key: string]: unknown;
}

export interface GeneratorDraft {
  type?: string;
  batchSize?: number;
  executor?: {
    coreSize?: number;
    maxSize?: number;
    queueCapacity?: number;
    keepAliveSeconds?: number;
    [key: string]: unknown;
  };
  [key: string]: unknown;
}

export interface SpelColumnDraft {
  name?: string;
  expression?: string;
}

/** Mirrors {@code MaterializationPolicyVO} — V2-native source row materialization. */
export type MaterializationMode = 'ORDERED' | 'LIMIT' | 'ONCE' | 'EQUAL' | 'WEIGHTED';

export interface MaterializationPolicyDraft {
  mode?: MaterializationMode | string;
  /** Maximum rows after mode-specific ordering/expansion; required for {@code LIMIT}. */
  limit?: number;
  /** Deterministic shuffle seed for {@code EQUAL} / {@code WEIGHTED}; defaults to 0 on the server. */
  seed?: number;
  /** Per-row weights aligned with pre-policy row order; required for {@code WEIGHTED}. */
  weights?: number[];
}

export interface SourceDraft {
  type?: string;
  dataSourceId?: string;
  sql?: string;
  iterator?: { type?: string; from?: number; to?: number; step?: number };
  materializationPolicy?: MaterializationPolicyDraft;
  policy?: {
    inMemory?: boolean;
    selectionStrategy?: string;
    limit?: number;
  };
  [key: string]: unknown;
}

export interface TransformDraft {
  name?: string;
  type?: string;
  sql?: string;
  script?: string;
  timeoutMs?: number;
  columns?: SpelColumnDraft[];
  [key: string]: unknown;
}

export interface SinkDraft {
  writers?: WriterDraft[];
  [key: string]: unknown;
}

export interface WriterDraft {
  type?: string;
  dataSourceId?: string;
  target?: string;
  template?: string;
  options?: Record<string, unknown>;
  [key: string]: unknown;
}

export interface ExecutionPolicyDraft {
  mode?: string;
  maxRowsInMemory?: number;
  previewRowLimit?: number;
  sourceChunkSize?: number;
  sinkBatchSize?: number;
  failOnLimitExceeded?: boolean;
  broadcastMaxRows?: number;
  [key: string]: unknown;
}

export interface SinkExecutionPolicyDraft {
  mode?: string;
  maxRetries?: number;
  retryBackoffMs?: number;
  parallelSinks?: boolean;
  [key: string]: unknown;
}

/** L2 workflow definition on a V2 template draft. */
export interface WorkflowSpecDraft {
  steps?: WorkflowStepDraft[];
}

/** Single workflow step; type-specific fields live on the object or in params JSON in the editor. */
export interface WorkflowStepDraft {
  id?: string;
  name?: string;
  type?: string;
  computeBlockId?: string;
  [key: string]: unknown;
}

/** Self-contained source → transform → sink unit inside a workflow. */
export interface ComputeBlockDraft {
  id?: string;
  sources?: Record<string, SourceDraft>;
  transformers?: TransformDraft[];
  transformGraph?: TransformGraphDraft;
  sinks?: SinkDraft[];
  sharedScopeId?: string;
  [key: string]: unknown;
}

/** L1 transform DAG inside a compute block. */
export interface TransformGraphDraft {
  transforms?: Record<string, TransformDraft>;
  nodes?: TransformNodeDraft[];
  edges?: TransformEdgeDraft[];
}

/** Node in a transform DAG. */
export interface TransformNodeDraft {
  id?: string;
  transformId?: string;
  outputAlias?: string;
}

/** Directed edge between transform DAG nodes. */
export interface TransformEdgeDraft {
  fromNodeId?: string;
  fromPort?: string;
  toNodeId?: string;
  toPort?: string;
}

export interface ValidationResult {
  valid: boolean;
  errors: string[];
  warnings: string[];
}

export interface PreviewResult {
  templateId: string;
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

export interface JdbcDriverPresetDto {
  id: string;
  groupKey: string;
  bundleKey: string;
  labelKey: string;
  driverClassName: string;
  alternateDriverClassNames: string[];
  urlTemplate: string;
  bundled: boolean;
}

export interface EditorDataSources {
  jdbcNames: string[];
  kafkaClusters: string[];
  elasticsearchClusters: string[];
}

/** Mirrors {@code AiProviderEntryDto}. */
export interface AiProviderEntry {
  type: string;
  label: string;
  description: string;
  remote: boolean;
}

/** Mirrors {@code AiParserEntryDto}. */
export interface AiParserEntry {
  id: string;
  label: string;
  description: string;
}

/** Mirrors {@code AiPromptTemplateDto}. */
export interface AiPromptTemplateEntry {
  id: string;
  label: string;
  prompt: string;
}

/** Mirrors {@code AiCatalogDto}. */
export interface AiCatalog {
  providers: AiProviderEntry[];
  parsers: AiParserEntry[];
  promptTemplates: AiPromptTemplateEntry[];
}

export interface DataSourcesOverview {
  persisted: DataSourceSummary[];
  runtimeKeys: string[];
  kafkaClusters: string[];
  elasticsearchClusters: string[];
  kafkaPersisted: MessagingClusterSummary[];
  elasticsearchPersisted: MessagingClusterSummary[];
  driverPresets: JdbcDriverPresetDto[];
}

export interface MessagingClusterSummary {
  name: string;
  clusterType: string;
  enabled: boolean;
  updatedAt: string | null;
  bootstrapServers?: string[] | null;
  uris?: string[] | null;
  username?: string | null;
  clientId?: string | null;
  acks?: string | null;
  compressionType?: string | null;
  retries?: number | null;
  securityProtocol?: string | null;
  saslMechanism?: string | null;
  properties?: Record<string, string> | null;
  pathPrefix?: string | null;
  connectionTimeoutMs?: number | null;
  socketTimeoutMs?: number | null;
  socketKeepAlive?: boolean | null;
  hasSaslJaasConfig?: boolean | null;
  hasPassword?: boolean | null;
  hasApiKey?: boolean | null;
}

export interface KafkaClusterUpsertPayload {
  name: string;
  bootstrapServers: string[];
  clientId?: string;
  acks?: string;
  compressionType?: string;
  retries?: number;
  securityProtocol?: string;
  saslMechanism?: string;
  saslJaasConfig?: string;
  properties?: Record<string, string>;
}

export interface ElasticsearchClusterUpsertPayload {
  name: string;
  uris: string[];
  username?: string;
  password?: string;
  apiKey?: string;
  pathPrefix?: string;
  connectionTimeoutMs?: number;
  socketTimeoutMs?: number;
  socketKeepAlive?: boolean;
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
