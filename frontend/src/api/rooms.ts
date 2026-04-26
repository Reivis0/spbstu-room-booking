import { apiClient } from './client';
import { Room, Building, RoomSchedule } from '../types';
import { UniversityCode, getUniversity, inferUniversityCode } from '../shared/university/universities';
import { mockBuildings, mockRooms } from '../shared/mockData';

const USE_MOCKS = process.env.REACT_APP_USE_MOCKS === 'true';

const shouldUseCatalogFallback = (error: unknown): boolean => {
  const status = (error as any)?.response?.status;
  return USE_MOCKS || status === 401 || status === 403 || status === 404;
};

const getMockRoomSchedule = (roomId: string, date: string): RoomSchedule => ({
  date,
  roomId,
  slots: [
    { from: '08:00', to: '10:00', type: 'free', status: 'free', label: 'Свободно' },
    { from: '10:15', to: '11:45', type: 'schedule', status: 'occupied', label: 'Пара по расписанию' },
    { from: '12:00', to: '13:30', type: 'free', status: 'free', label: 'Свободно' },
    { from: '14:00', to: '15:30', type: 'booking', status: 'occupied', label: 'Бронирование' },
    { from: '15:45', to: '18:00', type: 'free', status: 'free', label: 'Свободно' },
  ],
});

export interface RoomFilters {
  university?: UniversityCode;
  building?: string;
  capacity?: string | number;
}

export interface AvailabilityResponse {
  roomId: string;
  intervals: Array<{
    start: string;
    end: string;
    confidence: string;
  }>;
}

const getRoomBuildingId = (room: Room): string | undefined => {
  return room.buildingId || room.building_id;
};

const enrichBuilding = (building: Building): Building => {
  const universityCode = inferUniversityCode(
    building.universityCode ||
    building.university ||
    building.universityId ||
    building.university_id ||
    building.code
  );

  return {
    ...building,
    universityCode: building.universityCode || universityCode,
    university: building.university || universityCode,
  };
};

const enrichRoom = (room: Room): Room => {
  let equipment: string[] = [];
  try {
    const features = JSON.parse(room.features || '{}');
    if (Array.isArray(features.equipment)) {
      equipment = features.equipment;
    } else {
      equipment = Object.keys(features).filter(key => features[key] === true);
    }
  } catch (e) {
    if (process.env.NODE_ENV === 'development') {
      console.warn('[Rooms API] Failed to parse features:', room.features, e);
    }
  }

  const universityCode = inferUniversityCode(
    room.universityCode ||
    room.university ||
    room.universityId ||
    room.university_id ||
    room.code
  );

  return {
    ...room,
    equipment,
    universityCode: room.universityCode || universityCode,
    university: room.university || universityCode,
  };
};

export const roomsApi = {
  getAll: async (filters: RoomFilters = {}): Promise<Room[]> => {
    const params = {
      university: filters.university ? getUniversity(filters.university).apiValue : undefined,
      building_id: filters.building || undefined,
      capacity_min: filters.capacity || undefined,
      unpaged: true,
    };

    let response;
    try {
      response = await apiClient.get<Room[] | { content: Room[] }>('/rooms', { params });
    } catch (error) {
      if (filters.university && shouldUseCatalogFallback(error)) {
        return mockRooms[filters.university];
      }
      throw error;
    }

    // Support both paginated format (Spring Data Page) and flat array
    const data = response.data;
    const roomsArray = (data as any).content ? (data as any).content : (Array.isArray(data) ? data : []);
    let rooms = roomsArray.map((room: Room) => enrichRoom(room));

    if (filters.university && rooms.length === 0) {
      return mockRooms[filters.university];
    }

    if (filters.university) {
      const buildings = await roomsApi.getBuildings(filters.university);
      const allowedBuildingIds = new Set(buildings.map((building: Building) => building.id));
      rooms = rooms.filter((room: Room) => {
        return room.universityCode === filters.university || allowedBuildingIds.has(getRoomBuildingId(room) || '');
      });
    }

    return rooms;
  },

  getById: async (id: string): Promise<Room> => {
    const response = await apiClient.get<Room>(`/rooms/${id}`);
    return enrichRoom(response.data);
  },

  getSchedule: async (
    roomId: string,
    date: string,
    university?: UniversityCode
  ): Promise<RoomSchedule> => {
    try {
      const response = await apiClient.get<RoomSchedule>(
        `/rooms/${roomId}/schedule`,
        {
          params: {
            date,
            university: university ? getUniversity(university).apiValue : undefined,
          },
        }
      );
      return response.data;
    } catch (error) {
      if (shouldUseCatalogFallback(error)) {
        return getMockRoomSchedule(roomId, date);
      }
      throw error;
    }
  },

  getAvailability: async (
    roomId: string,
    date: string,
    university?: UniversityCode
  ): Promise<AvailabilityResponse> => {
    const response = await apiClient.get<AvailabilityResponse>(
      `/rooms/${roomId}/availability`,
      {
        params: {
          date,
          university: university ? getUniversity(university).apiValue : undefined,
        },
      }
    );
    return response.data;
  },

  getBuildings: async (university?: UniversityCode): Promise<Building[]> => {
    try {
      const response = await apiClient.get<Building[] | { content: Building[] }>('/buildings', {
        params: {
          university: university ? getUniversity(university).apiValue : undefined,
          unpaged: true,
        },
      });
      // Support both paginated format (Spring Data Page) and flat array
      const data = response.data;
      const buildingsArray = (data as any).content ? (data as any).content : (Array.isArray(data) ? data : []);
      const buildings = buildingsArray.map((building: Building) => enrichBuilding(building));
      // Backend already filters by university, no need for client-side filtering
      return buildings;
    } catch (error) {
      if (university && shouldUseCatalogFallback(error)) {
        return mockBuildings[university];
      }
      throw error;
    }
  },
};
