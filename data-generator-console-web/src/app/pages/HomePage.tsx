import type { ReactNode } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Card, Col, Descriptions, Row, Steps, Tag, Typography } from 'antd';
import {
  ClockCircleOutlined,
  DatabaseOutlined,
  FileTextOutlined,
  HistoryOutlined,
  SwapOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { fetchConsoleRuntime } from '../../api/runtime';
import { migrationUiEnabled } from '../../config/features';

type AreaCard = {
  path: string;
  titleKey: string;
  descKey: string;
  icon: ReactNode;
  migrationOnly?: boolean;
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
  {
    path: '/migration',
    titleKey: 'nav.migration',
    descKey: 'home.area.migration',
    icon: <SwapOutlined />,
    migrationOnly: true,
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
  const visibleAreas = AREAS.filter((area) => !area.migrationOnly || migrationUiEnabled);

  return (
    <section>
      <Typography.Title level={3}>{t('home.title')}</Typography.Title>
      <Typography.Paragraph type="secondary">{t('home.subtitle')}</Typography.Paragraph>

      <Typography.Title level={5}>{t('home.runtime.title')}</Typography.Title>
      <Descriptions bordered size="small" column={3} style={{ marginBottom: 24 }}>
        <Descriptions.Item label={t('home.runtime.v1')}>
          <Tag color={runtime?.v1ExecutionEnabled ? 'green' : 'default'}>
            {runtime?.v1ExecutionEnabled ? t('runtime.v1Enabled') : t('runtime.v1Disabled')}
          </Tag>
        </Descriptions.Item>
        <Descriptions.Item label={t('home.runtime.schedule')}>
          <Tag color={runtime?.scheduleEnabled ? 'blue' : 'default'}>
            {runtime?.scheduleEnabled ? t('home.runtime.on') : t('home.runtime.off')}
          </Tag>
        </Descriptions.Item>
        <Descriptions.Item label={t('home.runtime.distributed')}>
          <Tag color={runtime?.distributedEnabled ? 'purple' : 'default'}>
            {runtime?.distributedEnabled ? t('home.runtime.on') : t('home.runtime.off')}
          </Tag>
        </Descriptions.Item>
      </Descriptions>

      <Typography.Title level={5}>{t('home.workflow.title')}</Typography.Title>
      <Steps
        direction="vertical"
        size="small"
        style={{ marginBottom: 24, maxWidth: 720 }}
        items={[
          { title: t('home.workflow.step1.title'), description: t('home.workflow.step1.desc') },
          { title: t('home.workflow.step2.title'), description: t('home.workflow.step2.desc') },
          { title: t('home.workflow.step3.title'), description: t('home.workflow.step3.desc') },
          { title: t('home.workflow.step4.title'), description: t('home.workflow.step4.desc') },
        ]}
      />

      <Typography.Title level={5}>{t('home.areas.title')}</Typography.Title>
      <Row gutter={[16, 16]}>
        {visibleAreas.map((area) => (
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
