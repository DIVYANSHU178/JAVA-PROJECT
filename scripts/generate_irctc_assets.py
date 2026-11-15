import os

def main():
    os.makedirs("web/assets", exist_ok=True)
    print("🎨 Generating Authentic IRCTC Visual Assets & Portraits...")

    # 1. IRCTC Official Logo (web/assets/irctc-logo.svg)
    irctc_logo = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 240 60" width="240" height="60">
  <defs>
    <linearGradient id="irctcBlue" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#1d3557" />
      <stop offset="100%" stop-color="#0b2545" />
    </linearGradient>
    <linearGradient id="irctcOrange" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#fb8500" />
      <stop offset="100%" stop-color="#d90429" />
    </linearGradient>
  </defs>
  <!-- Stylized Emblem -->
  <g transform="translate(10, 5)">
    <circle cx="25" cy="25" r="23" fill="none" stroke="url(#irctcBlue)" stroke-width="3"/>
    <path d="M 12 25 C 12 15, 38 15, 38 25 C 38 35, 12 35, 12 25 Z" fill="none" stroke="url(#irctcOrange)" stroke-width="3.5" />
    <circle cx="25" cy="25" r="6" fill="#1d3557"/>
    <path d="M 25 8 L 25 15 M 25 35 L 25 42 M 8 25 L 15 25 M 35 25 L 42 25" stroke="#fb8500" stroke-width="2.5" stroke-linecap="round"/>
  </g>
  <!-- Typography -->
  <text x="68" y="34" font-family="'Plus Jakarta Sans', Arial, sans-serif" font-weight="900" font-size="28" fill="#1d3557" letter-spacing="1.5">IRCTC</text>
  <text x="68" y="47" font-family="'Plus Jakarta Sans', Arial, sans-serif" font-weight="600" font-size="8.5" fill="#e63946" letter-spacing="0.5">Indian Railway Catering &amp; Tourism Corp.</text>
</svg>"""
    with open("web/assets/irctc-logo.svg", "w", encoding="utf-8") as f:
        f.write(irctc_logo)

    # 2. Indian Railways Official Emblem (web/assets/indian-railways-logo.svg)
    ir_emblem = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 60" width="200" height="60">
  <defs>
    <linearGradient id="railMaroon" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#800000" />
      <stop offset="100%" stop-color="#560000" />
    </linearGradient>
    <linearGradient id="railGold" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#f59e0b" />
      <stop offset="100%" stop-color="#d97706" />
    </linearGradient>
  </defs>
  <g transform="translate(10, 5)">
    <!-- Outer Cogwheel Seal -->
    <circle cx="25" cy="25" r="23" fill="url(#railMaroon)" stroke="url(#railGold)" stroke-width="2"/>
    <circle cx="25" cy="25" r="17" fill="none" stroke="#ffffff" stroke-width="1.2" stroke-dasharray="3,2"/>
    <!-- Steam Engine Silhouette -->
    <path d="M 17 28 L 22 20 L 31 20 L 33 28 Z M 16 28 L 34 28 L 34 32 L 16 32 Z" fill="url(#railGold)"/>
    <circle cx="20" cy="33" r="2.5" fill="#ffffff"/>
    <circle cx="25" cy="33" r="2.5" fill="#ffffff"/>
    <circle cx="30" cy="33" r="2.5" fill="#ffffff"/>
    <rect x="23" y="16" width="3" height="4" fill="url(#railGold)"/>
  </g>
  <text x="68" y="27" font-family="'Plus Jakarta Sans', Arial, sans-serif" font-weight="800" font-size="14" fill="#800000" letter-spacing="0.5">भारतीय रेल</text>
  <text x="68" y="44" font-family="'Plus Jakarta Sans', Arial, sans-serif" font-weight="700" font-size="12" fill="#1e293b" letter-spacing="0.8">INDIAN RAILWAYS</text>
</svg>"""
    with open("web/assets/indian-railways-logo.svg", "w", encoding="utf-8") as f:
        f.write(ir_emblem)

    # 3. CRIS Logo (web/assets/cris-logo.svg)
    cris_logo = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 140 50" width="140" height="50">
  <rect width="140" height="50" rx="8" fill="#f8fafc" stroke="#e2e8f0"/>
  <text x="14" y="32" font-family="Arial, sans-serif" font-weight="900" font-size="20" fill="#0b2545" letter-spacing="2">CRIS</text>
  <text x="68" y="24" font-family="Arial, sans-serif" font-weight="700" font-size="7" fill="#64748b">CENTRE FOR</text>
  <text x="68" y="33" font-family="Arial, sans-serif" font-weight="700" font-size="7" fill="#64748b">RAILWAY INFO.</text>
  <text x="68" y="42" font-family="Arial, sans-serif" font-weight="700" font-size="7" fill="#e11d48">SYSTEMS</text>
</svg>"""
    with open("web/assets/cris-logo.svg", "w", encoding="utf-8") as f:
        f.write(cris_logo)

    # 4. Ask DISHA 2.0 Mascot Portrait (web/assets/ask-disha-avatar.svg)
    disha_avatar = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="100" height="100">
  <defs>
    <linearGradient id="dishaBg" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#3b82f6" />
      <stop offset="100%" stop-color="#1d4ed8" />
    </linearGradient>
    <linearGradient id="sareeGradient" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#1e3a8a" />
      <stop offset="100%" stop-color="#0f172a" />
    </linearGradient>
  </defs>
  <!-- Background Badge -->
  <circle cx="50" cy="50" r="48" fill="url(#dishaBg)" stroke="#ffffff" stroke-width="3"/>
  <circle cx="50" cy="50" r="43" fill="#ffffff"/>
  <!-- Hair & Head -->
  <ellipse cx="50" cy="40" rx="22" ry="24" fill="#1e293b"/>
  <!-- Face -->
  <ellipse cx="50" cy="44" rx="16" ry="17" fill="#fde047" opacity="0.3"/>
  <ellipse cx="50" cy="44" rx="15" ry="16" fill="#fcd34d"/>
  <!-- Hair Bangs -->
  <path d="M 35 34 C 40 28, 60 28, 65 34 C 62 40, 56 36, 50 36 C 44 36, 38 40, 35 34 Z" fill="#0f172a"/>
  <!-- Eyes -->
  <circle cx="44" cy="42" r="2.2" fill="#0f172a"/>
  <circle cx="56" cy="42" r="2.2" fill="#0f172a"/>
  <!-- Smile -->
  <path d="M 45 49 Q 50 54 55 49" fill="none" stroke="#be123c" stroke-width="2" stroke-linecap="round"/>
  <!-- Bindi -->
  <circle cx="50" cy="38" r="1.2" fill="#e11d48"/>
  <!-- Headset -->
  <path d="M 33 42 A 18 18 0 0 1 67 42" fill="none" stroke="#475569" stroke-width="2.5"/>
  <rect x="31" y="40" width="3.5" height="7" rx="1.5" fill="#1e293b"/>
  <rect x="65.5" y="40" width="3.5" height="7" rx="1.5" fill="#1e293b"/>
  <path d="M 67 45 Q 60 56 53 54" fill="none" stroke="#475569" stroke-width="1.8" stroke-linecap="round"/>
  <circle cx="52" cy="54" r="2" fill="#0f172a"/>
  <!-- Professional Saree Uniform -->
  <path d="M 28 85 C 28 66, 72 66, 72 85 Z" fill="url(#sareeGradient)"/>
  <path d="M 37 68 L 50 85 L 63 68" fill="#f59e0b"/>
  <!-- Name Tag -->
  <rect x="42" y="74" width="16" height="5" rx="1" fill="#ffffff"/>
  <text x="44" y="78" font-family="Arial, sans-serif" font-weight="900" font-size="4" fill="#0b2545">DISHA</text>
</svg>"""
    with open("web/assets/ask-disha-avatar.svg", "w", encoding="utf-8") as f:
        f.write(disha_avatar)

    # 5. Vande Bharat Express High-Definition Train Portrait Banner (web/assets/vande-bharat-banner.svg)
    vb_banner = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 360" width="100%" height="100%">
  <defs>
    <linearGradient id="skyGrad" x1="0%" y1="0%" x2="0%" y2="100%">
      <stop offset="0%" stop-color="#0b2545" />
      <stop offset="60%" stop-color="#1d4ed8" />
      <stop offset="100%" stop-color="#60a5fa" />
    </linearGradient>
    <linearGradient id="trainWhite" x1="0%" y1="0%" x2="100%" y2="0%">
      <stop offset="0%" stop-color="#f8fafc" />
      <stop offset="50%" stop-color="#ffffff" />
      <stop offset="100%" stop-color="#e2e8f0" />
    </linearGradient>
    <linearGradient id="saffronStripe" x1="0%" y1="0%" x2="100%" y2="0%">
      <stop offset="0%" stop-color="#ff7700" />
      <stop offset="100%" stop-color="#f97316" />
    </linearGradient>
    <linearGradient id="glassGrad" x1="0%" y1="0%" x2="0%" y2="100%">
      <stop offset="0%" stop-color="#0f172a" />
      <stop offset="100%" stop-color="#1e293b" />
    </linearGradient>
  </defs>
  <!-- Background Scenic Sky -->
  <rect width="800" height="360" fill="url(#skyGrad)"/>
  <!-- Distant Mountains -->
  <polygon points="0,260 120,180 260,240 420,160 580,250 720,190 800,240 800,360 0,360" fill="#1e3a8a" opacity="0.4"/>
  <polygon points="80,260 220,190 350,250 500,170 650,260 800,210 800,360 0,360" fill="#0f172a" opacity="0.5"/>
  <!-- High Speed Track Bed -->
  <polygon points="0,310 800,280 800,360 0,360" fill="#334155"/>
  <line x1="0" y1="318" x2="800" y2="288" stroke="#94a3b8" stroke-width="4"/>
  <line x1="0" y1="335" x2="800" y2="305" stroke="#94a3b8" stroke-width="5"/>
  <!-- Speed Lines -->
  <line x1="20" y1="230" x2="180" y2="230" stroke="#ffffff" stroke-width="2" opacity="0.6" stroke-dasharray="10,15"/>
  <line x1="50" y1="245" x2="250" y2="245" stroke="#ffffff" stroke-width="2" opacity="0.4" stroke-dasharray="15,20"/>
  <!-- Aerodynamic Vande Bharat Train Nose -->
  <!-- Main Body -->
  <path d="M 800 170 L 380 170 Q 220 172 150 225 Q 100 265 80 295 L 800 295 Z" fill="url(#trainWhite)"/>
  <!-- Sleek Dark Window Strip -->
  <path d="M 800 195 L 370 195 Q 240 195 180 228 Q 140 252 125 275 L 800 275 Z" fill="url(#glassGrad)"/>
  <!-- Iconic Vande Bharat Orange/Saffron Livery Band -->
  <path d="M 800 275 L 125 275 Q 115 285 105 290 L 800 290 Z" fill="url(#saffronStripe)"/>
  <path d="M 800 185 L 360 185 Q 260 185 200 215 L 800 215 Z" fill="#2563eb" opacity="0.9"/>
  <!-- Aerodynamic Cockpit Windshield -->
  <polygon points="190,225 280,210 270,245 160,250" fill="#0284c7" opacity="0.8"/>
  <!-- Sleek Headlight Beam -->
  <polygon points="90,288 0,320 0,260" fill="#fef08a" opacity="0.35"/>
  <circle cx="95" cy="288" r="5" fill="#fef08a"/>
  <!-- Overhead Catenary Electric Pantograph -->
  <polyline points="480,170 470,120 520,95 490,95" fill="none" stroke="#94a3b8" stroke-width="3"/>
  <line x1="470" y1="95" x2="540" y2="95" stroke="#ef4444" stroke-width="4"/>
  <line x1="0" y1="95" x2="800" y2="95" stroke="#64748b" stroke-width="1.5" stroke-dasharray="4,6"/>
  <!-- Branding Overlay Card -->
  <rect x="36" y="30" width="340" height="110" rx="12" fill="#0b2545" opacity="0.88" stroke="rgba(255,255,255,0.2)"/>
  <text x="56" y="65" font-family="'Plus Jakarta Sans', Arial, sans-serif" font-weight="900" font-size="22" fill="#ffffff">VANDE BHARAT 2.0</text>
  <text x="56" y="88" font-family="'Plus Jakarta Sans', Arial, sans-serif" font-weight="700" font-size="12" fill="#f97316" letter-spacing="1.5">INDIA'S PRIDE • 160 KM/H SEMI-HIGH SPEED</text>
  <text x="56" y="115" font-family="'Plus Jakarta Sans', Arial, sans-serif" font-weight="500" font-size="11" fill="#cbd5e1">54 Modern Routes • Executive Chair Car • 360° Rotating Seats</text>
</svg>"""
    with open("web/assets/vande-bharat-banner.svg", "w", encoding="utf-8") as f:
        f.write(vb_banner)

    # 6. Maharajas' Express Luxury Train Banner (web/assets/maharajas-express-banner.svg)
    me_banner = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 360" width="100%" height="100%">
  <defs>
    <linearGradient id="royalGold" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#f59e0b" />
      <stop offset="50%" stop-color="#fbbf24" />
      <stop offset="100%" stop-color="#b45309" />
    </linearGradient>
    <linearGradient id="royalBurgundy" x1="0%" y1="0%" x2="100%" y2="0%">
      <stop offset="0%" stop-color="#4a0404" />
      <stop offset="50%" stop-color="#7f1d1d" />
      <stop offset="100%" stop-color="#3b0707" />
    </linearGradient>
  </defs>
  <rect width="800" height="360" fill="url(#royalBurgundy)"/>
  <!-- Palace Silhouette in Background -->
  <polygon points="100,240 120,180 140,240" fill="#991b1b" opacity="0.4"/>
  <rect x="140" y="200" width="80" height="40" fill="#991b1b" opacity="0.4"/>
  <circle cx="180" cy="190" r="20" fill="#991b1b" opacity="0.4"/>
  <!-- Luxury Golden Coach -->
  <rect x="40" y="160" width="720" height="140" rx="8" fill="#5c0606" stroke="url(#royalGold)" stroke-width="4"/>
  <line x1="40" y1="180" x2="760" y2="180" stroke="url(#royalGold)" stroke-width="2"/>
  <line x1="40" y1="280" x2="760" y2="280" stroke="url(#royalGold)" stroke-width="2"/>
  <!-- Ornate Windows -->
  <rect x="70" y="195" width="80" height="65" rx="6" fill="#1e1b4b" stroke="url(#royalGold)" stroke-width="2"/>
  <rect x="180" y="195" width="80" height="65" rx="6" fill="#1e1b4b" stroke="url(#royalGold)" stroke-width="2"/>
  <rect x="290" y="195" width="80" height="65" rx="6" fill="#1e1b4b" stroke="url(#royalGold)" stroke-width="2"/>
  <rect x="400" y="195" width="80" height="65" rx="6" fill="#1e1b4b" stroke="url(#royalGold)" stroke-width="2"/>
  <rect x="510" y="195" width="80" height="65" rx="6" fill="#1e1b4b" stroke="url(#royalGold)" stroke-width="2"/>
  <rect x="620" y="195" width="80" height="65" rx="6" fill="#1e1b4b" stroke="url(#royalGold)" stroke-width="2"/>
  <!-- Warm Window Glow -->
  <circle cx="110" cy="225" r="18" fill="#fef08a" opacity="0.25"/>
  <circle cx="220" cy="225" r="18" fill="#fef08a" opacity="0.25"/>
  <circle cx="330" cy="225" r="18" fill="#fef08a" opacity="0.25"/>
  <!-- Royal Crest on Coach -->
  <circle cx="440" cy="227" r="16" fill="url(#royalGold)"/>
  <text x="432" y="233" font-family="serif" font-weight="900" font-size="16" fill="#4a0404">♔</text>
  <!-- Text Overlay -->
  <rect x="40" y="30" width="380" height="110" rx="12" fill="#2d0505" opacity="0.9" stroke="url(#royalGold)"/>
  <text x="60" y="65" font-family="'Plus Jakarta Sans', serif" font-weight="900" font-size="22" fill="url(#royalGold)">MAHARAJAS' EXPRESS</text>
  <text x="60" y="88" font-family="'Plus Jakarta Sans', Arial, sans-serif" font-weight="700" font-size="12" fill="#fef08a" letter-spacing="1.5">WORLD'S LEADING LUXURY TRAIN</text>
  <text x="60" y="115" font-family="'Plus Jakarta Sans', Arial, sans-serif" font-weight="500" font-size="11" fill="#fbcfe8">Heritage of India • Treasures of India • Indian Panorama</text>
</svg>"""
    with open("web/assets/maharajas-express-banner.svg", "w", encoding="utf-8") as f:
        f.write(me_banner)

    # 7. Bharat Gaurav Tourist Train Banner (web/assets/bharat-gaurav-banner.svg)
    bg_banner = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 360" width="100%" height="100%">
  <defs>
    <linearGradient id="bgGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#064e3b" />
      <stop offset="60%" stop-color="#047857" />
      <stop offset="100%" stop-color="#0f766e" />
    </linearGradient>
  </defs>
  <rect width="800" height="360" fill="url(#bgGrad)"/>
  <!-- Temple & Heritage Silhouettes -->
  <path d="M 620 280 L 650 160 L 680 280 Z" fill="#065f46" opacity="0.6"/>
  <path d="M 640 160 L 650 130 L 660 160 Z" fill="#f59e0b" opacity="0.8"/>
  <circle cx="650" cy="125" r="4" fill="#f59e0b"/>
  <!-- Tourist Train Illustration -->
  <rect x="60" y="190" width="680" height="95" rx="6" fill="#f8fafc" stroke="#f59e0b" stroke-width="3"/>
  <rect x="60" y="245" width="680" height="15" fill="#f97316"/>
  <rect x="60" y="215" width="680" height="8" fill="#1e3a8a"/>
  <!-- Windows -->
  <rect x="90" y="200" width="60" height="35" rx="3" fill="#0f172a"/>
  <rect x="170" y="200" width="60" height="35" rx="3" fill="#0f172a"/>
  <rect x="250" y="200" width="60" height="35" rx="3" fill="#0f172a"/>
  <rect x="330" y="200" width="60" height="35" rx="3" fill="#0f172a"/>
  <rect x="410" y="200" width="60" height="35" rx="3" fill="#0f172a"/>
  <rect x="490" y="200" width="60" height="35" rx="3" fill="#0f172a"/>
  <rect x="570" y="200" width="60" height="35" rx="3" fill="#0f172a"/>
  <rect x="650" y="200" width="60" height="35" rx="3" fill="#0f172a"/>
  <!-- Text Overlay -->
  <rect x="40" y="30" width="380" height="110" rx="12" fill="#022c22" opacity="0.9" stroke="#34d399"/>
  <text x="60" y="65" font-family="'Plus Jakarta Sans', Arial, sans-serif" font-weight="900" font-size="22" fill="#ffffff">BHARAT GAURAV</text>
  <text x="60" y="88" font-family="'Plus Jakarta Sans', Arial, sans-serif" font-weight="700" font-size="12" fill="#34d399" letter-spacing="1.5">DEKHO APNA DESH • TOURIST TRAIN</text>
  <text x="60" y="115" font-family="'Plus Jakarta Sans', Arial, sans-serif" font-weight="500" font-size="11" fill="#a7f3d0">Shri Ramayana Yatra • Jyotirlinga Darshan • All Inclusive Packages</text>
</svg>"""
    with open("web/assets/bharat-gaurav-banner.svg", "w", encoding="utf-8") as f:
        f.write(bg_banner)

    print("✅ All official SVG graphics and high-resolution portraits generated in web/assets/!")

if __name__ == '__main__':
    main()
