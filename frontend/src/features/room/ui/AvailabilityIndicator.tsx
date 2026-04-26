import React, { useEffect, useMemo, useState } from 'react';
import { formatDistanceToNowStrict } from 'date-fns';
import { ru } from 'date-fns/locale';
import { RoomAvailabilityStatus } from '../../../types';

interface AvailabilityIndicatorProps {
  status?: RoomAvailabilityStatus;
  nextSlotAt?: string;
  updatedAt?: string;
}

const getStatusText = (status: RoomAvailabilityStatus) => {
  switch (status) {
    case 'busy':
      return 'Занято';
    case 'unknown':
      return 'Статус уточняется';
    case 'free':
    default:
      return 'Свободно';
  }
};

const AvailabilityIndicator: React.FC<AvailabilityIndicatorProps> = ({
  status = 'free',
  nextSlotAt,
  updatedAt,
}) => {
  const [now, setNow] = useState(Date.now());

  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 30_000);
    return () => window.clearInterval(timer);
  }, []);

  const details = useMemo(() => {
    const nextSlotDate = nextSlotAt ? new Date(nextSlotAt) : null;
    const updatedDate = updatedAt ? new Date(updatedAt) : null;

    if (nextSlotDate && !Number.isNaN(nextSlotDate.getTime())) {
      const distance = formatDistanceToNowStrict(nextSlotDate, {
        locale: ru,
        addSuffix: false,
      });
      return status === 'free'
        ? `свободно еще ${distance}`
        : `освободится через ${distance}`;
    }

    if (updatedDate && !Number.isNaN(updatedDate.getTime())) {
      const seconds = Math.max(1, Math.round((now - updatedDate.getTime()) / 1000));
      return `обновлено ${seconds} сек назад`;
    }

    return 'обновляется в реальном времени';
  }, [nextSlotAt, now, status, updatedAt]);

  return (
    <div
      className={`availability-indicator availability-indicator--${status}`}
      aria-live="polite"
      aria-label={`Статус аудитории: ${getStatusText(status)}; ${details}`}
    >
      <span className="availability-indicator__dot" aria-hidden="true" />
      <span className="availability-indicator__text">{getStatusText(status)}</span>
      <span className="availability-indicator__meta">{details}</span>
    </div>
  );
};

export default AvailabilityIndicator;
