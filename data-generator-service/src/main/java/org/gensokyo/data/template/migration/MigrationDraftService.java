/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.migration;

import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.v2.SpelTransformVO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.vo.TemplateVO;
import org.gensokyo.data.template.querysource.V1QuerySourceDraftConverter;
import org.gensokyo.data.template.querysource.V1QuerySourceExtractor;
import org.gensokyo.kit.character.StrKit;
import org.gensokyo.kit.collect.CollectKit;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

/**
 * Unified V1 → V2 draft builder (query-source JDBC path or simple iterator path).
 *
 * @author Gensokyo
 * @since 2026-05-19
 */
public class MigrationDraftService {

    /**
     * Builds a V2 migration draft from a V1 template (query sources preferred over iterator).
     *
     * @param v1 V1 template definition
     * @return draft suitable for preview, compare, or promote
     * @throws IllegalArgumentException when no supported migration path exists
     */
    public TemplateV2DraftVO buildDraft(TemplateVO v1) {
        Objects.requireNonNull(v1, "v1");
        TemplateV2DraftVO draft;
        Map<String, QuerySourceVO> querySources = V1QuerySourceExtractor.extract(v1);
        if (CollectKit.isNotEmpty(querySources)) {
            draft = V1QuerySourceDraftConverter.convert(v1);
        }
        else if (V1IteratorDraftConverter.supports(v1)) {
            draft = V1IteratorDraftConverter.convert(v1);
        }
        else {
            throw new IllegalArgumentException(
                    "Template has no database-backed sources or supported iterator for V2 draft migration");
        }
        attachSpelTransformIfPresent(v1, draft);
        return draft;
    }

    private static void attachSpelTransformIfPresent(TemplateVO v1, TemplateV2DraftVO draft) {
        SpelTransformVO spel = V1ScriptToSpelDraftConverter.convert(v1);
        if (spel == null || CollectKit.isEmpty(spel.getColumns())) {
            return;
        }
        if (draft.getTransformers() == null) {
            draft.setTransformers(new ArrayList<>());
        }
        // Normalizer rejects draft when both singular transform and transformers are set.
        if (draft.getTransform() != null) {
            if (StrKit.isBlank(draft.getTransform().getName())) {
                draft.getTransform().setName("migrate-sql");
            }
            draft.getTransformers().add(0, draft.getTransform());
            draft.setTransform(null);
        }
        if (StrKit.isBlank(spel.getName())) {
            spel.setName("migrate-spel");
        }
        draft.getTransformers().add(spel);
    }
}
