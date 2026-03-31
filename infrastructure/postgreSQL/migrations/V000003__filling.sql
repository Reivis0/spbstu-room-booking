CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Insert Buildings
INSERT INTO buildings (id, code, name, address) VALUES ('db6d11ff-dbd1-4f07-85c6-764504eb73e5', '1', 'Главное здание', 'Санкт-Петербург, ул. Политехническая, д. 29');


-- Insert Users (updated emails for students)
INSERT INTO users (id, email, password_hash, firstname, lastname, role, is_active, created_at, updated_at) VALUES (
    '7f441a7b-3615-4aba-a70f-77eeed31957a', 'ivanov.a.a@edu.spbstu.ru', '774a6d3d548c7dd028297f04f5fa001fe4fe0dbd0482382cb924442b772c8c10', 'Иван', 'Иванов', 'teacher', True, '2025-10-20 09:00:00+00', '2025-10-20 09:00:00+00');

INSERT INTO users (id, email, password_hash, firstname, lastname, role, is_active, created_at, updated_at) VALUES (
    '5f3b4bcd-db2e-4c34-854b-477fc79dad7a', 'petrov.p.p@edu.spbstu.ru', 'b52cef0c57c8a4ad613d9c4de354212e5c0ead835962059dbc8b454b1de4f071', 'Петр', 'Петров', 'student', True, '2025-10-20 09:00:00+00', '2025-10-20 09:00:00+00');

INSERT INTO users (id, email, password_hash, firstname, lastname, role, is_active, created_at, updated_at) VALUES (
    '79d934c0-67cf-4e85-9229-a88d52e9afe7', 'sidorov.a.s@edu.spbstu.ru', '7e4c131d6b3db681bfac64050bab9c9e66b131b853219a088da02ab4f334688c', 'Алексей', 'Сидоров', 'student', True, '2025-10-20 09:00:00+00', '2025-10-20 09:00:00+00');

INSERT INTO users (id, email, password_hash, firstname, lastname, role, is_active, created_at, updated_at) VALUES (
    '23106058-2372-4dcd-b761-fb2eee028717', 'kozlov.m.k@edu.spbstu.ru',
    crypt('admin', gen_salt('bf', 10))
    , 'Михаил', 'Козлов', 'admin', True, '2025-10-20 09:00:00+00', '2025-10-20 09:00:00+00'
)
ON CONFLICT (email)
DO UPDATE SET
    password_hash = crypt('admin123', gen_salt('bf', 10)),
    role = 'admin';

INSERT INTO users (id, email, password_hash, firstname, lastname, role, is_active, created_at, updated_at) VALUES (
    '191f4d1e-bbbe-4ca5-b1e2-b285ebe07f38', 'smirnov.s.s@edu.spbstu.ru', '98abd83382b662eb2c80589d0ef79a547bbe16e08c6f0d4ed6d25c1f8393ba37', 'Сергей', 'Смирнов', 'student', True, '2025-10-20 09:00:00+00', '2025-10-20 09:00:00+00');

INSERT INTO users (id, email, password_hash, firstname, lastname, role, is_active, created_at, updated_at) VALUES (
    '38e278e3-7dc2-4d00-87dc-d6b822745ec7', 'popov.d.p@edu.spbstu.ru', '1ee848750a1769b60b70600de3e75671908c78f99deb7e2f29654273cc12e73e', 'Дмитрий', 'Попов', 'admin', True, '2025-10-20 09:00:00+00', '2025-10-20 09:00:00+00');

INSERT INTO users (id, email, password_hash, firstname, lastname, role, is_active, created_at, updated_at) VALUES (
    '1fc13a20-3652-463d-b587-259dbdf2ab28', 'vasilev.a.v@edu.spbstu.ru', '0f7e2d95908e87744b5e444e48a10225437f8edf6afceb8730a860a383118354', 'Андрей', 'Васильев', 'admin', True, '2025-10-20 09:00:00+00', '2025-10-20 09:00:00+00');

INSERT INTO users (id, email, password_hash, firstname, lastname, role, is_active, created_at, updated_at) VALUES (
    '88b7f450-3d4e-4d3f-b7dd-2b78919dc548', 'sokolov.v.s@edu.spbstu.ru', '4637da5ade89103e803dbd015232423c5d690cecf77973b0172f86f6b429ff12', 'Владимир', 'Соколов', 'admin', True, '2025-10-20 09:00:00+00', '2025-10-20 09:00:00+00');

INSERT INTO users (id, email, password_hash, firstname, lastname, role, is_active, created_at, updated_at) VALUES (
    'd32a9a33-00b3-4e6a-8059-eb48c32a43b7', 'mihaylov.n.m@edu.spbstu.ru', '964555a6c86a4931274d58f44b9f9599008c6d061e755ce7e6c12e84d97ed53f', 'Николай', 'Михайлов', 'admin', True, '2025-10-20 09:00:00+00', '2025-10-20 09:00:00+00');

INSERT INTO users (id, email, password_hash, firstname, lastname, role, is_active, created_at, updated_at) VALUES (
    'c9c7751d-7ef9-4787-806f-8402ebeb9c50', 'novikov.a.n@edu.spbstu.ru', '1bfaa8c15b0db42d7c44ccaae5f3a59973209115ebbf7554f1cea184e74c10f1', 'Александр', 'Новиков', 'student', True, '2025-10-20 09:00:00+00', '2025-10-20 09:00:00+00');

INSERT INTO users (id, email, password_hash, firstname, lastname, role, is_active, created_at, updated_at) VALUES (
    'f6746c4a-54c6-439d-8535-326aaeace6ae', 'fedorov.y.f@edu.spbstu.ru', '5748524196ee0e61aaeeff25914d5dca6620e17642e10815bb63096e7a2f3891', 'Юрий', 'Федоров', 'teacher', True, '2025-10-20 09:00:00+00', '2025-10-20 09:00:00+00');

INSERT INTO users (id, email, password_hash, firstname, lastname, role, is_active, created_at, updated_at) VALUES (
    'd9e6bd53-c7d3-4b69-aa33-37f2b663eb80', 'morozov.b.m@edu.spbstu.ru', '08d0d6cb96f9bf938a1f0660ef1310ba2c33d768e5932a263bac50d9b9b9e615', 'Борис', 'Морозов', 'student', True, '2025-10-20 09:00:00+00', '2025-10-20 09:00:00+00');

-- Insert Rooms
INSERT INTO rooms (id, building_id, name, code, capacity, features) VALUES (
    '4a8638dd-0408-4193-b209-0c5b9db87838', 'db6d11ff-dbd1-4f07-85c6-764504eb73e5', 'Аудитория 314', '314', 57, '{"equipment": ["проектор", "компьютер"]}'::jsonb);

INSERT INTO rooms (id, building_id, name, code, capacity, features) VALUES (
    'aabe4eb0-7720-4673-9d5f-81176f1afc3e', 'db6d11ff-dbd1-4f07-85c6-764504eb73e5', 'Аудитория 215', '215', 53, '{"equipment": ["доска"]}'::jsonb);

INSERT INTO rooms (id, building_id, name, code, capacity, features) VALUES (
    'af79e3c4-50be-4847-994d-ed61b1c6eac1', 'db6d11ff-dbd1-4f07-85c6-764504eb73e5', 'Аудитория 183', '183', 52, '{"equipment": ["WiFi"]}'::jsonb);

INSERT INTO rooms (id, building_id, name, code, capacity, features) VALUES (
    '3d1468fd-4e9f-4bac-8208-d7065b3ed7a4', 'db6d11ff-dbd1-4f07-85c6-764504eb73e5', 'Аудитория 146', '146', 74, '{"equipment": ["компьютер"]}'::jsonb);

INSERT INTO rooms (id, building_id, name, code, capacity, features) VALUES (
    '415a7465-f68a-4735-9ebb-3784da03fac8', 'db6d11ff-dbd1-4f07-85c6-764504eb73e5', 'Аудитория 179', '179', 44, '{"equipment": ["компьютер", "WiFi"]}'::jsonb);

INSERT INTO rooms (id, building_id, name, code, capacity, features) VALUES (
    '28b167d4-fe16-41ab-b0f9-41fd649ea8fa', 'db6d11ff-dbd1-4f07-85c6-764504eb73e5', 'Аудитория 313', '313', 42, '{"equipment": ["проектор"]}'::jsonb);

INSERT INTO rooms (id, building_id, name, code, capacity, features) VALUES (
    'a4d35401-8212-4f1b-baab-74fd9cba15cb', 'db6d11ff-dbd1-4f07-85c6-764504eb73e5', 'Аудитория 235', '235', 100, '{"equipment": ["микрофон", "доска"]}'::jsonb);

INSERT INTO rooms (id, building_id, name, code, capacity, features) VALUES (
    '86bd94cd-20d7-45bc-afc4-3c18becc60dc', 'db6d11ff-dbd1-4f07-85c6-764504eb73e5', 'Аудитория 214', '214', 86, '{"equipment": ["WiFi"]}'::jsonb);

INSERT INTO rooms (id, building_id, name, code, capacity, features) VALUES (
    'd3cd52bb-8627-4707-b934-5f6f412e6c2c', 'db6d11ff-dbd1-4f07-85c6-764504eb73e5', 'Аудитория 138', '138', 20, '{"equipment": ["микрофон", "доска", "проектор"]}'::jsonb);

INSERT INTO rooms (id, building_id, name, code, capacity, features) VALUES (
    '51a13c5e-1147-4651-b845-44f844c534a3', 'db6d11ff-dbd1-4f07-85c6-764504eb73e5', 'Аудитория 140', '140', 60, '{"equipment": ["WiFi", "доска"]}'::jsonb);

INSERT INTO rooms (id, building_id, name, code, capacity, features) VALUES (
    '7188fea6-ff57-472b-977d-dd8f95185c3b', 'db6d11ff-dbd1-4f07-85c6-764504eb73e5', 'Аудитория 111', '111', 47, '{"equipment": ["проектор", "компьютер", "доска"]}'::jsonb);

INSERT INTO rooms (id, building_id, name, code, capacity, features) VALUES (
    'c3cce682-94aa-41e4-a8a0-fd140abc1e9e', 'db6d11ff-dbd1-4f07-85c6-764504eb73e5', 'Аудитория 260', '260', 94, '{"equipment": ["компьютер"]}'::jsonb);

-- Insert Schedules Import
INSERT INTO schedules_import (id, room_id, starts_at, ends_at, source, hash, metadata, updated_at) VALUES (
    '632b7197-e89e-41f6-9477-f34d3a01123a', '4a8638dd-0408-4193-b209-0c5b9db87838', '2025-10-20T12:00:00+00', '2025-10-20T13:00:00+00', 'Основы российской государственности', '5bd3acc6d3ea08e2f5ac17fdc96124bc', '{"subject": "Основы российской государственности", "lecturer": "Пылькин Александр Александрович"}'::jsonb, '2025-10-20 09:00:00+00');

INSERT INTO schedules_import (id, room_id, starts_at, ends_at, source, hash, metadata, updated_at) VALUES (
    '6022f82c-ac96-4d71-bc55-c51fda8a1f6e', 'aabe4eb0-7720-4673-9d5f-81176f1afc3e', '2025-10-20T12:00:00+00', '2025-10-20T14:00:00+00', 'Лекции', '5bd3acc6d3ea08e2f5ac17fdc96124bc', '{"subject": "Лекции", "lecturer": "Доктор Иванов"}'::jsonb, '2025-10-20 09:00:00+00');

INSERT INTO schedules_import (id, room_id, starts_at, ends_at, source, hash, metadata, updated_at) VALUES (
    'c4610a7e-e842-4340-a145-c4d9a5c06c0f', 'af79e3c4-50be-4847-994d-ed61b1c6eac1', '2025-10-20T09:30:00+00', '2025-10-20T12:30:00+00', 'Основы российской государственности', 'cdb061583044c14026a669848cae9afe', '{"subject": "Основы российской государственности", "lecturer": "Доктор Иванов"}'::jsonb, '2025-10-20 09:00:00+00');

INSERT INTO schedules_import (id, room_id, starts_at, ends_at, source, hash, metadata, updated_at) VALUES (
    '6807e37e-2f08-42e9-b6d4-7215e4f7bde2', '3d1468fd-4e9f-4bac-8208-d7065b3ed7a4', '2025-10-20T12:30:00+00', '2025-10-20T14:30:00+00', 'Лекции', '0c2a6ceda3ed46f113f5c8d8b371df47', '{"subject": "Лекции", "lecturer": "Пылькин Александр Александрович"}'::jsonb, '2025-10-20 09:00:00+00');

INSERT INTO schedules_import (id, room_id, starts_at, ends_at, source, hash, metadata, updated_at) VALUES (
    'a141ec14-e07e-445e-9a93-da4130cc0dc3', '415a7465-f68a-4735-9ebb-3784da03fac8', '2025-10-20T09:30:00+00', '2025-10-20T12:30:00+00', 'Основы российской государственности', 'cdb061583044c14026a669848cae9afe', '{"subject": "Основы российской государственности", "lecturer": "Доктор Петров"}'::jsonb, '2025-10-20 09:00:00+00');

INSERT INTO schedules_import (id, room_id, starts_at, ends_at, source, hash, metadata, updated_at) VALUES (
    'dae65204-5ded-4845-b1ab-d21d6d9882f2', '28b167d4-fe16-41ab-b0f9-41fd649ea8fa', '2025-10-20T09:30:00+00', '2025-10-20T10:30:00+00', 'Лекции', 'cdb061583044c14026a669848cae9afe', '{"subject": "Лекции", "lecturer": "Доктор Петров"}'::jsonb, '2025-10-20 09:00:00+00');

INSERT INTO schedules_import (id, room_id, starts_at, ends_at, source, hash, metadata, updated_at) VALUES (
    '9261f3a1-e517-4aa4-ad53-386b68de46bf', 'a4d35401-8212-4f1b-baab-74fd9cba15cb', '2025-10-20T11:00:00+00', '2025-10-20T13:00:00+00', 'Основы российской государственности', '9bbdd7d831a55ad4ef4972de045d0710', '{"subject": "Основы российской государственности", "lecturer": "Пылькин Александр Александрович"}'::jsonb, '2025-10-20 09:00:00+00');

INSERT INTO schedules_import (id, room_id, starts_at, ends_at, source, hash, metadata, updated_at) VALUES (
    '90572477-afad-4e80-9792-7fdfeaed2bcf', '86bd94cd-20d7-45bc-afc4-3c18becc60dc', '2025-10-20T09:00:00+00', '2025-10-20T11:00:00+00', 'Лекции', '11d2d8c7157367e39e53728ec94e9264', '{"subject": "Лекции", "lecturer": "Доктор Иванов"}'::jsonb, '2025-10-20 09:00:00+00');

INSERT INTO schedules_import (id, room_id, starts_at, ends_at, source, hash, metadata, updated_at) VALUES (
    'c26215b5-09c0-4660-8a82-3b5594e8c542', 'd3cd52bb-8627-4707-b934-5f6f412e6c2c', '2025-10-20T10:30:00+00', '2025-10-20T12:30:00+00', 'Основы российской государственности', 'd061c28600ee0656dbcab043e158fd2f', '{"subject": "Основы российской государственности", "lecturer": "Доктор Иванов"}'::jsonb, '2025-10-20 09:00:00+00');

INSERT INTO schedules_import (id, room_id, starts_at, ends_at, source, hash, metadata, updated_at) VALUES (
    '05d304af-1436-420f-a652-90bdcb95c434', '51a13c5e-1147-4651-b845-44f844c534a3', '2025-10-20T10:30:00+00', '2025-10-20T12:30:00+00', 'Лекции', 'd061c28600ee0656dbcab043e158fd2f', '{"subject": "Лекции", "lecturer": "Пылькин Александр Александрович"}'::jsonb, '2025-10-20 09:00:00+00');

INSERT INTO schedules_import (id, room_id, starts_at, ends_at, source, hash, metadata, updated_at) VALUES (
    '7e26d700-e5d4-4b19-9fde-6d694eab3c6e', '7188fea6-ff57-472b-977d-dd8f95185c3b', '2025-10-20T11:30:00+00', '2025-10-20T12:30:00+00', 'Основы российской государственности', 'b991a1ba392b3e5ccc2d95eae4add248', '{"subject": "Основы российской государственности", "lecturer": "Доктор Петров"}'::jsonb, '2025-10-20 09:00:00+00');

INSERT INTO schedules_import (id, room_id, starts_at, ends_at, source, hash, metadata, updated_at) VALUES (
    '06a42dcb-bdf6-4632-b8d3-752aa94a4fba', 'c3cce682-94aa-41e4-a8a0-fd140abc1e9e', '2025-10-20T10:30:00+00', '2025-10-20T11:30:00+00', 'Лекции', 'd061c28600ee0656dbcab043e158fd2f', '{"subject": "Лекции", "lecturer": "Доктор Петров"}'::jsonb, '2025-10-20 09:00:00+00');

-- Insert Bookings
INSERT INTO bookings (id, user_id, room_id, starts_at, ends_at, status, created_at, updated_at) VALUES (
    '6c6fa419-0926-4b45-86c0-423d27f2371e', '7f441a7b-3615-4aba-a70f-77eeed31957a', '4a8638dd-0408-4193-b209-0c5b9db87838', '2025-10-24T17:00:00+00', '2025-10-24T19:00:00+00', 'cancelled', '2025-10-20 09:00:00+00', '2025-10-20 09:00:00+00');

INSERT INTO bookings (id, user_id, room_id, starts_at, ends_at, status, created_at, updated_at) VALUES (
    '3838d383-1267-497a-add6-809109ccb735', '5f3b4bcd-db2e-4c34-854b-477fc79dad7a', 'aabe4eb0-7720-4673-9d5f-81176f1afc3e', '2025-10-22T16:00:00+00', '2025-10-22T18:00:00+00', 'rejected', '2025-10-20 09:00:00+00', '2025-10-20 09:00:00+00');

INSERT INTO bookings (id, user_id, room_id, starts_at, ends_at, status, created_at, updated_at) VALUES (
    '877bcfc1-1173-4882-904d-8854500d0894', '79d934c0-67cf-4e85-9229-a88d52e9afe7', 'af79e3c4-50be-4847-994d-ed61b1c6eac1', '2025-10-27T15:00:00+00', '2025-10-27T17:00:00+00', 'rejected', '2025-10-20 09:00:00+00', '2025-10-20 09:00:00+00');

INSERT INTO bookings (id, user_id, room_id, starts_at, ends_at, status, created_at, updated_at) VALUES (
    '60444c67-6f6f-4e8d-b136-9da6e15b1fb1', '23106058-2372-4dcd-b761-fb2eee028717', '3d1468fd-4e9f-4bac-8208-d7065b3ed7a4', '2025-10-26T17:00:00+00', '2025-10-26T19:00:00+00', 'pending', '2025-10-20 09:00:00+00', '2025-10-20 09:00:00+00');

INSERT INTO bookings (id, user_id, room_id, starts_at, ends_at, status, created_at, updated_at) VALUES (
    'a113b55c-8a27-43a2-878b-b58728c85630', '191f4d1e-bbbe-4ca5-b1e2-b285ebe07f38', '415a7465-f68a-4735-9ebb-3784da03fac8', '2025-10-21T14:00:00+00', '2025-10-21T16:00:00+00', 'cancelled', '2025-10-20 09:00:00+00', '2025-10-20 09:00:00+00');

INSERT INTO bookings (id, user_id, room_id, starts_at, ends_at, status, created_at, updated_at) VALUES (
    '8dc4f8c1-7fc9-44f5-bcf6-bd601055615a', '38e278e3-7dc2-4d00-87dc-d6b822745ec7', '28b167d4-fe16-41ab-b0f9-41fd649ea8fa', '2025-10-24T15:00:00+00', '2025-10-24T17:00:00+00', 'pending', '2025-10-20 09:00:00+00', '2025-10-20 09:00:00+00');

INSERT INTO bookings (id, user_id, room_id, starts_at, ends_at, status, created_at, updated_at) VALUES (
    'c5655b78-baf2-4567-8f0c-46bb4f703881', '1fc13a20-3652-463d-b587-259dbdf2ab28', 'a4d35401-8212-4f1b-baab-74fd9cba15cb', '2025-10-23T17:00:00+00', '2025-10-23T19:00:00+00', 'cancelled', '2025-10-20 09:00:00+00', '2025-10-20 09:00:00+00');

INSERT INTO bookings (id, user_id, room_id, starts_at, ends_at, status, created_at, updated_at) VALUES (
    'e011ab06-f59e-49b9-9eac-46e915fda0fe', '88b7f450-3d4e-4d3f-b7dd-2b78919dc548', '86bd94cd-20d7-45bc-afc4-3c18becc60dc', '2025-10-23T17:00:00+00', '2025-10-23T19:00:00+00', 'confirmed', '2025-10-20 09:00:00+00', '2025-10-20 09:00:00+00');

INSERT INTO bookings (id, user_id, room_id, starts_at, ends_at, status, created_at, updated_at) VALUES (
    'c91b9a1f-289b-4c39-93c0-cff0b8a4b011', 'd32a9a33-00b3-4e6a-8059-eb48c32a43b7', 'd3cd52bb-8627-4707-b934-5f6f412e6c2c', '2025-10-24T16:00:00+00', '2025-10-24T18:00:00+00', 'confirmed', '2025-10-20 09:00:00+00', '2025-10-20 09:00:00+00');

INSERT INTO bookings (id, user_id, room_id, starts_at, ends_at, status, created_at, updated_at) VALUES (
    '88e628c1-9f44-4695-85d2-8664230aaac9', 'c9c7751d-7ef9-4787-806f-8402ebeb9c50', '51a13c5e-1147-4651-b845-44f844c534a3', '2025-10-22T16:00:00+00', '2025-10-22T18:00:00+00', 'pending', '2025-10-20 09:00:00+00', '2025-10-20 09:00:00+00');

INSERT INTO bookings (id, user_id, room_id, starts_at, ends_at, status, created_at, updated_at) VALUES (
    'f84ba2c3-23f5-443d-bb26-f382f97dd7b8', 'f6746c4a-54c6-439d-8535-326aaeace6ae', '7188fea6-ff57-472b-977d-dd8f95185c3b', '2025-10-26T16:00:00+00', '2025-10-26T18:00:00+00', 'cancelled', '2025-10-20 09:00:00+00', '2025-10-20 09:00:00+00');

INSERT INTO bookings (id, user_id, room_id, starts_at, ends_at, status, created_at, updated_at) VALUES (
    '271c27d8-8ed3-4a0a-8919-8b60ff857a6b', 'd9e6bd53-c7d3-4b69-aa33-37f2b663eb80', 'c3cce682-94aa-41e4-a8a0-fd140abc1e9e', '2025-10-21T16:00:00+00', '2025-10-21T18:00:00+00', 'pending', '2025-10-20 09:00:00+00', '2025-10-20 09:00:00+00');
