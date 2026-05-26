/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for nullable Vaadin field binding helpers.
 *
 * @author Gensokyo
 * @since 2026-05-25
 */
class VaadinFieldSupportTests {

    @Test
    void setText_null_clearsField() {
        TextField field = new TextField();
        field.setValue("before");
        VaadinFieldSupport.setText(field, null);
        Assertions.assertTrue(field.isEmpty());
    }

    @Test
    void setText_value_setsField() {
        TextField field = new TextField();
        VaadinFieldSupport.setText(field, "jdbc://x");
        Assertions.assertEquals("jdbc://x", field.getValue());
    }

    @Test
    void setCombo_null_clearsField() {
        ComboBox<String> box = new ComboBox<>();
        box.setItems("a", "b");
        box.setValue("a");
        VaadinFieldSupport.setCombo(box, null);
        Assertions.assertTrue(box.isEmpty());
    }
}
