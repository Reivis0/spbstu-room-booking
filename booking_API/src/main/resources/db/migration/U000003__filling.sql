DELETE FROM bookings
WHERE user_id IN (
    '7f441a7b-3615-4aba-a70f-77eeed31957a',
    '5f3b4bcd-db2e-4c34-854b-477fc79dad7a',
    '79d934c0-67cf-4e85-9229-a88d52e9afe7',
    '23106058-2372-4dcd-b761-fb2eee028717',
    '191f4d1e-bbbe-4ca5-b1e2-b285ebe07f38',
    '38e278e3-7dc2-4d00-87dc-d6b822745ec7',
    '1fc13a20-3652-463d-b587-259dbdf2ab28',
    '88b7f450-3d4e-4d3f-b7dd-2b78919dc548',
    'd32a9a33-00b3-4e6a-8059-eb48c32a43b7',
    'c9c7751d-7ef9-4787-806f-8402ebeb9c50',
    'f6746c4a-54c6-439d-8535-326aaeace6ae',
    'd9e6bd53-c7d3-4b69-aa33-37f2b663eb80'
);

DELETE FROM schedules_import
WHERE room_id IN (
    '4a8638dd-0408-4193-b209-0c5b9db87838',
    'aabe4eb0-7720-4673-9d5f-81176f1afc3e',
    'af79e3c4-50be-4847-994d-ed61b1c6eac1',
    '3d1468fd-4e9f-4bac-8208-d7065b3ed7a4',
    '415a7465-f68a-4735-9ebb-3784da03fac8',
    '28b167d4-fe16-41ab-b0f9-41fd649ea8fa',
    'a4d35401-8212-4f1b-baab-74fd9cba15cb',
    '86bd94cd-20d7-45bc-afc4-3c18becc60dc',
    'd3cd52bb-8627-4707-b934-5f6f412e6c2c',
    '51a13c5e-1147-4651-b845-44f844c534a3',
    '7188fea6-ff57-472b-977d-dd8f95185c3b',
    'c3cce682-94aa-41e4-a8a0-fd140abc1e9e'
);

DELETE FROM rooms
WHERE id IN (
    '4a8638dd-0408-4193-b209-0c5b9db87838',
    'aabe4eb0-7720-4673-9d5f-81176f1afc3e',
    'af79e3c4-50be-4847-994d-ed61b1c6eac1',
    '3d1468fd-4e9f-4bac-8208-d7065b3ed7a4',
    '415a7465-f68a-4735-9ebb-3784da03fac8',
    '28b167d4-fe16-41ab-b0f9-41fd649ea8fa',
    'a4d35401-8212-4f1b-baab-74fd9cba15cb',
    '86bd94cd-20d7-45bc-afc4-3c18becc60dc',
    'd3cd52bb-8627-4707-b934-5f6f412e6c2c',
    '51a13c5e-1147-4651-b845-44f844c534a3',
    '7188fea6-ff57-472b-977d-dd8f95185c3b',
    'c3cce682-94aa-41e4-a8a0-fd140abc1e9e'
);

DELETE FROM users
WHERE email IN (
    'ivanov.a.a@edu.spbstu.ru',
    'petrov.p.p@edu.spbstu.ru',
    'sidorov.a.s@edu.spbstu.ru',
    'kozlov.m.k@edu.spbstu.ru',
    'smirnov.s.s@edu.spbstu.ru',
    'popov.d.p@edu.spbstu.ru',
    'vasilev.a.v@edu.spbstu.ru',
    'sokolov.v.s@edu.spbstu.ru',
    'mihaylov.n.m@edu.spbstu.ru',
    'novikov.a.n@edu.spbstu.ru',
    'fedorov.y.f@edu.spbstu.ru',
    'morozov.b.m@edu.spbstu.ru'
);

DELETE FROM buildings
WHERE id = 'db6d11ff-dbd1-4f07-85c6-764504eb73e5';
