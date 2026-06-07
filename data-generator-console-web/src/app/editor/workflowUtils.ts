import type {
  ComputeBlockDraft,
  SinkDraft,
  TemplateV2Draft,
  TransformDraft,
  TransformGraphDraft,
  TransformNodeDraft,
  WorkflowSpecDraft,
  WorkflowStepDraft,
} from '../../api/types';
import { cloneDraft, inferTransformType, type TransformKind } from './draftUtils';

/** Supported workflow step types in the minimal editor. */
export const WORKFLOW_STEP_TYPES = [
  'log',
  'pause',
  'invoke_compute_block',
  'branch',
  'shared_scope',
] as const;

export type WorkflowStepType = (typeof WORKFLOW_STEP_TYPES)[number];

/** Log levels supported by the workflow log step editor. */
export const LOG_LEVELS = ['DEBUG', 'INFO', 'WARN', 'ERROR'] as const;

/** Shared scope lifecycle actions in the workflow editor. */
export const SHARED_SCOPE_ACTIONS = ['open', 'write', 'read', 'close'] as const;

const RESERVED_STEP_KEYS = new Set([
  'id',
  'name',
  'type',
  'computeBlockId',
  'level',
  'message',
  'fields',
  'durationMs',
  'until',
  'condition',
  'manual',
  'scopeId',
  'action',
  'entries',
  'thenSteps',
  'elseSteps',
  'thenComputeBlockId',
  'elseComputeBlockId',
]);

/**
 * @param draft template draft
 */
export function hasWorkflow(draft: TemplateV2Draft): boolean {
  return draft.workflow != null;
}

/**
 * @param draft template draft
 */
export function listWorkflowSteps(draft: TemplateV2Draft): WorkflowStepDraft[] {
  return draft.workflow?.steps ? [...draft.workflow.steps] : [];
}

/**
 * @param draft template draft
 */
export function listComputeBlocks(draft: TemplateV2Draft): ComputeBlockDraft[] {
  return draft.computeBlocks ? [...draft.computeBlocks] : [];
}

/**
 * @param draft template draft
 */
export function listComputeBlockIdOptions(draft: TemplateV2Draft): { value: string; label: string }[] {
  return listComputeBlocks(draft).map((block, index) => {
    const id = block.id ?? `block-${index + 1}`;
    return { value: id, label: id };
  });
}

/**
 * @param step workflow step
 */
export function stepParamsJson(step: WorkflowStepDraft): string {
  const params: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(step)) {
    if (!RESERVED_STEP_KEYS.has(key) && value !== undefined) {
      params[key] = value;
    }
  }
  return Object.keys(params).length > 0 ? JSON.stringify(params, null, 2) : '';
}

/**
 * @param step workflow step
 * @param jsonText params JSON (excluding id, name, type, computeBlockId)
 */
export function applyStepParamsJson(step: WorkflowStepDraft, jsonText: string): WorkflowStepDraft {
  const base: WorkflowStepDraft = {
    id: step.id,
    name: step.name,
    type: step.type,
    computeBlockId: step.computeBlockId,
  };
  const trimmed = jsonText.trim();
  if (!trimmed) {
    return base;
  }
  const parsed = JSON.parse(trimmed) as Record<string, unknown>;
  return { ...base, ...parsed };
}

/**
 * @param type step type
 */
export function defaultStepForType(type: WorkflowStepType, index: number): WorkflowStepDraft {
  const id = `${type.replace(/_/g, '-')}-${index + 1}`;
  switch (type) {
    case 'log':
      return { id, type, level: 'INFO', message: '' };
    case 'pause':
      return { id, type, durationMs: 50 };
    case 'invoke_compute_block':
      return { id, type, computeBlockId: '' };
    case 'branch':
      return { id, type, condition: 'true' };
    case 'shared_scope':
      return { id, type, scopeId: 'scope1', action: 'open' };
    default:
      return { id, type };
  }
}

/**
 * @param draft template draft
 */
export function enableWorkflow(draft: TemplateV2Draft): TemplateV2Draft {
  const next = cloneDraft(draft);
  if (!next.workflow) {
    next.workflow = {
      steps: [defaultStepForType('log', 0), defaultStepForType('invoke_compute_block', 1)],
    };
  }
  if (!next.computeBlocks || next.computeBlocks.length === 0) {
    next.computeBlocks = [defaultComputeBlock('block-1')];
  }
  return next;
}

/**
 * @param draft template draft
 */
export function disableWorkflow(draft: TemplateV2Draft): TemplateV2Draft {
  const next = cloneDraft(draft);
  delete next.workflow;
  delete next.computeBlocks;
  return next;
}

/**
 * @param id block id
 */
export function defaultComputeBlock(id: string): ComputeBlockDraft {
  return {
    id,
    sources: {
      seed: {
        type: 'iterator',
        iterator: { type: 'number', from: 1, to: 3, step: 1 },
      },
    },
    transformers: [{ type: 'sql', sql: 'SELECT value FROM seed' }],
  };
}

/**
 * @param draft template draft
 * @param steps replacement step list
 */
export function setWorkflowSteps(draft: TemplateV2Draft, steps: WorkflowStepDraft[]): TemplateV2Draft {
  const next = cloneDraft(draft);
  const workflow: WorkflowSpecDraft = { ...(next.workflow ?? {}), steps };
  next.workflow = workflow;
  return next;
}

/**
 * @param draft template draft
 * @param index step index
 * @param patch merged step fields
 */
export function applyWorkflowStepAt(
  draft: TemplateV2Draft,
  index: number,
  patch: WorkflowStepDraft,
): TemplateV2Draft {
  const steps = listWorkflowSteps(draft);
  const nextSteps = [...steps];
  while (nextSteps.length <= index) {
    nextSteps.push(defaultStepForType('log', nextSteps.length));
  }
  nextSteps[index] = { ...nextSteps[index], ...patch };
  return setWorkflowSteps(draft, nextSteps);
}

/**
 * @param draft template draft
 * @param type new step type
 */
export function addWorkflowStep(draft: TemplateV2Draft, type: WorkflowStepType = 'log'): TemplateV2Draft {
  const steps = listWorkflowSteps(draft);
  return setWorkflowSteps(draft, [...steps, defaultStepForType(type, steps.length)]);
}

/**
 * @param draft template draft
 * @param index step index to remove
 */
export function removeWorkflowStepAt(draft: TemplateV2Draft, index: number): TemplateV2Draft {
  const steps = listWorkflowSteps(draft).filter((_, i) => i !== index);
  return setWorkflowSteps(draft, steps);
}

/**
 * @param draft template draft
 * @param blocks replacement block list
 */
export function setComputeBlocks(draft: TemplateV2Draft, blocks: ComputeBlockDraft[]): TemplateV2Draft {
  const next = cloneDraft(draft);
  next.computeBlocks = blocks;
  return next;
}

/**
 * @param draft template draft
 * @param index block index
 * @param patch merged block fields
 */
export function applyComputeBlockAt(
  draft: TemplateV2Draft,
  index: number,
  patch: ComputeBlockDraft,
): TemplateV2Draft {
  const blocks = listComputeBlocks(draft);
  const nextBlocks = [...blocks];
  while (nextBlocks.length <= index) {
    nextBlocks.push(defaultComputeBlock(`block-${nextBlocks.length + 1}`));
  }
  nextBlocks[index] = { ...nextBlocks[index], ...patch };
  return setComputeBlocks(draft, nextBlocks);
}

/**
 * @param draft template draft
 */
export function addComputeBlock(draft: TemplateV2Draft): TemplateV2Draft {
  const blocks = listComputeBlocks(draft);
  return setComputeBlocks(draft, [...blocks, defaultComputeBlock(`block-${blocks.length + 1}`)]);
}

/**
 * @param draft template draft
 * @param index block index to remove
 */
export function removeComputeBlockAt(draft: TemplateV2Draft, index: number): TemplateV2Draft {
  return setComputeBlocks(
    draft,
    listComputeBlocks(draft).filter((_, i) => i !== index),
  );
}

/**
 * @param block compute block
 */
export function blockUsesTransformGraph(block: ComputeBlockDraft): boolean {
  return (block.transformGraph?.nodes?.length ?? 0) > 0;
}

/**
 * @param block compute block
 */
export function listTransformGraphNodes(block: ComputeBlockDraft): TransformNodeDraft[] {
  return block.transformGraph?.nodes ? [...block.transformGraph.nodes] : [];
}

/**
 * @param graph transform graph
 * @param nodeId target node id
 */
export function readDependsOn(graph: TransformGraphDraft | undefined, nodeId: string): string[] {
  if (!graph?.edges) {
    return [];
  }
  return graph.edges
    .filter((edge) => edge.toNodeId === nodeId && edge.toPort === 'in')
    .map((edge) => edge.fromNodeId ?? '')
    .filter(Boolean);
}

/**
 * @param graph transform graph
 * @param nodeId target node id
 * @param dependsOn upstream node ids
 */
export function applyDependsOn(
  graph: TransformGraphDraft,
  nodeId: string,
  dependsOn: string[],
): TransformGraphDraft {
  const edges = (graph.edges ?? []).filter(
    (edge) => !(edge.toNodeId === nodeId && edge.toPort === 'in'),
  );
  for (const fromNodeId of dependsOn) {
    if (fromNodeId && fromNodeId !== nodeId) {
      edges.push({ fromNodeId, fromPort: 'out', toNodeId: nodeId, toPort: 'in' });
    }
  }
  return { ...graph, edges };
}

/**
 * @param block compute block
 * @param graph replacement graph
 */
export function applyTransformGraph(block: ComputeBlockDraft, graph: TransformGraphDraft): ComputeBlockDraft {
  return { ...block, transformGraph: graph, transformers: undefined };
}

/**
 * @param block compute block
 */
export function enableTransformGraph(block: ComputeBlockDraft): ComputeBlockDraft {
  const graph: TransformGraphDraft = {
    transforms: {
      'step-1': { type: 'sql', name: 'step-1', sql: 'SELECT value FROM seed' },
    },
    nodes: [{ id: 'n1', transformId: 'step-1', outputAlias: 'output' }],
    edges: [],
  };
  return applyTransformGraph(block, graph);
}

/**
 * @param block compute block
 */
export function disableTransformGraph(block: ComputeBlockDraft): ComputeBlockDraft {
  const transformers = block.transformers?.length
    ? [...block.transformers]
    : [{ type: 'sql', sql: 'SELECT value FROM seed' }];
  return { ...block, transformGraph: undefined, transformers };
}

/**
 * @param block compute block
 * @param nodes replacement node list
 */
export function setTransformGraphNodes(
  block: ComputeBlockDraft,
  nodes: TransformNodeDraft[],
): ComputeBlockDraft {
  const graph: TransformGraphDraft = { ...(block.transformGraph ?? {}), nodes };
  return applyTransformGraph(block, graph);
}

/**
 * @param block compute block
 * @param index node index
 * @param patch merged node fields
 */
export function applyTransformGraphNodeAt(
  block: ComputeBlockDraft,
  index: number,
  patch: TransformNodeDraft,
): ComputeBlockDraft {
  const nodes = listTransformGraphNodes(block);
  const nextNodes = [...nodes];
  while (nextNodes.length <= index) {
    nextNodes.push({ id: `n${nextNodes.length + 1}`, transformId: '', outputAlias: 'output' });
  }
  nextNodes[index] = { ...nextNodes[index], ...patch };
  return setTransformGraphNodes(block, nextNodes);
}

/**
 * @param block compute block
 */
export function addTransformGraphNode(block: ComputeBlockDraft): ComputeBlockDraft {
  const nodes = listTransformGraphNodes(block);
  const nodeId = `n${nodes.length + 1}`;
  const transformId = `step-${nodes.length + 1}`;
  const graph = block.transformGraph ?? { transforms: {}, nodes: [], edges: [] };
  const transforms = { ...(graph.transforms ?? {}), [transformId]: { type: 'sql', sql: 'SELECT * FROM input' } };
  return applyTransformGraph(block, {
    ...graph,
    transforms,
    nodes: [...nodes, { id: nodeId, transformId, outputAlias: 'output' }],
  });
}

/**
 * @param block compute block
 * @param index node index to remove
 */
export function removeTransformGraphNodeAt(block: ComputeBlockDraft, index: number): ComputeBlockDraft {
  const nodes = listTransformGraphNodes(block);
  const removed = nodes[index];
  if (!removed?.id) {
    return setTransformGraphNodes(
      block,
      nodes.filter((_, i) => i !== index),
    );
  }
  const graph = block.transformGraph ?? {};
  const nextNodes = nodes.filter((_, i) => i !== index);
  const nextEdges = (graph.edges ?? []).filter(
    (edge) => edge.fromNodeId !== removed.id && edge.toNodeId !== removed.id,
  );
  return applyTransformGraph(block, { ...graph, nodes: nextNodes, edges: nextEdges });
}

/**
 * @param block compute block
 * @param nodeIndex node index
 * @param dependsOn upstream node ids
 */
export function applyNodeDependsOnAt(
  block: ComputeBlockDraft,
  nodeIndex: number,
  dependsOn: string[],
): ComputeBlockDraft {
  const nodes = listTransformGraphNodes(block);
  const node = nodes[nodeIndex];
  if (!node?.id) {
    return block;
  }
  const graph = block.transformGraph ?? { nodes, edges: [] };
  return applyTransformGraph(block, applyDependsOn(graph, node.id, dependsOn));
}

/**
 * @param block compute block
 * @param transformId transform definition id
 */
export function readGraphTransform(
  block: ComputeBlockDraft,
  transformId: string | undefined,
): TransformDraft | undefined {
  if (!transformId) {
    return undefined;
  }
  return block.transformGraph?.transforms?.[transformId];
}

/**
 * @param block compute block
 * @param transformId transform definition id
 */
export function readGraphTransformType(block: ComputeBlockDraft, transformId: string | undefined): TransformKind {
  return inferTransformType(readGraphTransform(block, transformId));
}

/**
 * @param block compute block
 * @param transformId transform definition id
 * @param patch merged transform fields
 */
export function applyGraphTransformAt(
  block: ComputeBlockDraft,
  transformId: string,
  patch: TransformDraft,
): ComputeBlockDraft {
  const graph = block.transformGraph ?? { transforms: {}, nodes: [], edges: [] };
  const existing = graph.transforms?.[transformId] ?? { type: 'sql', name: transformId };
  const transforms = {
    ...(graph.transforms ?? {}),
    [transformId]: { ...existing, ...patch, name: existing.name ?? transformId },
  };
  return applyTransformGraph(block, { ...graph, transforms });
}

/**
 * @param block compute block
 * @param transformId transform definition id
 */
export function readTransformScript(block: ComputeBlockDraft, transformId: string | undefined): string {
  const transform = readGraphTransform(block, transformId);
  return typeof transform?.script === 'string' ? transform.script : '';
}

/**
 * @param block compute block
 * @param transformId transform definition id
 * @param script replacement JavaScript
 */
export function applyTransformScriptAt(
  block: ComputeBlockDraft,
  transformId: string,
  script: string,
): ComputeBlockDraft {
  return applyGraphTransformAt(block, transformId, { type: 'js', script });
}

/**
 * @param block compute block
 * @param transformId transform definition id
 */
export function readTransformTimeoutMs(
  block: ComputeBlockDraft,
  transformId: string | undefined,
): number | undefined {
  const transform = readGraphTransform(block, transformId);
  return typeof transform?.timeoutMs === 'number' ? transform.timeoutMs : undefined;
}

/**
 * @param block compute block
 * @param transformId transform definition id
 * @param timeoutMs optional per-row timeout
 */
export function applyTransformTimeoutMsAt(
  block: ComputeBlockDraft,
  transformId: string,
  timeoutMs: number | undefined,
): ComputeBlockDraft {
  return applyGraphTransformAt(block, transformId, { type: 'js', timeoutMs });
}

/**
 * @param block compute block
 * @param transformId transform definition id
 * @param type transform kind
 */
export function applyGraphTransformTypeAt(
  block: ComputeBlockDraft,
  transformId: string,
  type: TransformKind,
): ComputeBlockDraft {
  const existing = readGraphTransform(block, transformId);
  if (type === 'js') {
    return applyGraphTransformAt(block, transformId, {
      type: 'js',
      script: typeof existing?.script === 'string' ? existing.script : 'row.value = row.value',
      timeoutMs: typeof existing?.timeoutMs === 'number' ? existing.timeoutMs : undefined,
    });
  }
  if (type === 'spel') {
    return applyGraphTransformAt(block, transformId, {
      type: 'spel',
      columns: existing?.columns?.length
        ? existing.columns
        : [{ name: 'value', expression: '#input' }],
    });
  }
  return applyGraphTransformAt(block, transformId, {
    type: 'sql',
    sql: typeof existing?.sql === 'string' ? existing.sql : 'SELECT * FROM input',
  });
}

/**
 * @param block compute block
 * @param transformId transform definition id
 */
export function readTransformSql(block: ComputeBlockDraft, transformId: string | undefined): string {
  if (!transformId) {
    return '';
  }
  const transform = block.transformGraph?.transforms?.[transformId];
  return typeof transform?.sql === 'string' ? transform.sql : '';
}

/**
 * @param block compute block
 * @param transformId transform definition id
 * @param sql replacement SQL
 */
export function applyTransformSqlAt(
  block: ComputeBlockDraft,
  transformId: string,
  sql: string,
): ComputeBlockDraft {
  const graph = block.transformGraph ?? { transforms: {}, nodes: [], edges: [] };
  const existing = graph.transforms?.[transformId] ?? { type: 'sql', name: transformId };
  const transforms = {
    ...(graph.transforms ?? {}),
    [transformId]: { ...existing, type: 'sql', sql },
  };
  return applyTransformGraph(block, { ...graph, transforms });
}

/**
 * Returns a cycle path when the block's transform DAG contains a cycle.
 *
 * @param block compute block
 */
export function findTransformGraphCyclePath(block: ComputeBlockDraft): string[] {
  const graph = block.transformGraph;
  if (!graph?.nodes?.length) {
    return [];
  }
  const outgoing = new Map<string, string[]>();
  for (const node of graph.nodes) {
    if (node.id) {
      outgoing.set(node.id, []);
    }
  }
  for (const edge of graph.edges ?? []) {
    if (edge.fromNodeId && edge.toNodeId) {
      const next = outgoing.get(edge.fromNodeId) ?? [];
      next.push(edge.toNodeId);
      outgoing.set(edge.fromNodeId, next);
    }
  }

  const state = new Map<string, 0 | 1 | 2>();
  const stack: string[] = [];

  const dfs = (nodeId: string): string[] => {
    const visited = state.get(nodeId);
    if (visited === 1) {
      const start = stack.indexOf(nodeId);
      return [...stack.slice(start), nodeId];
    }
    if (visited === 2) {
      return [];
    }
    state.set(nodeId, 1);
    stack.push(nodeId);
    for (const next of outgoing.get(nodeId) ?? []) {
      const cycle = dfs(next);
      if (cycle.length > 0) {
        return cycle;
      }
    }
    stack.pop();
    state.set(nodeId, 2);
    return [];
  };

  for (const nodeId of outgoing.keys()) {
    const cycle = dfs(nodeId);
    if (cycle.length > 0) {
      return cycle;
    }
  }
  return [];
}

/**
 * @param block compute block
 */
export function hasTransformGraphCycle(block: ComputeBlockDraft): boolean {
  return findTransformGraphCyclePath(block).length > 0;
}

/**
 * Maps a compute block to a linear draft slice for SourcesStep / TransformStep / SinksStep.
 *
 * @param block compute block
 */
export function computeBlockToScopedDraft(block: ComputeBlockDraft): TemplateV2Draft {
  const sink: SinkDraft = block.sinks?.[0] ?? { writers: [{ type: 'console' }] };
  return {
    sources: block.sources ?? {},
    transform: block.transformers?.[0],
    transformers: block.transformers,
    sink,
  };
}

/**
 * Merges scoped draft edits back into a compute block.
 *
 * @param block compute block
 * @param scoped scoped draft slice
 */
export function scopedDraftToComputeBlock(
  block: ComputeBlockDraft,
  scoped: TemplateV2Draft,
): ComputeBlockDraft {
  const writers = scoped.sink?.writers ?? [{ type: 'console' }];
  const transformers =
    scoped.transformers && scoped.transformers.length > 0
      ? [...scoped.transformers]
      : scoped.transform
        ? [scoped.transform]
        : block.transformers;
  return {
    ...block,
    sources: scoped.sources ?? block.sources,
    transformers: blockUsesTransformGraph(block) ? block.transformers : transformers,
    sinks: [{ writers }],
    transformGraph: block.transformGraph,
  };
}
