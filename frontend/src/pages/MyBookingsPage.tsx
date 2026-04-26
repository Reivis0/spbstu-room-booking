import React, { useMemo } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { Booking, Room } from '../types';
import { bookingsApi } from '../api/bookings';
import { roomsApi } from '../api/rooms';
import { useAuthStore } from '../store/useAuthStore';
import BookingCard from '../components/BookingCard/BookingCard';
import { UNIVERSITIES, UniversityCode, normalizeUniversityCode } from '../shared/university/universities';
import { useUniversitySearchParam } from '../shared/university/useUniversitySearchParam';

type BookingTab = 'all' | UniversityCode;

const getBookingUniversity = (booking: Booking, roomsMap: Map<string, Room>) => {
  const room = booking.room || roomsMap.get(booking.roomId);
  return normalizeUniversityCode(
    booking.universityCode ||
      booking.university ||
      room?.universityCode ||
      room?.university
  );
};

const MyBookingsPage: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { isAuthenticated, isLoading: authLoading } = useAuthStore();
  const { universityCode, setUniversityCode } = useUniversitySearchParam();
  const activeTab = (new URLSearchParams(window.location.search).get('university') || 'all') as BookingTab;

  React.useEffect(() => {
    if (authLoading) return;

    const token = localStorage.getItem('accessToken');
    if (!isAuthenticated && !token) {
      navigate('/login');
    }
  }, [authLoading, isAuthenticated, navigate]);

  const roomsQuery = useQuery({
    queryKey: ['rooms', 'all-for-bookings'],
    queryFn: () => roomsApi.getAll(),
  });

  const bookingsQuery = useQuery({
    queryKey: ['bookings', 'my'],
    queryFn: () => bookingsApi.getMyBookings(),
    enabled: !authLoading,
  });

  const cancelMutation = useMutation({
    mutationFn: bookingsApi.cancel,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['bookings'] });
    },
  });

  const roomsMap = useMemo(() => {
    return new Map((roomsQuery.data || []).map((room) => [room.id, room]));
  }, [roomsQuery.data]);

  const bookings = useMemo(() => {
    return (bookingsQuery.data || []).map((booking) => {
      const room = roomsMap.get(booking.roomId);
      return {
        ...booking,
        startTime: booking.startsAt ? new Date(booking.startsAt) : undefined,
        endTime: booking.endsAt ? new Date(booking.endsAt) : undefined,
        roomName: booking.roomName || room?.name || `Аудитория ${room?.code || booking.roomId}`,
        universityCode: getBookingUniversity(booking, roomsMap),
      };
    });
  }, [bookingsQuery.data, roomsMap]);

  const visibleBookings = activeTab === 'all'
    ? bookings
    : bookings.filter((booking) => normalizeUniversityCode(booking.universityCode) === activeTab);

  const upcomingBookings = visibleBookings
    .filter((booking) => {
      const startTime = booking.startTime || (booking.startsAt ? new Date(booking.startsAt) : new Date());
      return startTime > new Date() && booking.status !== 'cancelled';
    })
    .sort((a, b) => {
      const aTime = a.startTime || (a.startsAt ? new Date(a.startsAt) : new Date());
      const bTime = b.startTime || (b.startsAt ? new Date(b.startsAt) : new Date());
      return aTime.getTime() - bTime.getTime();
    });

  const pastBookings = visibleBookings
    .filter((booking) => {
      const startTime = booking.startTime || (booking.startsAt ? new Date(booking.startsAt) : new Date());
      return startTime <= new Date() || booking.status === 'cancelled';
    })
    .sort((a, b) => {
      const aTime = a.startTime || (a.startsAt ? new Date(a.startsAt) : new Date());
      const bTime = b.startTime || (b.startsAt ? new Date(b.startsAt) : new Date());
      return bTime.getTime() - aTime.getTime();
    });

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
      await cancelMutation.mutateAsync(id);
    } catch (err: any) {
      if (err.response?.status === 401 || err.response?.status === 403) {
        navigate('/login');
        return;
      }
      alert('Не удалось отменить бронирование. Попробуйте позже.');
      console.error(err);
    }
  };

  const setTab = (tab: BookingTab) => {
    if (tab === 'all') {
      const params = new URLSearchParams(window.location.search);
      params.delete('university');
      navigate(`/my-bookings${params.toString() ? `?${params.toString()}` : ''}`);
      return;
    }
    setUniversityCode(tab);
  };

  if (bookingsQuery.isLoading || roomsQuery.isLoading || authLoading) {
    return <div className="loading">Загрузка...</div>;
  }

  if (bookingsQuery.error || roomsQuery.error) {
    return <div className="error">Не удалось загрузить бронирования. Попробуйте позже.</div>;
  }

  return (
    <section className="my-bookings-page">
      <div className="container">
        <div className="bookings-header">
          <h1>Мои бронирования</h1>
          <p>Управляйте своими бронированиями аудиторий и фильтруйте их по вузам</p>
        </div>

        <div className="tabs bookings-tabs" role="tablist" aria-label="Фильтр бронирований по вузу">
          <button
            type="button"
            className={`tabs__btn${activeTab === 'all' ? ' is-active' : ''}`}
            role="tab"
            aria-selected={activeTab === 'all'}
            onClick={() => setTab('all')}
          >
            Все
          </button>
          {UNIVERSITIES.map((university) => (
            <button
              key={university.code}
              type="button"
              className={`tabs__btn${universityCode === university.code && activeTab !== 'all' ? ' is-active' : ''}`}
              role="tab"
              aria-selected={universityCode === university.code && activeTab !== 'all'}
              onClick={() => setTab(university.code)}
            >
              {university.label}
            </button>
          ))}
        </div>

        {visibleBookings.length === 0 ? (
          <div className="no-bookings">
            <p>Для выбранного фильтра бронирований пока нет</p>
          </div>
        ) : (
          <>
            {upcomingBookings.length > 0 && (
              <section className="bookings-section">
                <h2>Предстоящие бронирования</h2>
                <div className="bookings-grid">
                  {upcomingBookings.map((booking) => (
                    <BookingCard key={booking.id} booking={booking} onCancel={handleCancel} />
                  ))}
                </div>
              </section>
            )}

            {pastBookings.length > 0 && (
              <section className="bookings-section">
                <h2>Прошедшие бронирования</h2>
                <div className="bookings-grid">
                  {pastBookings.map((booking) => (
                    <BookingCard key={booking.id} booking={booking} onCancel={handleCancel} readOnly />
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
