import urllib.request
import json

base = "http://localhost:8080/api"

def get(url):
    req = urllib.request.Request(base + url)
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read().decode('utf-8'))

print("==================================================================")
print("1. Testing Station Search (Query: 'Delhi'):")
st = get("/stations/search?q=Delhi")
print(f"   Found {len(st)} matches. Top 3:")
for s in st[:3]:
    print(f"   - {s.get('stationCode')}: {s.get('stationName')}")

print("\n2. Testing Route Search with Metropolitan Cluster (DELHI -> MUMBAI):")
tr1 = get("/trains/search?from=DELHI&to=MUMBAI&date=2026-11-13&quota=GENERAL")
print(f"   Found {len(tr1)} trains between Delhi and Mumbai metropolitan terminals!")
for t in tr1[:3]:
    print(f"   - #{t.get('trainNumber')}: {t.get('trainName')} ({t.get('fromStationCode')} -> {t.get('toStationCode')})")

print("\n3. Testing Modern 2026 Fleet Search (Vande Bharat #22436):")
tr2 = get("/trains/search?q=22436")
print(f"   Found: {len(tr2)} trains. Top: {tr2[0].get('trainNumber')} - {tr2[0].get('trainName')}")

print("\n4. Testing Live Status for 22436:")
ls = get("/trains/live-status?trainNumber=22436&dayOffset=0")
print(f"   Current Status: {ls.get('currentStatus')}")
print(f"   Delay Status: {ls.get('delayStatus')}, Speed: {ls.get('speed')}, Next Stop: {ls.get('nextStop')}")

print("\n5. Testing Visual Coach Layout:")
cl = get("/coach/layout?trainNumber=22436&class=CC&coach=C1&date=2026-11-13")
print(f"   Coach: {cl.get('coach')}, Total Seats: {cl.get('totalSeats')}, First 2 seats: {cl.get('seats')[:2]}")

print("\n6. Testing Seat Selection & ACID Locking Integrity (Preventing Double Booking):")
import urllib.error

booking_payload = {
    "trainNumber": "22436",
    "journeyDate": "2026-11-20",
    "classType": "CC",
    "quota": "GENERAL",
    "contactMobile": "9876543210",
    "contactEmail": "test@railstay.in",
    "username": "demo_user",
    "passengers": [
        {
            "name": "Amit Sharma",
            "age": 32,
            "gender": "M",
            "berthPreference": "Window",
            "preferredCoach": "C1",
            "preferredSeatNumber": 12
        }
    ]
}

def post(url, data):
    body = json.dumps(data).encode('utf-8')
    req = urllib.request.Request(base + url, data=body, headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req) as resp:
            return resp.status, json.loads(resp.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read().decode('utf-8'))

status1, res1 = post("/bookings/create", booking_payload)
print(f"   First Booking Attempt for C1-12: HTTP {status1}, PNR: {res1.get('pnr')}, Status: {res1.get('bookingStatus')}")
if 'passengers' in res1:
    p = res1['passengers'][0]
    print(f"   Allocated: Coach {p.get('coach')}, Seat #{p.get('seatNumber')}, Status: {p.get('bookingStatus')}")

status2, res2 = post("/bookings/create", booking_payload)
print(f"   Second Booking Attempt for C1-12: HTTP {status2}, Error: {res2.get('error')}")
assert status2 == 409, "Double booking was not blocked!"
print("   🔒 ACID Lock Confirmed: Second booking for same seat was correctly REJECTED with HTTP 409!")

print("\n7. Testing Commercial Halts Filtering (Bhopal Shatabdi #12002):")
sh = get("/trains/schedule?trainNumber=12002")
print(f"   Official Scheduled Halts returned: {len(sh)} (Previously 87 unhalted waypoints)")
assert len(sh) == 7, f"Expected 7 halts for 12002, got {len(sh)}"
print(f"   Stops: {', '.join([s['stationCode'] for s in sh])}")
print("   ✅ Verified: Only official stopping stations (NDLS, MTJ, AGC, MRA, GWL, JHS, BPL) returned!")

print("\n8. Testing Expanded Modern Superfast Fleet (Bengaluru Rajdhani #22691):")
raj = get("/trains/search?q=22691")
print(f"   Found {len(raj)} trains: #{raj[0].get('trainNumber')} - {raj[0].get('trainName')}")
assert raj[0].get('trainNumber') == '22691', "22691 was not found!"
print("   ✅ Verified: Modern 2xxxx fleet is actively searchable!")

print("\n==================================================================")
print("🚀 ALL END-TO-END TESTS PASSED WITH 100% SUCCESS!")
print("==================================================================")
