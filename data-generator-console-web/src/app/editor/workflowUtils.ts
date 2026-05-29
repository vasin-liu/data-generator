import type {
  ComputeBlockDraft,
  SinkDraft,
  TemplateV2Draft,
  TransformGraphDraft,
  TransformNodeDraft,
  WorkflowSpecDraft,
  WorkflowStepDraft,
} from '../../api/types';
import { cloneDraft } from './draftUtils';

/** Supported workflow step types in the minimal editor. */
export const WORKFLOW_STEP_TYPES = [
  'log',
  'pause',
  'invoke_compute_block',
  'branch',
  'shared_scope',
] as const;

export type WorkflowStepType = (typeof WORKFLOW_STEP_TYPES)[number];

const RESERVED_STEP_KEYS = new Set(['id', 'name', 'type', 'computeBlockId']);

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
