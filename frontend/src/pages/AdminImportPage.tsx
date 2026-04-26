import React, { useState } from 'react';
import UniversitySelect from '../features/university/ui/UniversitySelect';
import { useUniversitySearchParam } from '../shared/university/useUniversitySearchParam';

const AdminImportPage: React.FC = () => {
  const { universityCode, setUniversityCode, university } = useUniversitySearchParam();
  const [fileName, setFileName] = useState('');

  return (
    <section className="admin-import">
      <div className="container">
        <div className="section-head">
          <h2>Импорт аудиторий</h2>
          <p>
            Выберите вуз и загрузите файл с корпусами и аудиториями. Этот экран подготовлен под bulk import,
            но сам backend-контракт импорта не меняется в рамках фронтовой задачи.
          </p>
        </div>

        <div className="admin-import__layout">
          <form className="admin-import__card" onSubmit={(event) => event.preventDefault()}>
            <UniversitySelect
              id="admin-import-university"
              label="Вуз для импорта"
              value={universityCode}
              onChange={setUniversityCode}
            />

            <div className="form-group">
              <label htmlFor="room-import-file">Файл аудиторий</label>
              <input
                id="room-import-file"
                type="file"
                accept=".csv,.xlsx,.xls"
                onChange={(event) => setFileName(event.target.files?.[0]?.name || '')}
              />
            </div>

            <div className="admin-import__dropzone">
              <strong>{fileName || 'rooms_import.xlsx'}</strong>
              <span>
                Ожидаемые поля: building, room, capacity, features, floor. Вуз будет применен как {university.label}.
              </span>
            </div>

            <div className="form-actions">
              <button type="button" className="btn btn--ghost">
                Скачать шаблон
              </button>
              <button type="submit" className="btn btn--primary">
                Проверить файл
              </button>
            </div>
          </form>

          <aside className="admin-import__preview">
            <h3>Предпросмотр</h3>
            <ul className="booking-list">
              <li>
                <span className="booking-list__title">Корпус 5 · Ауд. 5316</span>
                <span className="booking-list__meta">48 мест · новая аудитория</span>
              </li>
              <li>
                <span className="booking-list__title">Корпус 5 · Ауд. 5212</span>
                <span className="booking-list__meta">64 места · будет обновлена</span>
              </li>
              <li>
                <span className="booking-list__title">Корпус 3 · Ауд. 3401</span>
                <span className="booking-list__meta">96 мест · новая аудитория</span>
              </li>
            </ul>
            <span className="pill">cache.invalidate для {university.label}</span>
          </aside>
        </div>
      </div>
    </section>
  );
};

export default AdminImportPage;
