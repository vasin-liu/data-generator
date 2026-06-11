/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template;

import org.gensokyo.data.calcite.runtime.EffectiveExecutionPolicy;
import org.gensokyo.data.calcite.runtime.TemplateV2RunResult;
import org.gensokyo.data.calcite.runtime.TemplateV2Runner;
import org.gensokyo.data.calcite.runtime.TransformGraphPreviewSupport;
import org.gensokyo.data.model.v2.TransformEdgeVO;
import org.gensokyo.data.model.v2.TransformGraphVO;
import org.gensokyo.data.model.v2.TransformNodeVO;
import org.gensokyo.data.model.v2.workflow.ComputeBlockVO;
import org.gensokyo.data.model.v2.workflow.WorkflowSpecVO;
import org.gensokyo.data.calcite.sql.ExecutionShape;
import org.gensokyo.data.calcite.sql.ExecutionShapeClassifier;
import org.gensokyo.data.config.DataGeneratorProperties;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.v2.ExecutionPolicyVO;
import org.gensokyo.data.model.v2.Row;
import org.gensokyo.data.model.v2.RowSchema;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.v2.TransformVO;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.yaml.YamlParser;
import org.gensokyo.kit.character.StrKit;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Orchestrates Template V2 control-plane operations: validate, explain, and preview.
 *
 * @author Gensokyo
 * @since 2026-05-21
 */
public class TemplateV2ControlPlaneService {

    private final TemplateRepository repository;
    private final YamlParser yamlParser;
    private final TemplateV2DefinitionResolver definitionResolver;
    private final TemplateV2PlanExplainService planExplainService;
    private final TemplateV2Runner templateV2Runner;
    private final DataGeneratorProperties properties;

    /**
     * Creates the control-plane service with persistence and V2 resolution helpers.
     *
     * @param repository              template persistence
     * @param yamlParser              YAML parser for definition probes
     * @param definitionResolver      persisted V2 definition resolver
     * @param planExplainService      Calcite plan summary builder
     * @param templateV2Runner        V2 runtime runner for bounded preview
     * @param properties              service properties (default preview row cap)
     */
    public TemplateV2ControlPlaneService(
            TemplateRepository repository,
            YamlParser yamlParser,
            TemplateV2DefinitionResolver definitionResolver,
            TemplateV2PlanExplainService planExplainService,
            TemplateV2Runner templateV2Runner,
            DataGeneratorProperties properties) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.yamlParser = Objects.requireNonNull(yamlParser, "yamlParser");
        this.definitionResolver = Objects.requireNonNull(definitionResolver, "definitionResolver");
        this.planExplainService = Objects.requireNonNull(planExplainService, "planExplainService");
        this.templateV2Runner = Objects.requireNonNull(templateV2Runner, "templateV2Runner");
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    /**
     * Validates a Template V2 draft: normalizes singular/plural fields, runs structural validation,
     * and attaches execution-shape warnings when CHUNKED policy applies.
     *
     * @param draft operator draft; may use singular {@code transform} / {@code sink}
     * @return validation outcome with errors and optional warnings; never {@code null}
     */
    public TemplateV2ValidationResult validate(TemplateV2DraftVO draft) {
        if (draft == null) {
            return TemplateV2ValidationResult.invalid("Template V2 draft must not be null");
        }

        var errors = new ArrayList<String>();
        var warnings = new ArrayList<String>();

        TemplateV2VO normalized;
        try {
            normalized = TemplateV2Normalizer.normalize(draft);
        }
        catch (IllegalArgumentException e) {
            errors.add(e.getMessage());
            return TemplateV2ValidationResult.from(errors, warnings);
        }

        if (normalized == null) {
            errors.add("Template V2 must not be null");
            return TemplateV2ValidationResult.from(errors, warnings);
        }

        try {
            TemplateV2Validator.validate(normalized);
        }
        catch (IllegalArgumentException e) {
            errors.add(e.getMessage());
            return TemplateV2ValidationResult.from(errors, warnings);
        }

        appendChunkedExecutionShapeWarnings(normalized, warnings);
        warnings.addAll(TemplateV2Validator.collectWarnings(normalized));
        return TemplateV2ValidationResult.from(errors, warnings);
    }

    /**
     * Loads a persisted Template V2 definition and returns a bounded plan explain block.
     *
     * @param templateId persisted template id
     * @return plan explain block; never {@code null}
     * @throws IllegalArgumentException when the template is missing, content is empty, or not V2
     */
    public TemplateV2PlanExplain explain(Long templateId) {
        Objects.requireNonNull(templateId, "templateId");
        TemplatePO entity = repository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Template '%s' does not exist", templateId)));
        if (StrKit.isBlank(entity.getContentYaml())) {
            throw new IllegalArgumentException(String.format("Template '%s' has empty content", templateId));
        }

        TemplateV2VO v2 = definitionResolver.resolve(entity);
        TemplateVO v1Probe = tryParse(entity.getContentYaml(), TemplateVO.class);
        return planExplainService.explain(v1Probe, v2);
    }

    /**
     * Runs a bounded in-memory preview for a persisted template.
     *
     * @param templateId persisted template id
     * @param maxRows    optional row cap; when {@code null} or non-positive, uses {@link DataGeneratorProperties#getPreviewMaxRows()}
     * @return preview schema, truncated rows, and warnings; never {@code null}
     * @throws IllegalArgumentException when the template is missing, content is empty, execution mode is not
     *                                  {@code IN_MEMORY}, or V2 cannot be resolved
     */
    public TemplateV2PreviewDTO preview(Long templateId, Integer maxRows) {
        return preview(templateId, maxRows, null, null, null);
    }

    /**
     * Runs a bounded in-memory preview for a persisted template, optionally stopping after a transformer step.
     *
     * @param templateId            persisted template id
     * @param maxRows                 optional row cap; when {@code null} or non-positive, uses {@link DataGeneratorProperties#getPreviewMaxRows()}
     * @param throughTransformIndex   optional 0-based inclusive transformer index; when {@code null}, runs the full chain
     * @return preview schema, truncated rows, and warnings; never {@code null}
     * @throws IllegalArgumentException when the template is missing, content is empty, execution mode is not
     *                                  {@code IN_MEMORY}, V2 cannot be resolved, or the transform index is out of range
     */
    public TemplateV2PreviewDTO preview(Long templateId, Integer maxRows, Integer throughTransformIndex) {
        return preview(templateId, maxRows, throughTransformIndex, null, null);
    }

    /**
     * Runs a bounded in-memory preview, optionally stopping after a linear transformer step or a DAG node.
     *
     * @param templateId              persisted template id
     * @param maxRows                   optional row cap
     * @param throughTransformIndex     optional linear transformer index (mutually exclusive with node id)
     * @param computeBlockId            optional compute block for DAG staged preview
     * @param throughTransformNodeId    optional DAG node id inclusive cutoff
     * @return preview schema, truncated rows, and warnings
     */
    public TemplateV2PreviewDTO preview(
            Long templateId,
            Integer maxRows,
            Integer throughTransformIndex,
            String computeBlockId,
            String throughTransformNodeId) {
        Objects.requireNonNull(templateId, "templateId");
        int rowCap = resolvePreviewRowCap(maxRows);

        TemplatePO entity = repository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Template '%s' does not exist", templateId)));
        if (StrKit.isBlank(entity.getContentYaml())) {
            throw new IllegalArgumentException(String.format("Template '%s' has empty content", templateId));
        }

        TemplateV2VO v2 = definitionResolver.resolve(entity);
        var warnings = new ArrayList<String>();

        EffectiveExecutionPolicy effective = EffectiveExecutionPolicy.resolve(v2.getExecutionPolicy());
        String mode = effective.mode();
        if ("CHUNKED".equals(mode) || "STREAMING".equals(mode)) {
            throw new IllegalArgumentException(
                    "Preview supports IN_MEMORY execution only; template mode is " + mode);
        }

        TemplateV2VO runnable = prepareForPreview(
                v2, throughTransformIndex, computeBlockId, throughTransformNodeId);
        TemplateV2RunResult result = templateV2Runner.run(runnable);

        RowSchema schema = result.getSchema();
        List<Row> rows = result.getRows() == null ? List.of() : result.getRows();
        if (rows.size() > rowCap) {
            rows = List.copyOf(rows.subList(0, rowCap));
            warnings.add("Preview truncated result rows to " + rowCap);
        }

        return new TemplateV2PreviewDTO(schema, rows, warnings);
    }

    private int resolvePreviewRowCap(Integer maxRows) {
        if (maxRows != null && maxRows > 0) {
            return maxRows;
        }
        Integer configured = properties.getPreviewMaxRows();
        if (configured != null && configured > 0) {
            return configured;
        }
        return 100;
    }

    private static TemplateV2VO prepareForPreview(
            TemplateV2VO template,
            Integer throughTransformIndex,
            String computeBlockId,
            String throughTransformNodeId) {
        TemplateV2VO copy = copyTemplate(template);
        ExecutionPolicyVO policy = copy.getExecutionPolicy();
        if (policy == null) {
            policy = new ExecutionPolicyVO();
            copy.setExecutionPolicy(policy);
        }
        // Preview materializes rows in memory regardless of stored policy hints.
        policy.setMode("IN_MEMORY");
        boolean hasNodeCutoff = throughTransformNodeId != null && !throughTransformNodeId.isBlank();
        if (hasNodeCutoff && throughTransformIndex != null) {
            throw new IllegalArgumentException(
                    "Use either throughTransformIndex or throughTransformNodeId for staged preview, not both");
        }
        if (hasNodeCutoff) {
            applyThroughTransformNode(copy, computeBlockId, throughTransformNodeId);
        } else if (throughTransformIndex != null) {
            applyThroughTransformIndex(copy, throughTransformIndex);
        }
        return copy;
    }

    private static void applyThroughTransformNode(
            TemplateV2VO template, String computeBlockId, String throughTransformNodeId) {
        ComputeBlockVO block = resolveComputeBlockForPreview(template, computeBlockId);
        if (block.getTransformGraph() == null || block.getTransformGraph().getNodes() == null
                || block.getTransformGraph().getNodes().isEmpty()) {
            throw new IllegalArgumentException("Compute block '" + block.getId() + "' has no transformGraph to preview");
        }
        block.setTransformGraph(
                TransformGraphPreviewSupport.truncateThroughNode(block.getTransformGraph(), throughTransformNodeId));
    }

    private static ComputeBlockVO resolveComputeBlockForPreview(TemplateV2VO template, String computeBlockId) {
        List<ComputeBlockVO> blocks = template.getComputeBlocks();
        if (blocks == null || blocks.isEmpty()) {
            throw new IllegalArgumentException("Template has no compute blocks for DAG staged preview");
        }
        if (computeBlockId != null && !computeBlockId.isBlank()) {
            for (ComputeBlockVO block : blocks) {
                if (computeBlockId.equals(block.getId())) {
                    return block;
                }
            }
            throw new IllegalArgumentException("Compute block not found: " + computeBlockId);
        }
        for (ComputeBlockVO block : blocks) {
            if (block.getTransformGraph() != null
                    && block.getTransformGraph().getNodes() != null
                    && !block.getTransformGraph().getNodes().isEmpty()) {
                return block;
            }
        }
        throw new IllegalArgumentException("No compute block with transformGraph found");
    }

    private static void applyThroughTransformIndex(TemplateV2VO template, int throughTransformIndex) {
        List<TransformVO> transformers = template.getTransformers();
        if (transformers == null || transformers.isEmpty()) {
            throw new IllegalArgumentException("Template has no transformers to preview through");
        }
        if (throughTransformIndex < 0 || throughTransformIndex >= transformers.size()) {
            throw new IllegalArgumentException(String.format(
                    "throughTransformIndex must be between 0 and %d inclusive",
                    transformers.size() - 1));
        }
        // Staged preview materializes sources, then applies transformers up to the requested step only.
        template.setTransformers(new ArrayList<>(transformers.subList(0, throughTransformIndex + 1)));
    }

    private static TemplateV2VO copyTemplate(TemplateV2VO template) {
        TemplateV2VO copy = new TemplateV2VO();
        copy.setId(template.getId());
        copy.setInstanceId(template.getInstanceId());
        copy.setName(template.getName());
        copy.setGenerator(template.getGenerator());
        copy.setExecutionPolicy(template.getExecutionPolicy());
        copy.setSinkExecutionPolicy(template.getSinkExecutionPolicy());
        if (template.getSources() != null) {
            copy.setSources(new java.util.LinkedHashMap<>(template.getSources()));
        }
        copy.setTransformers(new ArrayList<>(template.getTransformers()));
        copy.setSinks(new ArrayList<>(template.getSinks()));
        copy.setWorkflow(copyWorkflow(template.getWorkflow()));
        copy.setComputeBlocks(copyComputeBlocks(template.getComputeBlocks()));
        return copy;
    }

    private static WorkflowSpecVO copyWorkflow(WorkflowSpecVO workflow) {
        if (workflow == null) {
            return null;
        }
        WorkflowSpecVO copy = new WorkflowSpecVO();
        copy.setSteps(workflow.getSteps() != null ? new ArrayList<>(workflow.getSteps()) : null);
        return copy;
    }

    private static List<ComputeBlockVO> copyComputeBlocks(List<ComputeBlockVO> blocks) {
        if (blocks == null) {
            return new ArrayList<>();
        }
        List<ComputeBlockVO> copies = new ArrayList<>(blocks.size());
        for (ComputeBlockVO block : blocks) {
            if (block == null) {
                continue;
            }
            ComputeBlockVO copy = new ComputeBlockVO();
            copy.setId(block.getId());
            copy.setSharedScopeId(block.getSharedScopeId());
            if (block.getSources() != null) {
                copy.setSources(new java.util.LinkedHashMap<>(block.getSources()));
            }
            copy.setTransformers(block.getTransformers() != null ? new ArrayList<>(block.getTransformers()) : null);
            copy.setTransformGraph(copyTransformGraph(block.getTransformGraph()));
            copy.setSinks(block.getSinks() != null ? new ArrayList<>(block.getSinks()) : null);
            copies.add(copy);
        }
        return copies;
    }

    private static TransformGraphVO copyTransformGraph(TransformGraphVO graph) {
        if (graph == null) {
            return null;
        }
        TransformGraphVO copy = new TransformGraphVO();
        if (graph.getTransforms() != null) {
            copy.setTransforms(new java.util.LinkedHashMap<>(graph.getTransforms()));
        }
        if (graph.getNodes() != null) {
            List<TransformNodeVO> nodes = new ArrayList<>(graph.getNodes().size());
            for (TransformNodeVO node : graph.getNodes()) {
                if (node == null) {
                    continue;
                }
                TransformNodeVO nodeCopy = new TransformNodeVO();
                nodeCopy.setId(node.getId());
                nodeCopy.setTransformId(node.getTransformId());
                nodeCopy.setOutputAlias(node.getOutputAlias());
                nodes.add(nodeCopy);
            }
            copy.setNodes(nodes);
        }
        if (graph.getEdges() != null) {
            List<TransformEdgeVO> edges = new ArrayList<>(graph.getEdges().size());
            for (TransformEdgeVO edge : graph.getEdges()) {
                if (edge == null) {
                    continue;
                }
                TransformEdgeVO edgeCopy = new TransformEdgeVO();
                edgeCopy.setFromNodeId(edge.getFromNodeId());
                edgeCopy.setFromPort(edge.getFromPort());
                edgeCopy.setToNodeId(edge.getToNodeId());
                edgeCopy.setToPort(edge.getToPort());
                edges.add(edgeCopy);
            }
            copy.setEdges(edges);
        }
        return copy;
    }

    private <T> T tryParse(String yaml, Class<T> clazz) {
        try {
            return yamlParser.parse(yaml, clazz);
        }
        catch (Exception ignored) {
            return null;
        }
    }

    private static void appendChunkedExecutionShapeWarnings(TemplateV2VO normalized, List<String> warnings) {
        ExecutionPolicyVO policy = normalized.getExecutionPolicy();
        if (policy == null || StrKit.isBlank(policy.getMode())) {
            return;
        }
        if (!"CHUNKED".equals(policy.getMode().trim().toUpperCase(Locale.ROOT))) {
            return;
        }

        EffectiveExecutionPolicy effective = EffectiveExecutionPolicy.resolve(policy);
        try {
            ExecutionShape shape = ExecutionShapeClassifier.classify(normalized);
            if (shape == ExecutionShape.MATERIALIZATION_REQUIRED) {
                warnings.add(
                        "CHUNKED mode requested but SQL shape is MATERIALIZATION_REQUIRED — runtime may fall back or approximate");
            }
            if (shape == ExecutionShape.ROW_LOCAL) {
                warnings.add("CHUNKED row-local shape — suitable for large JDBC export path");
            }
            if (shape == ExecutionShape.BROADCAST_JOIN) {
                warnings.add("Broadcast join shape — verify dimension source maxRows/broadcastMaxRows ("
                        + effective.broadcastMaxRows()
                        + ")");
            }
        }
        catch (RuntimeException e) {
            warnings.add("Execution shape classification failed: " + e.getMessage());
        }
    }
}
