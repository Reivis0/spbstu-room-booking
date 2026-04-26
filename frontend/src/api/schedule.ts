import { apiClient } from './client';
import { ScheduleByFloor, ScheduleRow, Page } from '../types';

export const scheduleApi = {
  getByFloors: async (date?: string): Promise<ScheduleByFloor> => {
    const params = date ? { date } : {};
    const response = await apiClient.get<ScheduleByFloor>('/schedule/floors', { params });
    return response.data || {};
  },

  getByFloor: async (floor: number, date?: string): Promise<ScheduleRow[]> => {
    const params = date ? { date, size: 1000 } : { size: 1000 };
    const response = await apiClient.get<Page<ScheduleRow> | ScheduleRow[]>(`/schedule/floors/${floor}`, { params });
    
    // Check if the response is a Page object or a direct array
    return Array.isArray(response.data) ? response.data : response.data.content || [];
  },

  getByRoom: async (roomId: string, date?: string): Promise<ScheduleRow[]> => {
    const params = date ? { date, size: 1000 } : { size: 1000 };
    const response = await apiClient.get<Page<ScheduleRow> | ScheduleRow[]>(`/schedule/room/${roomId}`, { params });
    
    // Check if the response is a Page object or a direct array
    return Array.isArray(response.data) ? response.data : response.data.content || [];
  },
};

