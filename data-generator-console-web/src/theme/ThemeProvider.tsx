import { App as AntApp, ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import enUS from 'antd/locale/en_US';
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import i18n from '../i18n';
import { buildConsoleTheme } from './consoleTheme';
import { THEME_STORAGE_KEY, type ConsoleThemeMode } from './types';

type ThemeContextValue = {
  mode: ConsoleThemeMode;
  setMode: (mode: ConsoleThemeMode) => void;
  toggleMode: () => void;
};

const ThemeContext = createContext<ThemeContextValue | null>(null);

function readInitialMode(): ConsoleThemeMode {
  const stored = localStorage.getItem(THEME_STORAGE_KEY);
  if (stored === 'light' || stored === 'dark') {
    return stored;
  }
  return window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark';
}

function antLocale() {
  return i18n.language.startsWith('zh') ? zhCN : enUS;
}

type Props = {
  children: ReactNode;
};

/**
 * Applies light/dark glass theme tokens to Ant Design and the document root.
 */
export function ThemeProvider({ children }: Props) {
  const [mode, setModeState] = useState<ConsoleThemeMode>(() => readInitialMode());
  const [localeKey, setLocaleKey] = useState(i18n.language);

  const setMode = useCallback((next: ConsoleThemeMode) => {
    setModeState(next);
  }, []);

  const toggleMode = useCallback(() => {
    setModeState((current) => (current === 'dark' ? 'light' : 'dark'));
  }, []);

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', mode);
    localStorage.setItem(THEME_STORAGE_KEY, mode);
    const meta = document.querySelector('meta[name="theme-color"]');
    meta?.setAttribute('content', mode === 'dark' ? '#070b14' : '#eef1f6');
  }, [mode]);

  useEffect(() => {
    const onLanguageChanged = (lng: string) => setLocaleKey(lng);
    i18n.on('languageChanged', onLanguageChanged);
    return () => i18n.off('languageChanged', onLanguageChanged);
  }, []);

  const themeConfig = useMemo(() => buildConsoleTheme(mode), [mode]);
  const contextValue = useMemo(
    () => ({ mode, setMode, toggleMode }),
    [mode, setMode, toggleMode],
  );

  return (
    <ThemeContext.Provider value={contextValue}>
      <ConfigProvider theme={themeConfig} locale={antLocale()} key={localeKey}>
        <AntApp>{children}</AntApp>
      </ConfigProvider>
    </ThemeContext.Provider>
  );
}

export function useConsoleTheme(): ThemeContextValue {
  const ctx = useContext(ThemeContext);
  if (!ctx) {
    throw new Error('useConsoleTheme must be used within ThemeProvider');
  }
  return ctx;
}
