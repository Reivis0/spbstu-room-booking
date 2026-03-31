import { apiClient } from './client';
import { ScheduleByFloor, ScheduleRow } from '../types';

export const scheduleApi = {
  getByFloors: async (date?: string): Promise<ScheduleByFloor> => {
    const params = date ? { date } : {};
    const response = await apiClient.get<ScheduleByFloor>('/schedule/floors', { params });
    return response.data;
  },

  getByFloor: async (floor: number, date?: string): Promise<ScheduleRow[]> => {
    const params = date ? { date } : {};
    const response = await apiClient.get<ScheduleRow[]>(`/schedule/floors/${floor}`, { params });
    return response.data;
  },

  getByRoom: async (roomId: string, date?: string): Promise<ScheduleRow[]> => {
    const params = date ? { date } : {};
    const response = await apiClient.get<ScheduleRow[]>(`/schedule/room/${roomId}`, { params });
    return response.data;
  },
};

