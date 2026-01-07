import React, { useState, useEffect } from 'react';
import { Room, Building } from '../types';
import { roomsApi } from '../api/rooms';
import RoomCard from '../components/RoomCard/RoomCard';
import EmptyState from '../components/EmptyState/EmptyState';

const RoomsPage: React.FC = () => {
  const [rooms, setRooms] = useState<Room[]>([]);
  const [buildings, setBuildings] = useState<Building[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filters, setFilters] = useState({
    building: '',
    floor: '',
    capacity: '',
  });

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      setError(null);
      const [roomsData, buildingsData] = await Promise.all([
        roomsApi.getAll(),
        roomsApi.getBuildings(),
      ]);
      
      const buildingsMap = new Map(buildingsData.map(b => [b.id, b]));
      
      const enrichedRooms = roomsData.map(room => {
        const building = buildingsMap.get(room.buildingId);
        return {
          ...room,
          building: building?.name || room.building || 'Неизвестное здание',
        };
      });
      
      setRooms(enrichedRooms);
      setBuildings(buildingsData);
    } catch (err) {
      setError('Не удалось загрузить аудитории. Попробуйте позже.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const filteredRooms = rooms.filter((room) => {
    if (filters.building && room.buildingId !== filters.building && room.building !== filters.building) return false;
    if (filters.floor && room.floor && room.floor.toString() !== filters.floor) return false;
    if (filters.capacity && room.capacity < parseInt(filters.capacity)) return false;
    return true;
  });

  const uniqueBuildings = Array.from(new Set(rooms.map((r) => r.building))).filter(Boolean);
  const floors = Array.from(new Set(rooms.map((r) => r.floor).filter((f): f is number => f !== undefined))).sort(
    (a, b) => a - b
  );

  const handleFilterChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFilters({ ...filters, [name]: value });
  };

  if (loading) {
    return (
      <div className="search">
        <div className="container">
          <div className="loading">Загрузка...</div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="search">
        <div className="container">
          <div className="error">{error}</div>
        </div>
      </div>
    );
  }

  return (
    <section className="search">
      <div className="container">
        <div className="section-head">
          <h2>Выберите аудиторию</h2>
          <p>
            Уточните параметры ниже, чтобы увидеть подходящие аудитории и их доступные временные слоты.
          </p>
        </div>
        
        <div className="filters-panel" style={{
          display: 'flex',
          flexWrap: 'wrap',
          gap: '1rem',
          marginBottom: '2rem',
          padding: '1.5rem',
          backgroundColor: 'var(--surface)',
          borderRadius: 'var(--radius)',
          boxShadow: 'var(--shadow)',
          alignItems: 'flex-end'
        }}>
          <div style={{ flex: '1 1 200px', minWidth: '150px' }}>
            <label htmlFor="building" style={{ 
              display: 'block', 
              marginBottom: '0.5rem', 
              fontWeight: '600',
              color: 'var(--text)'
            }}>
              Корпус
            </label>
            <select
              id="building"
              name="building"
              value={filters.building}
              onChange={handleFilterChange}
              style={{
                width: '100%',
                padding: '0.75rem 1rem',
                border: '1px solid var(--border)',
                borderRadius: '12px',
                fontSize: '1rem',
                backgroundColor: 'var(--background)',
                color: 'var(--text)',
                cursor: 'pointer',
                transition: 'border-color 0.2s ease'
              }}
              onFocus={(e) => e.target.style.borderColor = 'var(--primary)'}
              onBlur={(e) => e.target.style.borderColor = 'var(--border)'}
            >
              <option value="">Все корпуса</option>
              {buildings.map((building) => (
                <option key={building.id} value={building.id}>
                  {building.name}
                </option>
              ))}
              {uniqueBuildings.map((buildingName) => (
                <option key={buildingName} value={buildingName}>
                  {buildingName}
                </option>
              ))}
            </select>
          </div>

          <div style={{ flex: '1 1 150px', minWidth: '120px' }}>
            <label htmlFor="floor" style={{ 
              display: 'block', 
              marginBottom: '0.5rem', 
              fontWeight: '600',
              color: 'var(--text)'
            }}>
              Этаж
            </label>
            <select
              id="floor"
              name="floor"
              value={filters.floor}
              onChange={handleFilterChange}
              style={{
                width: '100%',
                padding: '0.75rem 1rem',
                border: '1px solid var(--border)',
                borderRadius: '12px',
                fontSize: '1rem',
                backgroundColor: 'var(--background)',
                color: 'var(--text)',
                cursor: 'pointer',
                transition: 'border-color 0.2s ease'
              }}
              onFocus={(e) => e.target.style.borderColor = 'var(--primary)'}
              onBlur={(e) => e.target.style.borderColor = 'var(--border)'}
            >
              <option value="">Все этажи</option>
              {floors.map((floor) => (
                <option key={floor} value={floor.toString()}>
                  {floor}
                </option>
              ))}
            </select>
          </div>

          <div style={{ flex: '1 1 150px', minWidth: '120px' }}>
            <label htmlFor="capacity" style={{ 
              display: 'block', 
              marginBottom: '0.5rem', 
              fontWeight: '600',
              color: 'var(--text)'
            }}>
              Вместимость (мин.)
            </label>
            <input
              type="number"
              id="capacity"
              name="capacity"
              min="10"
              max="250"
              step="10"
              value={filters.capacity}
              onChange={handleFilterChange}
              placeholder="Любая"
              style={{
                width: '100%',
                padding: '0.75rem 1rem',
                border: '1px solid var(--border)',
                borderRadius: '12px',
                fontSize: '1rem',
                backgroundColor: 'var(--background)',
                color: 'var(--text)',
                transition: 'border-color 0.2s ease'
              }}
              onFocus={(e) => e.target.style.borderColor = 'var(--primary)'}
              onBlur={(e) => e.target.style.borderColor = 'var(--border)'}
            />
          </div>

          <div style={{ flex: '0 0 auto', alignSelf: 'flex-end' }}>
            <button
              type="button"
              className="btn btn--ghost"
              onClick={() => setFilters({ building: '', floor: '', capacity: '' })}
              style={{
                padding: '0.75rem 1.5rem',
                whiteSpace: 'nowrap',
                borderRadius: '12px'
              }}
            >
              Сбросить
            </button>
          </div>
        </div>

        <div className="results">
          <div className="results__toolbar" style={{ marginBottom: '1.5rem' }}>
            <div>
              <span className="pill pill--muted">
                {filteredRooms.length} аудиторий найдено
              </span>
            </div>
          </div>
          <div className="room-cards" style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))',
            gap: '1.5rem'
          }}>
            {filteredRooms.length === 0 ? (
              <EmptyState message="По выбранным параметрам аудиторий не найдено. Попробуйте изменить фильтры." />
            ) : (
              filteredRooms.map((room) => <RoomCard key={room.id} room={room} />)
            )}
          </div>
        </div>
      </div>
    </section>
  );
};

export default RoomsPage;
