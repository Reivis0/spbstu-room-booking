import { apiClient } from './client';
import { Booking, BookingFormData } from '../types';

export const bookingsApi = {
  getAll: async (): Promise<Booking[]> => {
    const response = await apiClient.get<Booking[]>('/bookings');
    return response.data;
  },

  getMyBookings: async (): Promise<Booking[]> => {
    const response = await apiClient.get<Booking[]>('/bookings/my');
    return response.data;
  },

  create: async (data: BookingFormData): Promise<Booking> => {
    const requestData = {
      roomId: data.roomId,
      startsAt: data.startsAt || data.startTime || '',
      endsAt: data.endsAt || data.endTime || '',
    };
    const response = await apiClient.post<Booking>('/bookings', requestData);
    return response.data;
  },

  cancel: async (id: string): Promise<void> => {
    await apiClient.delete(`/bookings/${id}`);
  },
};


