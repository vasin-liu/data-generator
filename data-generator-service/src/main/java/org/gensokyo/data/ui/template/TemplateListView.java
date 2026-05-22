/*
 * Copyright © 2021 - 2026 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: https://www.pcitech.com/
 * Address: PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou, China (Zip code: 510653)
 */
package org.gensokyo.data.ui.template;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import org.gensokyo.data.model.po.TemplatePO;
import org.gensokyo.data.repository.TemplateRepository;
import org.gensokyo.data.template.editor.TemplateEditorService;
import org.gensokyo.data.ui.MainLayout;
import org.gensokyo.data.ui.template.editor.TemplateEditorView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Template catalog with navigation to the V2 editor.
 *
 * @author Gensokyo
 * @since 2026-05-23
 */
@Component
@Route(value = "templates", layout = MainLayout.class)
@PageTitle("Templates | Data Generator")
public class TemplateListView extends VerticalLayout {

    private final TemplateRepository templateRepository;
    private final TemplateEditorService templateEditorService;
    private final Grid<TemplatePO> grid = new Grid<>(TemplatePO.class, false);
    private final Checkbox includeArchived = new Checkbox("Include archived");

    /**
     * @param templateRepository  template persistence
     * @param templateEditorService archive/restore
     */
    @Autowired
    public TemplateListView(TemplateRepository templateRepository, TemplateEditorService templateEditorService) {
        this.templateRepository = templateRepository;
        this.templateEditorService = templateEditorService;
        setSizeFull();
        setPadding(true);
        configureGrid();
        Button refresh = new Button("Refresh", e -> refreshGrid());
        Button create = new Button("New template", e -> getUI().ifPresent(
                ui -> ui.navigate(TemplateEditorView.class, "new")));
        create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        includeArchived.addValueChangeListener(e -> refreshGrid());
        HorizontalLayout toolbar = new HorizontalLayout(create, refresh, includeArchived);
        toolbar.setAlignItems(Alignment.CENTER);
        add(toolbar, grid);
        refreshGrid();
    }

    private void configureGrid() {
        grid.addColumn(TemplatePO::getId).setHeader("Id").setSortable(true);
        grid.addColumn(TemplatePO::getName).setHeader("Name").setSortable(true);
        grid.addColumn(row -> Boolean.TRUE.equals(row.getArchived()) ? "yes" : "no").setHeader("Archived");
        grid.addComponentColumn(row -> new RouterLink(
                "Edit",
                TemplateEditorView.class,
                String.valueOf(row.getId())))
                .setHeader("Edit");
        grid.addComponentColumn(row -> {
            Button archive = new Button(Boolean.TRUE.equals(row.getArchived()) ? "Restore" : "Archive");
            archive.addClickListener(e -> toggleArchive(row));
            return archive;
        }).setHeader("Actions");
        grid.setSizeFull();
    }

    private void refreshGrid() {
        List<TemplatePO> rows = includeArchived.getValue()
                ? templateRepository.findAll()
                : templateRepository.findByArchivedFalse();
        grid.setItems(rows);
    }

    private void toggleArchive(TemplatePO row) {
        try {
            if (Boolean.TRUE.equals(row.getArchived())) {
                templateEditorService.restore(row.getId());
                Notification.show("Restored " + row.getId());
            } else {
                templateEditorService.archive(row.getId());
                Notification.show("Archived " + row.getId());
            }
            refreshGrid();
        } catch (IllegalArgumentException ex) {
            Notification.show(ex.getMessage());
        }
    }
}
