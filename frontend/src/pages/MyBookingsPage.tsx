import React, { useMemo } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
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

const translateReason = (reason?: string): string => {
  if (!reason) return 'Причина не указана';
  
  const mapping: Record<string, string> = {
    'external_step_failed': 'Техническая ошибка при проверке доступности',
    'availability_conflict': 'Аудитория уже занята на это время',
    'user_booking_limit_exceeded': 'Превышен лимит ваших активных бронирований',
    'booking_not_found': 'Запись не найдена',
  };

  return mapping[reason] || reason;
};

const MyBookingsPage: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { isAuthenticated, isLoading: authLoading } = useAuthStore();
  const { universityCode, setUniversityCode } = useUniversitySearchParam();
  const activeTab = (new URLSearchParams(window.location.search).get('university') || 'all') as BookingTab;

  const prevBookingsRef = React.useRef<Record<string, string>>({});

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
    refetchInterval: 5000, // Поллинг каждые 5 секунд для реактивности
  });

  const cancelMutation = useMutation({
    mutationFn: bookingsApi.cancel,
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: ['bookings'] });
      toast.success('Бронирование успешно отменено');
    },
    onError: () => {
      toast.error('Не удалось отменить бронирование');
    }
  });

  const roomsMap = useMemo(() => {
    return new Map((roomsQuery.data || []).map((room: Room) => [room.id, room]));
  }, [roomsQuery.data]);

  const bookings = useMemo(() => {
    return (bookingsQuery.data || []).map((booking: Booking) => {
      const room = roomsMap.get(booking.roomId);
      return {
        ...booking,
        startTime: booking.startsAt ? new Date(booking.startsAt) : undefined,
        endTime: booking.endsAt ? new Date(booking.endsAt) : undefined,
        roomName: booking.roomName || room?.name || `Аудитория #${booking.roomId.slice(0, 8)}`,
        universityCode: getBookingUniversity(booking, roomsMap),
      };
    });
  }, [bookingsQuery.data, roomsMap]);

  // Отслеживание изменений статуса для уведомлений
  React.useEffect(() => {
    if (bookings.length > 0) {
      const currentBookings: Record<string, string> = {};
      const prevBookings = prevBookingsRef.current;
      
      bookings.forEach((b) => {
        currentBookings[b.id] = b.status;
        
        // Если статус изменился по сравнению с предыдущим запросом
        if (prevBookings[b.id] && prevBookings[b.id] !== b.status) {
          const roomName = b.roomName;
          if (b.status === 'confirmed') {
            toast.success(`Бронирование ${roomName} подтверждено!`, { duration: 5000 });
          } else if (b.status === 'rejected') {
            toast.error(
              `Отказ: ${roomName}. ${translateReason(b.cancellationReason)}`,
              { duration: 8000 }
            );
          }
        }
      });
      prevBookingsRef.current = currentBookings;
    }
  }, [bookings]);

  const visibleBookings = activeTab === 'all'
    ? bookings
    : bookings.filter((booking: Booking) => normalizeUniversityCode(booking.universityCode) === activeTab);

  const upcomingBookings = visibleBookings
    .filter((booking: Booking) => {
      const startTime = booking.startTime || (booking.startsAt ? new Date(booking.startsAt) : new Date());
      return startTime > new Date() && !['cancelled', 'rejected'].includes(booking.status);
    })
    .sort((a: Booking, b: Booking) => {
      const aTime = a.startTime || (a.startsAt ? new Date(a.startsAt) : new Date());
      const bTime = b.startTime || (b.startsAt ? new Date(b.startsAt) : new Date());
      return aTime.getTime() - bTime.getTime();
    });

  const pastBookings = visibleBookings
    .filter((booking: Booking) => {
      const startTime = booking.startTime || (booking.startsAt ? new Date(booking.startsAt) : new Date());
      return startTime <= new Date() || ['cancelled', 'rejected'].includes(booking.status);
    })
    .sort((a: Booking, b: Booking) => {
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
      }
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
                  {upcomingBookings.map((booking: Booking) => (
                    <BookingCard key={booking.id} booking={booking} onCancel={handleCancel} />
                  ))}
                </div>
              </section>
            )}

            {pastBookings.length > 0 && (
              <section className="bookings-section">
                <h2>Прошедшие бронирования</h2>
                <div className="bookings-grid">
                  {pastBookings.map((booking: Booking) => (
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
