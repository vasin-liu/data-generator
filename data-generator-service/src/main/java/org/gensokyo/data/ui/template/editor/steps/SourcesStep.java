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
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import org.gensokyo.data.iterator.NumberIteratorVO;
import org.gensokyo.data.model.v2.IteratorSourceVO;
import org.gensokyo.data.model.v2.QuerySourceVO;
import org.gensokyo.data.model.v2.SourceVO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.ui.template.editor.EditorStep;
import org.gensokyo.data.ui.template.editor.TemplateEditorModel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Primary source configuration (query JDBC or number iterator).
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
public class SourcesStep implements EditorStep {

    private static final String KEY_INPUT = "input";

    private final VerticalLayout root = new VerticalLayout();
    private final ComboBox<String> sourceType = new ComboBox<>("Source type");
    private final TextField sourceKey = new TextField("Source name");
    private final ComboBox<String> dataSourceId = new ComboBox<>("Data source id");
    private final TextArea sql = new TextArea("SQL");
    private final IntegerField fromField = new IntegerField("From");
    private final IntegerField toField = new IntegerField("To");
    private final IntegerField stepField = new IntegerField("Step");
    private final FormLayout queryForm = new FormLayout();
    private final FormLayout iteratorForm = new FormLayout();

    /**
     * @param jdbcSourceNames datasource ids for query source dropdown
     */
    public SourcesStep(Set<String> jdbcSourceNames) {
        sourceType.setItems("query", "iterator");
        sourceKey.setValue(KEY_INPUT);
        sourceKey.setReadOnly(true);
        dataSourceId.setItems(jdbcSourceNames);
        sql.setMinHeight("120px");
        queryForm.add(dataSourceId, sql);
        iteratorForm.add(fromField, toField, stepField);
        root.add(sourceType, sourceKey, queryForm, iteratorForm);
        root.setSpacing(true);
        sourceType.addValueChangeListener(e -> toggleForms(e.getValue()));
    }

    @Override
    public Component getView() {
        return root;
    }

    @Override
    public void refreshFromModel(TemplateEditorModel model) {
        TemplateV2DraftVO draft = model.getDraft();
        Map<String, SourceVO> sources = draft.getSources();
        if (sources == null || sources.isEmpty()) {
            sourceType.setValue("iterator");
            toggleForms("iterator");
            setEnabled(model.isSaveAllowed());
            return;
        }
        SourceVO source = sources.getOrDefault(KEY_INPUT, sources.values().iterator().next());
        String type = source.getType() != null ? source.getType().toLowerCase() : "iterator";
        sourceType.setValue(type);
        toggleForms(type);
        if (source instanceof QuerySourceVO query) {
            dataSourceId.setValue(query.getDataSourceId());
            sql.setValue(query.getSql());
        } else if (source instanceof IteratorSourceVO iterator
                && iterator.getIterator() instanceof NumberIteratorVO number) {
            fromField.setValue((int) number.getFrom());
            toField.setValue((int) number.getTo());
            stepField.setValue(number.getStep());
        }
        setEnabled(model.isSaveAllowed());
    }

    @Override
    public void applyToModel(TemplateEditorModel model) {
        if (!model.isSaveAllowed()) {
            return;
        }
        TemplateV2DraftVO draft = model.getDraft();
        String type = sourceType.getValue();
        SourceVO source;
        if ("query".equalsIgnoreCase(type)) {
            QuerySourceVO query = new QuerySourceVO();
            query.setDataSourceId(dataSourceId.getValue());
            query.setSql(sql.getValue());
            source = query;
        } else {
            IteratorSourceVO iteratorSource = new IteratorSourceVO();
            NumberIteratorVO number = new NumberIteratorVO();
            number.setType("number");
            number.setFrom(fromField.getValue() != null ? fromField.getValue() : 1);
            number.setTo(toField.getValue() != null ? toField.getValue() : 3);
            number.setStep(stepField.getValue() != null ? stepField.getValue() : 1);
            iteratorSource.setIterator(number);
            source = iteratorSource;
        }
        Map<String, SourceVO> sources = new LinkedHashMap<>();
        sources.put(KEY_INPUT, source);
        draft.setSources(sources);
    }

    private void toggleForms(String type) {
        boolean query = "query".equalsIgnoreCase(type);
        queryForm.setVisible(query);
        iteratorForm.setVisible(!query);
    }

    private void setEnabled(boolean enabled) {
        sourceType.setReadOnly(!enabled);
        dataSourceId.setReadOnly(!enabled);
        sql.setReadOnly(!enabled);
        fromField.setReadOnly(!enabled);
        toField.setReadOnly(!enabled);
        stepField.setReadOnly(!enabled);
    }
}
