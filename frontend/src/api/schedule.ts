import { apiClient } from './client';
import { ScheduleByFloor, ScheduleRow } from '../types';
import { UniversityCode, getUniversity } from '../shared/university/universities';
import { mockSchedule } from '../shared/mockData';
import { roomsApi } from './rooms';

const USE_MOCKS = process.env.REACT_APP_USE_MOCKS === 'true';

const shouldUseScheduleFallback = (error: unknown): boolean => {
  const status = (error as any)?.response?.status;
  return USE_MOCKS || status === 401 || status === 403 || status === 404;
};

const getDate = (date?: string): string => {
  return date || new Date().toISOString().slice(0, 10);
};

const slotStatusToScheduleStatus = (status: string): ScheduleRow['status'] => {
  return status === 'free' ? 'free' : 'busy';
};

const scheduleRowsByRoom = async (
  roomId: string,
  roomName: string,
  date: string,
  university?: UniversityCode
): Promise<ScheduleRow[]> => {
  const schedule = await roomsApi.getSchedule(roomId, date, university);
  return schedule.slots.map((slot) => ({
    room: roomName,
    time: `${slot.from} - ${slot.to}`,
    status: slotStatusToScheduleStatus(slot.status),
    event: slot.label || (slot.status === 'free' ? 'Свободно' : 'Занято'),
  }));
};

export const scheduleApi = {
  getByFloors: async (
    date?: string,
    university?: UniversityCode
  ): Promise<ScheduleByFloor> => {
    try {
      const targetDate = getDate(date);
      const rooms = await roomsApi.getAll({ university });
      const rowsByFloor: ScheduleByFloor = {};

      await Promise.all(
        rooms.map(async (room) => {
          const floor = room.floor || 1;
          const rows = await scheduleRowsByRoom(room.id, room.name, targetDate, university);
          rowsByFloor[floor] = [...(rowsByFloor[floor] || []), ...rows];
        })
      );

      if (university && Object.keys(rowsByFloor).length === 0) {
        return mockSchedule[university];
      }

      return rowsByFloor;
    } catch (error) {
      if (university && shouldUseScheduleFallback(error)) {
        return mockSchedule[university];
      }
      throw error;
    }
  },

  getByFloor: async (
    floor: number,
    date?: string,
    university?: UniversityCode
  ): Promise<ScheduleRow[]> => {
    const floors = await scheduleApi.getByFloors(date, university);
    return floors[floor] || [];
  },

  getByRoom: async (
    roomId: string,
    date?: string,
    university?: UniversityCode
  ): Promise<ScheduleRow[]> => {
    const response = await apiClient.get<{ name: string }>(`/rooms/${roomId}`, {
      params: {
        university: university ? getUniversity(university).apiValue : undefined,
      },
    });
    return scheduleRowsByRoom(roomId, response.data.name, getDate(date), university);
  },
};
