import { useQuery } from '@tanstack/react-query';
import { Alert, Button, Collapse, Spin, Tabs, message } from 'antd';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { migrationUiEnabled } from '../../config/features';
import { fetchEditor, fetchEditorScaffold } from '../../api/editor';
import { fetchJdbcNames } from '../../api/runtime';
import type { TemplateEditorPayload, TemplateV2Draft } from '../../api/types';
import { cloneDraft } from '../editor/draftUtils';
import { EditorTabHint } from '../editor/EditorTabHint';
import { MigrationTab } from '../editor/MigrationTab';
import { ReviewPanel } from '../editor/ReviewPanel';
import { YamlPanel } from '../editor/YamlPanel';
import { ExecutionStep } from '../editor/steps/ExecutionStep';
import { GeneralStep } from '../editor/steps/GeneralStep';
import { SinksStep } from '../editor/steps/SinksStep';
import { SourcesStep } from '../editor/steps/SourcesStep';
import { TransformStep } from '../editor/steps/TransformStep';
import { WorkflowPanel } from '../editor/WorkflowPanel';
import { ConsolePageHeader } from '../../components/ConsolePageHeader';

const TAB_KEYS = ['general', 'sources', 'transform', 'sinks', 'execution', 'workflow', 'review', 'migration'] as const;
type TabKey = (typeof TAB_KEYS)[number];

/**
 * Template wizard editor (parity with Vaadin {@code TemplateEditorView}).
 */
export function TemplateEditorPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const { id } = useParams<{ id: string }>();
  const [searchParams, setSearchParams] = useSearchParams();
  const isNew = location.pathname.endsWith('/templates/new');
  const routeId = !isNew && id ? id.trim() : '';
  const invalidId = !isNew && routeId.length === 0;

  const [draft, setDraft] = useState<TemplateV2Draft | null>(null);
  const [meta, setMeta] = useState<Omit<TemplateEditorPayload, 'draft'> | null>(null);

  const loadQuery = useQuery({
    queryKey: ['editor', id],
    queryFn: async () => {
      if (invalidId) {
        const scaffold = await fetchEditorScaffold();
        return scaffold;
      }
      return isNew ? fetchEditorScaffold() : fetchEditor(routeId);
    },
    enabled: isNew || routeId.length > 0,
  });

  const jdbcQuery = useQuery({
    queryKey: ['jdbc-names'],
    queryFn: fetchJdbcNames,
  });

  const applyPayload = useCallback((payload: TemplateEditorPayload) => {
    setDraft(cloneDraft(payload.draft));
    setMeta({
      templateId: payload.templateId,
      kind: payload.kind,
      v1Yaml: payload.v1Yaml,
      archived: payload.archived,
    });
  }, []);

  useEffect(() => {
    if (loadQuery.data) {
      applyPayload(loadQuery.data);
    }
  }, [loadQuery.data, applyPayload]);

  useEffect(() => {
    if (invalidId) {
      message.warning(t('editor.invalidId'));
      navigate('/templates/new', { replace: true });
    }
  }, [invalidId, navigate, t]);

  const templateId = meta?.templateId ?? (isNew ? null : routeId);
  const saveAllowed = meta != null && meta.kind !== 'V1' && !meta.archived;
  const activeTab = (searchParams.get('tab') as TabKey) || 'general';

  const setTab = (key: string) => {
    setSearchParams({ tab: key }, { replace: true });
  };

  const onSaved = (newId: string) => {
    setMeta((m) => (m ? { ...m, templateId: newId } : m));
    if (isNew || invalidId) {
      navigate(`/templates/${newId}?tab=review`, { replace: true });
    }
  };

  const title = useMemo(() => {
    if (isNew || invalidId) {
      return t('editor.new');
    }
    return t('editor.edit', { id: templateId ?? routeId });
  }, [isNew, invalidId, templateId, routeId, t]);

  if (loadQuery.isError) {
    return (
      <Alert
        type="error"
        showIcon
        message={t('templates.loadError')}
        description={(loadQuery.error as Error).message}
      />
    );
  }

  if (loadQuery.isLoading || !draft || !meta) {
    return <Spin style={{ display: 'block', margin: '48px auto' }} />;
  }

  const stepProps = {
    draft,
    readOnly: !saveAllowed,
    onChange: setDraft,
  };

  const tabItems = [
    {
      key: 'general',
      label: t('editor.tab.general'),
      children: (
        <>
          <EditorTabHint tab="general" isNew={isNew} />
          <GeneralStep {...stepProps} />
        </>
      ),
    },
    {
      key: 'sources',
      label: t('editor.tab.sources'),
      children: (
        <>
          <EditorTabHint tab="sources" isNew={isNew} />
          <SourcesStep {...stepProps} jdbcNames={jdbcQuery.data ?? []} />
        </>
      ),
    },
    {
      key: 'transform',
      label: t('editor.tab.transform'),
      children: <TransformStep {...stepProps} />,
    },
    {
      key: 'sinks',
      label: t('editor.tab.sinks'),
      children: <SinksStep {...stepProps} jdbcNames={jdbcQuery.data ?? []} />,
    },
    {
      key: 'execution',
      label: t('editor.tab.execution'),
      children: <ExecutionStep {...stepProps} />,
    },
    {
      key: 'workflow',
      label: t('editor.tab.workflow'),
      children: <WorkflowPanel {...stepProps} jdbcNames={jdbcQuery.data ?? []} />,
    },
    {
      key: 'review',
      label: t('editor.tab.review'),
      children: (
        <>
          <EditorTabHint tab="review" isNew={isNew} />
          <ReviewPanel
            draft={draft}
            templateId={templateId}
            kind={meta.kind}
            archived={meta.archived}
            saveAllowed={saveAllowed}
            onSaved={onSaved}
          />
        </>
      ),
    },
    ...(migrationUiEnabled
      ? [
          {
            key: 'migration' as const,
            label: t('editor.tab.migration'),
            children:
              templateId == null ? (
                <Alert type="info" message={t('editor.migration.saveFirst')} />
              ) : (
                <MigrationTab
                  templateId={templateId}
                  kind={meta.kind}
                  onPromoted={() => loadQuery.refetch()}
                  onDraftApply={(d) => setDraft(cloneDraft(d))}
                />
              ),
          },
        ]
      : []),
  ];

  const editorActions =
    templateId != null ? (
      <>
        <Button onClick={() => navigate(`/jobs?templateId=${templateId}`)}>{t('templates.viewJobs')}</Button>
        <Button onClick={() => navigate(`/schedules?templateId=${templateId}`)}>
          {t('templates.schedules')}
        </Button>
        <Button>
          <Link to="/templates">{t('editor.back')}</Link>
        </Button>
      </>
    ) : (
      <Button>
        <Link to="/templates">{t('editor.back')}</Link>
      </Button>
    );

  return (
    <div>
      <ConsolePageHeader
        title={title}
        crumbs={[
          { label: t('nav.home'), path: '/' },
          { label: t('nav.templates'), path: '/templates' },
          { label: isNew ? t('editor.new') : String(templateId ?? routeId) },
        ]}
        extra={editorActions}
      />
      {meta.kind === 'V1' && (
        <Alert type="info" message={t('editor.v1.note')} style={{ marginBottom: 16 }} />
      )}
      {meta.archived && (
        <Alert type="warning" message={t('review.status.archived')} style={{ marginBottom: 16 }} />
      )}
      <Collapse
        style={{ marginBottom: 16 }}
        items={[
          {
            key: 'yaml',
            label: t('editor.yaml.mode'),
            children: (
              <YamlPanel
                draft={draft}
                templateId={templateId}
                saveAllowed={saveAllowed}
                v1Yaml={meta.v1Yaml}
                onApplied={applyPayload}
                onDraftParsed={(d) => setDraft(cloneDraft(d))}
              />
            ),
          },
        ]}
      />
      <Tabs activeKey={activeTab} onChange={setTab} items={tabItems} />
    </div>
  );
}
