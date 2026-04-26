import React, { useState } from 'react';
import { useRoomSchedule } from '../../hooks/useRoomSchedule';
import { ScheduleSlot } from '../../types';
import { UniversityCode } from '../../shared/university/universities';
import { format, addDays, startOfDay, differenceInDays } from 'date-fns';
import { ru } from 'date-fns/locale';
import './DayTimeline.css';

interface DayTimelineProps {
  roomId: string;
  university?: UniversityCode;
}

const toMinutes = (time: string) => {
  const [h, m] = time.split(':').map(Number);
  return h * 60 + m;
};

const DAY_START = 480; // 08:00
const DAY_END = 1260;  // 21:00
const DAY_SPAN = DAY_END - DAY_START;

const formatDayLabel = (dateStr: string): string => {
  const d = new Date(dateStr);
  const today = startOfDay(new Date());
  const diff = differenceInDays(startOfDay(d), today);

  if (diff === 0) return 'Сегодня';
  if (diff === 1) return 'Завтра';

  return format(d, 'eee, d MMM', { locale: ru });
};

const DayTimeline: React.FC<DayTimelineProps> = ({ roomId, university }) => {
  const [date, setDate] = useState(format(new Date(), 'yyyy-MM-dd'));
  const { data, loading, error } = useRoomSchedule(roomId, date, university);

  const nextDays = Array.from({ length: 7 }, (_, i) => {
    return format(addDays(new Date(), i), 'yyyy-MM-dd');
  });

  const renderSlot = (slot: ScheduleSlot, index: number) => {
    const startMin = Math.max(toMinutes(slot.from), DAY_START);
    const endMin = Math.min(toMinutes(slot.to), DAY_END);
    
    if (endMin <= DAY_START || startMin >= DAY_END) return null;

    const left = ((startMin - DAY_START) / DAY_SPAN) * 100;
    const width = ((endMin - startMin) / DAY_SPAN) * 100;

    let className = 'day-timeline__slot';
    if (slot.status === 'occupied') {
      className += slot.type === 'booking' 
        ? ' day-timeline__slot--occupied-booking' 
        : ' day-timeline__slot--occupied-schedule';
    } else {
      className += ' day-timeline__slot--free';
    }

    return (
      <div
        key={index}
        className={className}
        style={{ left: `${left}%`, width: `${width}%` }}
        title={`${slot.label || (slot.status === 'free' ? 'Свободно' : 'Занято')} (${slot.from}–${slot.to})`}
      />
    );
  };

  return (
    <div className="day-timeline">
      <div className="day-timeline__days">
        {nextDays.map((d) => (
          <button
            key={d}
            type="button"
            onClick={() => setDate(d)}
            className={`day-timeline__day-btn ${date === d ? 'day-timeline__day-btn--active' : ''}`}
          >
            {formatDayLabel(d)}
          </button>
        ))}
      </div>

      {loading && <div className="day-timeline__loading">Загрузка...</div>}
      
      {error && <div className="day-timeline__error">Не удалось загрузить данные</div>}

      {data && (
        <>
          <div className="day-timeline__bar-container">
            {data.slots.map((slot: ScheduleSlot, i: number) => renderSlot(slot, i))}
          </div>
          
          <div className="day-timeline__time-labels">
            <span>08:00</span>
            <span>12:00</span>
            <span>16:00</span>
            <span>20:00</span>
            <span>21:00</span>
          </div>

          <div className="day-timeline__legend">
            {data.slots
              .filter((s: ScheduleSlot) => s.status === 'occupied')
              .map((slot: ScheduleSlot, i: number) => (
                <div key={i} className="day-timeline__legend-item">
                  <span className={`day-timeline__dot ${slot.type === 'booking' ? 'day-timeline__dot--booking' : 'day-timeline__dot--schedule'}`} />
                  <span className="day-timeline__slot-time">{slot.from}–{slot.to}</span>
                  <span className="day-timeline__slot-label">{slot.label}</span>
                </div>
              ))
            }
            {data.slots.filter((s: ScheduleSlot) => s.status === 'occupied').length === 0 && (
              <div className="day-timeline__empty">✓ Весь день свободен</div>
            )}
          </div>
        </>
      )}
    </div>
  );
};

export default DayTimeline;
