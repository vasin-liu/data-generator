import { Card, Col, Row, Typography } from 'antd';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';

const AREAS = [
  { path: '/templates', titleKey: 'nav.templates' },
  { path: '/datasources', titleKey: 'nav.datasources' },
  { path: '/jobs', titleKey: 'nav.jobs' },
  { path: '/schedules', titleKey: 'nav.schedules' },
  { path: '/migration', titleKey: 'nav.migration' },
] as const;

/**
 * Console home with navigation cards.
 */
export function HomePage() {
  const { t } = useTranslation();
  const navigate = useNavigate();

  return (
    <section>
      <Typography.Title level={3}>{t('home.title')}</Typography.Title>
      <Typography.Paragraph type="secondary">{t('home.subtitle')}</Typography.Paragraph>
      <Row gutter={[16, 16]}>
        {AREAS.map((area) => (
          <Col xs={24} sm={12} md={6} key={area.path}>
            <Card hoverable onClick={() => navigate(area.path)}>
              <Typography.Text strong>{t(area.titleKey)}</Typography.Text>
            </Card>
          </Col>
        ))}
      </Row>
    </section>
  );
}
