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
import EditBookingModal from '../components/EditBookingModal/EditBookingModal';
import CancelBookingModal from '../components/CancelBookingModal/CancelBookingModal';

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

const AdminBookingsPage: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { isAuthenticated, isLoading: authLoading, user } = useAuthStore();
  const { universityCode, setUniversityCode } = useUniversitySearchParam();
  const activeTab = (new URLSearchParams(window.location.search).get('university') || 'all') as BookingTab;

  const [editingBooking, setEditingBooking] = React.useState<Booking | null>(null);
  const [cancellingBooking, setCancellingBooking] = React.useState<Booking | null>(null);
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
    queryKey: ['bookings', 'all'],
    queryFn: () => bookingsApi.getAll(),
    enabled: !authLoading && user?.role === 'admin',
    refetchInterval: 5000,
  });

  const cancelMutation = useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) => bookingsApi.cancel(id, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['bookings'] });
      toast.success('Бронирование успешно отменено');
      setCancellingBooking(null);
    },
    onError: () => {
      toast.error('Не удалось отменить бронирование');
    }
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<import('../types').BookingFormData> }) => bookingsApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['bookings'] });
      toast.success('Бронирование успешно обновлено');
      setEditingBooking(null);
    },
    onError: (error: any) => {
      const message = error.response?.data?.message || 'Не удалось обновить бронирование';
      toast.error(message);
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

  React.useEffect(() => {
    if (bookings.length > 0) {
      const currentBookings: Record<string, string> = {};
      const prevBookings = prevBookingsRef.current;
      
      bookings.forEach((b) => {
        currentBookings[b.id] = b.status;
        
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
    const bookingToCancel = bookings.find((b: Booking) => b.id === id);
    if (bookingToCancel) {
      setCancellingBooking(bookingToCancel);
    }
  };

  const handleConfirmCancel = async (id: string, reason: string) => {
    const token = localStorage.getItem('accessToken');
    if (!isAuthenticated && !token) {
      navigate('/login');
      return;
    }

    try {
      await cancelMutation.mutateAsync({ id, reason });
    } catch (err: any) {
      if (err.response?.status === 401 || err.response?.status === 403) {
        navigate('/login');
      }
    }
  };

  const handleEdit = (booking: Booking) => {
    setEditingBooking(booking);
  };

  const handleSaveEdit = async (id: string, data: { startsAt: string; endsAt: string }) => {
    await updateMutation.mutateAsync({ id, data: { roomId: editingBooking?.roomId, ...data } });
  };

  const setTab = (tab: BookingTab) => {
    if (tab === 'all') {
      const params = new URLSearchParams(window.location.search);
      params.delete('university');
      navigate(`/admin/bookings${params.toString() ? `?${params.toString()}` : ''}`);
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
          <h1>Управление бронированиями</h1>
          <p>Просмотр всех бронирований системы с возможностью отмены</p>
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
                <h2>Активные бронирования</h2>
                <div className="bookings-grid">
                  {upcomingBookings.map((booking: Booking) => (
                    <BookingCard key={booking.id} booking={booking} onCancel={handleCancel} onEdit={handleEdit} isAdminView={true} />
                  ))}
                </div>
              </section>
            )}

            {pastBookings.length > 0 && (
              <section className="bookings-section">
                <h2>Прошлые / Отмененные бронирования</h2>
                <div className="bookings-grid">
                  {pastBookings.map((booking: Booking) => (
                    <BookingCard key={booking.id} booking={booking} onCancel={handleCancel} onEdit={handleEdit} readOnly isAdminView={true} />
                  ))}
                </div>
              </section>
            )}
          </>
        )}
      </div>

      <EditBookingModal 
        booking={editingBooking}
        isOpen={editingBooking !== null}
        onClose={() => setEditingBooking(null)}
        onSave={handleSaveEdit}
      />

      <CancelBookingModal 
        booking={cancellingBooking}
        isOpen={cancellingBooking !== null}
        onClose={() => setCancellingBooking(null)}
        onConfirm={handleConfirmCancel}
      />
    </section>
  );
};

export default AdminBookingsPage;
