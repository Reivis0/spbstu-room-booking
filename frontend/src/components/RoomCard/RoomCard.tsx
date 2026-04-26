import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Room } from '../../types';
import { useAuthStore } from '../../store/useAuthStore';
import DayTimeline from '../DayTimeline/DayTimeline';
import AvailabilityIndicator from '../../features/room/ui/AvailabilityIndicator';
import {
  UniversityCode,
  getUniversity,
  normalizeUniversityCode,
} from '../../shared/university/universities';

interface RoomCardProps {
  room: Room;
  university?: UniversityCode;
}

const equipmentLabels: Record<string, string> = {
  projector: 'Проектор',
  whiteboard: 'Доска',
  microphone: 'Микрофон',
  computer: 'Компьютеры',
  wifi: 'Wi-Fi',
  проектор: 'Проектор',
  микрофон: 'Микрофон',
  доска: 'Доска',
  компьютер: 'Компьютеры',
  WiFi: 'Wi-Fi',
};

const RoomCard: React.FC<RoomCardProps> = ({ room, university }) => {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuthStore();
  const [showSchedule, setShowSchedule] = useState(false);
  const roomUniversity = getUniversity(
    university || normalizeUniversityCode(room.universityCode || room.university)
  );

  let equipment = room.equipment || [];
  if (equipment.length === 0 && room.features) {
    try {
      const features = JSON.parse(room.features);
      if (Array.isArray(features.equipment)) {
        equipment = features.equipment;
      } else {
        equipment = Object.keys(features).filter(key => features[key] === true);
      }
    } catch (e) {
      if (process.env.NODE_ENV === 'development') {
        console.warn('[RoomCard] Failed to parse features:', room.features, e);
      }
    }
  }

  const handleBookingClick = (e: React.MouseEvent) => {
    const token = localStorage.getItem('accessToken');
    if (!isAuthenticated && !token) {
      e.preventDefault();
      navigate('/login');
    }
  };
  
  return (
    <article className="room-card">
      <div className="room-card__head">
        <div style={{ flex: 1 }}>
          <h3 className="room-card__title">{room.name}</h3>
          <p className="room-card__meta">
            {room.building && <span style={{ fontWeight: '600' }}>{room.building}</span>}
            {room.building && (room.floor || room.capacity) && ' • '}
            {room.capacity && <span>{room.capacity} мест</span>}
            {room.floor && room.capacity && ' • '}
            {room.floor && <span>{room.floor} этаж</span>}
          </p>
        </div>
        <Link 
          to={`/booking/${room.id}?university=${roomUniversity.code}`} 
          className="btn btn--primary"
          onClick={handleBookingClick}
        >
          Забронировать
        </Link>
      </div>

      <div className="room-card__status-row">
        <span className="room-card__university-badge">{roomUniversity.badgeLabel}</span>
        <AvailabilityIndicator
          status={room.availabilityStatus || 'free'}
          nextSlotAt={room.nextSlotAt}
          updatedAt={room.statusUpdatedAt}
        />
      </div>
      
      {equipment.length > 0 && (
        <div className="room-card__chips">
          {equipment.map((tag) => (
            <span className="chip" key={tag}>
              {equipmentLabels[tag] || tag}
            </span>
          ))}
        </div>
      )}
      
      {room.nextSlots && room.nextSlots.length > 0 && (
        <div className="slot-list">
          {room.nextSlots.map((slot) => (
            <span className="slot" key={slot}>
              {slot}
            </span>
          ))}
        </div>
      )}

      <div className="room-card__footer" style={{ borderTop: '1px solid var(--border)', paddingTop: '12px', marginTop: '4px' }}>
        <button
          type="button"
          onClick={() => setShowSchedule(!showSchedule)}
          style={{
            background: 'none',
            border: 'none',
            color: 'var(--primary-dark)',
            fontSize: '0.85rem',
            fontWeight: '600',
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            padding: 0,
            cursor: 'pointer'
          }}
        >
          <svg 
            width="16" 
            height="16" 
            viewBox="0 0 24 24" 
            fill="none" 
            stroke="currentColor" 
            strokeWidth="2.5" 
            strokeLinecap="round" 
            strokeLinejoin="round"
            style={{ 
              transition: 'transform 0.2s ease',
              transform: showSchedule ? 'rotate(180deg)' : 'rotate(0deg)'
            }}
          >
            <polyline points="6 9 12 15 18 9"></polyline>
          </svg>
          {showSchedule ? 'Скрыть расписание' : 'Показать расписание'}
        </button>

        {showSchedule && <DayTimeline roomId={room.id} university={roomUniversity.code} />}
      </div>
    </article>
  );
};

export default RoomCard;
