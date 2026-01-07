import React, { useState, useEffect } from 'react';
import ScheduleTable, { ScheduleRow } from '../components/ScheduleTable/ScheduleTable';
import EmptyState from '../components/EmptyState/EmptyState';
import { scheduleApi } from '../api/schedule';
import { ScheduleByFloor } from '../types';

const SchedulePage: React.FC = () => {
  const [selectedFloor, setSelectedFloor] = useState<number>(1);
  const [schedule, setSchedule] = useState<ScheduleByFloor>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadSchedule();
  }, []);

  const loadSchedule = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await scheduleApi.getByFloors();
      setSchedule(data);
    } catch (err) {
      const mockSchedule: ScheduleByFloor = {
        1: [
          {
            room: 'Ауд. 112',
            time: '08:30–10:00',
            status: 'busy',
            event: 'Лекция по математическому анализу',
          },
          {
            room: 'Ауд. 112',
            time: '10:15–11:45',
            status: 'free',
            event: 'Свободно',
          },
          {
            room: 'Ауд. 117',
            time: '12:00–13:30',
            status: 'busy',
            event: 'Практика по программированию',
          },
        ],
        2: [
          {
            room: 'Ауд. 217',
            time: '09:00–11:00',
            status: 'busy',
            event: 'Презентация магистров',
          },
          {
            room: 'Ауд. 217',
            time: '11:30–13:00',
            status: 'free',
            event: 'Свободно',
          },
        ],
        3: [],
        4: [],
      };
      setSchedule(mockSchedule);
      console.warn('API недоступен, используются моковые данные:', err);
    } finally {
      setLoading(false);
    }
  };

  const scheduleRows = schedule[selectedFloor] || [];

  if (loading) {
    return (
      <section className="schedule">
        <div className="container">
          <div className="loading">Загрузка расписания...</div>
        </div>
      </section>
    );
  }

  if (error) {
    return (
      <section className="schedule">
        <div className="container">
          <div className="error">{error}</div>
        </div>
      </section>
    );
  }

  return (
    <section className="schedule">
      <div className="container">
        <div className="section-head">
          <h2>Свободные слоты по этажам</h2>
          <p>Быстрый просмотр свободных окон на ближайшие дни. Выберите этаж, чтобы увидеть расписание.</p>
        </div>
        <div className="tabs" role="tablist">
          {[1, 2, 3, 4].map((floor) => (
            <button
              key={floor}
              className={`tabs__btn${selectedFloor === floor ? ' is-active' : ''}`}
              role="tab"
              type="button"
              aria-selected={selectedFloor === floor}
              onClick={() => setSelectedFloor(floor)}
            >
              {floor} этаж
            </button>
          ))}
        </div>
        <div className="schedule__table">
          {scheduleRows.length === 0 ? (
            <EmptyState message="Нет данных для выбранного этажа." />
          ) : (
            <ScheduleTable rows={scheduleRows} />
          )}
        </div>
      </div>
    </section>
  );
};

export default SchedulePage;
