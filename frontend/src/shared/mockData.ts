import { Building, Room, ScheduleByFloor } from '../types';
import { UniversityCode } from './university/universities';

export const mockBuildings: Record<UniversityCode, Building[]> = {
  SPBSTU: [
    { id: 'spbstu-main', code: 'MAIN', name: 'Главный учебный корпус', address: 'Политехническая ул., 29', universityCode: 'SPBSTU' },
    { id: 'spbstu-hydro', code: 'HYDRO', name: 'Гидрокорпус-2', address: 'Политехническая ул., 29', universityCode: 'SPBSTU' },
  ],
  SPBU: [
    { id: 'spbu-vo12', code: 'VO12', name: '12 линия В.О.', address: '12 линия В.О.', universityCode: 'SPBU' },
    { id: 'spbu-mendeleev', code: 'MEN', name: 'Менделеевский центр', address: 'Университетская наб.', universityCode: 'SPBU' },
  ],
  LETI: [
    { id: 'leti-5', code: 'K5', name: 'Корпус 5', address: 'ул. Профессора Попова, 5', universityCode: 'LETI' },
    { id: 'leti-3', code: 'K3', name: 'Корпус 3', address: 'ул. Профессора Попова, 5', universityCode: 'LETI' },
  ],
};

export const mockRooms: Record<UniversityCode, Room[]> = {
  SPBSTU: [
    {
      id: 'spbstu-120',
      buildingId: 'spbstu-main',
      name: 'Ауд. 120',
      code: '120',
      capacity: 96,
      features: '{"equipment":["projector","whiteboard","wifi"]}',
      floor: 1,
      building: 'Главный учебный корпус',
      equipment: ['projector', 'whiteboard', 'wifi'],
      nextSlots: ['10:15', '12:00', '13:45'],
      universityCode: 'SPBSTU',
      availabilityStatus: 'free',
      statusUpdatedAt: new Date().toISOString(),
    },
    {
      id: 'spbstu-314',
      buildingId: 'spbstu-hydro',
      name: 'Ауд. 314',
      code: '314',
      capacity: 48,
      features: '{"equipment":["projector","computer"]}',
      floor: 3,
      building: 'Гидрокорпус-2',
      equipment: ['projector', 'computer'],
      nextSlots: ['15:30', '17:15'],
      universityCode: 'SPBSTU',
      availabilityStatus: 'busy',
      statusUpdatedAt: new Date().toISOString(),
    },
  ],
  SPBU: [
    {
      id: 'spbu-340',
      buildingId: 'spbu-vo12',
      name: 'Ауд. 340',
      code: '340',
      capacity: 120,
      features: '{"equipment":["projector","whiteboard","wifi"]}',
      floor: 3,
      building: '12 линия В.О.',
      equipment: ['projector', 'whiteboard', 'wifi'],
      nextSlots: ['12:00', '13:45'],
      universityCode: 'SPBU',
      availabilityStatus: 'free',
      statusUpdatedAt: new Date().toISOString(),
    },
    {
      id: 'spbu-216',
      buildingId: 'spbu-mendeleev',
      name: 'Ауд. 216',
      code: '216',
      capacity: 86,
      features: '{"equipment":["projector","wifi"]}',
      floor: 2,
      building: 'Менделеевский центр',
      equipment: ['projector', 'wifi'],
      nextSlots: ['10:15', '15:30'],
      universityCode: 'SPBU',
      availabilityStatus: 'busy',
      statusUpdatedAt: new Date().toISOString(),
    },
  ],
  LETI: [
    {
      id: 'leti-5316',
      buildingId: 'leti-5',
      name: 'Ауд. 5316',
      code: '5316',
      capacity: 48,
      features: '{"equipment":["projector","whiteboard","wifi"]}',
      floor: 3,
      building: 'Корпус 5',
      equipment: ['projector', 'whiteboard', 'wifi'],
      nextSlots: ['10:15', '12:00', '13:45'],
      universityCode: 'LETI',
      availabilityStatus: 'free',
      statusUpdatedAt: new Date().toISOString(),
    },
    {
      id: 'leti-5212',
      buildingId: 'leti-5',
      name: 'Ауд. 5212',
      code: '5212',
      capacity: 64,
      features: '{"equipment":["projector","computer"]}',
      floor: 2,
      building: 'Корпус 5',
      equipment: ['projector', 'computer'],
      nextSlots: ['12:00', '15:30'],
      universityCode: 'LETI',
      availabilityStatus: 'free',
      statusUpdatedAt: new Date().toISOString(),
    },
  ],
};

export const mockSchedule: Record<UniversityCode, ScheduleByFloor> = {
  SPBSTU: {
    1: [
      { room: 'Ауд. 120', time: '10:15–11:45', status: 'free', event: 'Свободно' },
      { room: 'Ауд. 112', time: '12:00–13:30', status: 'busy', event: 'Лекция' },
    ],
    2: [{ room: 'Ауд. 217', time: '11:30–13:00', status: 'free', event: 'Свободно' }],
  },
  SPBU: {
    2: [{ room: 'Ауд. 216', time: '10:15–11:45', status: 'busy', event: 'Семинар' }],
    3: [{ room: 'Ауд. 340', time: '12:00–13:30', status: 'free', event: 'Свободно' }],
  },
  LETI: {
    2: [{ room: 'Ауд. 5212', time: '12:00–13:30', status: 'free', event: 'Свободно' }],
    3: [{ room: 'Ауд. 5316', time: '10:15–11:45', status: 'free', event: 'Свободно' }],
  },
};
