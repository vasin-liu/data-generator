import { Route, Routes } from 'react-router-dom';
import { ConsoleLayout } from './layout/ConsoleLayout';
import { DatasourcesPage } from './pages/DatasourcesPage';
import { HomePage } from './pages/HomePage';
import { JobDetailPage } from './pages/JobDetailPage';
import { JobsPage } from './pages/JobsPage';
import { MigrationPage } from './pages/MigrationPage';
import { TemplateEditorPage } from './pages/TemplateEditorPage';
import { TemplatesPage } from './pages/TemplatesPage';

/**
 * Root route tree for the operator console SPA.
 */
export function App() {
  return (
    <Routes>
      <Route element={<ConsoleLayout />}>
        <Route index element={<HomePage />} />
        <Route path="templates" element={<TemplatesPage />} />
        <Route path="templates/new" element={<TemplateEditorPage />} />
        <Route path="templates/:id" element={<TemplateEditorPage />} />
        <Route path="datasources" element={<DatasourcesPage />} />
        <Route path="jobs" element={<JobsPage />} />
        <Route path="jobs/:instanceId" element={<JobDetailPage />} />
        <Route path="migration" element={<MigrationPage />} />
      </Route>
    </Routes>
  );
}
