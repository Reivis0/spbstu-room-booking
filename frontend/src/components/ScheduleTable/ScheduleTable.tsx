import React from 'react';

export interface ScheduleRow {
  room: string;
  time: string;
  status: 'free' | 'busy';
  event: string;
}

interface ScheduleTableProps {
  rows: ScheduleRow[];
}

const ScheduleTable: React.FC<ScheduleTableProps> = ({ rows }) => {
  if (!rows || rows.length === 0) {
    return null;
  }

  return (
    <table className="schedule-table">
      <thead>
        <tr>
          <th>Аудитория</th>
          <th>Время</th>
          <th>Статус</th>
          <th>Мероприятие</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((row, index) => (
          <tr key={`${row.room}-${index}`}>
            <td>{row.room}</td>
            <td>{row.time}</td>
            <td>
              <span className={`status status--${row.status}`}>
                {row.status === 'free' ? 'Свободно' : 'Занято'}
              </span>
            </td>
            <td>{row.event}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
};

export default ScheduleTable;

