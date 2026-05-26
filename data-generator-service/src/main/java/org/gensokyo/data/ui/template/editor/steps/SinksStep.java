/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui.template.editor.steps;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.vo.stage.WriteStageVO;
import org.gensokyo.data.model.vo.writer.ConsoleWriterVO;
import org.gensokyo.data.model.vo.writer.WriterVO;
import org.gensokyo.data.ui.VaadinFieldSupport;
import org.gensokyo.data.ui.i18n.ConsoleI18n;
import org.gensokyo.data.ui.template.editor.EditorStep;
import org.gensokyo.data.ui.template.editor.TemplateEditorModel;
import org.gensokyo.data.writer.ElasticsearchWriterVO;
import org.gensokyo.data.writer.JdbcWriterVO;
import org.gensokyo.data.writer.KafkaWriterVO;

import java.util.List;
import java.util.Set;

/**
 * Sink writer configuration (console, JDBC, Kafka, Elasticsearch).
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
public class SinksStep implements EditorStep {

    private final VerticalLayout root = new VerticalLayout();
    private final ComboBox<String> writerType = new ComboBox<>("Writer type");
    private final ComboBox<String> dataSourceId = new ComboBox<>("Data source id");
    private final TextField target = new TextField("Target (table / topic / index)");
    private final FormLayout detailForm = new FormLayout();

    /**
     * @param jdbcSourceNames datasource ids for JDBC writer
     */
    public SinksStep(Set<String> jdbcSourceNames) {
        writerType.setItems("console", "jdbc", "kafka", "elasticsearch");
        dataSourceId.setItems(jdbcSourceNames);
        detailForm.add(writerType, dataSourceId, target);
        root.add(detailForm);
        writerType.addValueChangeListener(e -> toggleDetail(e.getValue()));
        applyI18n();
    }

    private void applyI18n() {
        writerType.setLabel(ConsoleI18n.tr("sink.writerType"));
        dataSourceId.setLabel(ConsoleI18n.tr("sink.datasource"));
        target.setLabel(ConsoleI18n.tr("sink.target"));
    }

    @Override
    public Component getView() {
        return root;
    }

    @Override
    public void refreshFromModel(TemplateEditorModel model) {
        WriteStageVO sink = model.getDraft().getSink();
        WriterVO writer = firstWriter(sink);
        String type = writer != null && writer.getType() != null ? writer.getType().toLowerCase() : "console";
        writerType.setValue(normalizeType(type));
        if (writer != null) {
            VaadinFieldSupport.setCombo(dataSourceId, writer.getDataSourceId());
            VaadinFieldSupport.setText(target, writer.getTarget());
        } else {
            dataSourceId.clear();
            target.clear();
        }
        toggleDetail(writerType.getValue());
        boolean enabled = model.isSaveAllowed();
        writerType.setReadOnly(!enabled);
        dataSourceId.setReadOnly(!enabled);
        target.setReadOnly(!enabled);
    }

    @Override
    public void applyToModel(TemplateEditorModel model) {
        if (!model.isSaveAllowed()) {
            return;
        }
        TemplateV2DraftVO draft = model.getDraft();
        WriteStageVO sink = new WriteStageVO();
        sink.setWriters(List.of(buildWriter()));
        draft.setSink(sink);
    }

    private WriterVO buildWriter() {
        String type = writerType.getValue();
        WriterVO writer = switch (type) {
            case "jdbc" -> new JdbcWriterVO();
            case "kafka" -> new KafkaWriterVO();
            case "elasticsearch" -> new ElasticsearchWriterVO();
            default -> new ConsoleWriterVO();
        };
        writer.setType(type);
        if (!"console".equals(type)) {
            writer.setDataSourceId(dataSourceId.getValue());
            writer.setTarget(target.getValue());
        }
        return writer;
    }

    private static WriterVO firstWriter(WriteStageVO sink) {
        if (sink == null || sink.getWriters() == null || sink.getWriters().isEmpty()) {
            return null;
        }
        return sink.getWriters().getFirst();
    }

    private static String normalizeType(String type) {
        return switch (type) {
            case "jdbc", "kafka", "elasticsearch" -> type;
            default -> "console";
        };
    }

    private void toggleDetail(String type) {
        boolean console = "console".equalsIgnoreCase(type);
        dataSourceId.setVisible(!console);
        target.setVisible(!console);
    }
}
