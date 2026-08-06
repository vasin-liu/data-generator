import { Route, Routes } from 'react-router-dom';
import { AuditPage } from './pages/AuditPage';
import { ConsoleLayout } from './layout/ConsoleLayout';
import { DatasourcesPage } from './pages/DatasourcesPage';
import { HomePage } from './pages/HomePage';
import { JobDetailPage } from './pages/JobDetailPage';
import { JobsPage } from './pages/JobsPage';
import { SchedulesPage } from './pages/SchedulesPage';
import { TemplateEditorPage } from './pages/TemplateEditorPage';
import { TemplatesPage } from './pages/TemplatesPage';
import { GeoAssetsPage } from './pages/GeoAssetsPage';
import { UdfsPage } from './pages/UdfsPage';

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
        <Route path="schedules" element={<SchedulesPage />} />
        <Route path="udfs" element={<UdfsPage />} />
        <Route path="geo-assets" element={<GeoAssetsPage />} />
        <Route path="audit" element={<AuditPage />} />
      </Route>
    </Routes>
  );
}
