import React from 'react';
import { format } from 'date-fns';
import { Booking } from '../../types';

interface BookingCardProps {
  booking: Booking;
  onCancel?: (id: string) => void;
  readOnly?: boolean;
}

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
      default:
        return status;
    }
  };

  const startTime = booking.startTime || (booking.startsAt ? new Date(booking.startsAt) : new Date());
  const endTime = booking.endTime || (booking.endsAt ? new Date(booking.endsAt) : new Date());
  const createdAt = booking.createdAt ? new Date(booking.createdAt) : undefined;

  const canCancel =
    !readOnly &&
    booking.status !== 'cancelled' &&
    startTime > new Date() &&
    onCancel;

  return (
    <div className="booking-card">
      <div className="booking-card-header">
        <h3>{booking.roomName || `Аудитория #${booking.roomId}`}</h3>
        <span
          className="booking-status"
          style={{ color: getStatusColor(booking.status) }}
        >
          {getStatusText(booking.status)}
        </span>
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

