/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import org.gensokyo.data.iterator.DatabaseIteratorVO;
import org.gensokyo.data.model.v2.IteratorSourceVO;
import org.gensokyo.data.model.v2.SinkExecutionPolicyVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.vo.FieldVO;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.model.vo.iterator.IteratorVO;
import org.gensokyo.data.model.vo.reader.ReaderVO;
import org.gensokyo.data.model.vo.stage.ReadStageVO;
import org.gensokyo.data.model.vo.stage.StageVO;
import org.gensokyo.data.reader.JdbcReaderVO;
import org.gensokyo.kit.collect.CollectKit;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Converts simple V1 iterator templates (number/constant) into a minimal V2 SQL draft.
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
public final class V1IteratorDraftConverter {

    private static final String INPUT_SOURCE = "input";
    private static final Set<String> SUPPORTED_ITERATOR_TYPES = Set.of("number", "constant");

    private V1IteratorDraftConverter() {
    }

    /**
     * Returns whether this template can be migrated via the iterator draft path (Wave 1).
     *
     * @param template V1 template
     * @return {@code true} when a number/constant iterator is present without JDBC readers
     */
    public static boolean supports(TemplateVO template) {
        if (template == null || template.getIterator() == null) {
            return false;
        }
        if (template.getIterator() instanceof DatabaseIteratorVO) {
            return false;
        }
        if (hasJdbcReader(template)) {
            return false;
        }
        return isSupportedIteratorType(template.getIterator());
    }

    /**
     * Builds a V2 draft with {@link IteratorSourceVO}, simple SQL transform, and console sink.
     *
     * @param template V1 template (must {@link #supports} this converter)
     * @return V2 draft, or {@code null} when {@code template} is null
     * @throws IllegalArgumentException when the template shape is not supported
     */
    public static TemplateV2DraftVO convert(TemplateVO template) {
        if (template == null) {
            return null;
        }
        if (!supports(template)) {
            throw new IllegalArgumentException(
                    "Template cannot be converted with iterator draft migration (supported: number/constant iterator without JDBC readers)");
        }

        TemplateV2DraftVO draft = new TemplateV2DraftVO();
        draft.setId(template.getId());
        draft.setInstanceId(template.getInstanceId());
        draft.setName(template.getName());
        draft.setGenerator(template.getGenerator());

        IteratorSourceVO source = new IteratorSourceVO();
        source.setIterator(template.getIterator());
        Map<String, org.gensokyo.data.model.v2.SourceVO> sources = new LinkedHashMap<>();
        sources.put(INPUT_SOURCE, source);
        draft.setSources(sources);

        SqlTransformVO transform = new SqlTransformVO();
        transform.setSql("SELECT * FROM " + INPUT_SOURCE);
        draft.setTransform(transform);

        if (template.getOutput() != null) {
            draft.setSink(template.getOutput());
        }

        SinkExecutionPolicyVO sinkExecutionPolicy = new SinkExecutionPolicyVO();
        sinkExecutionPolicy.setMode("FAIL_FAST");
        draft.setSinkExecutionPolicy(sinkExecutionPolicy);
        return draft;
    }

    private static boolean isSupportedIteratorType(IteratorVO iterator) {
        String type = iterator.getType();
        if (type == null || type.isBlank()) {
            // V1 default iterator implementation is number when type is omitted
            return true;
        }
        return SUPPORTED_ITERATOR_TYPES.contains(type.trim().toLowerCase(Locale.ROOT));
    }

    private static boolean hasJdbcReader(TemplateVO template) {
        if (CollectKit.isEmpty(template.getFields())) {
            return false;
        }
        for (FieldVO field : template.getFields()) {
            if (field == null || CollectKit.isEmpty(field.getStages())) {
                continue;
            }
            for (StageVO stage : field.getStages()) {
                if (!(stage instanceof ReadStageVO readStage) || CollectKit.isEmpty(readStage.getReaders())) {
                    continue;
                }
                for (ReaderVO reader : readStage.getReaders()) {
                    if (reader instanceof JdbcReaderVO) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
