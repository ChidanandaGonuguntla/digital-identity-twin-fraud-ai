import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { ProtectedRoute } from '@/app/ProtectedRoute';
import { RoleRoute } from '@/app/RoleRoute';
import { Shell } from '@/components/layout/Shell';
import { LoginPage } from '@/pages/LoginPage';
import { OidcCallbackPage } from '@/pages/OidcCallbackPage';
import { UnauthorizedPage } from '@/pages/UnauthorizedPage';
import { WelcomePage } from '@/pages/WelcomePage';
import { CommandCenterPage } from '@/pages/CommandCenterPage';
import { LiveDecisionsPage } from '@/pages/LiveDecisionsPage';
import { TwinExplorerPage } from '@/pages/TwinExplorerPage';
import { ModelMonitoringPage } from '@/pages/ModelMonitoringPage';
import { ModelGovernancePage } from '@/pages/ModelGovernancePage';
import { PlatformOpsPage } from '@/pages/PlatformOpsPage';
import { AuditTrailPage } from '@/pages/AuditTrailPage';
import { CaseManagementPage } from '@/pages/CaseManagementPage';
import { StepUpPage } from '@/pages/StepUpPage';
import { FeatureStorePage } from '@/pages/FeatureStorePage';
import { SettingsPage } from '@/pages/SettingsPage';

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/login/callback" element={<OidcCallbackPage />} />
        <Route path="/unauthorized" element={<UnauthorizedPage />} />
        <Route element={<ProtectedRoute />}>
          <Route element={<RoleRoute />}>
            <Route element={<Shell />}>
              <Route path="/" element={<Navigate to="/welcome" replace />} />
              <Route path="/welcome" element={<WelcomePage />} />
              <Route path="/command-center" element={<CommandCenterPage />} />
              <Route path="/live-decisions" element={<LiveDecisionsPage />} />
              <Route path="/twins" element={<TwinExplorerPage />} />
              <Route path="/models" element={<ModelMonitoringPage />} />
              <Route path="/model-governance" element={<ModelGovernancePage />} />
              <Route path="/feature-store" element={<FeatureStorePage />} />
              <Route path="/ops" element={<PlatformOpsPage />} />
              <Route path="/audit" element={<AuditTrailPage />} />
              <Route path="/cases" element={<CaseManagementPage />} />
              <Route path="/step-up" element={<StepUpPage />} />
              <Route path="/settings" element={<SettingsPage />} />
            </Route>
          </Route>
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
