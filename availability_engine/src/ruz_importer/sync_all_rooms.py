#!/usr/bin/env python3
import json
import urllib.request
import subprocess
import time

DB_USER = "postgres"
DB_NAME = "booking_mvp_db"
RUZ_BASE = "https://ruz.spbstu.ru/api/v1/ruz"

def run_sql(sql):
    cmd = ["docker", "exec", "booking-postgres", "psql", "-U", DB_USER, "-d", DB_NAME, "-t", "-c", sql]
    res = subprocess.run(cmd, capture_output=True, text=True)
    return res.returncode == 0

def fetch_json(url):
    try:
        with urllib.request.urlopen(url, timeout=15) as response:
            return json.loads(response.read().decode())
    except Exception as e:
        print(f"  [error] {url}: {e}")
        return None

def main():
    print("[sync] === Full SPbSTU rooms sync ===")
    
    # 1. Добавляем колонки, если их нет
    run_sql("ALTER TABLE rooms ADD COLUMN IF NOT EXISTS ruz_id INTEGER;")
    run_sql("ALTER TABLE rooms ADD COLUMN IF NOT EXISTS building_ruz_id INTEGER;")
    run_sql("ALTER TABLE rooms ADD COLUMN IF NOT EXISTS ruz_name VARCHAR(255);")
    run_sql("CREATE UNIQUE INDEX IF NOT EXISTS idx_rooms_ruz_id ON rooms(ruz_id);")
    
    # 2. Получаем все корпуса
    print("[sync] Fetching all buildings...")
    resp = fetch_json(f"{RUZ_BASE}/buildings")
    if not resp or 'buildings' not in resp:
        print("Failed to fetch buildings")
        return
    buildings = resp['buildings']
    print(f"[sync] Found {len(buildings)} buildings")

    total_synced = 0
    total_inserted = 0

    for b in buildings:
        bid = b['id']
        bname = b.get('name', 'Unknown')
        print(f"[sync] Building {bid}: {bname}")
        
        rooms_data = fetch_json(f"{RUZ_BASE}/buildings/{bid}/rooms")
        if not rooms_data: continue
        rooms = rooms_data if isinstance(rooms_data, list) else rooms_data.get('rooms', [])
        print(f"  Found {len(rooms)} rooms")

        for r in rooms:
            rid = r['id']
            rname = r.get('name', '').replace("'", "''")
            
            # Пытаемся обновить существующую по совпадению имени/кода
            # Если не нашли — вставляем новую
            # Используем ON CONFLICT по ruz_id для предотвращения дублей
            sql = f"""
            INSERT INTO rooms (name, code, ruz_id, building_ruz_id, ruz_name, building_id)
            VALUES ('{rname}', '{rname}', {rid}, {bid}, '{rname}',NULL)
            ON CONFLICT (ruz_id) DO UPDATE 
            SET building_ruz_id = EXCLUDED.building_ruz_id, 
                ruz_name = EXCLUDED.ruz_name;
            """
            if run_sql(sql):
                total_synced += 1
        
        time.sleep(0.1) # Rate limiting

    print(f"\n[sync] === Sync complete ===")
    print(f"[sync] Total rooms in DB now: {total_synced}")

if __name__ == "__main__":
    main()
