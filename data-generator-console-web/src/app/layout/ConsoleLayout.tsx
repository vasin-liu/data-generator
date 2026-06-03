import { useQuery } from '@tanstack/react-query';
import { Layout, Menu, Select, Tag } from 'antd';
import type { MenuProps } from 'antd';
import { useTranslation } from 'react-i18next';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { fetchConsoleRuntime } from '../../api/runtime';
import { migrationUiEnabled } from '../../config/features';

const { Header, Sider, Content } = Layout;

/**
 * Shell layout: sidebar navigation, V1 runtime banner, locale switch.
 */
export function ConsoleLayout() {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();

  const runtimeQuery = useQuery({
    queryKey: ['console-runtime'],
    queryFn: fetchConsoleRuntime,
  });

  const selectedKey = (() => {
    const p = location.pathname;
    if (p.includes('/templates')) return '/templates';
    if (p.includes('/jobs')) return '/jobs';
    if (p.includes('/schedules')) return '/schedules';
    if (p.includes('/datasources')) return '/datasources';
    if (p.includes('/migration')) return '/migration';
    return '/';
  })();

  const menuItems: MenuProps['items'] = [
    { key: '/', label: t('nav.home') },
    { key: '/templates', label: t('nav.templates') },
    { key: '/datasources', label: t('nav.datasources') },
    { key: '/jobs', label: t('nav.jobs') },
    { key: '/schedules', label: t('nav.schedules') },
    ...(migrationUiEnabled ? [{ key: '/migration', label: t('nav.migration') }] : []),
  ];

  const v1Enabled = runtimeQuery.data?.v1ExecutionEnabled ?? false;

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider width={220} theme="dark">
        <div className="console-brand-sider">Data Generator</div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[selectedKey]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>
      <Layout>
        <Header className="console-header">
          <Tag color={v1Enabled ? 'green' : 'default'}>
            {v1Enabled ? t('runtime.v1Enabled') : t('runtime.v1Disabled')}
          </Tag>
          <Select
            size="small"
            style={{ width: 100 }}
            value={i18n.language.startsWith('zh') ? 'zh-CN' : 'en'}
            onChange={(lng) => {
              void i18n.changeLanguage(lng);
              localStorage.setItem('dg.locale', lng);
            }}
            options={[
              { value: 'zh-CN', label: '中文' },
              { value: 'en', label: 'English' },
            ]}
          />
        </Header>
        <Content className="console-content">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
