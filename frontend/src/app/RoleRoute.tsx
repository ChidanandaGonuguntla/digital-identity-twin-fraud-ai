import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { canAccessRoute } from '@/config/pagePermissions';
import { useAuthStore } from '@/store/authStore';

export function RoleRoute() {
  const user = useAuthStore((state) => state.user);
  const location = useLocation();

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  if (!canAccessRoute(user.role, location.pathname)) {
    return <Navigate to="/unauthorized" replace />;
  }

  return <Outlet />;
}
