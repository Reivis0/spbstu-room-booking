import { UniversityCode } from '../shared/university/universities';

export type RoomAvailabilityStatus = 'free' | 'busy' | 'unknown';

export interface Building {
  id: string;
  code: string;
  name: string;
  address: string;
  universityId?: string;
  university_id?: string;
  university?: UniversityCode | string;
  universityCode?: UniversityCode | string;
}

export interface Room {
  id: string;
  buildingId: string;
  name: string;
  code: string;
  capacity: number;
  features: string;
  title?: string;
  floor?: number;
  building?: string;
  building_id?: string;
  type?: string;
  equipment?: string[];
  description?: string;
  nextSlots?: string[];
  universityId?: string;
  university_id?: string;
  university?: UniversityCode | string;
  universityCode?: UniversityCode | string;
  availabilityStatus?: RoomAvailabilityStatus;
  nextSlotAt?: string;
  statusUpdatedAt?: string;
}

export interface Booking {
  id: string;
  roomId: string;
  userId: string;
  startsAt: string;
  endsAt: string;
  title?: string;
  status: 'pending' | 'confirmed' | 'cancelled';
  createdAt?: string;
  updatedAt?: string;
  roomName?: string;
  userName?: string;
  startTime?: Date;
  endTime?: Date;
  purpose?: string;
  university?: UniversityCode | string;
  universityCode?: UniversityCode | string;
  room?: Room;
  isChain?: boolean;
  chainId?: string;
  chainItems?: Booking[];
}

export interface User {
  id: string;
  email: string;
  firstname: string;
  lastname: string;
  name: string;
  role: 'student' | 'teacher' | 'admin' | string;
}

export interface BookingFormData {
  roomId: string;
  title?: string;
  startsAt?: string;
  endsAt?: string;
  startTime?: string;
  endTime?: string;
  purpose?: string;
}

export interface ScheduleRow {
  room: string;
  time: string;
  status: 'free' | 'busy';
  event: string;
}

export interface ScheduleByFloor {
  [floor: number]: ScheduleRow[];
}

export interface ScheduleSlot {
  from: string;
  to: string;
  type: 'schedule' | 'booking' | 'free';
  label: string;
  status: 'occupied' | 'free';
}

export interface RoomSchedule {
  date: string;
  roomId: string;
  slots: ScheduleSlot[];
}
