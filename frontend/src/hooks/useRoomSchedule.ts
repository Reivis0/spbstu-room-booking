import { useState, useEffect } from 'react';
import { RoomSchedule } from '../types';
import { roomsApi } from '../api/rooms';

export function useRoomSchedule(roomId: string, date: string) {
  const [data, setData] = useState<RoomSchedule | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    if (!roomId || !date) return;

    const fetchSchedule = async () => {
      setLoading(true);
      setError(null);
      try {
        const schedule = await roomsApi.getSchedule(roomId, date);
        setData(schedule);
      } catch (err) {
        setError(err instanceof Error ? err : new Error('Failed to fetch schedule'));
        console.error('[useRoomSchedule] Error:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchSchedule();
  }, [roomId, date]);

  return { data, loading, error };
}
