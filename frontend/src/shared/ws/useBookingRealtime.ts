import { useEffect } from 'react';
import { QueryClient } from '@tanstack/react-query';
import { UniversityCode, normalizeUniversityCode } from '../university/universities';

type RealtimeEventType = 'booking.status_changed' | 'cache.invalidate';

interface BookingRealtimeEvent {
  type?: RealtimeEventType;
  event?: RealtimeEventType;
  university?: string;
  uni?: string;
  roomId?: string;
  status?: string;
  nextSlotAt?: string;
  payload?: {
    university?: string;
    uni?: string;
    roomId?: string;
    status?: string;
    nextSlotAt?: string;
  };
}

const buildWebSocketUrl = () => {
  if (process.env.REACT_APP_WS_URL) {
    return process.env.REACT_APP_WS_URL;
  }

  const apiUrl = process.env.REACT_APP_API_URL || '/api/v1';
  const origin = window.location.origin.replace(/^http/, 'ws');

  if (apiUrl.startsWith('http')) {
    return apiUrl.replace(/^http/, 'ws').replace(/\/api\/v1\/?$/, '/api/v1/ws');
  }

  return `${origin}${apiUrl.replace(/\/$/, '')}/ws`;
};

export const useBookingRealtime = (queryClient: QueryClient) => {
  useEffect(() => {
    let socket: WebSocket | null = null;
    let reconnectTimer: number | undefined;
    let closedByEffect = false;

    const connect = () => {
      try {
        socket = new WebSocket(buildWebSocketUrl());
      } catch (error) {
        if (process.env.NODE_ENV === 'development') {
          console.warn('[bookingRealtime] WebSocket init failed:', error);
        }
        return;
      }

      socket.onmessage = (message) => {
        try {
          const event = JSON.parse(message.data) as BookingRealtimeEvent;
          const eventType = event.type || event.event;
          const payload = event.payload || {};
          const university = normalizeUniversityCode(
            event.university || event.uni || payload.university || payload.uni
          );
          const roomId = event.roomId || payload.roomId;
          const status = event.status || payload.status;
          const nextSlotAt = event.nextSlotAt || payload.nextSlotAt;

          if (eventType === 'cache.invalidate') {
            queryClient.invalidateQueries({ queryKey: ['rooms', university] });
            queryClient.invalidateQueries({ queryKey: ['buildings', university] });
            queryClient.invalidateQueries({ queryKey: ['schedule', university] });
            return;
          }

          if (eventType === 'booking.status_changed') {
            if (roomId && status) {
              queryClient.setQueriesData({ queryKey: ['rooms', university] }, (oldData: unknown) => {
                if (!Array.isArray(oldData)) {
                  return oldData;
                }

                return oldData.map((room) => {
                  if (!room || typeof room !== 'object' || (room as { id?: string }).id !== roomId) {
                    return room;
                  }

                  return {
                    ...room,
                    availabilityStatus: status,
                    nextSlotAt,
                    statusUpdatedAt: new Date().toISOString(),
                  };
                });
              });
            }

            queryClient.invalidateQueries({ queryKey: ['bookings'] });
          }
        } catch (error) {
          if (process.env.NODE_ENV === 'development') {
            console.warn('[bookingRealtime] Failed to handle message:', error);
          }
        }
      };

      socket.onclose = () => {
        if (!closedByEffect) {
          reconnectTimer = window.setTimeout(connect, 5_000);
        }
      };

      socket.onerror = () => {
        socket?.close();
      };
    };

    connect();

    return () => {
      closedByEffect = true;
      if (reconnectTimer) {
        window.clearTimeout(reconnectTimer);
      }
      socket?.close();
    };
  }, [queryClient]);
};

export const getRealtimeQueryScope = (universityCode: UniversityCode) => ['rooms', universityCode];
