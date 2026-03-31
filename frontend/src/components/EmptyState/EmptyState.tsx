import React from 'react';

interface EmptyStateProps {
  message: string;
}

const EmptyState: React.FC<EmptyStateProps> = ({ message }) => {
  return (
    <div className="empty">
      <p>{message}</p>
    </div>
  );
};

export default EmptyState;

