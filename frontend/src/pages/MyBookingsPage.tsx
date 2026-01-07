import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Booking } from '../types';
import { bookingsApi } from '../api/bookings';
import { roomsApi } from '../api/rooms';
import { useAuthStore } from '../store/useAuthStore';
import BookingCard from '../components/BookingCard/BookingCard';

const MyBookingsPage: React.FC = () => {
  const navigate = useNavigate();
  const { isAuthenticated, isLoading: authLoading } = useAuthStore();
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (authLoading) {
      return;
    }
    
    const token = localStorage.getItem('accessToken');
    if (!isAuthenticated && !token) {
      navigate('/login');
      return;
    }

    loadBookings();
  }, [isAuthenticated, authLoading, navigate]);

  const loadBookings = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await bookingsApi.getMyBookings();
      
      const roomIds = Array.from(new Set(data.map(b => b.roomId)));
      const roomsMap = new Map();
      
      await Promise.all(
        roomIds.map(async (roomId) => {
          try {
            const room = await roomsApi.getById(roomId);
            roomsMap.set(roomId, room);
          } catch (err) {
            console.warn(`[MyBookingsPage] Failed to load room ${roomId}:`, err);
          }
        })
      );
      
      const bookingsWithDates = data.map((booking) => {
        const room = roomsMap.get(booking.roomId);
        return {
          ...booking,
          startTime: booking.startsAt ? new Date(booking.startsAt) : undefined,
          endTime: booking.endsAt ? new Date(booking.endsAt) : undefined,
          roomName: room?.name || `Аудитория ${room?.code || booking.roomId}`,
        };
      });
      setBookings(bookingsWithDates);
    } catch (err: any) {
      if (err.response?.status === 401 || err.response?.status === 403) {
        return;
      }
      setError('Не удалось загрузить бронирования. Попробуйте позже.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = async (id: string) => {
    if (!window.confirm('Вы уверены, что хотите отменить бронирование?')) {
      return;
    }

    const token = localStorage.getItem('accessToken');
    if (!isAuthenticated && !token) {
      navigate('/login');
      return;
    }

    try {
      await bookingsApi.cancel(id);
      setBookings(bookings.map(booking => 
        booking.id === id 
          ? { ...booking, status: 'cancelled' as const }
          : booking
      ));
    } catch (err: any) {
      if (err.response?.status === 401 || err.response?.status === 403) {
        navigate('/login');
        return;
      }
      alert('Не удалось отменить бронирование. Попробуйте позже.');
      console.error(err);
    }
  };

  const upcomingBookings = bookings
    .filter((b) => {
      const startTime = b.startTime || (b.startsAt ? new Date(b.startsAt) : new Date());
      return startTime > new Date() && b.status !== 'cancelled';
    })
    .sort((a, b) => {
      const aTime = a.startTime || (a.startsAt ? new Date(a.startsAt) : new Date());
      const bTime = b.startTime || (b.startsAt ? new Date(b.startsAt) : new Date());
      return aTime.getTime() - bTime.getTime();
    });

  const pastBookings = bookings
    .filter((b) => {
      const startTime = b.startTime || (b.startsAt ? new Date(b.startsAt) : new Date());
      return startTime <= new Date() || b.status === 'cancelled';
    })
    .sort((a, b) => {
      const aTime = a.startTime || (a.startsAt ? new Date(a.startsAt) : new Date());
      const bTime = b.startTime || (b.startsAt ? new Date(b.startsAt) : new Date());
      return bTime.getTime() - aTime.getTime();
    });

  if (loading) {
    return <div className="loading">Загрузка...</div>;
  }

  if (error) {
    return <div className="error">{error}</div>;
  }

  return (
    <section className="my-bookings-page">
      <div className="container">
        <div className="bookings-header">
          <h1>Мои бронирования</h1>
          <p>Управляйте своими бронированиями аудиторий</p>
        </div>

        {bookings.length === 0 ? (
          <div className="no-bookings">
            <p>У вас пока нет бронирований</p>
          </div>
        ) : (
          <>
            {upcomingBookings.length > 0 && (
              <section className="bookings-section">
                <h2>Предстоящие бронирования</h2>
                <div className="bookings-grid">
                  {upcomingBookings.map((booking) => (
                    <BookingCard
                      key={booking.id}
                      booking={booking}
                      onCancel={handleCancel}
                    />
                  ))}
                </div>
              </section>
            )}

            {pastBookings.length > 0 && (
              <section className="bookings-section">
                <h2>Прошедшие бронирования</h2>
                <div className="bookings-grid">
                  {pastBookings.map((booking) => (
                    <BookingCard
                      key={booking.id}
                      booking={booking}
                      onCancel={handleCancel}
                      readOnly
                    />
                  ))}
                </div>
              </section>
            )}
          </>
        )}
      </div>
    </section>
  );
};

export default MyBookingsPage;

