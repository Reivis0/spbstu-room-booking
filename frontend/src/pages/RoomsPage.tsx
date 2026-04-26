import React, { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import { Room, Building } from '../types';
import { roomsApi } from '../api/rooms';
import RoomCard from '../components/RoomCard/RoomCard';
import EmptyState from '../components/EmptyState/EmptyState';
import UniversitySelect from '../features/university/ui/UniversitySelect';
import { useUniversitySearchParam } from '../shared/university/useUniversitySearchParam';
import { normalizeUniversityCode } from '../shared/university/universities';

const getRoomUniversity = (room: Room, fallbackUniversity: string) => {
  return room.universityCode || room.university
    ? normalizeUniversityCode(room.universityCode || room.university)
    : normalizeUniversityCode(fallbackUniversity);
};

const getBuildingUniversity = (building: Building, fallbackUniversity: string) => {
  return building.universityCode || building.university
    ? normalizeUniversityCode(building.universityCode || building.university)
    : normalizeUniversityCode(fallbackUniversity);
};

const RoomsPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const { universityCode, setUniversityCode } = useUniversitySearchParam();
  const buildingFilter = searchParams.get('building') || '';
  const floorFilter = searchParams.get('floor') || '';
  const capacityFilter = searchParams.get('capacity') || '';

  const roomsQuery = useQuery({
    queryKey: ['rooms', universityCode, buildingFilter, capacityFilter],
    queryFn: () =>
      roomsApi.getAll({
        university: universityCode,
        building: buildingFilter,
        capacity: capacityFilter,
      }),
  });

  const buildingsQuery = useQuery({
    queryKey: ['buildings', universityCode],
    queryFn: () => roomsApi.getBuildings(universityCode),
  });

  const buildings = useMemo(() => {
    const data = buildingsQuery.data || [];
    return data.filter((building: Building) => getBuildingUniversity(building, universityCode) === universityCode);
  }, [buildingsQuery.data, universityCode]);

  const rooms = useMemo(() => {
    const buildingsMap = new Map((buildingsQuery.data || []).map((building: Building) => [building.id, building]));

    return (roomsQuery.data || []).map((room: Room) => {
      const building = buildingsMap.get(room.buildingId);
      return {
        ...room,
        building: building?.name || room.building || 'Неизвестное здание',
        universityCode: room.universityCode || room.university || universityCode,
      };
    });
  }, [buildingsQuery.data, roomsQuery.data, universityCode]);

  const filteredRooms = useMemo(() => {
    return rooms.filter((room: Room) => {
      if (getRoomUniversity(room, universityCode) !== universityCode) return false;
      if (buildingFilter && room.buildingId !== buildingFilter && room.building !== buildingFilter) return false;
      if (floorFilter && room.floor && room.floor.toString() !== floorFilter) return false;
      if (capacityFilter && room.capacity < parseInt(capacityFilter, 10)) return false;
      return true;
    });
  }, [buildingFilter, capacityFilter, floorFilter, rooms, universityCode]);

  const uniqueBuildingNames = Array.from(new Set(filteredRooms.map((room: Room) => room.building))).filter(Boolean);
  const floors = Array.from(
    new Set(rooms.map((room: Room) => room.floor).filter((floor): floor is number => floor !== undefined))
  ).sort((a, b) => a - b);

  const setFilter = (name: string, value: string) => {
    setSearchParams((currentParams) => {
      const nextParams = new URLSearchParams(currentParams);
      if (value) {
        nextParams.set(name, value);
      } else {
        nextParams.delete(name);
      }
      return nextParams;
    });
  };

  const resetFilters = () => {
    setSearchParams((currentParams) => {
      const nextParams = new URLSearchParams(currentParams);
      nextParams.delete('building');
      nextParams.delete('floor');
      nextParams.delete('capacity');
      return nextParams;
    });
  };

  const loading = roomsQuery.isLoading || buildingsQuery.isLoading;
  const error = roomsQuery.error || buildingsQuery.error;

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
          <div className="error">Не удалось загрузить аудитории. Попробуйте позже.</div>
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
            Найдите пространство под пару, консультацию, встречу команды или учебное мероприятие.
            Выберите университет, сузьте список по корпусу и вместимости, а POLYBOOK покажет только
            подходящие аудитории с ближайшими свободными слотами и актуальным статусом доступности.
          </p>
        </div>

        <div className="filters-panel filters-panel--room-search">
          <UniversitySelect
            id="rooms-university"
            label="Вуз"
            value={universityCode}
            onChange={setUniversityCode}
          />

          <div className="filters-panel__field">
            <label htmlFor="building">Корпус</label>
            <select
              id="building"
              name="building"
              value={buildingFilter}
              onChange={(event) => setFilter('building', event.target.value)}
            >
              <option value="">Все корпуса</option>
              {buildings.map((building) => (
                <option key={building.id} value={building.id}>
                  {building.name}
                </option>
              ))}
              {uniqueBuildingNames.map((buildingName) => (
                <option key={buildingName} value={buildingName}>
                  {buildingName}
                </option>
              ))}
            </select>
          </div>

          <div className="filters-panel__field">
            <label htmlFor="floor">Этаж</label>
            <select
              id="floor"
              name="floor"
              value={floorFilter}
              onChange={(event) => setFilter('floor', event.target.value)}
            >
              <option value="">Все этажи</option>
              {floors.map((floor) => (
                <option key={floor} value={floor.toString()}>
                  {floor}
                </option>
              ))}
            </select>
          </div>

          <div className="filters-panel__field">
            <label htmlFor="capacity">Вместимость (мин.)</label>
            <input
              type="number"
              id="capacity"
              name="capacity"
              min="10"
              max="250"
              step="10"
              value={capacityFilter}
              onChange={(event) => setFilter('capacity', event.target.value)}
              placeholder="Любая"
            />
          </div>

          <div className="filters-panel__actions">
            <button type="button" className="btn btn--ghost" onClick={resetFilters}>
              Сбросить
            </button>
          </div>
        </div>

        <div className="results">
          <div className="results__toolbar">
            <div>
              <span className="pill pill--muted">{filteredRooms.length} аудиторий найдено</span>
            </div>
          </div>
          <div className="room-cards room-cards--grid">
            {filteredRooms.length === 0 ? (
              <EmptyState message="По выбранным параметрам аудиторий не найдено. Попробуйте изменить фильтры." />
            ) : (
              filteredRooms.map((room) => (
                <RoomCard key={room.id} room={room} university={universityCode} />
              ))
            )}
          </div>
        </div>
      </div>
    </section>
  );
};

export default RoomsPage;
