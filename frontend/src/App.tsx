import React, { useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Layout from './components/Layout/Layout';
import ProtectedRoute from './components/ProtectedRoute/ProtectedRoute';
import HomePage from './pages/HomePage';
import RoomsPage from './pages/RoomsPage';
import BookingPage from './pages/BookingPage';
import MyBookingsPage from './pages/MyBookingsPage';
import LoginPage from './pages/LoginPage';
import SchedulePage from './pages/SchedulePage';
import { useAuthStore } from './store/useAuthStore';

const App: React.FC = () => {
  const { checkAuth } = useAuthStore();

  useEffect(() => {
    checkAuth();
  }, [checkAuth]);

  return (
    <Router>
      <Layout>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/rooms" element={<RoomsPage />} />
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
          <Route path="/schedule" element={<SchedulePage />} />
          <Route path="/login" element={<LoginPage />} />
        </Routes>
      </Layout>
    </Router>
  );
};

export default App;

