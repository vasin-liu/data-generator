/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui.datasource;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import org.gensokyo.data.datasource.DataSourceConfigService;
import org.gensokyo.data.datasource.DataSourceConfigSummary;
import org.gensokyo.data.ui.i18n.ConsoleI18n;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Dialog to add or edit a persisted JDBC datasource.
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
public class DataSourceFormDialog extends Dialog {

    private final DataSourceConfigService dataSourceConfigService;
    private final Runnable onSaved;

    private final TextField name = new TextField();
    private final TextField url = new TextField();
    private final TextField username = new TextField();
    private final PasswordField password = new PasswordField();
    private final TextField driverClass = new TextField();
    private final AtomicReference<byte[]> uploadedJar = new AtomicReference<>();

    /**
     * @param dataSourceConfigService persistence + runtime
     * @param onSaved                 refresh callback after save
     */
    public DataSourceFormDialog(DataSourceConfigService dataSourceConfigService, Runnable onSaved) {
        this.dataSourceConfigService = dataSourceConfigService;
        this.onSaved = onSaved;
        applyI18n();
        FormLayout form = new FormLayout(name, url, username, password, driverClass);
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setMaxFiles(1);
        upload.setAcceptedFileTypes(".jar", "application/java-archive");
        upload.addSucceededListener(event -> {
            try (InputStream in = buffer.getInputStream()) {
                uploadedJar.set(in.readAllBytes());
            } catch (IOException ex) {
                Notification.show(ConsoleI18n.tr("datasources.dialog.upload.failed", ex.getMessage()));
            }
        });
        Button test = new Button(ConsoleI18n.tr("datasources.dialog.test"), e -> testConnection());
        Button save = new Button(ConsoleI18n.tr("common.save"), e -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button(ConsoleI18n.tr("common.cancel"), e -> close());
        setWidth("32rem");
        getFooter().add(cancel, test, save);
        add(form, upload);
    }

    private void applyI18n() {
        setHeaderTitle(ConsoleI18n.tr("datasources.dialog.title"));
        name.setLabel(ConsoleI18n.tr("datasources.dialog.name"));
        url.setLabel(ConsoleI18n.tr("datasources.dialog.url"));
        username.setLabel(ConsoleI18n.tr("datasources.dialog.username"));
        password.setLabel(ConsoleI18n.tr("datasources.dialog.password"));
        driverClass.setLabel(ConsoleI18n.tr("datasources.dialog.driver"));
    }

    /**
     * Pre-fills the dialog for edit mode.
     *
     * @param summary existing config
     */
    public void edit(DataSourceConfigSummary summary) {
        name.setValue(summary.name());
        name.setReadOnly(true);
        url.setValue(summary.url());
        username.setValue(summary.username() != null ? summary.username() : "");
        driverClass.setValue(summary.driverClassName());
    }

    private void testConnection() {
        try {
            String message = dataSourceConfigService.testConnection(
                    url.getValue(),
                    username.getValue(),
                    password.getValue(),
                    driverClass.getValue(),
                    null);
            Notification.show(message);
        } catch (Exception ex) {
            Notification.show(ConsoleI18n.tr("datasources.dialog.test.failed", ex.getMessage()));
        }
    }

    private void save() {
        try {
            MultipartFile driverFile = toMultipart(uploadedJar.get());
            dataSourceConfigService.save(
                    name.getValue(),
                    url.getValue(),
                    username.getValue(),
                    password.getValue(),
                    driverClass.getValue(),
                    driverFile);
            Notification.show(ConsoleI18n.tr("datasources.dialog.saved", name.getValue()));
            close();
            onSaved.run();
        } catch (Exception ex) {
            Notification.show(ConsoleI18n.tr("datasources.dialog.save.failed", ex.getMessage()));
        }
    }

    private static MultipartFile toMultipart(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return new MultipartFile() {
            @Override
            public String getName() {
                return "driverFile";
            }

            @Override
            public String getOriginalFilename() {
                return "uploaded-driver.jar";
            }

            @Override
            public String getContentType() {
                return "application/java-archive";
            }

            @Override
            public boolean isEmpty() {
                return bytes.length == 0;
            }

            @Override
            public long getSize() {
                return bytes.length;
            }

            @Override
            public byte[] getBytes() {
                return bytes;
            }

            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream(bytes);
            }

            @Override
            public void transferTo(java.io.File dest) throws IOException {
                java.nio.file.Files.write(dest.toPath(), bytes);
            }
        };
    }
}
