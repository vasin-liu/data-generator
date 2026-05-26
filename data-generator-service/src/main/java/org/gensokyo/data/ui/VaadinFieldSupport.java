/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

/**
 * Helpers for binding nullable model values into Vaadin fields (setValue rejects null).
 *
 * @author Gensokyo
 * @since 2026-05-25
 */
public final class VaadinFieldSupport {

    private VaadinFieldSupport() {
    }

    /**
     * Sets a text field from a nullable model value.
     *
     * @param field bound field
     * @param value nullable text
     */
    public static void setText(TextField field, String value) {
        if (value == null) {
            field.clear();
        } else {
            field.setValue(value);
        }
    }

    /**
     * Sets a text area from a nullable model value.
     *
     * @param field bound field
     * @param value nullable text
     */
    public static void setText(TextArea field, String value) {
        if (value == null) {
            field.clear();
        } else {
            field.setValue(value);
        }
    }

    /**
     * Sets a combo box from a nullable model value.
     *
     * @param field bound field
     * @param value nullable item
     */
    public static void setCombo(ComboBox<String> field, String value) {
        if (value == null) {
            field.clear();
        } else {
            field.setValue(value);
        }
    }

    /**
     * Sets an integer field from a nullable model value.
     *
     * @param field bound field
     * @param value nullable integer
     */
    public static void setInteger(IntegerField field, Integer value) {
        if (value == null) {
            field.clear();
        } else {
            field.setValue(value);
        }
    }
}
