import { useQuery } from '@tanstack/react-query';
import { roomsApi } from '../api/rooms';
import { UniversityCode } from '../shared/university/universities';

export function useRoomSchedule(roomId: string, date: string, university?: UniversityCode) {
  const query = useQuery({
    queryKey: ['roomSchedule', university, roomId, date],
    queryFn: () => roomsApi.getSchedule(roomId, date, university),
    enabled: Boolean(roomId && date),
  });

  return {
    data: query.data || null,
    loading: query.isLoading,
    error: query.error instanceof Error ? query.error : null,
  };
}
