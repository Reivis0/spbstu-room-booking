import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../../store/useAuthStore';

interface AdminRouteProps {
  children: React.ReactNode;
}

const AdminRoute: React.FC<AdminRouteProps> = ({ children }) => {
  const { user, isAuthenticated, isLoading } = useAuthStore();
  const location = useLocation();

  if (isLoading) {
    return <div className="loading">Загрузка...</div>;
  }

  const token = localStorage.getItem('accessToken');

  if (!isAuthenticated && !token) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (user && user.role !== 'admin') {
    return <Navigate to="/my-bookings" replace />;
  }

  return <>{children}</>;
};

export default AdminRoute;
