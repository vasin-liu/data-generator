import type {
  ExecutionPolicyDraft,
  GeneratorDraft,
  MaterializationPolicyDraft,
  SinkExecutionPolicyDraft,
  SourceDraft,
  SpelColumnDraft,
  TemplateV2Draft,
  TransformDraft,
  WriterDraft,
} from '../../api/types';

const INPUT_KEY = 'input';
const EDITABLE_WRITER_TYPES = new Set(['console', 'jdbc', 'kafka', 'elasticsearch']);

/** Source kinds supported in the form editor. */
export type EditableSourceKind =
  | 'query'
  | 'iterator'
  | 'csv'
  | 'json'
  | 'excel'
  | 'ai'
  | 'geojson';

export const EDITABLE_SOURCE_KINDS: EditableSourceKind[] = [
  'query',
  'iterator',
  'csv',
  'json',
  'excel',
  'ai',
  'geojson',
];

/**
 * @param draft source draft
 */
export function cloneDraft(draft: TemplateV2Draft): TemplateV2Draft {
  return JSON.parse(JSON.stringify(draft)) as TemplateV2Draft;
}

/**
 * @param draft template draft
 */
export function listSourceKeys(draft: TemplateV2Draft): string[] {
  return Object.keys(draft.sources ?? {});
}

/**
 * @param draft template draft
 * @param key source map key
 */
export function readSource(draft: TemplateV2Draft, key: string): SourceDraft | undefined {
  return draft.sources?.[key];
}

/**
 * @param source source draft node
 */
export function inferSourceKind(source?: SourceDraft): EditableSourceKind | 'other' {
  const type = source?.type?.toLowerCase();
  if (EDITABLE_SOURCE_KINDS.includes(type as EditableSourceKind)) {
    return type as EditableSourceKind;
  }
  return 'other';
}

/** @deprecated use {@link inferSourceKind} */
export function inferSourceType(source?: SourceDraft): 'query' | 'iterator' | 'other' {
  const kind = inferSourceKind(source);
  if (kind === 'query' || kind === 'iterator') {
    return kind;
  }
  return 'other';
}

/**
 * @param source source draft node
 */
export function isEditableSource(source?: SourceDraft): boolean {
  return inferSourceKind(source) !== 'other';
}

/**
 * @param kind source kind for a new node
 */
/** Default optional source policy for new sources (non-blocking fields). */
export function defaultSourcePolicy(): NonNullable<SourceDraft['policy']> {
  return { inMemory: false, selectionStrategy: 'ORDER' };
}

/** Empty materialization policy (omit from draft until the operator sets a mode). */
export function defaultMaterializationPolicy(): MaterializationPolicyDraft {
  return {};
}

export function defaultSourceForKind(kind: EditableSourceKind): SourceDraft {
  const policy = defaultSourcePolicy();
  switch (kind) {
    case 'query':
      return { type: 'query', dataSourceId: '', sql: 'SELECT 1', policy };
    case 'iterator':
      return {
        type: 'iterator',
        iterator: { type: 'number', from: 1, to: 3, step: 1 },
        policy,
      };
    case 'csv':
      return { type: 'csv', path: '', charset: 'UTF-8', delimiter: ',', header: true, policy };
    case 'json':
      return { type: 'json', path: '', charset: 'UTF-8', root: '', policy };
    case 'excel':
      return { type: 'excel', path: '', sheets: [{ name: 'Sheet1' }], policy };
    case 'ai':
      return { type: 'ai', api: '', prompt: '', parser: '', policy };
    case 'geojson':
      return { type: 'geojson', path: '', policy };
    default:
      return { type: kind, policy };
  }
}

/**
 * @param draft template draft
 * @param key source map key
 * @param patch fields merged into the existing source node
 */
export function applySourceMergeAt(
  draft: TemplateV2Draft,
  key: string,
  patch: SourceDraft,
): TemplateV2Draft {
  const next = cloneDraft(draft);
  const existing = next.sources?.[key] ?? {};
  next.sources = {
    ...(next.sources ?? {}),
    [key]: { ...existing, ...patch, type: patch.type ?? existing.type },
  };
  return next;
}

/**
 * @param draft template draft
 * @param key source map key
 * @param kind new source kind (replaces node, preserves policy)
 */
export function setSourceKindAt(
  draft: TemplateV2Draft,
  key: string,
  kind: EditableSourceKind,
): TemplateV2Draft {
  const policy = draft.sources?.[key]?.policy;
  const node = defaultSourceForKind(kind);
  if (policy) {
    node.policy = policy;
  }
  return applySourceMergeAt(draft, key, node);
}

/**
 * @param draft template draft
 */
export function suggestSourceKey(draft: TemplateV2Draft): string {
  const keys = new Set(listSourceKeys(draft));
  if (!keys.has(INPUT_KEY)) {
    return INPUT_KEY;
  }
  let index = 2;
  while (keys.has(`source${index}`)) {
    index += 1;
  }
  return `source${index}`;
}

/**
 * @param draft template draft
 * @param key source map key
 * @param type source kind
 * @param fields bound form values
 */
export function applySourceAt(
  draft: TemplateV2Draft,
  key: string,
  type: 'query' | 'iterator',
  fields: {
    dataSourceId?: string;
    sql?: string;
    from?: number;
    to?: number;
    step?: number;
  },
): TemplateV2Draft {
  const next = cloneDraft(draft);
  const existing = next.sources?.[key] ?? {};
  let source: SourceDraft;
  if (type === 'query') {
    source = {
      ...existing,
      type: 'query',
      dataSourceId: fields.dataSourceId,
      sql: fields.sql,
    };
    delete source.iterator;
  } else {
    source = {
      ...existing,
      type: 'iterator',
      iterator: {
        ...(existing.iterator ?? {}),
        type: 'number',
        from: fields.from ?? 1,
        to: fields.to ?? 3,
        step: fields.step ?? 1,
      },
    };
    delete source.dataSourceId;
    delete source.sql;
  }
  next.sources = { ...(next.sources ?? {}), [key]: source };
  return next;
}

/**
 * @param draft template draft
 * @param key new source key
 * @param type initial source kind
 */
export function addSource(
  draft: TemplateV2Draft,
  key: string,
  kind: EditableSourceKind = 'iterator',
): TemplateV2Draft {
  if (draft.sources?.[key]) {
    return draft;
  }
  return applySourceMergeAt(draft, key, defaultSourceForKind(kind));
}

/**
 * @param draft template draft
 * @param key source map key to remove
 */
export function removeSourceAt(draft: TemplateV2Draft, key: string): TemplateV2Draft {
  const next = cloneDraft(draft);
  if (!next.sources?.[key]) {
    return next;
  }
  const { [key]: _removed, ...rest } = next.sources;
  next.sources = rest;
  return next;
}

/**
 * @param draft template draft
 * @param oldKey current key
 * @param newKey desired key
 */
export function renameSourceKey(
  draft: TemplateV2Draft,
  oldKey: string,
  newKey: string,
): TemplateV2Draft {
  const trimmed = newKey.trim();
  if (!trimmed || oldKey === trimmed || draft.sources?.[trimmed]) {
    return draft;
  }
  const next = cloneDraft(draft);
  const entry = next.sources?.[oldKey];
  if (!entry) {
    return draft;
  }
  const sources = { ...next.sources };
  delete sources[oldKey];
  sources[trimmed] = entry;
  next.sources = sources;
  return next;
}

/**
 * @param draft template draft
 * @param key source map key
 * @param partial policy fields to merge
 */
export function applySourcePolicyAt(
  draft: TemplateV2Draft,
  key: string,
  partial: NonNullable<SourceDraft['policy']>,
): TemplateV2Draft {
  const next = cloneDraft(draft);
  const source = next.sources?.[key];
  if (!source) {
    return draft;
  }
  next.sources = {
    ...(next.sources ?? {}),
    [key]: {
      ...source,
      policy: { ...(source.policy ?? {}), ...partial },
    },
  };
  return next;
}

/**
 * @param draft template draft
 * @param key source map key
 * @param partial materialization policy fields to merge; {@code undefined} values remove keys
 */
export function applySourceMaterializationPolicyAt(
  draft: TemplateV2Draft,
  key: string,
  partial: MaterializationPolicyDraft,
): TemplateV2Draft {
  const next = cloneDraft(draft);
  const source = next.sources?.[key];
  if (!source) {
    return draft;
  }
  const merged: MaterializationPolicyDraft = { ...(source.materializationPolicy ?? {}), ...partial };
  for (const field of ['mode', 'limit', 'seed', 'weights'] as const) {
    if (field in partial && partial[field] === undefined) {
      delete merged[field];
    }
  }
  const updated: SourceDraft = { ...source };
  if (!merged.mode) {
    delete updated.materializationPolicy;
  } else {
    updated.materializationPolicy = merged;
  }
  next.sources = { ...(next.sources ?? {}), [key]: updated };
  return next;
}

/** @deprecated use {@link readSource} with an explicit key */
export function readPrimarySource(draft: TemplateV2Draft): SourceDraft | undefined {
  const sources = draft.sources;
  if (!sources) {
    return undefined;
  }
  return sources[INPUT_KEY] ?? Object.values(sources)[0];
}

/** @deprecated use {@link inferSourceType} on {@link readSource} */
export function readSourceType(draft: TemplateV2Draft): 'query' | 'iterator' {
  const source = readPrimarySource(draft);
  return inferSourceType(source) === 'query' ? 'query' : 'iterator';
}

/** @deprecated use {@link applySourceAt} */
export function applyPrimarySource(
  draft: TemplateV2Draft,
  type: 'query' | 'iterator',
  fields: Parameters<typeof applySourceAt>[3],
): TemplateV2Draft {
  const key = listSourceKeys(draft)[0] ?? INPUT_KEY;
  return applySourceAt(draft, key, type, fields);
}

/**
 * @param draft template draft
 */
export function usesTransformerChain(draft: TemplateV2Draft): boolean {
  return (draft.transformers?.length ?? 0) > 0;
}

/**
 * @param transform transform draft node
 */
export function inferTransformType(transform?: TransformDraft): 'sql' | 'spel' {
  if (transform?.type?.toLowerCase() === 'spel' || (transform?.columns?.length ?? 0) > 0) {
    return 'spel';
  }
  return 'sql';
}

/**
 * @param draft template draft
 */
export function readTransformType(draft: TemplateV2Draft): 'sql' | 'spel' {
  const node = usesTransformerChain(draft) ? draft.transformers?.[0] : draft.transform;
  return inferTransformType(node);
}

/**
 * @param draft template draft
 */
export function readSpelColumns(draft: TemplateV2Draft): SpelColumnDraft[] {
  const node = usesTransformerChain(draft) ? draft.transformers?.[0] : draft.transform;
  return node?.columns ? [...node.columns] : [];
}

/**
 * @param draft template draft
 */
export function listTransformers(draft: TemplateV2Draft): TransformDraft[] {
  if (usesTransformerChain(draft)) {
    return draft.transformers ? [...draft.transformers] : [];
  }
  return draft.transform ? [draft.transform] : [];
}

function defaultSqlTransform(name = 'transform'): TransformDraft {
  return { name, type: 'sql', sql: 'SELECT * FROM input' };
}

/**
 * @param draft template draft
 */
export function switchToSingleTransform(draft: TemplateV2Draft): TemplateV2Draft {
  const next = cloneDraft(draft);
  const first = next.transformers?.[0] ?? next.transform ?? defaultSqlTransform();
  next.transform = first;
  delete next.transformers;
  return next;
}

/**
 * @param draft template draft
 */
export function switchToChainTransform(draft: TemplateV2Draft): TemplateV2Draft {
  const next = cloneDraft(draft);
  next.transformers =
    next.transformers && next.transformers.length > 0
      ? [...next.transformers]
      : next.transform
        ? [next.transform]
        : [defaultSqlTransform('step1')];
  delete next.transform;
  return next;
}

/**
 * @param draft template draft
 * @param transformers full chain (replaces singular transform)
 */
export function setTransformers(draft: TemplateV2Draft, transformers: TransformDraft[]): TemplateV2Draft {
  const next = cloneDraft(draft);
  next.transformers = transformers;
  delete next.transform;
  return next;
}

/**
 * @param draft template draft
 * @param index transformer index
 * @param patch partial transformer
 */
export function applyTransformerAt(
  draft: TemplateV2Draft,
  index: number,
  patch: TransformDraft,
): TemplateV2Draft {
  const list = listTransformers(draft);
  const nextList = [...list];
  while (nextList.length <= index) {
    nextList.push(defaultSqlTransform(`step${nextList.length + 1}`));
  }
  const merged: TransformDraft = { ...nextList[index], ...patch };
  if (inferTransformType(merged) === 'spel') {
    merged.type = 'spel';
    delete merged.sql;
  } else {
    merged.type = 'sql';
    delete merged.columns;
  }
  nextList[index] = merged;
  const base = usesTransformerChain(draft) ? draft : switchToChainTransform(draft);
  return setTransformers(base, nextList);
}

/**
 * @param draft template draft
 * @param type initial transform kind
 */
export function addTransformer(draft: TemplateV2Draft, type: 'sql' | 'spel' = 'sql'): TemplateV2Draft {
  const base = usesTransformerChain(draft) ? draft : switchToChainTransform(draft);
  const list = listTransformers(base);
  const node: TransformDraft =
    type === 'spel'
      ? { name: `step${list.length + 1}`, type: 'spel', columns: [{ name: 'value', expression: '#input' }] }
      : defaultSqlTransform(`step${list.length + 1}`);
  return setTransformers(base, [...list, node]);
}

/**
 * @param draft template draft
 * @param index transformer index
 */
export function removeTransformerAt(draft: TemplateV2Draft, index: number): TemplateV2Draft {
  const list = listTransformers(draft).filter((_, i) => i !== index);
  if (list.length === 0) {
    return switchToSingleTransform(draft);
  }
  return setTransformers(draft, list);
}

/**
 * @param draft template draft
 * @param sql SQL text
 */
export function applyTransformSql(draft: TemplateV2Draft, sql: string): TemplateV2Draft {
  if (usesTransformerChain(draft)) {
    const existing = draft.transformers?.[0] ?? {};
    return applyTransformerAt(draft, 0, { ...existing, type: 'sql', sql });
  }
  const next = cloneDraft(draft);
  const existing = next.transform ?? {};
  next.transform = { ...existing, type: 'sql', sql };
  delete next.transform.columns;
  return next;
}

/**
 * @param draft template draft
 * @param columns SpEL column mappings
 */
export function applySpelColumns(draft: TemplateV2Draft, columns: SpelColumnDraft[]): TemplateV2Draft {
  if (usesTransformerChain(draft)) {
    const existing = draft.transformers?.[0] ?? {};
    return applyTransformerAt(draft, 0, { ...existing, type: 'spel', columns });
  }
  const next = cloneDraft(draft);
  const existing = next.transform ?? {};
  next.transform = { ...existing, type: 'spel', columns };
  delete next.transform.sql;
  return next;
}

/**
 * @param draft template draft
 * @param type transform kind
 * @param sql SQL text when type is sql
 * @param columns SpEL columns when type is spel
 */
export function applyTransformType(
  draft: TemplateV2Draft,
  type: 'sql' | 'spel',
  sql: string,
  columns: SpelColumnDraft[],
): TemplateV2Draft {
  return type === 'sql' ? applyTransformSql(draft, sql) : applySpelColumns(draft, columns);
}

/** @deprecated use {@link applyTransformType} */
export function applyTransform(
  draft: TemplateV2Draft,
  type: 'sql' | 'spel',
  sql: string,
  spelColumn: string,
  spelExpression: string,
): TemplateV2Draft {
  return applyTransformType(
    draft,
    type,
    sql,
    [{ name: spelColumn, expression: spelExpression }],
  );
}

/**
 * @param draft template draft
 */
export function listWriters(draft: TemplateV2Draft): WriterDraft[] {
  return draft.sink?.writers ? [...draft.sink.writers] : [];
}

/**
 * @param writer writer draft node
 */
export function isEditableWriter(writer?: WriterDraft): boolean {
  const type = writer?.type?.toLowerCase();
  return type != null && EDITABLE_WRITER_TYPES.has(type);
}

/**
 * @param draft template draft
 * @param index writer index
 */
export function readWriterAt(draft: TemplateV2Draft, index: number): WriterDraft | undefined {
  return draft.sink?.writers?.[index];
}

/** @deprecated use {@link readWriterAt} */
export function readWriter(draft: TemplateV2Draft): WriterDraft | undefined {
  return readWriterAt(draft, 0);
}

/**
 * @param draft template draft
 * @param index writer index
 * @param patch partial writer fields
 */
export function applyWriterAt(
  draft: TemplateV2Draft,
  index: number,
  patch: Partial<WriterDraft> & { type?: string },
): TemplateV2Draft {
  const next = cloneDraft(draft);
  if (!next.sink) {
    next.sink = { writers: [] };
  }
  const writers = [...(next.sink.writers ?? [])];
  while (writers.length <= index) {
    writers.push({ type: 'console' });
  }
  writers[index] = { ...writers[index], ...patch };
  next.sink = { ...next.sink, writers };
  return next;
}

/**
 * @param draft template draft
 * @param type writer kind
 */
export function addWriter(draft: TemplateV2Draft, type = 'console'): TemplateV2Draft {
  const next = cloneDraft(draft);
  if (!next.sink) {
    next.sink = { writers: [] };
  }
  const writers = [...(next.sink.writers ?? []), { type }];
  next.sink = { ...next.sink, writers };
  return next;
}

/**
 * @param draft template draft
 * @param index writer index to remove
 */
export function removeWriterAt(draft: TemplateV2Draft, index: number): TemplateV2Draft {
  const next = cloneDraft(draft);
  if (!next.sink?.writers?.[index]) {
    return next;
  }
  const writers = next.sink.writers.filter((_, i) => i !== index);
  next.sink = { ...next.sink, writers };
  return next;
}

/** @deprecated use {@link applyWriterAt} */
export function applySink(
  draft: TemplateV2Draft,
  writerType: string,
  dataSourceId?: string,
  target?: string,
): TemplateV2Draft {
  const patch: Partial<WriterDraft> & { type: string } = { type: writerType };
  if (writerType === 'jdbc' || writerType === 'kafka' || writerType === 'elasticsearch') {
    patch.dataSourceId = dataSourceId;
    patch.target = target;
  }
  const writers = listWriters(draft);
  if (writers.length === 0) {
    return addWriter(applyWriterAt(draft, 0, patch), writerType);
  }
  return applyWriterAt(draft, 0, patch);
}

/**
 * @param draft template draft
 * @param partial execution policy fields to merge
 */
export function patchExecutionPolicy(
  draft: TemplateV2Draft,
  partial: ExecutionPolicyDraft,
): TemplateV2Draft {
  const next = cloneDraft(draft);
  next.executionPolicy = { ...(next.executionPolicy ?? {}), ...partial };
  return next;
}

/**
 * @param draft template draft
 * @param partial sink execution policy fields to merge
 */
export function patchSinkExecutionPolicy(
  draft: TemplateV2Draft,
  partial: SinkExecutionPolicyDraft,
): TemplateV2Draft {
  const next = cloneDraft(draft);
  next.sinkExecutionPolicy = { ...(next.sinkExecutionPolicy ?? {}), ...partial };
  return next;
}

/**
 * @param draft template draft
 * @param partial generator fields to merge
 */
export function patchGenerator(draft: TemplateV2Draft, partial: GeneratorDraft): TemplateV2Draft {
  const next = cloneDraft(draft);
  next.generator = { ...(next.generator ?? {}), ...partial };
  return next;
}

/**
 * @param draft template draft
 */
export function ensureGenerator(draft: TemplateV2Draft): TemplateV2Draft {
  const next = cloneDraft(draft);
  if (!next.generator) {
    next.generator = { type: 'SYNC', batchSize: 100 };
  }
  return next;
}
