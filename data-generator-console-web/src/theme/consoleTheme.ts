import { theme, type ThemeConfig } from 'antd';
import type { ConsoleThemeMode } from './types';

/**
 * Ant Design theme tokens tuned for glass surfaces and readable contrast in each mode.
 */
export function buildConsoleTheme(mode: ConsoleThemeMode): ThemeConfig {
  const isDark = mode === 'dark';

  return {
    algorithm: isDark ? theme.darkAlgorithm : theme.defaultAlgorithm,
    token: {
      colorPrimary: isDark ? '#06b6d4' : '#0071e3',
      colorInfo: isDark ? '#38bdf8' : '#0071e3',
      colorSuccess: isDark ? '#34d399' : '#248a3d',
      colorWarning: isDark ? '#fbbf24' : '#b45309',
      colorError: isDark ? '#f87171' : '#d70015',
      colorBgBase: isDark ? '#070b14' : '#eef1f6',
      colorBgContainer: isDark ? 'rgba(15, 23, 42, 0.78)' : 'rgba(255, 255, 255, 0.82)',
      colorBgElevated: isDark ? 'rgba(17, 24, 39, 0.94)' : 'rgba(255, 255, 255, 0.94)',
      colorBorder: isDark ? 'rgba(148, 163, 184, 0.22)' : 'rgba(0, 0, 0, 0.1)',
      colorBorderSecondary: isDark ? 'rgba(148, 163, 184, 0.12)' : 'rgba(0, 0, 0, 0.06)',
      colorText: isDark ? '#f1f5f9' : '#1d1d1f',
      colorTextSecondary: isDark ? '#94a3b8' : '#6e6e73',
      colorTextLightSolid: '#ffffff',
      colorLink: isDark ? '#67e8f9' : '#0071e3',
      colorLinkHover: isDark ? '#a5f3fc' : '#0077ed',
      borderRadius: 12,
      borderRadiusLG: 16,
      fontFamily:
        "-apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'Segoe UI', system-ui, sans-serif",
      fontSize: 14,
      controlHeight: 36,
      wireframe: false,
    },
    components: {
      Layout: {
        bodyBg: 'transparent',
        headerBg: 'transparent',
        siderBg: 'transparent',
      },
      Button: {
        primaryShadow: isDark ? '0 0 0 2px rgba(6, 182, 212, 0.2)' : '0 1px 2px rgba(0, 0, 0, 0.08)',
        defaultBg: isDark ? 'rgba(15, 23, 42, 0.55)' : 'rgba(255, 255, 255, 0.85)',
        defaultColor: isDark ? '#e2e8f0' : '#1d1d1f',
        defaultBorderColor: isDark ? 'rgba(148, 163, 184, 0.28)' : 'rgba(0, 0, 0, 0.12)',
      },
      Card: {
        colorBgContainer: isDark ? 'rgba(15, 23, 42, 0.55)' : 'rgba(255, 255, 255, 0.78)',
        colorBorderSecondary: isDark ? 'rgba(148, 163, 184, 0.18)' : 'rgba(0, 0, 0, 0.08)',
      },
      Table: {
        headerBg: isDark ? 'rgba(15, 23, 42, 0.88)' : 'rgba(245, 245, 247, 0.95)',
        rowHoverBg: isDark ? 'rgba(6, 182, 212, 0.08)' : 'rgba(0, 113, 227, 0.06)',
        headerColor: isDark ? '#cbd5e1' : '#1d1d1f',
      },
      Segmented: {
        itemSelectedBg: isDark ? 'rgba(6, 182, 212, 0.22)' : '#ffffff',
        itemSelectedColor: isDark ? '#ecfeff' : '#0071e3',
        trackBg: isDark ? 'rgba(15, 23, 42, 0.55)' : 'rgba(0, 0, 0, 0.06)',
      },
      Tag: {
        defaultBg: isDark ? 'rgba(51, 65, 85, 0.55)' : 'rgba(0, 0, 0, 0.06)',
        defaultColor: isDark ? '#e2e8f0' : '#1d1d1f',
      },
      Tabs: {
        itemColor: isDark ? '#94a3b8' : '#6e6e73',
        itemSelectedColor: isDark ? '#ecfeff' : '#0071e3',
        inkBarColor: isDark ? '#06b6d4' : '#0071e3',
      },
    },
  };
}
