import { apiClient } from './client';
import { Room, Building, RoomSchedule, Page } from '../types';

export interface AvailabilityResponse {
  roomId: string;
  intervals: Array<{
    start: string;
    end: string;
    confidence: string;
  }>;
}

export const roomsApi = {
  getAll: async (params?: { page?: number; size?: number; buildingId?: string }): Promise<Room[]> => {
    // Using a large size as a quick fix for missing rooms
    const response = await apiClient.get<Page<Room> | Room[]>('/rooms', {
      params: { size: 2000, ...params },
    });

    // Check if the response is a Page object or a direct array
    const data = Array.isArray(response.data) ? response.data : response.data.content || [];

    return data.map((room) => {
      let equipment: string[] = [];
      try {
        if (room.features) {
          const features = JSON.parse(room.features);
          if (Array.isArray(features.equipment)) {
            equipment = features.equipment;
          } else {
            equipment = Object.keys(features).filter(key => features[key] === true);
          }
        }
      } catch (e) {
        if (process.env.NODE_ENV === 'development') {
          console.warn('[Rooms API] Failed to parse features for room:', room.name, e);
        }
      }
      return {
        ...room,
        equipment,
      };
    });
  },

  getById: async (id: string): Promise<Room> => {
    const response = await apiClient.get<Room>(`/rooms/${id}`);
    const room = response.data;
    let equipment: string[] = [];
    try {
      if (room.features) {
        const features = JSON.parse(room.features);
        if (Array.isArray(features.equipment)) {
          equipment = features.equipment;
        } else {
          equipment = Object.keys(features).filter(key => features[key] === true);
        }
      }
    } catch (e) {
      if (process.env.NODE_ENV === 'development') {
        console.warn('[Rooms API] Failed to parse features:', e);
      }
    }
    return {
      ...room,
      equipment,
    };
  },

  getSchedule: async (roomId: string, date: string): Promise<RoomSchedule> => {
    const response = await apiClient.get<RoomSchedule>(
      `/rooms/${roomId}/schedule`,
      {
        params: { date },
      }
    );
    
    // Ensure schedule has a non-null slots array
    return {
      ...response.data,
      slots: response.data?.slots || [],
    };
  },

  getAvailability: async (roomId: string, date: string): Promise<AvailabilityResponse> => {
    const response = await apiClient.get<AvailabilityResponse>(
      `/rooms/${roomId}/availability`,
      {
        params: { date },
      }
    );
    return response.data;
  },

  getBuildings: async (params?: { page?: number; size?: number }): Promise<Building[]> => {
    const response = await apiClient.get<Page<Building> | Building[]>('/buildings', {
      params: { size: 500, ...params },
    });
    
    // Check if the response is a Page object or a direct array
    return Array.isArray(response.data) ? response.data : response.data.content || [];
  },
};


