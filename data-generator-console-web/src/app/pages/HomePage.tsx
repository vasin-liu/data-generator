import type { ReactNode } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Card, Col, Row, Steps, Tag, Typography } from 'antd';
import {
  ClockCircleOutlined,
  DatabaseOutlined,
  FileTextOutlined,
  HistoryOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { fetchConsoleRuntime } from '../../api/runtime';

type AreaCard = {
  path: string;
  titleKey: string;
  descKey: string;
  icon: ReactNode;
};

const AREAS: AreaCard[] = [
  {
    path: '/templates',
    titleKey: 'nav.templates',
    descKey: 'home.area.templates',
    icon: <FileTextOutlined />,
  },
  {
    path: '/datasources',
    titleKey: 'nav.datasources',
    descKey: 'home.area.datasources',
    icon: <DatabaseOutlined />,
  },
  {
    path: '/jobs',
    titleKey: 'nav.jobs',
    descKey: 'home.area.jobs',
    icon: <HistoryOutlined />,
  },
  {
    path: '/schedules',
    titleKey: 'nav.schedules',
    descKey: 'home.area.schedules',
    icon: <ClockCircleOutlined />,
  },
];

/**
 * Console home with runtime status, getting-started workflow, and navigation cards.
 */
export function HomePage() {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const runtimeQuery = useQuery({
    queryKey: ['console-runtime'],
    queryFn: fetchConsoleRuntime,
  });

  const runtime = runtimeQuery.data;

  return (
    <section data-testid="console-home">
      <div className="home-hero">
        <div className="home-hero-kicker">{t('home.runtime.title')}</div>
        <Typography.Title level={2} className="home-hero-title">
          {t('home.title')}
        </Typography.Title>
        <Typography.Paragraph type="secondary" style={{ marginBottom: 0, maxWidth: 720 }}>
          {t('home.subtitle')}
        </Typography.Paragraph>
      </div>

      <Typography.Title level={5} className="home-section-title">
        {t('home.runtime.title')}
      </Typography.Title>
      <div className="home-runtime-grid">
        <div className="home-runtime-card">
          <div className="home-runtime-label">{t('home.runtime.v1')}</div>
          <Tag color={runtime?.v1ExecutionEnabled ? 'cyan' : 'default'}>
            {runtime?.v1ExecutionEnabled ? t('runtime.v1Enabled') : t('runtime.v1Disabled')}
          </Tag>
        </div>
        <div className="home-runtime-card">
          <div className="home-runtime-label">{t('home.runtime.schedule')}</div>
          <Tag color={runtime?.scheduleEnabled ? 'blue' : 'default'}>
            {runtime?.scheduleEnabled ? t('home.runtime.on') : t('home.runtime.off')}
          </Tag>
        </div>
        <div className="home-runtime-card">
          <div className="home-runtime-label">{t('home.runtime.distributed')}</div>
          <Tag color={runtime?.distributedEnabled ? 'purple' : 'default'}>
            {runtime?.distributedEnabled ? t('home.runtime.on') : t('home.runtime.off')}
          </Tag>
        </div>
      </div>

      <Typography.Title level={5} className="home-section-title">
        {t('home.workflow.title')}
      </Typography.Title>
      <div className="home-workflow">
        <Steps
          direction="vertical"
          size="small"
          items={[
            { title: t('home.workflow.step1.title'), description: t('home.workflow.step1.desc') },
            { title: t('home.workflow.step2.title'), description: t('home.workflow.step2.desc') },
            { title: t('home.workflow.step3.title'), description: t('home.workflow.step3.desc') },
            { title: t('home.workflow.step4.title'), description: t('home.workflow.step4.desc') },
          ]}
        />
      </div>

      <Typography.Title level={5} className="home-section-title">
        {t('home.areas.title')}
      </Typography.Title>
      <Row gutter={[16, 16]}>
        {AREAS.map((area) => (
          <Col xs={24} sm={12} md={8} lg={6} key={area.path}>
            <Card hoverable className="home-area-card" onClick={() => navigate(area.path)}>
              <div className="home-area-card-icon">{area.icon}</div>
              <Typography.Text strong>{t(area.titleKey)}</Typography.Text>
              <Typography.Paragraph type="secondary" style={{ marginBottom: 0, marginTop: 8 }}>
                {t(area.descKey)}
              </Typography.Paragraph>
            </Card>
          </Col>
        ))}
      </Row>
    </section>
  );
}
