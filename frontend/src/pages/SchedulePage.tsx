import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import ScheduleTable from '../components/ScheduleTable/ScheduleTable';
import EmptyState from '../components/EmptyState/EmptyState';
import { scheduleApi } from '../api/schedule';
import { ScheduleByFloor } from '../types';
import UniversitySelect from '../features/university/ui/UniversitySelect';
import { useUniversitySearchParam } from '../shared/university/useUniversitySearchParam';

const SchedulePage: React.FC = () => {
  const [selectedFloor, setSelectedFloor] = useState<number>(1);
  const { universityCode, setUniversityCode, university } = useUniversitySearchParam();

  const scheduleQuery = useQuery({
    queryKey: ['schedule', universityCode],
    queryFn: () => scheduleApi.getByFloors(undefined, universityCode),
  });

  const schedule: ScheduleByFloor = scheduleQuery.data || {};
  const scheduleRows = schedule[selectedFloor] || [];

  if (scheduleQuery.isLoading) {
    return (
      <section className="schedule">
        <div className="container">
          <div className="loading">Загрузка расписания...</div>
        </div>
      </section>
    );
  }

  if (scheduleQuery.error) {
    return (
      <section className="schedule">
        <div className="container">
          <div className="error">Не удалось загрузить расписание. Попробуйте позже.</div>
        </div>
      </section>
    );
  }

  return (
    <section className="schedule">
      <div className="container">
        <div className="section-head">
          <h2>Расписание свободных аудиторий</h2>
          <p>
            Посмотрите, где в {university.label} есть свободные окна для занятий, консультаций
            или встреч. Переключайтесь между этажами, сравнивайте занятые и доступные интервалы
            и быстро находите аудиторию, которая подойдет по времени.
          </p>
        </div>

        <div className="schedule__filters">
          <UniversitySelect
            id="schedule-university"
            label="Вуз"
            value={universityCode}
            onChange={setUniversityCode}
          />
        </div>

        <div className="tabs" role="tablist" aria-label="Этажи">
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
            <EmptyState message="Нет данных для выбранного этажа и вуза." />
          ) : (
            <ScheduleTable rows={scheduleRows} />
          )}
        </div>
      </div>
    </section>
  );
};

export default SchedulePage;
