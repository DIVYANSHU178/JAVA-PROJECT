import urllib.request
import json
import os
import sys

def main():
    print("==================================================================")
    print("🚆 Fetching Official Indian Railways Dataset (DataMeet)")
    print("==================================================================")
    
    os.makedirs("data", exist_ok=True)
    
    stations_url = "https://raw.githubusercontent.com/datameet/railways/master/stations.json"
    trains_url = "https://raw.githubusercontent.com/datameet/railways/master/trains.json"
    schedules_url = "https://raw.githubusercontent.com/datameet/railways/master/schedules.json"
    
    # 1. Download stations
    stations_file = "data/stations.json"
    if not os.path.exists(stations_file):
        print("Downloading stations.json (~8,990 stations)...")
        urllib.request.urlretrieve(stations_url, stations_file)
        print("Downloaded stations.json!")
    else:
        print("stations.json already cached.")
        
    # 2. Download trains
    trains_file = "data/trains.json"
    if not os.path.exists(trains_file):
        print("Downloading trains.json (~5,208 trains)...")
        urllib.request.urlretrieve(trains_url, trains_file)
        print("Downloaded trains.json!")
    else:
        print("trains.json already cached.")

    # 3. Download schedules
    schedules_file = "data/schedules.json"
    if not os.path.exists(schedules_file):
        print("Downloading schedules.json (~417,000 stops)...")
        urllib.request.urlretrieve(schedules_url, schedules_file)
        print("Downloaded schedules.json!")
    else:
        print("schedules.json already cached.")

    print("\nParsing datasets and preparing optimized PostgreSQL migration script...")
    with open(stations_file, 'r', encoding='utf-8') as f:
        stations_data = json.load(f)
        
    with open(trains_file, 'r', encoding='utf-8') as f:
        trains_data = json.load(f)

    with open(schedules_file, 'r', encoding='utf-8') as f:
        schedules_data = json.load(f)

    print(f"Loaded {len(stations_data['features'])} stations, {len(trains_data['features'])} trains, {len(schedules_data)} schedule entries.")

    sql_file = "data/schema_and_data.sql"
    with open(sql_file, 'w', encoding='utf-8') as sql:
        sql.write("""-- ============================================================================
-- RailStay IRCTC-Grade Database Schema & Real Indian Railways Dataset
-- ============================================================================

DROP TABLE IF EXISTS cancellations_refunds CASCADE;
DROP TABLE IF EXISTS booking_passengers CASCADE;
DROP TABLE IF EXISTS bookings CASCADE;
DROP TABLE IF EXISTS train_route_stops CASCADE;
DROP TABLE IF EXISTS trains CASCADE;
DROP TABLE IF EXISTS stations CASCADE;

CREATE TABLE stations (
    station_code VARCHAR(50) PRIMARY KEY,
    station_name VARCHAR(255) NOT NULL,
    state VARCHAR(150),
    zone VARCHAR(50),
    latitude NUMERIC(10, 7),
    longitude NUMERIC(10, 7)
);

CREATE TABLE trains (
    train_number VARCHAR(50) PRIMARY KEY,
    train_name VARCHAR(255) NOT NULL,
    train_type VARCHAR(100),
    zone VARCHAR(50),
    from_station_code VARCHAR(50),
    from_station_name VARCHAR(255),
    to_station_code VARCHAR(50),
    to_station_name VARCHAR(255),
    departure_time VARCHAR(50),
    arrival_time VARCHAR(50),
    duration_h INT DEFAULT 0,
    duration_m INT DEFAULT 0,
    distance_km INT DEFAULT 0,
    has_1a BOOLEAN DEFAULT TRUE,
    has_2a BOOLEAN DEFAULT TRUE,
    has_3a BOOLEAN DEFAULT TRUE,
    has_sl BOOLEAN DEFAULT TRUE,
    has_cc BOOLEAN DEFAULT FALSE,
    fare_1a NUMERIC(10, 2) DEFAULT 2800.00,
    fare_2a NUMERIC(10, 2) DEFAULT 1800.00,
    fare_3a NUMERIC(10, 2) DEFAULT 1200.00,
    fare_sl NUMERIC(10, 2) DEFAULT 550.00,
    fare_cc NUMERIC(10, 2) DEFAULT 850.00
);

CREATE TABLE train_route_stops (
    id SERIAL PRIMARY KEY,
    train_number VARCHAR(50) REFERENCES trains(train_number) ON DELETE CASCADE,
    station_code VARCHAR(50),
    station_name VARCHAR(255),
    arrival_time VARCHAR(50),
    departure_time VARCHAR(50),
    stop_day INT DEFAULT 1,
    stop_sequence INT DEFAULT 1
);

CREATE TABLE bookings (
    pnr VARCHAR(50) PRIMARY KEY,
    train_number VARCHAR(50) REFERENCES trains(train_number),
    train_name VARCHAR(255),
    journey_date DATE NOT NULL,
    from_station_code VARCHAR(50),
    from_station_name VARCHAR(255),
    to_station_code VARCHAR(50),
    to_station_name VARCHAR(255),
    class_type VARCHAR(20) NOT NULL,
    quota VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    total_fare NUMERIC(10, 2) NOT NULL,
    booking_status VARCHAR(50) NOT NULL DEFAULT 'CONFIRMED',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE booking_passengers (
    passenger_id SERIAL PRIMARY KEY,
    pnr VARCHAR(50) REFERENCES bookings(pnr) ON DELETE CASCADE,
    passenger_name VARCHAR(255) NOT NULL,
    age INT NOT NULL,
    gender VARCHAR(20) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    coach VARCHAR(20),
    seat_number INT,
    berth_type VARCHAR(50),
    booking_status VARCHAR(50) NOT NULL, -- CNF, RAC, WL
    current_status VARCHAR(50) NOT NULL, -- CNF, RAC, WL
    waitlist_number INT DEFAULT 0
);

CREATE TABLE cancellations_refunds (
    cancellation_id SERIAL PRIMARY KEY,
    pnr VARCHAR(50),
    original_fare NUMERIC(10, 2) NOT NULL,
    deduction NUMERIC(10, 2) NOT NULL,
    refund_amount NUMERIC(10, 2) NOT NULL,
    cancelled_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

BEGIN;
""")

        # 1. Insert Stations
        print("Writing station inserts...")
        seen_stations = set()
        station_rows = []
        for feat in stations_data['features']:
            props = feat.get('properties', {})
            geom = feat.get('geometry', {})
            code = (props.get('code') or '').strip().upper()
            name = (props.get('name') or '').strip()
            state = (props.get('state') or '').strip()
            zone = (props.get('zone') or '').strip()
            coords = geom.get('coordinates', [0, 0]) if geom else [0, 0]
            lon = coords[0] if len(coords) > 0 else 0
            lat = coords[1] if len(coords) > 1 else 0
            
            if code and code not in seen_stations:
                seen_stations.add(code)
                name_esc = name.replace("'", "''")
                state_esc = state.replace("'", "''")
                zone_esc = zone.replace("'", "''")
                station_rows.append(f"('{code}', '{name_esc}', '{state_esc}', '{zone_esc}', {lat}, {lon})")

        # Write in batches of 500
        for i in range(0, len(station_rows), 500):
            batch = station_rows[i:i+500]
            sql.write(f"INSERT INTO stations (station_code, station_name, state, zone, latitude, longitude) VALUES\n" + ",\n".join(batch) + "\nON CONFLICT DO NOTHING;\n")

        # 2. Insert Trains
        print("Writing train inserts...")
        seen_trains = set()
        train_rows = []
        for feat in trains_data['features']:
            props = feat.get('properties', {})
            number = (str(props.get('number') or '')).strip().upper()
            name = (props.get('name') or '').strip()
            ttype = (props.get('type') or 'Express').strip()
            zone = (props.get('zone') or '').strip()
            from_code = (props.get('from_station_code') or '').strip().upper()
            from_name = (props.get('from_station_name') or '').strip()
            to_code = (props.get('to_station_code') or '').strip().upper()
            to_name = (props.get('to_station_name') or '').strip()
            dep = (props.get('departure') or '00:00:00').strip()
            arr = (props.get('arrival') or '00:00:00').strip()
            dur_h = props.get('duration_h') or 0
            dur_m = props.get('duration_m') or 0
            dist = props.get('distance') or 500
            
            fare_sl = max(180, int(dist * 0.45))
            fare_3a = max(550, int(dist * 1.15))
            fare_2a = max(850, int(dist * 1.65))
            fare_1a = max(1450, int(dist * 2.60))
            fare_cc = max(420, int(dist * 0.90))

            if number and number not in seen_trains:
                seen_trains.add(number)
                name_esc = name.replace("'", "''")
                ttype_esc = ttype.replace("'", "''")
                from_name_esc = from_name.replace("'", "''")
                to_name_esc = to_name.replace("'", "''")
                has_cc = 'TRUE' if 'SHATABDI' in name_esc or 'VANDE' in name_esc else 'FALSE'
                has_1a = 'TRUE' if 'RAJDHANI' in name_esc or 'DURONTO' in name_esc else 'FALSE'
                train_rows.append(f"('{number}', '{name_esc}', '{ttype_esc}', '{zone}', '{from_code}', '{from_name_esc}', '{to_code}', '{to_name_esc}', '{dep}', '{arr}', {dur_h}, {dur_m}, {dist}, {has_1a}, TRUE, TRUE, TRUE, {has_cc}, {fare_1a}, {fare_2a}, {fare_3a}, {fare_sl}, {fare_cc})")

        for i in range(0, len(train_rows), 500):
            batch = train_rows[i:i+500]
            sql.write(f"INSERT INTO trains (train_number, train_name, train_type, zone, from_station_code, from_station_name, to_station_code, to_station_name, departure_time, arrival_time, duration_h, duration_m, distance_km, has_1a, has_2a, has_3a, has_sl, has_cc, fare_1a, fare_2a, fare_3a, fare_sl, fare_cc) VALUES\n" + ",\n".join(batch) + "\nON CONFLICT DO NOTHING;\n")

        # 3. Insert Schedules
        print("Writing schedules...")
        train_schedules_map = {}
        for entry in schedules_data:
            tnum = str(entry.get('train_number') or '').strip().upper()
            if tnum in seen_trains:
                if tnum not in train_schedules_map:
                    train_schedules_map[tnum] = []
                train_schedules_map[tnum].append(entry)

        stop_rows = []
        for tnum, stops in train_schedules_map.items():
            for seq, s in enumerate(stops, start=1):
                scode = (s.get('station_code') or '').strip().upper()
                sname = (s.get('station_name') or '').strip().replace("'", "''")
                arr = str(s.get('arrival') or '00:00:00')
                dep = str(s.get('departure') or '00:00:00')
                day = s.get('day') or 1
                if arr == 'None': arr = dep
                if dep == 'None': dep = arr
                stop_rows.append(f"('{tnum}', '{scode}', '{sname}', '{arr}', '{dep}', {day}, {seq})")

        for i in range(0, len(stop_rows), 1000):
            batch = stop_rows[i:i+1000]
            sql.write(f"INSERT INTO train_route_stops (train_number, station_code, station_name, arrival_time, departure_time, stop_day, stop_sequence) VALUES\n" + ",\n".join(batch) + ";\n")

        sql.write("""
COMMIT;

-- Fast B-Tree Indexes after bulk load
CREATE INDEX idx_stations_name ON stations(LOWER(station_name));
CREATE INDEX idx_stations_code ON stations(station_code);
CREATE INDEX idx_trains_from_to ON trains(from_station_code, to_station_code);
CREATE INDEX idx_trains_name ON trains(LOWER(train_name));
CREATE INDEX idx_route_train_number ON train_route_stops(train_number);
CREATE INDEX idx_route_station_code ON train_route_stops(station_code);
CREATE INDEX idx_bookings_pnr ON bookings(pnr);
CREATE INDEX idx_bookings_train_date ON bookings(train_number, journey_date);
CREATE INDEX idx_passengers_pnr ON booking_passengers(pnr);
CREATE INDEX idx_passengers_phone ON booking_passengers(phone);
""")

    print(f"✅ Generated ultra-optimized migration: {len(seen_stations)} stations, {len(seen_trains)} trains, {len(stop_rows)} stops.")

if __name__ == '__main__':
    main()
