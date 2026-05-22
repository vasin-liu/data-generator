/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui.template.editor;

import com.vaadin.flow.component.Component;

/**
 * One wizard step in the template editor.
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
public interface EditorStep {

    /**
     * @return root component for the tab
     */
    Component getView();

    /**
     * Loads values from the shared model into form fields.
     *
     * @param model editor model
     */
    void refreshFromModel(TemplateEditorModel model);

    /**
     * Writes form values back into the shared model.
     *
     * @param model editor model
     */
    void applyToModel(TemplateEditorModel model);
}
