/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template.editor;

import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Category and tag helpers for template catalog metadata.
 *
 * @author Gensokyo
 * @since 2026-06-04
 */
public final class TemplateMetadataSupport {

    private TemplateMetadataSupport() {
    }

    /**
     * @param tags comma-separated storage form
     * @return tag list
     */
    public static List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * @param tags tag list from draft
     * @return comma-separated storage form
     */
    public static String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        String joined = tags.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(","));
        return joined.isEmpty() ? null : joined;
    }

    /**
     * Copies category/tags from draft onto the entity row.
     *
     * @param entity persisted row
     * @param draft  V2 draft
     */
    public static void syncToEntity(TemplatePO entity, TemplateV2DraftVO draft) {
        String category = draft.getCategory();
        entity.setCategory(category == null || category.isBlank() ? null : category.trim());
        entity.setTags(joinTags(draft.getTags()));
    }

    /**
     * Fills draft category/tags from entity when missing in YAML.
     *
     * @param entity persisted row
     * @param draft  loaded draft
     */
    public static void mergeFromEntity(TemplatePO entity, TemplateV2DraftVO draft) {
        if ((draft.getCategory() == null || draft.getCategory().isBlank()) && entity.getCategory() != null) {
            draft.setCategory(entity.getCategory());
        }
        if ((draft.getTags() == null || draft.getTags().isEmpty()) && entity.getTags() != null) {
            draft.setTags(new ArrayList<>(splitTags(entity.getTags())));
        }
    }

    /**
     * @param rows all templates
     * @return distinct sorted categories
     */
    public static List<String> distinctCategories(List<TemplatePO> rows) {
        Set<String> values = new LinkedHashSet<>();
        for (TemplatePO row : rows) {
            if (row.getCategory() != null && !row.getCategory().isBlank()) {
                values.add(row.getCategory().trim());
            }
        }
        return values.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    /**
     * @param rows all templates
     * @return distinct sorted tags
     */
    public static List<String> distinctTags(List<TemplatePO> rows) {
        Set<String> values = new LinkedHashSet<>();
        for (TemplatePO row : rows) {
            splitTags(row.getTags()).forEach(values::add);
        }
        return values.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    /**
     * @param row     template row
     * @param category filter (optional)
     * @param tag      filter (optional)
     * @return whether the row matches
     */
    public static boolean matchesTaxonomy(TemplatePO row, String category, String tag) {
        if (category != null && !category.isBlank()) {
            String rowCategory = row.getCategory();
            if (rowCategory == null
                    || !rowCategory.trim().equalsIgnoreCase(category.trim())) {
                return false;
            }
        }
        if (tag != null && !tag.isBlank()) {
            String needle = tag.trim().toLowerCase(Locale.ROOT);
            boolean found = splitTags(row.getTags()).stream()
                    .anyMatch(t -> t.equalsIgnoreCase(needle));
            if (!found) {
                return false;
            }
        }
        return true;
    }
}
