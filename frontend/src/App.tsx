import React, { useEffect } from 'react';
import { QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Layout from './components/Layout/Layout';
import ProtectedRoute from './components/ProtectedRoute/ProtectedRoute';
import HomePage from './pages/HomePage';
import RoomsPage from './pages/RoomsPage';
import BookingPage from './pages/BookingPage';
import MyBookingsPage from './pages/MyBookingsPage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import SchedulePage from './pages/SchedulePage';
import AdminImportPage from './pages/AdminImportPage';
import { useAuthStore } from './store/useAuthStore';
import { queryClient } from './shared/api/queryClient';
import { useBookingRealtime } from './shared/ws/useBookingRealtime';

const RealtimeBridge: React.FC = () => {
  useBookingRealtime(queryClient);
  return null;
};

const App: React.FC = () => {
  const { checkAuth } = useAuthStore();

  useEffect(() => {
    checkAuth();
  }, [checkAuth]);

  return (
    <QueryClientProvider client={queryClient}>
      <Router>
        <RealtimeBridge />
        <Layout>
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route
              path="/rooms"
              element={<RoomsPage />}
            />
            <Route 
              path="/booking/:roomId?" 
              element={
                <ProtectedRoute>
                  <BookingPage />
                </ProtectedRoute>
              } 
            />
            <Route 
              path="/my-bookings" 
              element={
                <ProtectedRoute>
                  <MyBookingsPage />
                </ProtectedRoute>
              } 
            />
            <Route
              path="/schedule"
              element={<SchedulePage />}
            />
            <Route
              path="/admin"
              element={
                <ProtectedRoute>
                  <AdminImportPage />
                </ProtectedRoute>
              }
            />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
          </Routes>
        </Layout>
      </Router>
    </QueryClientProvider>
  );
};

export default App;
