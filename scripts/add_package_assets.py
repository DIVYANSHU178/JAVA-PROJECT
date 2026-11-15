import os

def create_extra_assets():
    os.makedirs('web/assets', exist_ok=True)
    
    # Tejas Express Banner
    tejas = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 360" width="100%" height="100%">
  <defs>
    <linearGradient id="tejasYellow" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#f59e0b" />
      <stop offset="50%" stop-color="#d97706" />
      <stop offset="100%" stop-color="#b45309" />
    </linearGradient>
  </defs>
  <rect width="800" height="360" fill="#1e293b"/>
  <polygon points="0,300 800,270 800,360 0,360" fill="#0f172a"/>
  <!-- Tejas Express Bright Yellow Livery -->
  <path d="M 800 180 L 360 180 Q 220 180 150 230 Q 110 265 90 295 L 800 295 Z" fill="url(#tejasYellow)"/>
  <path d="M 800 205 L 350 205 Q 230 205 170 235 Q 130 260 120 280 L 800 280 Z" fill="#0f172a"/>
  <polygon points="190,230 280,215 270,250 160,255" fill="#38bdf8" opacity="0.8"/>
  <rect x="36" y="30" width="340" height="110" rx="12" fill="#0b2545" opacity="0.88" stroke="rgba(255,255,255,0.2)"/>
  <text x="56" y="65" font-family="'Plus Jakarta Sans', Arial, sans-serif" font-weight="900" font-size="22" fill="#fbbf24">TEJAS EXPRESS</text>
  <text x="56" y="88" font-family="'Plus Jakarta Sans', Arial, sans-serif" font-weight="700" font-size="12" fill="#ffffff" letter-spacing="1.5">INDIA'S FIRST CORPORATE LUXURY TRAIN</text>
  <text x="56" y="115" font-family="'Plus Jakarta Sans', Arial, sans-serif" font-weight="500" font-size="11" fill="#cbd5e1">Onboard Infotainment • Automated Doors • Gourmet Dining</text>
</svg>"""
    with open('web/assets/tejas-express-banner.svg', 'w', encoding='utf-8') as f:
        f.write(tejas)

    # Tirupati Package
    tirupati = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 250" width="100%" height="100%">
  <defs>
    <linearGradient id="tirupatiGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#78350f" />
      <stop offset="100%" stop-color="#b45309" />
    </linearGradient>
  </defs>
  <rect width="400" height="250" fill="url(#tirupatiGrad)"/>
  <path d="M 200 40 L 230 140 L 170 140 Z" fill="#f59e0b" opacity="0.85"/>
  <circle cx="200" cy="35" r="8" fill="#fde047"/>
  <rect x="150" y="140" width="100" height="60" fill="#d97706"/>
  <circle cx="200" cy="170" r="16" fill="#78350f"/>
  <rect x="0" y="170" width="400" height="80" fill="rgba(0,0,0,0.65)"/>
  <text x="20" y="205" font-family="'Plus Jakarta Sans', Arial, sans-serif" font-weight="800" font-size="18" fill="#ffffff">Tirupati Balaji Darshan</text>
  <text x="20" y="228" font-family="'Plus Jakarta Sans', Arial, sans-serif" font-weight="600" font-size="12" fill="#fde047">Special Train Tour • 2 Nights / 3 Days • ₹8,450</text>
</svg>"""
    with open('web/assets/tirupati-package.svg', 'w', encoding='utf-8') as f:
        f.write(tirupati)

    # Kashmir Package
    kashmir = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 250" width="100%" height="100%">
  <defs>
    <linearGradient id="kashmirGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#0c4a6e" />
      <stop offset="100%" stop-color="#0284c7" />
    </linearGradient>
  </defs>
  <rect width="400" height="250" fill="url(#kashmirGrad)"/>
  <polygon points="0,180 80,80 180,180" fill="#f0fdf4" opacity="0.8"/>
  <polygon points="120,180 220,60 320,180" fill="#ffffff" opacity="0.9"/>
  <polygon points="260,180 340,90 400,180" fill="#f0fdf4" opacity="0.8"/>
  <path d="M 100 210 Q 150 200 220 210 Q 200 220 120 220 Z" fill="#7c2d12"/>
  <line x1="160" y1="195" x2="160" y2="210" stroke="#f8fafc" stroke-width="2"/>
  <rect x="0" y="170" width="400" height="80" fill="rgba(0,0,0,0.65)"/>
  <text x="20" y="205" font-family="'Plus Jakarta Sans', Arial, sans-serif" font-weight="800" font-size="18" fill="#ffffff">Kashmir Heaven on Earth</text>
  <text x="20" y="228" font-family="'Plus Jakarta Sans', Arial, sans-serif" font-weight="600" font-size="12" fill="#38bdf8">Srinagar, Gulmarg &amp; Pahalgam • 5N / 6D • ₹18,999</text>
</svg>"""
    with open('web/assets/kashmir-package.svg', 'w', encoding='utf-8') as f:
        f.write(kashmir)

    # Shirdi Package
    shirdi = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 250" width="100%" height="100%">
  <defs>
    <linearGradient id="shirdiGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#831843" />
      <stop offset="100%" stop-color="#be185d" />
    </linearGradient>
  </defs>
  <rect width="400" height="250" fill="url(#shirdiGrad)"/>
  <circle cx="200" cy="90" r="45" fill="#fbcfe8" opacity="0.3"/>
  <path d="M 180 120 C 180 80, 220 80, 220 120 Z" fill="#ffffff" opacity="0.8"/>
  <circle cx="200" cy="80" r="16" fill="#fdf2f8"/>
  <rect x="0" y="170" width="400" height="80" fill="rgba(0,0,0,0.65)"/>
  <text x="20" y="205" font-family="'Plus Jakarta Sans', Arial, sans-serif" font-weight="800" font-size="18" fill="#ffffff">Shirdi Sai Divine Yatra</text>
  <text x="20" y="228" font-family="'Plus Jakarta Sans', Arial, sans-serif" font-weight="600" font-size="12" fill="#f472b6">Shirdi &amp; Shani Shingnapur • 1N / 2D • ₹4,299</text>
</svg>"""
    with open('web/assets/shirdi-package.svg', 'w', encoding='utf-8') as f:
        f.write(shirdi)

    # Golden Triangle
    golden = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 250" width="100%" height="100%">
  <defs>
    <linearGradient id="goldGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#1e1b4b" />
      <stop offset="100%" stop-color="#312e81" />
    </linearGradient>
  </defs>
  <rect width="400" height="250" fill="url(#goldGrad)"/>
  <path d="M 200 60 Q 220 90 220 110 L 180 110 Q 180 90 200 60 Z" fill="#fcd34d" opacity="0.8"/>
  <rect x="170" y="110" width="60" height="35" fill="#fcd34d" opacity="0.8"/>
  <line x1="145" y1="75" x2="145" y2="145" stroke="#fcd34d" stroke-width="4"/>
  <line x1="255" y1="75" x2="255" y2="145" stroke="#fcd34d" stroke-width="4"/>
  <rect x="0" y="170" width="400" height="80" fill="rgba(0,0,0,0.65)"/>
  <text x="20" y="205" font-family="'Plus Jakarta Sans', Arial, sans-serif" font-weight="800" font-size="18" fill="#ffffff">Golden Triangle Tour</text>
  <text x="20" y="228" font-family="'Plus Jakarta Sans', Arial, sans-serif" font-weight="600" font-size="12" fill="#fbbf24">Delhi - Agra - Jaipur • 4N / 5D • ₹14,500</text>
</svg>"""
    with open('web/assets/golden-triangle-package.svg', 'w', encoding='utf-8') as f:
        f.write(golden)

    print("✅ Created Tejas and 4 package SVGs in web/assets/")

if __name__ == '__main__':
    create_extra_assets()
