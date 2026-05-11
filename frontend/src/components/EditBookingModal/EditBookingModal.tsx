import React, { useState, useEffect } from 'react';
import { format } from 'date-fns';
import { Booking } from '../../types';

interface EditBookingModalProps {
  booking: Booking | null;
  isOpen: boolean;
  onClose: () => void;
  onSave: (id: string, data: { startsAt: string; endsAt: string }) => Promise<void>;
}

const EditBookingModal: React.FC<EditBookingModalProps> = ({ booking, isOpen, onClose, onSave }) => {
  const [startsAt, setStartsAt] = useState('');
  const [endsAt, setEndsAt] = useState('');
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    if (booking && isOpen) {
      // Need to format dates to YYYY-MM-DDThh:mm for datetime-local
      const formatForInput = (dateStr: string) => {
        const d = new Date(dateStr);
        // adjust for timezone offset to show local time in input
        const local = new Date(d.getTime() - d.getTimezoneOffset() * 60000);
        return local.toISOString().slice(0, 16);
      };
      
      setStartsAt(formatForInput(booking.startsAt));
      setEndsAt(formatForInput(booking.endsAt));
    }
  }, [booking, isOpen]);

  if (!isOpen || !booking) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSaving(true);
    try {
      // Convert back to ISO string for backend
      const startsAtIso = new Date(startsAt).toISOString();
      const endsAtIso = new Date(endsAt).toISOString();
      await onSave(booking.id, { startsAt: startsAtIso, endsAt: endsAtIso });
      onClose();
    } catch (err) {
      // error handling is typically done in the parent via toast
    } finally {
      setIsSaving(false);
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
        <h2 style={{ marginTop: 0, marginBottom: '16px', fontSize: '1.25rem' }}>Редактирование брони</h2>
        <p style={{ marginBottom: '20px', color: '#666', fontSize: '0.9rem' }}>
          {booking.roomName || `Аудитория #${booking.roomId.slice(0, 8)}`}
        </p>
        
        <form onSubmit={handleSubmit}>
          <div className="form-group" style={{ marginBottom: '16px' }}>
            <label style={{ display: 'block', marginBottom: '8px', fontWeight: 600, fontSize: '0.9rem' }}>Начало:</label>
            <input
              type="datetime-local"
              value={startsAt}
              onChange={(e) => setStartsAt(e.target.value)}
              required
              style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid #ddd', boxSizing: 'border-box' }}
            />
          </div>

          <div className="form-group" style={{ marginBottom: '24px' }}>
            <label style={{ display: 'block', marginBottom: '8px', fontWeight: 600, fontSize: '0.9rem' }}>Окончание:</label>
            <input
              type="datetime-local"
              value={endsAt}
              onChange={(e) => setEndsAt(e.target.value)}
              min={startsAt}
              required
              style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid #ddd', boxSizing: 'border-box' }}
            />
          </div>

          <div style={{ display: 'flex', gap: '12px', justifyContent: 'flex-end' }}>
            <button type="button" onClick={onClose} className="btn btn--ghost" disabled={isSaving}>
              Отмена
            </button>
            <button type="submit" className="btn btn--primary" disabled={isSaving}>
              {isSaving ? 'Сохранение...' : 'Сохранить'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default EditBookingModal;
