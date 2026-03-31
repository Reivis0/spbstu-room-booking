import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Room } from '../../types';
import { useAuthStore } from '../../store/useAuthStore';

interface RoomCardProps {
  room: Room;
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

const RoomCard: React.FC<RoomCardProps> = ({ room }) => {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuthStore();

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
          to={`/booking/${room.id}`} 
          className="btn btn--primary"
          onClick={handleBookingClick}
        >
          Забронировать
        </Link>
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
    </article>
  );
};

export default RoomCard;
