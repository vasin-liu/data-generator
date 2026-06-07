import { useQuery } from '@tanstack/react-query';
import { Alert, Button, Collapse, Space, Spin, Tabs, message } from 'antd';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { fetchEditor, fetchEditorScaffold } from '../../api/editor';
import { fetchScenarioScaffold } from '../../api/scenarios';
import { fetchEditorDataSources } from '../../api/runtime';
import type { EditorDataSources } from '../../api/types';
import type { TemplateEditorPayload, TemplateV2Draft } from '../../api/types';
import { cloneDraft } from '../editor/draftUtils';
import { EditorTabHint } from '../editor/EditorTabHint';
import { ReviewPanel } from '../editor/ReviewPanel';
import { YamlPanel } from '../editor/YamlPanel';
import { ExecutionStep } from '../editor/steps/ExecutionStep';
import { GeneralStep } from '../editor/steps/GeneralStep';
import { SinksStep } from '../editor/steps/SinksStep';
import { SourcesStep } from '../editor/steps/SourcesStep';
import { TransformStep } from '../editor/steps/TransformStep';
import { WorkflowPanel } from '../editor/WorkflowPanel';
import { ConsolePageHeader } from '../../components/ConsolePageHeader';
import { TemplateStatusTag } from '../../components/TemplateStatusTag';

const TAB_KEYS = ['general', 'sources', 'transform', 'sinks', 'execution', 'workflow', 'review'] as const;
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
  const scenarioId = searchParams.get('scenario')?.trim() ?? '';
  const routeId = !isNew && id ? id.trim() : '';
  const invalidId = !isNew && routeId.length === 0;

  const [draft, setDraft] = useState<TemplateV2Draft | null>(null);
  const [meta, setMeta] = useState<(Omit<TemplateEditorPayload, 'draft'> & { status: string | null }) | null>(null);

  const loadQuery = useQuery({
    queryKey: ['editor', id, scenarioId],
    queryFn: async () => {
      if (invalidId) {
        const scaffold = await fetchEditorScaffold();
        return scaffold;
      }
      if (isNew && scenarioId) {
        return fetchScenarioScaffold(scenarioId);
      }
      return isNew ? fetchEditorScaffold() : fetchEditor(routeId);
    },
    enabled: isNew || routeId.length > 0,
  });

  const dsQuery = useQuery({
    queryKey: ['editor-data-sources'],
    queryFn: fetchEditorDataSources,
  });

  const editorDataSources: EditorDataSources = dsQuery.data ?? {
    jdbcNames: [],
    kafkaClusters: [],
    elasticsearchClusters: [],
  };

  const applyPayload = useCallback((payload: TemplateEditorPayload) => {
    setDraft(cloneDraft(payload.draft));
    setMeta({
      templateId: payload.templateId,
      kind: payload.kind,
      v1Yaml: payload.v1Yaml,
      archived: payload.archived,
      status: payload.status,
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
  const saveAllowed = meta != null && !meta.archived;
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
    const errMsg = (loadQuery.error as Error).message;
    const v1Hidden = errMsg.includes('Legacy V1');
    return (
      <Alert
        type="error"
        showIcon
        message={v1Hidden ? t('editor.v1.hidden') : t('editor.loadError')}
        description={errMsg}
        action={
          v1Hidden ? (
            <Button type="primary" onClick={() => navigate('/templates')}>
              {t('editor.back')}
            </Button>
          ) : undefined
        }
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
          <SourcesStep {...stepProps} editorDataSources={editorDataSources} />
        </>
      ),
    },
    {
      key: 'transform',
      label: t('editor.tab.transform'),
      children: (
        <>
          <EditorTabHint tab="transform" isNew={isNew} />
          <TransformStep {...stepProps} />
        </>
      ),
    },
    {
      key: 'sinks',
      label: t('editor.tab.sinks'),
      children: (
        <>
          <EditorTabHint tab="sinks" isNew={isNew} />
          <SinksStep {...stepProps} editorDataSources={editorDataSources} />
        </>
      ),
    },
    {
      key: 'execution',
      label: t('editor.tab.execution'),
      children: (
        <>
          <EditorTabHint tab="execution" isNew={isNew} />
          <ExecutionStep {...stepProps} />
        </>
      ),
    },
    {
      key: 'workflow',
      label: t('editor.tab.workflow'),
      children: (
        <>
          <EditorTabHint tab="workflow" isNew={isNew} />
          <WorkflowPanel {...stepProps} editorDataSources={editorDataSources} />
        </>
      ),
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
            status={meta.status}
            archived={meta.archived}
            saveAllowed={saveAllowed}
            onSaved={onSaved}
            onPublished={() => setMeta((m) => (m ? { ...m, status: 'PUBLISHED' } : m))}
          />
        </>
      ),
    },
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
    <div data-testid="template-editor-page">
      <ConsolePageHeader
        title={
          <Space wrap>
            {title}
            {meta.status ? <TemplateStatusTag status={meta.status} /> : null}
          </Space>
        }
        subtitle={draft.name?.trim() ? draft.name : undefined}
        crumbs={[
          { label: t('nav.home'), path: '/' },
          { label: t('nav.templates'), path: '/templates' },
          { label: isNew ? t('editor.new') : String(templateId ?? routeId) },
        ]}
        extra={editorActions}
      />
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
      <Tabs activeKey={activeTab} onChange={setTab} items={tabItems} data-testid="template-editor-tabs" />
    </div>
  );
}
