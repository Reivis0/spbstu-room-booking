import React, { useState } from 'react';
import { Booking } from '../../types';

interface CancelBookingModalProps {
  booking: Booking | null;
  isOpen: boolean;
  onClose: () => void;
  onConfirm: (id: string, reason: string) => Promise<void>;
}

const CancelBookingModal: React.FC<CancelBookingModalProps> = ({ booking, isOpen, onClose, onConfirm }) => {
  const [reason, setReason] = useState('');
  const [isCancelling, setIsCancelling] = useState(false);

  // Reset reason when modal opens with a new booking
  React.useEffect(() => {
    if (isOpen) {
      setReason('');
      setIsCancelling(false);
    }
  }, [isOpen]);

  if (!isOpen || !booking) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsCancelling(true);
    try {
      await onConfirm(booking.id, reason);
      onClose();
    } catch (err) {
      // errors handled by parent
    } finally {
      setIsCancelling(false);
    }
  };

  return (
    <div className="modal-overlay" style={{
      position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
      backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex',
      alignItems: 'center', justifyContent: 'center', zIndex: 1000
    }}>
      <div className="modal-content" style={{
        backgroundColor: '#fff', padding: '24px', borderRadius: '12px',
        width: '100%', maxWidth: '400px', boxShadow: '0 4px 20px rgba(0,0,0,0.15)'
      }}>
        <h2 style={{ marginTop: 0, marginBottom: '16px', fontSize: '1.25rem' }}>Отмена бронирования</h2>
        <p style={{ marginBottom: '20px', color: '#666', fontSize: '0.9rem' }}>
          Вы собираетесь отменить бронирование: <br />
          <strong>{booking.roomName || `Аудитория #${booking.roomId.slice(0, 8)}`}</strong><br />
          Пользователь: {booking.userName || `Пользователь #${booking.userId.slice(0, 8)}`}
        </p>
        
        <form onSubmit={handleSubmit}>
          <div className="form-group" style={{ marginBottom: '24px' }}>
            <label style={{ display: 'block', marginBottom: '8px', fontWeight: 600, fontSize: '0.9rem' }}>Причина отмены (обязательно):</label>
            <textarea
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="Укажите причину для пользователя..."
              required
              rows={3}
              style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid #ddd', boxSizing: 'border-box', resize: 'vertical' }}
            />
          </div>

          <div style={{ display: 'flex', gap: '12px', justifyContent: 'flex-end' }}>
            <button type="button" onClick={onClose} className="btn btn--ghost" disabled={isCancelling}>
              Назад
            </button>
            <button type="submit" className="booking-cancel-btn" disabled={isCancelling} style={{ margin: 0, color: 'white', backgroundColor: '#f44336' }}>
              {isCancelling ? 'Отмена...' : 'Подтвердить отмену'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default CancelBookingModal;
