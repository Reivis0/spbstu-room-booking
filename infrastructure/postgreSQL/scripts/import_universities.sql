INSERT INTO buildings (id, university_id, code, name, address) VALUES
  (uuid_generate_v4(),
   (SELECT id FROM universities WHERE id='spbgu'),
   'SPBGU-MEN', 'Менделеевская линия',
   'Университетская наб., 7–9, Санкт-Петербург'),
  (uuid_generate_v4(),
   (SELECT id FROM universities WHERE id='spbgu'),
   'SPBGU-HIST', 'Исторический факультет',
   'Менделеевская линия, 5, Санкт-Петербург'),
  (uuid_generate_v4(),
   (SELECT id FROM universities WHERE id='spbgu'),
   'SPBGU-PHIL', 'Философский факультет',
   'Менделеевская линия, 5, Санкт-Петербург')
ON CONFLICT (code) DO NOTHING;

INSERT INTO rooms (id, building_id, code, name, capacity, features)
SELECT
  uuid_generate_v4(),
  b.id,
  b.code || '-L1',
  'Лекционный зал ' || b.code,
  150,
  '{"type": "lecture_hall", "projector": true, "whiteboard": true}'::jsonb
FROM buildings b WHERE code LIKE 'SPBGU-%'
ON CONFLICT DO NOTHING;

INSERT INTO rooms (id, building_id, code, name, capacity, features)
SELECT
  uuid_generate_v4(),
  b.id,
  b.code || '-S1',
  'Аудитория семинаров ' || b.code,
  30,
  '{"type": "seminar_room", "projector": false, "whiteboard": true}'::jsonb
FROM buildings b WHERE code LIKE 'SPBGU-%'
ON CONFLICT DO NOTHING;

INSERT INTO rooms (id, building_id, code, name, capacity, features)
SELECT
  uuid_generate_v4(),
  b.id,
  b.code || '-LAB1',
  'Компьютерный класс ' || b.code,
  20,
  '{"type": "lab", "computers": 20, "projector": true}'::jsonb
FROM buildings b WHERE code LIKE 'SPBGU-%'
ON CONFLICT DO NOTHING;


INSERT INTO buildings (id, university_id, code, name, address) VALUES
  (uuid_generate_v4(),
   (SELECT id FROM universities WHERE id='leti'),
   'LETI-MAIN', 'Главный корпус ЛЭТИ',
   '5-я линия В.О., 14, Санкт-Петербург'),
  (uuid_generate_v4(),
   (SELECT id FROM universities WHERE id='leti'),
   'LETI-CORP2', 'Корпус 2',
   'ул. Профессора Попова, 5, Санкт-Петербург'),
  (uuid_generate_v4(),
   (SELECT id FROM universities WHERE id='leti'),
   'LETI-CORP3', 'Корпус 3 (Лабораторный)',
   'ул. Профессора Попова, 5, Санкт-Петербург')
ON CONFLICT (code) DO NOTHING;

INSERT INTO rooms (id, building_id, code, name, capacity, features)
SELECT
  uuid_generate_v4(),
  b.id,
  b.code || '-L1',
  'Лекционная ' || b.code,
  200,
  '{"type": "lecture_hall", "projector": true, "whiteboard": true, "microphone": true}'::jsonb
FROM buildings b WHERE code LIKE 'LETI-%'
ON CONFLICT DO NOTHING;

INSERT INTO rooms (id, building_id, code, name, capacity, features)
SELECT
  uuid_generate_v4(),
  b.id,
  b.code || '-S1',
  'Аудитория для практик ' || b.code,
  40,
  '{"type": "seminar_room", "projector": true, "whiteboard": true}'::jsonb
FROM buildings b WHERE code LIKE 'LETI-%'
ON CONFLICT DO NOTHING;

INSERT INTO rooms (id, building_id, code, name, capacity, features)
SELECT
  uuid_generate_v4(),
  b.id,
  b.code || '-LAB1',
  'Лабораторная ' || b.code,
  15,
  '{"type": "lab", "computers": 15, "hardware": "oscilloscopes"}'::jsonb
FROM buildings b WHERE code LIKE 'LETI-%'
ON CONFLICT DO NOTHING;
