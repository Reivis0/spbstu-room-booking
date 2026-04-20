-- Migration: V000009__universities
-- Description: Update university metadata
-- Author: team

-- Updates for existing universities (using values from V5 foundation)
UPDATE universities SET 
    ruz_api_url = 'https://ruz.spbstu.ru/api/v1/ruz',
    data_format = 'json'
WHERE id = 'spbptu';

UPDATE universities SET 
    ruz_api_url = 'https://timetable.spbu.ru/api/v1',
    data_format = 'json'
WHERE id = 'spbgu';

UPDATE universities SET 
    ruz_api_url = 'https://digital.etu.ru/api/mobile/schedule',
    data_format = 'json'
WHERE id = 'leti';
