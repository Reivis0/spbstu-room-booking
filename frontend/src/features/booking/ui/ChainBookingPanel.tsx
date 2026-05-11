import React, { useMemo, useState, useEffect } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { format, parseISO } from 'date-fns';
import toast from 'react-hot-toast';
import { bookingsApi, ChainBookingRequest } from '../../../api/bookings';
import { roomsApi } from '../../../api/rooms';
import { Room } from '../../../types';
import { UniversityCode, getUniversity } from '../../../shared/university/universities';

interface ChainBookingPanelProps {
  university: UniversityCode;
  room?: Room | null;
  startsAt?: string;
  endsAt?: string;
  purpose?: string;
  onCreated: () => void;
}

const addMinutes = (value: string, minutes: number) => {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  date.setMinutes(date.getMinutes() + minutes);
  return format(date, "yyyy-MM-dd'T'HH:mm");
};

const ChainBookingPanel: React.FC<ChainBookingPanelProps> = ({
  university,
  room,
  startsAt,
  endsAt,
  purpose,
  onCreated,
}) => {
  const selectedUniversity = getUniversity(university);
  
  // Fetch all rooms in the same building
  const { data: buildingRooms = [], isLoading: loadingRooms } = useQuery({
    queryKey: ['rooms-in-building', university, room?.buildingId],
    queryFn: () => room?.buildingId ? roomsApi.getAll({ university, building: room.buildingId }) : Promise.resolve([]),
    enabled: !!room?.buildingId,
  });

  const slotMinutes = useMemo(() => {
    if (!startsAt || !endsAt) return 90;
    const start = new Date(startsAt);
    const end = new Date(endsAt);
    const diff = Math.round((end.getTime() - start.getTime()) / 60_000);
    return diff > 0 ? diff : 90;
  }, [endsAt, startsAt]);

  const [items, setItems] = useState<Array<{roomId: string, startsAt: string, endsAt: string, isAvailable: boolean, checking: boolean}>>([]);

  // Initialize and re-sync items when props change
  useEffect(() => {
    if (startsAt && endsAt && room?.id) {
      const firstStart = format(new Date(startsAt), "yyyy-MM-dd'T'HH:mm");
      const firstEnd = format(new Date(endsAt), "yyyy-MM-dd'T'HH:mm");
      
      setItems([0, 1, 2].map(index => ({
        roomId: room.id,
        startsAt: index === 0 ? firstStart : addMinutes(firstStart, slotMinutes * index),
        endsAt: index === 0 ? firstEnd : addMinutes(firstEnd, slotMinutes * index),
        isAvailable: true,
        checking: false,
      })));
    }
  }, [startsAt, endsAt, room?.id, slotMinutes]);

  const runCheck = async (index: number, roomId: string, startStr: string, endStart: string) => {
    if (!roomId || !startStr || !endStart) return;

    setItems(prev => prev.map((it, i) => i === index ? { ...it, checking: true } : it));
    
    try {
      const date = format(new Date(startStr), 'yyyy-MM-dd');
      const schedule = await roomsApi.getSchedule(roomId, date, university);
      
      const start = new Date(startStr);
      const end = new Date(endStart);
      
      const isOccupied = schedule.slots.some(slot => {
        if (slot.status === 'free') return false;
        const sFrom = parseISO(slot.from);
        const sTo = parseISO(slot.to);
        return (start < sTo) && (end > sFrom);
      });

      setItems(prev => prev.map((it, i) => i === index ? { ...it, isAvailable: !isOccupied, checking: false } : it));
    } catch (e) {
      setItems(prev => prev.map((it, i) => i === index ? { ...it, checking: false } : it));
    }
  };

  // Run check for all items when they are initialized or buildingRooms are loaded
  useEffect(() => {
    if (items.length > 0 && !loadingRooms) {
      items.forEach((item, index) => {
        if (!item.checking) {
           runCheck(index, item.roomId, item.startsAt, item.endsAt);
        }
      });
    }
  }, [items.length, loadingRooms]);

  const createChainMutation = useMutation({
    mutationFn: (payload: ChainBookingRequest) => bookingsApi.createChain(payload),
    onSuccess: () => {
      toast.success('Цепочка успешно забронирована!', { duration: 5000 });
      onCreated();
    },
    onError: (err: any) => {
      if (err.response?.status === 409) {
        const conflicts = err.response.data;
        let roomInfo = 'Один из слотов уже занят.';
        
        if (Array.isArray(conflicts) && conflicts.length > 0 && conflicts[0].roomId) {
          const conflictingRoom = buildingRooms.find((r: Room) => r.id === conflicts[0].roomId);
          const name = conflictingRoom ? conflictingRoom.name : conflicts[0].roomId;
          roomInfo = `Аудитория ${name} уже занята.`;
        }
        
        toast.error(`Цепочка не создана. ${roomInfo} Выполнен откат Saga.`, { duration: 8000 });
      } else {
        const data = err.response?.data;
        const msg = typeof data === 'string' ? data : (data?.message || 'Ошибка создания цепочки');
        toast.error(msg, { duration: 6000 });
      }
    }
  });

  const updateItem = (index: number, field: string, value: string) => {
    setItems((currentItems) => {
      const updated = { ...currentItems[index], [field]: value };
      const newList = currentItems.map((item, itemIndex) =>
        itemIndex === index ? updated : item
      );
      // Run check for the updated item
      runCheck(index, updated.roomId, updated.startsAt, updated.endsAt);
      return newList;
    });
  };

  const canSubmit = items.length > 0 && items.every((item) => item.roomId && item.startsAt && item.endsAt && item.isAvailable);

  const handleSubmit = () => {
    createChainMutation.mutate({
      university,
      purpose,
      title: purpose,
      items: items.map((item) => ({
        roomId: item.roomId,
        startsAt: new Date(item.startsAt).toISOString(),
        endsAt: new Date(item.endsAt).toISOString(),
      })),
    });
  };

  return (
    <aside className="chain-booking-panel">
      <div className="chain-booking-panel__head">
        <div>
          <h2>Цепочка бронирований</h2>
          <p>Проверьте доступность и выберите аудитории в корпусе: {room?.building}</p>
        </div>
        <span className="pill">Saga</span>
      </div>

      <div className="chain-booking-panel__items">
        {items.map((item, index) => (
          <div className="chain-booking-panel__item" key={index} style={{ 
            borderLeft: item.isAvailable ? '4px solid #4caf50' : '4px solid #f44336',
            opacity: item.checking ? 0.7 : 1
          }}>
            <span className="chain-booking-panel__index">{index + 1}</span>
            
            <div className="form-group" style={{ marginBottom: '10px' }}>
              <label style={{ fontSize: '0.8rem', fontWeight: 600 }}>Аудитория:</label>
              <select 
                value={item.roomId} 
                onChange={(e) => updateItem(index, 'roomId', e.target.value)}
                style={{ width: '100%', padding: '8px', borderRadius: '8px', border: '1px solid #ddd', backgroundColor: '#fff' }}
              >
                {buildingRooms.map(r => (
                  <option key={r.id} value={r.id}>{r.name} ({r.capacity} мест)</option>
                ))}
              </select>
            </div>

            <div className="form-row" style={{ display: 'flex', gap: '8px' }}>
              <div style={{ flex: 1 }}>
                <label style={{ fontSize: '0.7rem' }}>Начало</label>
                <input
                  type="datetime-local"
                  value={item.startsAt}
                  onChange={(event) => updateItem(index, 'startsAt', event.target.value)}
                  style={{ width: '100%', padding: '6px', fontSize: '0.85rem' }}
                />
              </div>
              <div style={{ flex: 1 }}>
                <label style={{ fontSize: '0.7rem' }}>Конец</label>
                <input
                  type="datetime-local"
                  value={item.endsAt}
                  onChange={(event) => updateItem(index, 'endsAt', event.target.value)}
                  style={{ width: '100%', padding: '6px', fontSize: '0.85rem' }}
                />
              </div>
            </div>

            <div style={{ marginTop: '8px', fontSize: '0.8rem', fontWeight: 600 }}>
              {item.checking ? (
                <span style={{ color: '#666' }}>⌛ Проверка доступности...</span>
              ) : item.isAvailable ? (
                <span style={{ color: '#4caf50' }}>✓ Свободно</span>
              ) : (
                <span style={{ color: '#f44336' }}>✗ Занято! Выберите другую аудиторию</span>
              )}
            </div>
          </div>
        ))}
      </div>

      <button
        type="button"
        className="btn btn--primary chain-booking-panel__submit"
        disabled={!canSubmit || createChainMutation.isPending || loadingRooms}
        onClick={handleSubmit}
      >
        {createChainMutation.isPending ? 'Бронирование...' : 'Забронировать цепочку'}
      </button>
    </aside>
  );
};

export default ChainBookingPanel;
