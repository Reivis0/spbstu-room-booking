import { apiClient } from './client';
import { Room, Building } from '../types';

export interface AvailabilityResponse {
  roomId: string;
  intervals: Array<{
    start: string;
    end: string;
    confidence: string;
  }>;
}

export const roomsApi = {
  getAll: async (): Promise<Room[]> => {
    const response = await apiClient.get<Room[]>('/rooms');
    return response.data.map((room) => {
      let equipment: string[] = [];
      try {
        const features = JSON.parse(room.features || '{}');
        if (Array.isArray(features.equipment)) {
          equipment = features.equipment;
        } else {
          equipment = Object.keys(features).filter(key => features[key] === true);
        }
        if (process.env.NODE_ENV === 'development' && equipment.length > 0) {
          console.log(`[Rooms API] Room ${room.name}: parsed equipment:`, equipment);
        }
      } catch (e) {
        if (process.env.NODE_ENV === 'development') {
          console.warn('[Rooms API] Failed to parse features:', room.features, e);
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
      const features = JSON.parse(room.features || '{}');
      if (Array.isArray(features.equipment)) {
        equipment = features.equipment;
      } else {
        equipment = Object.keys(features).filter(key => features[key] === true);
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

  getAvailability: async (roomId: string, date: string): Promise<AvailabilityResponse> => {
    const response = await apiClient.get<AvailabilityResponse>(
      `/rooms/${roomId}/availability`,
      {
        params: { date },
      }
    );
    return response.data;
  },

  getBuildings: async (): Promise<Building[]> => {
    const response = await apiClient.get<Building[]>('/buildings');
    return response.data;
  },
};


