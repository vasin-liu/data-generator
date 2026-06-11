/** Stable selectors shared across Playwright specs. */
export const TestIds = {
  shell: 'console-shell',
  brand: 'console-brand',
  home: 'console-home',
  themeToggle: 'theme-toggle',
  themeDark: 'theme-dark',
  themeLight: 'theme-light',
  nav: {
    home: 'nav-home',
    templates: 'nav-templates',
    datasources: 'nav-datasources',
    jobs: 'nav-jobs',
    schedules: 'nav-schedules',
    audit: 'nav-audit',
  },
  pages: {
    templates: 'templates-page',
    templateEditor: 'template-editor-page',
    templateEditorTabs: 'template-editor-tabs',
    datasources: 'datasources-page',
    jobs: 'jobs-page',
    jobDetail: 'job-detail-page',
    schedules: 'schedules-page',
    audit: 'audit-page',
  },
  actions: {
    templatesNew: 'templates-new-button',
    datasourcesNew: 'datasources-new-button',
    schedulesNew: 'schedules-new-button',
    reviewSave: 'review-save',
    reviewSaveAndReturn: 'review-save-and-return',
    editorTemplateName: 'editor-template-name',
  },
} as const;

export type NavTestId = (typeof TestIds.nav)[keyof typeof TestIds.nav];
