import { apiClient } from './client';
import { Booking, BookingFormData } from '../types';
import { UniversityCode, getUniversity } from '../shared/university/universities';

export interface ChainBookingRequest {
  university: UniversityCode;
  purpose?: string;
  title?: string;
  items: Array<{
    roomId: string;
    startsAt: string;
    endsAt: string;
  }>;
}

export const bookingsApi = {
  getAll: async (): Promise<Booking[]> => {
    const response = await apiClient.get<Booking[]>('/bookings');
    return response.data;
  },

  getMyBookings: async (university?: UniversityCode): Promise<Booking[]> => {
    const response = await apiClient.get<Booking[]>('/bookings/my', {
      params: {
        university: university ? getUniversity(university).apiValue : undefined,
      },
    });
    return response.data;
  },

  create: async (data: BookingFormData): Promise<Booking> => {
    const requestData = {
      roomId: data.roomId,
      startsAt: data.startsAt || data.startTime || '',
      endsAt: data.endsAt || data.endTime || '',
      title: data.title || data.purpose || undefined,
    };
    const response = await apiClient.post<Booking>('/bookings', requestData);
    return response.data;
  },

  createChain: async (data: ChainBookingRequest): Promise<Booking[]> => {
    const response = await apiClient.post<Booking[]>('/bookings/chain', {
      ...data,
      university: getUniversity(data.university).apiValue,
    });
    return response.data;
  },

  cancel: async (id: string): Promise<void> => {
    await apiClient.delete(`/bookings/${id}`);
  },
};

