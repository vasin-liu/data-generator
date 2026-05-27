import type { SourceDraft, TemplateV2Draft, TransformDraft, WriterDraft } from '../../api/types';

const INPUT_KEY = 'input';

/**
 * @param draft source draft
 */
export function cloneDraft(draft: TemplateV2Draft): TemplateV2Draft {
  return JSON.parse(JSON.stringify(draft)) as TemplateV2Draft;
}

/**
 * @param draft template draft
 */
export function readPrimarySource(draft: TemplateV2Draft): SourceDraft | undefined {
  const sources = draft.sources;
  if (!sources) {
    return undefined;
  }
  return sources[INPUT_KEY] ?? Object.values(sources)[0];
}

/**
 * @param draft template draft
 */
export function readSourceType(draft: TemplateV2Draft): 'query' | 'iterator' {
  const source = readPrimarySource(draft);
  const type = source?.type?.toLowerCase();
  return type === 'query' ? 'query' : 'iterator';
}

/**
 * @param draft template draft
 * @param type source kind
 * @param fields bound form values
 */
export function applyPrimarySource(
  draft: TemplateV2Draft,
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
  let source: SourceDraft;
  if (type === 'query') {
    source = { type: 'query', dataSourceId: fields.dataSourceId, sql: fields.sql };
  } else {
    source = {
      type: 'iterator',
      iterator: {
        type: 'number',
        from: fields.from ?? 1,
        to: fields.to ?? 3,
        step: fields.step ?? 1,
      },
    };
  }
  next.sources = { [INPUT_KEY]: source };
  return next;
}

/**
 * @param draft template draft
 */
export function readTransformType(draft: TemplateV2Draft): 'sql' | 'spel' {
  const transform = draft.transform;
  if (transform?.type?.toLowerCase() === 'spel' || (transform?.columns?.length ?? 0) > 0) {
    return 'spel';
  }
  return 'sql';
}

/**
 * @param draft template draft
 * @param type transform kind
 * @param sql SQL text
 * @param spelColumn column name
 * @param spelExpression SpEL expression
 */
export function applyTransform(
  draft: TemplateV2Draft,
  type: 'sql' | 'spel',
  sql: string,
  spelColumn: string,
  spelExpression: string,
): TemplateV2Draft {
  const next = cloneDraft(draft);
  if (type === 'spel') {
    next.transform = {
      type: 'spel',
      columns: [{ name: spelColumn, expression: spelExpression }],
    } satisfies TransformDraft;
  } else {
    next.transform = { type: 'sql', sql } satisfies TransformDraft;
  }
  return next;
}

/**
 * @param draft template draft
 */
export function readWriter(draft: TemplateV2Draft): WriterDraft | undefined {
  return draft.sink?.writers?.[0];
}

/**
 * @param draft template draft
 * @param writerType writer kind
 * @param dataSourceId JDBC id
 * @param target table/topic/index
 */
export function applySink(
  draft: TemplateV2Draft,
  writerType: string,
  dataSourceId?: string,
  target?: string,
): TemplateV2Draft {
  const next = cloneDraft(draft);
  const writer: WriterDraft = { type: writerType };
  if (writerType === 'jdbc' || writerType === 'kafka' || writerType === 'elasticsearch') {
    writer.dataSourceId = dataSourceId;
    writer.target = target;
  }
  next.sink = { writers: [writer] };
  return next;
}

/**
 * @param draft template draft
 */
export function ensureGenerator(draft: TemplateV2Draft): TemplateV2Draft {
  const next = cloneDraft(draft);
  if (!next.generator) {
    next.generator = { batchSize: 100 };
  }
  return next;
}
