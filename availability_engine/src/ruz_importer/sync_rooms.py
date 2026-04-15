#!/usr/bin/env python3
import json
import sys
import urllib.request
import subprocess
import os

DB_USER = "postgres"
DB_NAME = "booking_mvp_db"
RUZ_BASE = "https://ruz.spbstu.ru/api/v1/ruz"

def run_sql(sql):
    cmd = ["docker", "exec", "booking-postgres", "psql", "-U", DB_USER, "-d", DB_NAME, "-t", "-c", sql]
    res = subprocess.run(cmd, capture_output=True, text=True)
    if res.returncode != 0:
        print(f"SQL Error: {res.stderr}")
        return None
    return res.stdout.strip()

def fetch_json(url):
    try:
        with urllib.request.urlopen(url, timeout=15) as response:
            return json.loads(response.read().decode())
    except Exception as e:
        print(f"Fetch Error {url}: {e}")
        return None

def main():
    print("[sync] Fetching all buildings from RUZ API...")
    resp = fetch_json(f"{RUZ_BASE}/buildings")
    if not resp or 'buildings' not in resp:
        print("Failed to fetch buildings")
        return
    buildings = resp['buildings']

    synced_count = 0
    total_rooms_in_db = run_sql("SELECT count(*) FROM rooms")
    print(f"[sync] Total rooms in our DB: {total_rooms_in_db}")

    for b in buildings:
        bid = b['id']
        bname = b.get('name', '')
        print(f"[sync] Processing building {bid}: {bname}")
        
        rooms_data = fetch_json(f"{RUZ_BASE}/buildings/{bid}/rooms")
        if not rooms_data:
            continue
            
        rooms = rooms_data if isinstance(rooms_data, list) else rooms_data.get('rooms', [])
        
        for room in rooms:
            rid = room['id']
            rname = room.get('name', '').replace("'", "''")
            
            # Обновляем БД: матчинг по code (номер комнаты) или по имени
            sql = f"UPDATE rooms SET ruz_id = {rid}, building_ruz_id = {bid} WHERE (code = '{rname}' OR name = '{rname}' OR name = 'Аудитория {rname}') AND ruz_id IS NULL;"
            run_sql(sql)

    final_synced = run_sql("SELECT count(*) FROM rooms WHERE ruz_id IS NOT NULL")
    print(f"\n[sync] Done!")
    print(f"[sync] Rooms successfully matched and synced: {final_synced} / {total_rooms_in_db}")

if __name__ == "__main__":
    main()
