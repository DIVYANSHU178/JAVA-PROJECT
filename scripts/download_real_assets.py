import os
import urllib.request
import re

def download_file(url, target_path):
    os.makedirs(os.path.dirname(target_path), exist_ok=True)
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'})
        with urllib.request.urlopen(req, timeout=15) as res:
            data = res.read()
            with open(target_path, 'wb') as f:
                f.write(data)
            print(f"✅ Downloaded {target_path} ({len(data)} bytes)")
            return True
    except Exception as e:
        print(f"❌ Failed to download {url}: {e}")
        return False

def main():
    print("📥 Sourcing Real High-Resolution Photographic Assets for IRCTC Clone...")
    
    # 1. Real Vande Bharat Express High-Definition Photography
    download_file(
        'https://upload.wikimedia.org/wikipedia/commons/f/f3/Vande_bharat_Express.jpg',
        'web/assets/vande-bharat-real.jpg'
    )
    
    # 2. Real Taj Mahal (Golden Triangle Heritage)
    download_file(
        'https://images.unsplash.com/photo-1564507592333-c60657eea523?auto=format&fit=crop&w=1200&q=80',
        'web/assets/golden-triangle-real.jpg'
    )
    
    # 3. Real Kashmir (Dal Lake & Mountains)
    download_file(
        'https://images.unsplash.com/photo-1598091383021-15ddea10925d?auto=format&fit=crop&w=1200&q=80',
        'web/assets/kashmir-real.jpg'
    )
    
    # 4. Real Tirupati Balaji Temple (Dravidian Architecture)
    download_file(
        'https://images.unsplash.com/photo-1582510003544-4d00b7f74220?auto=format&fit=crop&w=1200&q=80',
        'web/assets/tirupati-real.jpg'
    )
    
    # 5. Real Shirdi Sai Divine Yatra (Spiritual Indian Temple)
    download_file(
        'https://images.unsplash.com/photo-1609766857041-ed402ea8069a?auto=format&fit=crop&w=1200&q=80',
        'web/assets/shirdi-real.jpg'
    )
    
    # 6. Real Ask DISHA Customer Support Professional Portrait
    download_file(
        'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&w=400&q=80',
        'web/assets/ask-disha-real.jpg'
    )
    
    # 7. Real Royal Palace & Heritage Train (Maharajas Express)
    download_file(
        'https://images.unsplash.com/photo-1599661046289-e31897846e41?auto=format&fit=crop&w=1200&q=80',
        'web/assets/maharajas-express-real.jpg'
    )
    
    # 8. Real High-Speed Indian Railway Journey (Tejas / Vande Bharat)
    download_file(
        'https://images.unsplash.com/photo-1534008897995-27a23e859048?auto=format&fit=crop&w=1200&q=80',
        'web/assets/tejas-express-real.jpg'
    )
    
    # 9. Real Bharat Gaurav Tourist Train (Indian Heritage Track)
    download_file(
        'https://images.unsplash.com/photo-1474487548417-781cb71495f3?auto=format&fit=crop&w=1200&q=80',
        'web/assets/bharat-gaurav-real.jpg'
    )

if __name__ == '__main__':
    main()
