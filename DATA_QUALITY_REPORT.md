# Отчет о качестве данных (Data Quality Audit)

## 1. Анализ "Пустых" комнат (Empty Imports)
* **SQL Запрос:** 
  ```sql
  SELECT university_id, COUNT(*) as empty_rooms_count 
  FROM rooms r 
  WHERE NOT EXISTS (SELECT 1 FROM schedules_import si WHERE si.ruz_room_id = r.ruz_id AND si.university_id = r.university_id)
  GROUP BY university_id;
  ```
* **Результат:** 0 пустых комнат.
* **Вывод:** Импортер работает корректно. Все комнаты, попавшие в таблицу `rooms`, имеют актуальное расписание.

## 2. Анализ дубликатов и коллизий времени
* **SQL Запрос:**
  ```sql
  SELECT s1.university_id, s1.room_name, s1.starts_at, s1.ends_at, s1.subject as sub1, s2.subject as sub2
  FROM schedules_import s1
  JOIN schedules_import s2 ON s1.ruz_room_id = s2.ruz_room_id 
      AND s1.university_id = s2.university_id
      AND s1.id < s2.id
      AND s1.starts_at < s2.ends_at AND s2.starts_at < s1.ends_at;
  ```
* **Результат:** Обнаружены пересечения в СПбГУ (аудитория "10-вш").
* **Вывод:** Это не баг дедупликации (записи имеют разные хеши), а специфика вузовского расписания (потоковые лекции или разные группы в одном зале). Алгоритм дедупликации работает верно, предотвращая вставку идентичных записей.

## 3. Аномалии и целостность
* **Результаты:** 
  - Найдено 22 записи с длительностью ~9 часов (в основном "Военная подготовка" в СПбПУ). Это является корректными данными для учебного процесса.
  - Битых ссылок (rooms без buildings) не обнаружено.
  - Пустых имен зданий не обнаружено.

## 4. План действий (Action Items)
1. **Business Logic:** При расчете доступности в `Availability Engine` необходимо объединять перекрывающиеся интервалы в один блок "Занято", чтобы корректно отображать пользователю свободные окна.
2. **Data Retention:** Добавить в `ruz-importer` шаг очистки старых записей (`DELETE FROM schedules_import WHERE ends_at < now() - interval '1 day'`), чтобы база не раздувалась со временем.
