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
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import org.gensokyo.data.model.v2.SpelColumnMapping;
import org.gensokyo.data.model.v2.SpelTransformVO;
import org.gensokyo.data.model.v2.SqlTransformVO;
import org.gensokyo.data.model.v2.TemplateV2DraftVO;
import org.gensokyo.data.model.v2.TransformVO;
import org.gensokyo.data.ui.template.editor.EditorStep;
import org.gensokyo.data.ui.template.editor.TemplateEditorModel;

import java.util.ArrayList;

/**
 * SQL or SpEL transform step.
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
public class TransformStep implements EditorStep {

    private final VerticalLayout root = new VerticalLayout();
    private final ComboBox<String> transformType = new ComboBox<>("Transform type");
    private final TextArea sqlField = new TextArea("SQL");
    private final TextField spelColumn = new TextField("SpEL column name");
    private final TextArea spelExpression = new TextArea("SpEL expression");
    private final FormLayout sqlForm = new FormLayout();
    private final FormLayout spelForm = new FormLayout();

    /**
     * Creates the transform step UI.
     */
    public TransformStep() {
        transformType.setItems("sql", "spel");
        sqlField.setMinHeight("140px");
        spelExpression.setMinHeight("80px");
        sqlForm.add(sqlField);
        spelForm.add(spelColumn, spelExpression);
        root.add(transformType, sqlForm, spelForm);
        transformType.addValueChangeListener(e -> toggle(e.getValue()));
    }

    @Override
    public Component getView() {
        return root;
    }

    @Override
    public void refreshFromModel(TemplateEditorModel model) {
        TransformVO transform = model.getDraft().getTransform();
        if (transform instanceof SpelTransformVO spel && !spel.getColumns().isEmpty()) {
            transformType.setValue("spel");
            SpelColumnMapping first = spel.getColumns().getFirst();
            spelColumn.setValue(first.getName());
            spelExpression.setValue(first.getExpression());
        } else {
            transformType.setValue("sql");
            if (transform instanceof SqlTransformVO sql) {
                sqlField.setValue(sql.getSql());
            } else {
                sqlField.clear();
            }
        }
        toggle(transformType.getValue());
        boolean enabled = model.isSaveAllowed();
        transformType.setReadOnly(!enabled);
        sqlField.setReadOnly(!enabled);
        spelColumn.setReadOnly(!enabled);
        spelExpression.setReadOnly(!enabled);
    }

    @Override
    public void applyToModel(TemplateEditorModel model) {
        if (!model.isSaveAllowed()) {
            return;
        }
        TemplateV2DraftVO draft = model.getDraft();
        if ("spel".equalsIgnoreCase(transformType.getValue())) {
            SpelTransformVO spel = new SpelTransformVO();
            SpelColumnMapping mapping = new SpelColumnMapping();
            mapping.setName(spelColumn.getValue());
            mapping.setExpression(spelExpression.getValue());
            spel.setColumns(new ArrayList<>(java.util.List.of(mapping)));
            draft.setTransform(spel);
        } else {
            SqlTransformVO sql = new SqlTransformVO();
            sql.setSql(sqlField.getValue());
            draft.setTransform(sql);
        }
    }

    private void toggle(String type) {
        boolean spel = "spel".equalsIgnoreCase(type);
        sqlForm.setVisible(!spel);
        spelForm.setVisible(spel);
    }
}
