import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import enUS from 'antd/locale/en_US';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { App } from './app/App';
import i18n from './i18n';
import './index.css';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: 1, refetchOnWindowFocus: false },
  },
});

function antLocale() {
  return i18n.language.startsWith('zh') ? zhCN : enUS;
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <ConfigProvider locale={antLocale()}>
        <BrowserRouter basename="/console">
          <App />
        </BrowserRouter>
      </ConfigProvider>
    </QueryClientProvider>
  </StrictMode>,
);
