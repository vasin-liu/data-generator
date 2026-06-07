import { useQuery } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import {
  AuditOutlined,
  ClockCircleOutlined,
  DatabaseOutlined,
  FileTextOutlined,
  HistoryOutlined,
  HomeOutlined,
  MoonOutlined,
  SunOutlined,
} from '@ant-design/icons';
import { Segmented, Select } from 'antd';
import { useTranslation } from 'react-i18next';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { fetchConsoleRuntime } from '../../api/runtime';
import { useConsoleTheme } from '../../theme/ThemeProvider';
import type { ConsoleThemeMode } from '../../theme/types';

type NavItem = {
  key: string;
  testId: string;
  label: string;
  icon: ReactNode;
};

/**
 * Apple-inspired glass shell: floating top dock navigation (no sidebar).
 */
export function ConsoleLayout() {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const { mode, setMode } = useConsoleTheme();

  const runtimeQuery = useQuery({
    queryKey: ['console-runtime'],
    queryFn: fetchConsoleRuntime,
  });

  const selectedKey = (() => {
    const p = location.pathname;
    if (p.includes('/templates')) return '/templates';
    if (p.includes('/jobs')) return '/jobs';
    if (p.includes('/schedules')) return '/schedules';
    if (p.includes('/audit')) return '/audit';
    if (p.includes('/datasources')) return '/datasources';
    return '/';
  })();

  const navItems: NavItem[] = [
    { key: '/', testId: 'nav-home', label: t('nav.home'), icon: <HomeOutlined /> },
    { key: '/templates', testId: 'nav-templates', label: t('nav.templates'), icon: <FileTextOutlined /> },
    { key: '/datasources', testId: 'nav-datasources', label: t('nav.datasources'), icon: <DatabaseOutlined /> },
    { key: '/jobs', testId: 'nav-jobs', label: t('nav.jobs'), icon: <HistoryOutlined /> },
    { key: '/schedules', testId: 'nav-schedules', label: t('nav.schedules'), icon: <ClockCircleOutlined /> },
    { key: '/audit', testId: 'nav-audit', label: t('nav.audit'), icon: <AuditOutlined /> },
  ];

  const v1Enabled = runtimeQuery.data?.v1ExecutionEnabled ?? false;

  return (
    <div className="console-shell" data-testid="console-shell">
      <div className="console-ambient" aria-hidden="true" />

      <header className="console-topdock">
        <div className="console-topdock-inner">
          <button
            type="button"
            className="console-brand"
            data-testid="console-brand"
            onClick={() => navigate('/')}
          >
            <span className="console-brand-icon">DG</span>
            <span className="console-brand-copy">
              <span className="console-brand-title">Data Generator</span>
              <span className="console-brand-sub">{t('layout.controlPlane')}</span>
            </span>
          </button>

          <nav className="console-nav-rail" aria-label={t('layout.mainNav')}>
            {navItems.map((item) => {
              const active = selectedKey === item.key;
              return (
                <button
                  key={item.key}
                  type="button"
                  data-testid={item.testId}
                  className={`console-nav-pill${active ? ' is-active' : ''}`}
                  aria-current={active ? 'page' : undefined}
                  onClick={() => navigate(item.key)}
                >
                  <span className="console-nav-pill-icon">{item.icon}</span>
                  <span className="console-nav-pill-label">{item.label}</span>
                </button>
              );
            })}
          </nav>

          <div className="console-topdock-tools">
            <div className="console-header-meta">
              <span className={`console-runtime-pill${v1Enabled ? ' is-on' : ''}`}>
                {v1Enabled ? t('runtime.v1Enabled') : t('runtime.v1Disabled')}
              </span>
              {runtimeQuery.data?.scheduleEnabled ? (
                <span className="console-runtime-pill is-on">{t('home.runtime.scheduleOn')}</span>
              ) : null}
              {runtimeQuery.data?.distributedEnabled ? (
                <span className="console-runtime-pill is-on">{t('home.runtime.distributedOn')}</span>
              ) : null}
            </div>

            <Segmented
              size="small"
              className="console-theme-toggle"
              data-testid="theme-toggle"
              value={mode}
              onChange={(value) => setMode(value as ConsoleThemeMode)}
              options={[
                {
                  value: 'light',
                  icon: <SunOutlined />,
                  label: <span data-testid="theme-light">{t('theme.light')}</span>,
                },
                {
                  value: 'dark',
                  icon: <MoonOutlined />,
                  label: <span data-testid="theme-dark">{t('theme.dark')}</span>,
                },
              ]}
            />

            <Select
              size="small"
              className="console-locale-select"
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
          </div>
        </div>
      </header>

      <main className="console-stage">
        <div className="console-glass-panel">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
