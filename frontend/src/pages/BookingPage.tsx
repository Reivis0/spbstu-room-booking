import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { format } from 'date-fns';
import { Room, BookingFormData } from '../types';
import { roomsApi } from '../api/rooms';
import { bookingsApi } from '../api/bookings';
import { useAuthStore } from '../store/useAuthStore';

const BookingPage: React.FC = () => {
  const { roomId } = useParams<{ roomId?: string }>();
  const navigate = useNavigate();
  const { isAuthenticated, isLoading: authLoading } = useAuthStore();

  const [room, setRoom] = useState<Room | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [formData, setFormData] = useState<BookingFormData>({
    roomId: roomId || '',
    startTime: '',
    endTime: '',
    purpose: '',
    title: '',
  });

  const loadRoom = async () => {
    if (!roomId) return;

    try {
      setLoading(true);
      const data = await roomsApi.getById(roomId);
      setRoom(data);
    } catch (err: any) {
      if (err.response?.status === 401 || err.response?.status === 403) {
        return;
      }
      setError('Не удалось загрузить информацию об аудитории.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (authLoading) {
      return;
    }
    
    const token = localStorage.getItem('accessToken');
    if (!isAuthenticated && !token) {
      navigate('/login');
      return;
    }

    if (roomId) {
      loadRoom();
      setFormData((prev) => ({ ...prev, roomId }));
    }
  }, [roomId, isAuthenticated, authLoading, navigate]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    const token = localStorage.getItem('accessToken');
    if (!isAuthenticated && !token) {
      navigate('/login');
      return;
    }

    const startTime = formData.startTime || formData.startsAt;
    const endTime = formData.endTime || formData.endsAt;

    if (!formData.roomId || !startTime || !endTime) {
      setError('Пожалуйста, заполните все обязательные поля.');
      return;
    }

    const start = new Date(startTime);
    const end = new Date(endTime);

    if (end <= start) {
      setError('Время окончания должно быть позже времени начала.');
      return;
    }

    try {
      setSubmitting(true);
      const bookingData = {
        roomId: formData.roomId,
        startsAt: start.toISOString(),
        endsAt: end.toISOString(),
      };
      await bookingsApi.create(bookingData);
      navigate('/my-bookings');
    } catch (err: any) {
      if (err.response?.status === 401 || err.response?.status === 403) {
        navigate('/login');
        return;
      }
      setError(err.response?.data?.message || 'Не удалось создать бронирование.');
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  const getMinDateTime = () => {
    const now = new Date();
    now.setMinutes(0, 0, 0);
    return format(now, "yyyy-MM-dd'T'HH:mm");
  };

  if (loading) {
    return <div className="loading">Загрузка...</div>;
  }

  if (error && !room) {
    return <div className="error">{error}</div>;
  }

  return (
    <section className="booking-page">
      <div className="container">
        <div className="booking-header">
          <h1>Бронирование аудитории</h1>
          {room && (
            <div className="room-summary">
              <h2>{room.name}</h2>
              <p>
                {room.building}, {room.floor} этаж • Вместимость: {room.capacity} мест
              </p>
            </div>
          )}
        </div>

        <form onSubmit={handleSubmit} className="booking-form">
        {error && <div className="error">{error}</div>}

        {!roomId && (
          <div className="form-group">
            <label htmlFor="roomId">Аудитория:</label>
            <input
              id="roomId"
              type="text"
              value={formData.roomId}
              onChange={(e) => setFormData({ ...formData, roomId: e.target.value })}
              placeholder="Введите ID аудитории или выберите из списка"
              required
            />
          </div>
        )}

        <div className="form-row">
          <div className="form-group">
            <label htmlFor="startTime">Начало:</label>
            <input
              id="startTime"
              type="datetime-local"
              value={formData.startTime}
              onChange={(e) => setFormData({ ...formData, startTime: e.target.value })}
              min={getMinDateTime()}
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="endTime">Окончание:</label>
            <input
              id="endTime"
              type="datetime-local"
              value={formData.endTime}
              onChange={(e) => setFormData({ ...formData, endTime: e.target.value })}
              min={formData.startTime || getMinDateTime()}
              required
            />
          </div>
        </div>

        <div className="form-group">
          <label htmlFor="purpose">Цель бронирования (необязательно):</label>
          <textarea
            id="purpose"
            value={formData.purpose || formData.title || ''}
            onChange={(e) => setFormData({ ...formData, purpose: e.target.value, title: e.target.value })}
            placeholder="Опишите цель использования аудитории (необязательно)"
            rows={4}
          />
        </div>

        <div className="form-actions">
          <button
            type="button"
            onClick={() => navigate(-1)}
            className="btn btn-secondary"
          >
            Отмена
          </button>
          <button
            type="submit"
            disabled={submitting}
            className="btn btn-primary"
          >
            {submitting ? 'Создание...' : 'Забронировать'}
          </button>
        </div>
      </form>
      </div>
    </section>
  );
};

export default BookingPage;

