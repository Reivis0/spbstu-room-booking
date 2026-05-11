import React, { useMemo, useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { format } from 'date-fns';
import toast from 'react-hot-toast';
import { bookingsApi, ChainBookingRequest } from '../../../api/bookings';
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
  // Return local time string for datetime-local input
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
  const slotMinutes = useMemo(() => {
    if (!startsAt || !endsAt) return 90;
    const start = new Date(startsAt);
    const end = new Date(endsAt);
    const diff = Math.round((end.getTime() - start.getTime()) / 60_000);
    return diff > 0 ? diff : 90;
  }, [endsAt, startsAt]);

  const [items, setItems] = useState(() => {
    const firstRoomId = room?.id || '';
    return [0, 1, 2].map((index) => ({
      roomId: firstRoomId,
      startsAt: index === 0 ? startsAt || '' : addMinutes(startsAt || '', slotMinutes * index),
      endsAt: index === 0 ? endsAt || '' : addMinutes(endsAt || '', slotMinutes * index),
    }));
  });

  React.useEffect(() => {
    setItems((currentItems) =>
      currentItems.map((item, index) => ({
        ...item,
        roomId: item.roomId || room?.id || '',
        startsAt: index === 0 ? startsAt || item.startsAt : addMinutes(startsAt || item.startsAt, slotMinutes * index),
        endsAt: index === 0 ? endsAt || item.endsAt : addMinutes(endsAt || item.endsAt, slotMinutes * index),
      }))
    );
  }, [endsAt, room?.id, slotMinutes, startsAt]);

  const createChainMutation = useMutation({
    mutationFn: (payload: ChainBookingRequest) => bookingsApi.createChain(payload),
    onSuccess: () => {
      toast.success('Цепочка успешно забронирована! Все слоты подтверждены.', { duration: 5000 });
      onCreated();
    },
    onError: (err: any) => {
      if (err.response?.status === 409) {
        const conflicts = err.response.data;
        const roomInfo = conflicts && conflicts.length > 0 ? `Аудитория ${conflicts[0].roomId || 'выбранная'} уже занята.` : '';
        toast.error(`Цепочка не создана. ${roomInfo} Выполнен полный откат (Saga Rollback).`, { duration: 8000 });
      } else {
        const errorMessage = err.response?.data?.message || 'Ошибка при создании цепочки. Попробуйте позже.';
        toast.error(errorMessage, { duration: 6000 });
      }
    }
  });

  const updateItem = (index: number, field: 'roomId' | 'startsAt' | 'endsAt', value: string) => {
    setItems((currentItems) =>
      currentItems.map((item, itemIndex) =>
        itemIndex === index ? { ...item, [field]: value } : item
      )
    );
  };

  const canSubmit = items.every((item) => item.roomId && item.startsAt && item.endsAt);

  const handleSubmit = () => {
    createChainMutation.mutate({
      university,
      purpose,
      title: purpose,
      items: items.map((item) => {
        const start = new Date(item.startsAt);
        const end = new Date(item.endsAt);
        return {
          roomId: item.roomId,
          startsAt: format(start, "yyyy-MM-dd'T'HH:mm:ssXXX"),
          endsAt: format(end, "yyyy-MM-dd'T'HH:mm:ssXXX"),
        };
      }),
    });
  };

  return (
    <aside className="chain-booking-panel" aria-labelledby="chain-booking-title">
      <div className="chain-booking-panel__head">
        <div>
          <h2 id="chain-booking-title">Цепочка бронирований</h2>
          <p>
            {selectedUniversity.label}: несколько смежных слотов будут отправлены в Booking API одной заявкой.
          </p>
        </div>
        <span className="pill">Saga</span>
      </div>

      <div className="chain-booking-panel__items">
        {items.map((item, index) => (
          <div className="chain-booking-panel__item" key={index}>
            <span className="chain-booking-panel__index">{index + 1}</span>
            <label>
              Аудитория
              <input
                value={item.roomId}
                onChange={(event) => updateItem(index, 'roomId', event.target.value)}
                placeholder="ID аудитории"
              />
            </label>
            <label>
              Начало
              <input
                type="datetime-local"
                value={item.startsAt}
                onChange={(event) => updateItem(index, 'startsAt', event.target.value)}
              />
            </label>
            <label>
              Конец
              <input
                type="datetime-local"
                value={item.endsAt}
                onChange={(event) => updateItem(index, 'endsAt', event.target.value)}
              />
            </label>
          </div>
        ))}
      </div>

      {createChainMutation.error && (
        <div className="chain-booking-panel__error">
          Не удалось создать цепочку. Возможно, endpoint еще не доступен на бэкенде.
        </div>
      )}

      <button
        type="button"
        className="btn btn--primary chain-booking-panel__submit"
        disabled={!canSubmit || createChainMutation.isPending}
        onClick={handleSubmit}
      >
        {createChainMutation.isPending ? 'Создание...' : 'Забронировать цепочку'}
      </button>
    </aside>
  );
};

export default ChainBookingPanel;
