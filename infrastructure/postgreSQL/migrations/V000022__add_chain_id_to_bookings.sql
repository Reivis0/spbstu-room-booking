-- Добавляем идентификатор цепочки
ALTER TABLE bookings ADD COLUMN chain_id UUID;

-- Индекс для оптимизации работы Saga Orchestrator (поиск всех звеньев цепи)
CREATE INDEX idx_bookings_chain_id ON bookings(chain_id);

-- Комментарий для документации
COMMENT ON COLUMN bookings.chain_id IS 'Identifier for atomic group of bookings (Saga Pattern)';
