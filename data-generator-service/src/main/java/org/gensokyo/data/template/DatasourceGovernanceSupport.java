/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.template;

import org.gensokyo.data.constant.Const;
import org.gensokyo.data.datasource.api.CatalogEntry;
import org.gensokyo.data.datasource.api.CatalogEntrySource;
import org.gensokyo.data.datasource.api.ConnectionCatalog;
import org.gensokyo.data.datasource.api.ConnectionKind;
import org.gensokyo.data.model.v2.InlineDataSourceVO;
import org.gensokyo.data.model.v2.PostGisQuerySourceVO;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.v2.SourceVO;
import org.gensokyo.data.model.v2.TemplateV2VO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.JdbcWriterVO;
import org.gensokyo.data.model.vo.writer.WriterVO;
import org.gensokyo.data.writer.ElasticsearchWriterVO;
import org.gensokyo.data.writer.KafkaWriterVO;
import org.gensokyo.kit.character.StrKit;
import org.gensokyo.kit.collect.CollectKit;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Datasource governance: managed-only refs, BOOTSTRAP policy, and grandfather rules (D-13..D-17).
 *
 * @author Gensokyo
 * @version 3.0.0-SNAPSHOT
 * @since 2026-06-27
 */
public final class DatasourceGovernanceSupport {

    private DatasourceGovernanceSupport() {
    }

    /**
     * Collects hard-fail datasource governance violations for publish/run paths.
     *
     * @param template                  normalized template
     * @param catalog                   live catalog for BOOTSTRAP/MANAGED lookup
     * @param managedConnectionsRequired when true, inline connection blocks are rejected
     * @param bootstrapRefsAllowed       when false, BOOTSTRAP catalog refs are rejected
     * @param grandfatherPublishedRun    when true, skip enforcement (unchanged published template run)
     * @return validation error messages
     */
    public static List<String> collectViolations(
            TemplateV2VO template,
            ConnectionCatalog catalog,
            boolean managedConnectionsRequired,
            boolean bootstrapRefsAllowed,
            boolean grandfatherPublishedRun) {
        if (grandfatherPublishedRun || template == null) {
            return List.of();
        }
        if (!managedConnectionsRequired && bootstrapRefsAllowed) {
            return List.of();
        }
        List<String> errors = new ArrayList<>();
        collectFromSources(template, catalog, managedConnectionsRequired, bootstrapRefsAllowed, errors);
        collectFromSinks(template, catalog, managedConnectionsRequired, bootstrapRefsAllowed, errors);
        return errors;
    }

    /**
     * Collects non-fatal warnings for draft save when governance is enabled (D-16).
     *
     * @param template                  normalized template
     * @param catalog                   live catalog
     * @param managedConnectionsRequired when true, inline blocks produce warnings
     * @param bootstrapRefsAllowed       when false, BOOTSTRAP refs produce warnings
     * @return warning messages
     */
    public static List<String> collectWarnings(
            TemplateV2VO template,
            ConnectionCatalog catalog,
            boolean managedConnectionsRequired,
            boolean bootstrapRefsAllowed) {
        return collectViolations(template, catalog, managedConnectionsRequired, bootstrapRefsAllowed, false);
    }

    /**
     * Collects managed catalog connection references from a template graph.
     *
     * @param template normalized template
     * @return distinct managed refs (kind + name)
     */
    public static List<ConnectionRef> collectManagedRefs(TemplateV2VO template) {
        if (template == null) {
            return List.of();
        }
        Set<String> seen = new LinkedHashSet<>();
        List<ConnectionRef> refs = new ArrayList<>();
        if (!CollectKit.isEmpty(template.getSources())) {
            for (var entry : template.getSources().entrySet()) {
                SourceVO source = entry.getValue();
                if (source instanceof QuerySourceVO query && StrKit.isNotBlank(query.getDataSourceId())) {
                    addRef(seen, refs, ConnectionKind.JDBC, query.getDataSourceId());
                } else if (source instanceof PostGisQuerySourceVO postGis && StrKit.isNotBlank(postGis.getDataSourceId())) {
                    addRef(seen, refs, ConnectionKind.JDBC, postGis.getDataSourceId());
                }
            }
        }
        if (!CollectKit.isEmpty(template.getSinks())) {
            for (WriteStageVO stage : template.getSinks()) {
                if (stage == null || CollectKit.isEmpty(stage.getWriters())) {
                    continue;
                }
                for (WriterVO writer : stage.getWriters()) {
                    if (writer == null || StrKit.isBlank(writer.getDataSourceId())) {
                        continue;
                    }
                    String type = writer.getType() == null ? "" : writer.getType().trim();
                    if (Const.WriterType.JDBC.equalsIgnoreCase(type)) {
                        addRef(seen, refs, ConnectionKind.JDBC, writer.getDataSourceId());
                    } else if ("KAFKA".equalsIgnoreCase(type)) {
                        addRef(seen, refs, ConnectionKind.KAFKA, writer.getDataSourceId());
                    } else if ("ELASTICSEARCH".equalsIgnoreCase(type)) {
                        addRef(seen, refs, ConnectionKind.ELASTICSEARCH, writer.getDataSourceId());
                    }
                }
            }
        }
        return refs;
    }

    private static void addRef(Set<String> seen, List<ConnectionRef> refs, ConnectionKind kind, String name) {
        String key = kind.name() + ":" + name.trim();
        if (seen.add(key)) {
            refs.add(new ConnectionRef(kind, name.trim()));
        }
    }

    /**
     * Managed catalog connection reference extracted from a template.
     *
     * @param kind connection kind
     * @param name catalog entry name
     */
    public record ConnectionRef(ConnectionKind kind, String name) {
    }

    private static void collectFromSources(
            TemplateV2VO template,
            ConnectionCatalog catalog,
            boolean managedRequired,
            boolean bootstrapAllowed,
            List<String> errors) {
        if (CollectKit.isEmpty(template.getSources())) {
            return;
        }
        for (var entry : template.getSources().entrySet()) {
            String path = "sources." + entry.getKey();
            SourceVO source = entry.getValue();
            if (source instanceof QuerySourceVO query) {
                collectJdbcRef(path, query.getDataSourceId(), query.getDataSource(), catalog, managedRequired, bootstrapAllowed, errors);
            } else if (source instanceof PostGisQuerySourceVO postGis) {
                collectJdbcRef(path, postGis.getDataSourceId(), postGis.getDataSource(), catalog, managedRequired, bootstrapAllowed, errors);
            }
        }
    }

    private static void collectFromSinks(
            TemplateV2VO template,
            ConnectionCatalog catalog,
            boolean managedRequired,
            boolean bootstrapAllowed,
            List<String> errors) {
        if (CollectKit.isEmpty(template.getSinks())) {
            return;
        }
        for (int i = 0; i < template.getSinks().size(); i++) {
            WriteStageVO stage = template.getSinks().get(i);
            if (stage == null || CollectKit.isEmpty(stage.getWriters())) {
                continue;
            }
            for (WriterVO writer : stage.getWriters()) {
                collectWriterRef("sinks[" + i + "]", writer, catalog, managedRequired, bootstrapAllowed, errors);
            }
        }
    }

    private static void collectWriterRef(
            String path,
            WriterVO writer,
            ConnectionCatalog catalog,
            boolean managedRequired,
            boolean bootstrapAllowed,
            List<String> errors) {
        if (writer == null || writer.getType() == null) {
            return;
        }
        String type = writer.getType().trim();
        if (Const.WriterType.JDBC.equalsIgnoreCase(type) && writer instanceof JdbcWriterVO jdbc) {
            collectJdbcRef(path, jdbc.getDataSourceId(), jdbc.getDataSource(), catalog, managedRequired, bootstrapAllowed, errors);
        } else if ("KAFKA".equalsIgnoreCase(type) && writer instanceof KafkaWriterVO) {
            collectManagedRef(path, writer.getDataSourceId(), ConnectionKind.KAFKA, catalog, managedRequired, bootstrapAllowed, errors);
        } else if ("ELASTICSEARCH".equalsIgnoreCase(type) && writer instanceof ElasticsearchWriterVO) {
            collectManagedRef(path, writer.getDataSourceId(), ConnectionKind.ELASTICSEARCH, catalog, managedRequired, bootstrapAllowed, errors);
        }
    }

    private static void collectJdbcRef(
            String path,
            String dataSourceId,
            InlineDataSourceVO inline,
            ConnectionCatalog catalog,
            boolean managedRequired,
            boolean bootstrapAllowed,
            List<String> errors) {
        if (StrKit.isNotBlank(dataSourceId)) {
            collectManagedRef(path, dataSourceId, ConnectionKind.JDBC, catalog, managedRequired, bootstrapAllowed, errors);
            return;
        }
        if (managedRequired && inline != null) {
            errors.add(path + ": inline dataSource is not allowed when managed connections are required; use dataSourceId");
        }
    }

    private static void collectManagedRef(
            String path,
            String name,
            ConnectionKind kind,
            ConnectionCatalog catalog,
            boolean managedRequired,
            boolean bootstrapAllowed,
            List<String> errors) {
        if (StrKit.isBlank(name)) {
            if (managedRequired) {
                errors.add(path + ": managed " + kind + " connection reference (dataSourceId) is required");
            }
            return;
        }
        if (!bootstrapAllowed) {
            CatalogEntry entry = catalog.findEntry(name.trim(), kind).orElse(null);
            if (entry != null && entry.source() == CatalogEntrySource.BOOTSTRAP) {
                errors.add(path + ": BOOTSTRAP connection '" + name + "' is not allowed in this environment; use a MANAGED entry");
            }
        }
    }
}
