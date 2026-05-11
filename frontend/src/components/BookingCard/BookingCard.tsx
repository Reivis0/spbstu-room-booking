import React from 'react';
import { format } from 'date-fns';
import { Booking } from '../../types';
import { getUniversity } from '../../shared/university/universities';

interface BookingCardProps {
  booking: Booking;
  onCancel?: (id: string) => void;
  readOnly?: boolean;
}

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

const BookingCard: React.FC<BookingCardProps> = ({
  booking,
  onCancel,
  readOnly = false,
}) => {
  const getStatusColor = (status: string) => {
    switch (status) {
      case 'confirmed':
        return '#4caf50';
      case 'pending':
        return '#ff9800';
      case 'cancelled':
      case 'rejected':
        return '#f44336';
      default:
        return '#666';
    }
  };

  const getStatusText = (status: string) => {
    switch (status) {
      case 'confirmed':
        return 'Подтверждено';
      case 'pending':
        return 'Ожидает подтверждения';
      case 'cancelled':
        return 'Отменено';
      case 'rejected':
        return 'Отклонено';
      default:
        return status;
    }
  };

  const startTime = booking.startTime || (booking.startsAt ? new Date(booking.startsAt) : new Date());
  const endTime = booking.endTime || (booking.endsAt ? new Date(booking.endsAt) : new Date());
  const createdAt = booking.createdAt ? new Date(booking.createdAt) : undefined;
  const university = getUniversity(booking.universityCode || booking.university);

  const canCancel =
    !readOnly &&
    !['cancelled', 'rejected'].includes(booking.status) &&
    startTime > new Date() &&
    onCancel;

  const showReason = (booking.status === 'cancelled' || booking.status === 'rejected') && booking.cancellationReason;

  return (
    <div className="booking-card">
      <div className="booking-card-header">
        <div>
          <span className="booking-card__university-badge">{university.badgeLabel}</span>
          <h3>{booking.roomName || `Аудитория #${booking.roomId}`}</h3>
        </div>
        <div className="booking-card__status-stack">
          {booking.isChain && <span className="booking-status booking-status--chain">Цепочка</span>}
          <span
            className="booking-status"
            style={{ color: getStatusColor(booking.status) }}
          >
            {getStatusText(booking.status)}
          </span>
        </div>
      </div>

      <div className="booking-card-info">
        <div className="booking-info-item">
          <span className="booking-info-label">Дата:</span>
          <span>
            {format(startTime, 'd MMMM yyyy')}
          </span>
        </div>
        <div className="booking-info-item">
          <span className="booking-info-label">Время:</span>
          <span>
            {format(startTime, 'HH:mm')} -{' '}
            {format(endTime, 'HH:mm')}
          </span>
        </div>
        {createdAt && (
          <div className="booking-info-item">
            <span className="booking-info-label">Создано:</span>
            <span>
              {format(createdAt, 'd MMM yyyy, HH:mm')}
            </span>
          </div>
        )}
        {showReason && (
          <div className="booking-info-item booking-info-item--reason">
            <span className="booking-info-label">Причина:</span>
            <span className="booking-cancellation-reason">{translateReason(booking.cancellationReason)}</span>
          </div>
        )}
      </div>

      {canCancel && (
        <button
          onClick={() => onCancel(booking.id)}
          className="booking-cancel-btn"
        >
          Отменить бронирование
        </button>
      )}
    </div>
  );
};

export default BookingCard;
