#!/usr/bin/env python3
"""
Convert the 12 RailGaadi presentation slides into a 16:9 landscape PDF deck.
Opens natively in Apple Preview, Adobe Acrobat, and any browser.
"""

import os
from reportlab.lib.pagesizes import landscape
from reportlab.lib import colors
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, Image
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import inch

PAGE_WIDTH = 13.333 * inch
PAGE_HEIGHT = 7.5 * inch

C_NAVY = colors.HexColor("#0f172a")
C_BLUE = colors.HexColor("#1e3a8a")
C_ACCENT = colors.HexColor("#2563eb")
C_LIGHT_BLUE = colors.HexColor("#93c5fd")
C_DARK = colors.HexColor("#1e293b")
C_MUTED = colors.HexColor("#64748b")
C_BG = colors.HexColor("#f8fafc")
C_BORDER = colors.HexColor("#e2e8f0")
C_WHITE = colors.white

def draw_header_and_footer(canvas, doc, title_text, category_text="INDUSTRIAL TRAINING SEMINAR (40 DAYS)"):
    canvas.saveState()
    # Navy top banner
    canvas.setFillColor(C_NAVY)
    canvas.rect(0, PAGE_HEIGHT - 1.15 * inch, PAGE_WIDTH, 1.15 * inch, fill=1, stroke=0)
    # Blue stripe
    canvas.setFillColor(C_ACCENT)
    canvas.rect(0, PAGE_HEIGHT - 1.21 * inch, PAGE_WIDTH, 0.06 * inch, fill=1, stroke=0)
    
    # Category text
    canvas.setFillColor(C_LIGHT_BLUE)
    canvas.setFont("Helvetica-Bold", 10)
    canvas.drawString(0.8 * inch, PAGE_HEIGHT - 0.45 * inch, category_text.upper())
    
    # Title text
    canvas.setFillColor(C_WHITE)
    canvas.setFont("Helvetica-Bold", 20)
    canvas.drawString(0.8 * inch, PAGE_HEIGHT - 0.85 * inch, title_text)
    
    # Footer
    canvas.setFillColor(C_MUTED)
    canvas.setFont("Helvetica", 9)
    canvas.drawString(0.8 * inch, 0.4 * inch, "Poornima College of Engineering, Jaipur  •  Department of Information Technology  •  Session 2026–2027")
    canvas.drawRightString(PAGE_WIDTH - 0.8 * inch, 0.4 * inch, f"Slide {canvas._pageNumber} of 12")
    canvas.setStrokeColor(C_BORDER)
    canvas.setLineWidth(0.5)
    canvas.line(0.8 * inch, 0.65 * inch, PAGE_WIDTH - 0.8 * inch, 0.65 * inch)
    canvas.restoreState()

def build_pdf(output_path):
    doc = SimpleDocTemplate(
        output_path,
        pagesize=(PAGE_WIDTH, PAGE_HEIGHT),
        leftMargin=0.8 * inch,
        rightMargin=0.8 * inch,
        topMargin=1.45 * inch,
        bottomMargin=0.85 * inch,
    )

    styles = getSampleStyleSheet()
    
    card_title_style = ParagraphStyle(
        'CardTitle',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=12,
        leading=15,
        textColor=C_BLUE,
        spaceAfter=8
    )

    bullet_style = ParagraphStyle(
        'BulletText',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=9.5,
        leading=13.5,
        textColor=C_DARK,
        spaceAfter=5
    )

    logo_path = os.path.join(os.path.dirname(__file__), "report_assets", "poornima_logo.jpg")

    story = []

    # -------------------------------------------------------------
    # SLIDE 1: Title Slide
    # -------------------------------------------------------------
    def draw_slide_1(canvas, doc):
        canvas.saveState()
        # Navy blue gradient background fill
        canvas.setFillColor(C_NAVY)
        canvas.rect(0, 0, PAGE_WIDTH, PAGE_HEIGHT, fill=1, stroke=0)
        
        # Left blue stripe
        canvas.setFillColor(C_ACCENT)
        canvas.rect(0, 0, 0.35 * inch, PAGE_HEIGHT, fill=1, stroke=0)
        
        # Logo
        if os.path.exists(logo_path):
            canvas.drawImage(logo_path, PAGE_WIDTH - 2.8 * inch, PAGE_HEIGHT - 2.0 * inch, width=2.0 * inch, height=1.3 * inch, preserveAspectRatio=True)
            
        # Title text
        canvas.setFillColor(C_LIGHT_BLUE)
        canvas.setFont("Helvetica-Bold", 12)
        canvas.drawString(1.0 * inch, PAGE_HEIGHT - 1.8 * inch, "INDUSTRIAL TRAINING SEMINAR (40 DAYS DURATION)")
        
        canvas.setFillColor(C_WHITE)
        canvas.setFont("Helvetica-Bold", 34)
        canvas.drawString(1.0 * inch, PAGE_HEIGHT - 2.4 * inch, "RailGaadi")
        
        canvas.setFont("Helvetica", 16)
        canvas.setFillColor(colors.HexColor("#cbd5e1"))
        canvas.drawString(1.0 * inch, PAGE_HEIGHT - 2.85 * inch, "Real-Time Train Telemetry & Interactive Spatial Tracking Platform")
        
        # Panels for Presented To & Presented By
        card_y = 1.5 * inch
        card_w = 5.4 * inch
        card_h = 2.0 * inch
        
        # Left card: Presented To
        canvas.setFillColor(colors.HexColor("#1e293b"))
        canvas.setStrokeColor(colors.HexColor("#334155"))
        canvas.roundRect(1.0 * inch, card_y, card_w, card_h, 8, fill=1, stroke=1)
        
        canvas.setFillColor(C_LIGHT_BLUE)
        canvas.setFont("Helvetica-Bold", 11)
        canvas.drawString(1.25 * inch, card_y + card_h - 0.35 * inch, "PRESENTED TO:")
        
        canvas.setFillColor(C_WHITE)
        canvas.setFont("Helvetica-Bold", 12)
        canvas.drawString(1.25 * inch, card_y + card_h - 0.7 * inch, "Dr. Gajendra Singh Rajawat, Ms. Priya Shekhawat")
        canvas.setFont("Helvetica", 10)
        canvas.setFillColor(colors.HexColor("#cbd5e1"))
        canvas.drawString(1.25 * inch, card_y + card_h - 0.95 * inch, "Faculty Coordinators – Industrial Training Seminar")
        canvas.drawString(1.25 * inch, card_y + card_h - 1.2 * inch, "Department of Information Technology")
        canvas.drawString(1.25 * inch, card_y + card_h - 1.45 * inch, "Poornima College of Engineering, Jaipur")

        # Right card: Presented By
        right_x = 6.8 * inch
        canvas.setFillColor(colors.HexColor("#1e293b"))
        canvas.setStrokeColor(colors.HexColor("#334155"))
        canvas.roundRect(right_x, card_y, card_w, card_h, 8, fill=1, stroke=1)
        
        canvas.setFillColor(C_LIGHT_BLUE)
        canvas.setFont("Helvetica-Bold", 11)
        canvas.drawString(right_x + 0.25 * inch, card_y + card_h - 0.35 * inch, "PRESENTED BY:")
        
        canvas.setFillColor(C_WHITE)
        canvas.setFont("Helvetica-Bold", 12)
        canvas.drawString(right_x + 0.25 * inch, card_y + card_h - 0.7 * inch, "Nikhil Kumar (PCE23IT034)")
        canvas.setFont("Helvetica", 10)
        canvas.setFillColor(colors.HexColor("#cbd5e1"))
        canvas.drawString(right_x + 0.25 * inch, card_y + card_h - 0.95 * inch, "Bachelor of Technology — VI Semester")
        canvas.drawString(right_x + 0.25 * inch, card_y + card_h - 1.2 * inch, "Department of Information Technology")
        canvas.drawString(right_x + 0.25 * inch, card_y + card_h - 1.45 * inch, "Poornima College of Engineering, Jaipur")

        # Footer
        canvas.setFillColor(colors.HexColor("#94a3b8"))
        canvas.setFont("Helvetica", 9)
        canvas.drawString(1.0 * inch, 0.7 * inch, "Poornima College of Engineering, Jaipur  •  Department of IT  •  Session 2026–2027")
        canvas.drawRightString(PAGE_WIDTH - 1.0 * inch, 0.7 * inch, "Slide 1 of 12")
        canvas.restoreState()

    story.append(Spacer(1, 0.1))
    story.append(PageBreak())

    # Helper function for regular slides
    def add_card_slide(slide_num, title, category, cards_data, cols=2):
        nonlocal story
        card_cells = []
        for c in cards_data:
            content = [Paragraph(f"<b>📌 {c['title']}</b>", card_title_style), Spacer(1, 4)]
            for b in c['bullets']:
                content.append(Paragraph(f"• {b}", bullet_style))
            card_cells.append(content)

        t_data = []
        if cols == 2:
            col_width = (PAGE_WIDTH - 1.6 * inch - 0.3 * inch) / 2
            t_data.append([card_cells[0], card_cells[1]])
            col_widths = [col_width, col_width]
        elif cols == 3:
            col_width = (PAGE_WIDTH - 1.6 * inch - 0.4 * inch) / 3
            t_data.append([card_cells[0], card_cells[1], card_cells[2]])
            col_widths = [col_width, col_width, col_width]
        elif cols == 6:
            col_width = (PAGE_WIDTH - 1.6 * inch - 0.4 * inch) / 3
            t_data.append([card_cells[0], card_cells[1], card_cells[2]])
            t_data.append([card_cells[3], card_cells[4], card_cells[5]])
            col_widths = [col_width, col_width, col_width]

        t = Table(t_data, colWidths=col_widths)
        t.setStyle(TableStyle([
            ('BACKGROUND', (0,0), (-1,-1), C_BG),
            ('BOX', (0,0), (-1,-1), 1, C_BORDER),
            ('INNERGRID', (0,0), (-1,-1), 10, colors.white),
            ('TOPPADDING', (0,0), (-1,-1), 12),
            ('BOTTOMPADDING', (0,0), (-1,-1), 12),
            ('LEFTPADDING', (0,0), (-1,-1), 14),
            ('RIGHTPADDING', (0,0), (-1,-1), 14),
            ('VALIGN', (0,0), (-1,-1), 'TOP'),
        ]))

        story.append(t)
        if slide_num < 12:
            story.append(PageBreak())

    # Slide 2
    add_card_slide(2, "Introduction & Problem Statement", "PROJECT OVERVIEW & CONTEXT", [
        {
            "title": "Background & Need",
            "bullets": [
                "<b>Indian Railways:</b> Operates 13,000+ passenger trains daily carrying 24+ million passengers across 68,000+ km of tracks.",
                "<b>Existing Limitations:</b> National apps use static text-only checklists with zero spatial terrain or elevation visualization.",
                "<b>Frequent Rate Limits:</b> Commercial APIs enforce strict rate-limits, leaving passengers with stale delay updates.",
                "<b>Project Goal:</b> Build a high-performance vector-map train telemetry dashboard with offline fallback and zero framework bloat."
            ]
        },
        {
            "title": "Project Objectives",
            "bullets": [
                "<b>Dynamic Spatial Tracking:</b> High-FPS smooth train marker animation interpolated along geo-accurate railway tracks.",
                "<b>Comprehensive Telemetry:</b> Live speedometers, delay status, platform ETA countdowns, and gradient charts.",
                "<b>Resilient Architecture:</b> Local proxy server, exponential backoff polling, and offline cache for resilience.",
                "<b>Modern Lightweight Stack:</b> Vanilla ES6+ architecture built on Vite 6 with zero heavy SPA framework dependencies."
            ]
        }
    ], cols=2)

    # Slide 3
    add_card_slide(3, "Pre-requisites & Development Environment", "TECHNICAL PREREQUISITES", [
        {
            "title": "1. Core Conceptual Knowledge",
            "bullets": [
                "<b>Modern JavaScript (ES6+):</b> Async/await, Promises, CustomEvents, ES Modules, and event-driven patterns.",
                "<b>Spatial Geometry:</b> Great-Circle distance (Haversine formula), bearing calculations, and polyline coordinate slicing.",
                "<b>HTML5 Canvas API:</b> 2D graphics rendering contexts, devicePixelRatio scaling for Retina crispness.",
                "<b>Modern Responsive CSS:</b> CSS custom properties, grid/flexbox, glassmorphism, and dark mode."
            ]
        },
        {
            "title": "2. Toolchain & Runtime",
            "bullets": [
                "<b>Node.js 20 LTS:</b> Modern JavaScript runtime environment for tooling and proxy handling.",
                "<b>Vite 6 Bundler:</b> Next-gen ESM dev server with Hot Module Replacement and Rollup minifier.",
                "<b>MapLibre GL JS 5.1:</b> Open-source WebGL vector tile mapping engine for smooth 60fps pan & zoom.",
                "<b>Git & GitHub:</b> Version control, branch management, and continuous revision history."
            ]
        },
        {
            "title": "3. APIs & Hardware Setup",
            "bullets": [
                "<b>External Train APIs:</b> Indian Railways live status endpoints (via secure proxy).",
                "<b>Open-Meteo API:</b> Real-time route weather (temperature, wind, condition code).",
                "<b>Test Devices:</b> Multi-resolution testing across Desktop (1080p/4K) and mobile browsers."
            ]
        }
    ], cols=3)

    # Slide 4
    add_card_slide(4, "System Architecture & Decoupled Layers", "SYSTEM DESIGN & DATA PIPELINE", [
        {
            "title": "Client-Side Layered Model",
            "bullets": [
                "<b>1. External APIs & Proxy:</b> Vite reverse proxy forwards requests to external endpoints, eliminating CORS header blocks.",
                "<b>2. Core Service Layer:</b> apiService.js (fetching & fallback), geoService.js (GIS distance/bearing math), pwa.js (offline service worker).",
                "<b>3. Reactive State Store:</b> store.js implements a publish/subscribe pattern; single source of truth for train telemetry.",
                "<b>4. UI View Layer:</b> Completely decoupled components (mapView.js, stationBoard.js, elevationChart.js) listening to store events."
            ]
        },
        {
            "title": "Key Architectural Advantages",
            "bullets": [
                "<b>Zero Framework Overhead:</b> No 300kB React or Angular bundle — initial JavaScript payload is under 45 kB.",
                "<b>Instant Sub-Second Boot:</b> Native ES modules allow the browser to load components in parallel with zero transpilation lag.",
                "<b>Robust Error Boundary:</b> Network disconnection or API failure degrades smoothly to local simulated fallback data.",
                "<b>Predictable State Changes:</b> All telemetry mutations funnel through discrete actions with timestamped logs."
            ]
        }
    ], cols=2)

    # Slide 5
    add_card_slide(5, "Training Progress: Requirements & Foundation", "40-DAY TRAINING SCHEDULE (WEEKS 1 & 2)", [
        {
            "title": "Week 1: Days 1–7 — Conception & API Prototyping",
            "bullets": [
                "<b>Days 1–2:</b> Analyzed existing Indian Railways tracking portals and identified pain points in UX and delay reporting.",
                "<b>Days 3–4:</b> Evaluated free vs. commercial APIs (IRCTC, RapidAPI, Open-Meteo) and documented rate limit constraints.",
                "<b>Days 5–6:</b> Set up Vite 6 development environment, Git repository workflow, and initialized base project structure.",
                "<b>Day 7:</b> Built mock JSON train schemas (12050 Gatimaan, 22436 Vande Bharat, 12952 Rajdhani) for offline testing."
            ]
        },
        {
            "title": "Week 2: Days 8–14 — Design System & State Management",
            "bullets": [
                "<b>Days 8–9:</b> Built cohesive CSS design tokens (index.css), curated typography (Outfit & JetBrains Mono), and dark theme.",
                "<b>Days 10–11:</b> Created responsive shell layout: collapsible navigation, live clock, search bar, and telemetry cards.",
                "<b>Days 12–13:</b> Developed central reactive store (store.js) using a pure JavaScript Pub/Sub event subscriber model.",
                "<b>Day 14:</b> Validated multi-component state reactivity: telemetry updates triggered automated re-renders across views."
            ]
        }
    ], cols=2)

    # Slide 6
    add_card_slide(6, "Training Progress: Spatial Map & Live Polling", "40-DAY TRAINING SCHEDULE (WEEKS 3 & 4)", [
        {
            "title": "Week 3: Days 15–21 — Vector Map & Geo Interpolation",
            "bullets": [
                "<b>Days 15–16:</b> Integrated MapLibre GL 5.1 engine with CARTO dark-matter vector basemap tiles.",
                "<b>Days 17–18:</b> Implemented geoService.js to compute Haversine distances, station route polylines, and progress percentages.",
                "<b>Days 19–20:</b> Engineered smooth train marker movement with forward-looking bearing angles along track curves.",
                "<b>Day 21:</b> Built custom station pin markers with interactive modal popups showing scheduled vs. actual arrival times."
            ]
        },
        {
            "title": "Week 4: Days 22–28 — API Proxy & Real-Time Polling",
            "bullets": [
                "<b>Days 22–23:</b> Configured Vite development reverse proxy to bypass browser Cross-Origin Resource Sharing (CORS) blocks.",
                "<b>Days 24–25:</b> Implemented asynchronous polling loop with 30-second heartbeats and exponential backoff on HTTP 429 errors.",
                "<b>Days 26–27:</b> Built automated fallback switch to high-fidelity cached local database if the network goes offline.",
                "<b>Day 28:</b> Integrated Open-Meteo weather endpoint to display live destination temperature, humidity, and atmospheric conditions."
            ]
        }
    ], cols=2)

    # Slide 7
    add_card_slide(7, "Training Progress: Station Board, Charts & Polish", "40-DAY TRAINING SCHEDULE (WEEKS 5 & 6)", [
        {
            "title": "Week 5: Days 29–35 — Station Board & Canvas Visuals",
            "bullets": [
                "<b>Days 29–30:</b> Implemented Live Station Board module with tabbed filters: All Arrivals, Departures, and Delayed trains.",
                "<b>Days 31–32:</b> Developed HTML5 Canvas chart (elevationChart.js) rendering route elevation profiles and speed curves.",
                "<b>Days 33–34:</b> Applied Retina window.devicePixelRatio scaling to eliminate blurry canvas lines on high-DPI Mac/mobile screens.",
                "<b>Day 35:</b> Added Service Worker (sw.js) and Web App Manifest (manifest.json) for offline PWA installation capability."
            ]
        },
        {
            "title": "Week 6: Days 36–40 — Testing, Benchmarking & ITS Docs",
            "bullets": [
                "<b>Days 36–37:</b> Conducted comprehensive edge-case testing: rapid train switching, network drops, and corrupted payloads.",
                "<b>Day 38:</b> Optimized production bundle via Vite build (achieved 43.2 kB gzipped JS, zero render-blocking scripts).",
                "<b>Day 39:</b> Compiled full Industrial Training Seminar report adhering to Poornima College guidelines and format.",
                "<b>Day 40:</b> Prepared final PowerPoint presentation deck and rehearsed technical presentation flow."
            ]
        }
    ], cols=2)

    # Slide 8
    add_card_slide(8, "Core Application Modules & Features", "IMPLEMENTATION HIGHLIGHTS", [
        {"title": "1. Interactive Spatial Map", "bullets": ["High-FPS vector map with CARTO dark-matter basemap.", "Pulsing train marker with dynamic directional bearing.", "Color-coded station waypoints."]},
        {"title": "2. Real-Time Telemetry Card", "bullets": ["Live speedometer gauge (km/h).", "Delay status pill with dynamic color alerts.", "Next station ETA countdown & remaining distance."]},
        {"title": "3. Elevation & Speed Profiles", "bullets": ["HTML5 Canvas cross-section chart of terrain elevation.", "Overlay speed curve tracking acceleration zones.", "Hover crosshair displaying track statistics."]},
        {"title": "4. Live Station Board", "bullets": ["Airport-style electronic flipboard display.", "Filterable by arrivals, departures, platforms.", "Real-time delay notifications with auto-refresh."]},
        {"title": "5. Weather Telemetry", "bullets": ["Open-Meteo live temperature, wind, visibility.", "Contextual weather warning badges (rain/fog).", "Automatic 15-minute response caching."]},
        {"title": "6. Offline PWA & Dark Mode", "bullets": ["Progressive Web App installable on desktop & mobile.", "Service worker caching ensures offline map lookup.", "Sleek dark-mode palette for low glare."]}
    ], cols=6)

    # Slide 9 — Code Snippets
    def draw_slide_9_code(canvas, doc):
        canvas.saveState()
        # White background
        canvas.setFillColor(colors.white)
        canvas.rect(0, 0, PAGE_WIDTH, PAGE_HEIGHT, fill=1, stroke=0)

        # Header
        canvas.setFillColor(C_NAVY)
        canvas.rect(0, PAGE_HEIGHT - 1.15 * inch, PAGE_WIDTH, 1.15 * inch, fill=1, stroke=0)
        canvas.setFillColor(C_ACCENT)
        canvas.rect(0, PAGE_HEIGHT - 1.21 * inch, PAGE_WIDTH, 0.06 * inch, fill=1, stroke=0)
        canvas.setFillColor(C_LIGHT_BLUE)
        canvas.setFont("Helvetica-Bold", 10)
        canvas.drawString(0.8 * inch, PAGE_HEIGHT - 0.45 * inch, "CORE SYSTEM CODE IMPLEMENTATION")
        canvas.setFillColor(colors.white)
        canvas.setFont("Helvetica-Bold", 20)
        canvas.drawString(0.8 * inch, PAGE_HEIGHT - 0.85 * inch, "Key Code Implementations & Algorithms")

        # Footer
        canvas.setFillColor(C_MUTED)
        canvas.setFont("Helvetica", 9)
        canvas.drawString(0.8 * inch, 0.4 * inch, "Poornima College of Engineering, Jaipur  •  Department of IT  •  Session 2026–2027")
        canvas.drawRightString(PAGE_WIDTH - 0.8 * inch, 0.4 * inch, "Slide 9 of 13")
        canvas.setStrokeColor(C_BORDER)
        canvas.setLineWidth(0.5)
        canvas.line(0.8 * inch, 0.65 * inch, PAGE_WIDTH - 0.8 * inch, 0.65 * inch)

        snippets = [
            ("1. Geodesic Distance — geoService.js",
             "Haversine formula: great-circle distance between train & upcoming stations.",
             ["static distanceKm(lat1, lon1, lat2, lon2) {",
              "  const R = 6371; // Earth radius in km",
              "  const dLat = (lat2-lat1) * Math.PI / 180;",
              "  const dLon = (lon2-lon1) * Math.PI / 180;",
              "  const a = Math.sin(dLat/2)**2 +",
              "            Math.cos(lat1*Math.PI/180) *",
              "            Math.cos(lat2*Math.PI/180) *",
              "            Math.sin(dLon/2)**2;",
              "  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));",
              "}"]),
            ("2. Pub/Sub Reactive Store — store.js",
             "Event-driven subscriber pattern, no React needed.",
             ["subscribe(listener) {",
              "  this.listeners.add(listener);",
              "  return () => this.listeners.delete(listener);",
              "}",
              "",
              "setState(updates) {",
              "  this.state = { ...this.state, ...updates };",
              "  const keys = Object.keys(updates);",
              "  this.listeners.forEach(fn => fn(this.state, keys));",
              "}"]),
            ("3. Retina Canvas Scaling — analyticsView.js",
             "Scales pixel buffer by devicePixelRatio for crisp 4K / Retina output.",
             ["const ctx = canvas.getContext('2d');",
              "const dpr = window.devicePixelRatio || 1;",
              "const rect = canvas.getBoundingClientRect();",
              "",
              "canvas.width  = rect.width  * dpr;",
              "canvas.height = rect.height * dpr;",
              "ctx.scale(dpr, dpr); // Crisp on any display",
              "ctx.clearRect(0, 0, rect.width, rect.height);"]),
            ("4. CORS Reverse Proxy — vite.config.ts",
             "Rewrites /api/* requests server-side, eliminating browser CORS blocks.",
             ["export default defineConfig({",
              "  server: {",
              "    port: 8080,",
              "    proxy: {",
              "      '/api/railradar': {",
              "        target: 'https://api.railradar.in/v1',",
              "        changeOrigin: true,",
              "        rewrite: p => p.replace(",
              "          /^\/api\/railradar/, '')",
              "      }",
              "    }}",
              "}});"])
        ]

        col_x  = [0.8 * inch, 7.1 * inch]
        row_y  = [PAGE_HEIGHT - 1.55 * inch, PAGE_HEIGHT - 4.45 * inch]
        cw = 5.7 * inch
        ch = 2.65 * inch

        for i, (title, note, lines) in enumerate(snippets):
            col = i % 2
            row = i // 2
            lx = col_x[col]
            ty = row_y[row]

            # Card background
            canvas.setFillColor(C_BG)
            canvas.setStrokeColor(C_BORDER)
            canvas.setLineWidth(0.75)
            canvas.roundRect(lx, ty - ch, cw, ch, 6, fill=1, stroke=1)

            # Card title
            canvas.setFillColor(C_BLUE)
            canvas.setFont("Helvetica-Bold", 10)
            canvas.drawString(lx + 0.18 * inch, ty - 0.28 * inch, title)

            # Note
            canvas.setFillColor(C_MUTED)
            canvas.setFont("Helvetica", 8.5)
            canvas.drawString(lx + 0.18 * inch, ty - 0.48 * inch, note)

            # Code box
            code_box_x = lx + 0.15 * inch
            code_box_y = ty - ch + 0.1 * inch
            code_box_w = cw - 0.3 * inch
            code_box_h = ch - 0.65 * inch
            canvas.setFillColor(colors.HexColor("#0f172a"))
            canvas.setStrokeColor(colors.HexColor("#334155"))
            canvas.roundRect(code_box_x, code_box_y, code_box_w, code_box_h, 5, fill=1, stroke=1)

            canvas.setFillColor(colors.HexColor("#f1f5f9"))
            canvas.setFont("Courier", 8)
            for j, line in enumerate(lines[:10]):
                canvas.drawString(code_box_x + 0.1 * inch,
                                  code_box_y + code_box_h - 0.22 * inch - j * 0.165 * inch,
                                  line)

        canvas.restoreState()

    story.append(Spacer(1, 0.1))
    story.append(PageBreak())

    # Slide 10 — Technical Challenges (renumbered from 9)
    add_card_slide(10, "Technical Challenges & Solutions", "ENGINEERING CHALLENGES", [
        {
            "title": "Challenge 1: Browser CORS Restrictions",
            "bullets": [
                "<b>Problem:</b> Public train APIs do not emit Access-Control-Allow-Origin headers, causing browser fetch errors.",
                "<b>Solution:</b> Implemented a lightweight reverse proxy in vite.config.js, stripping CORS roadblocks during development."
            ]
        },
        {
            "title": "Challenge 2: Rate Limits & API Quotas",
            "bullets": [
                "<b>Problem:</b> Free-tier train APIs enforce strict rate limits; rapid polling caused HTTP 429 Too Many Requests errors.",
                "<b>Solution:</b> Built exponential backoff retry logic and an automatic seamless failover to pre-indexed high-precision local data."
            ]
        },
        {
            "title": "Challenge 3: Blurry Canvas on Retina Displays",
            "bullets": [
                "<b>Problem:</b> Standard HTML5 canvas dimensions resulted in pixelated, blurry text and graph lines on modern Mac screens.",
                "<b>Solution:</b> Scaled canvas coordinate buffer by window.devicePixelRatio while fixing CSS dimensions for razor-sharp rendering."
            ]
        },
        {
            "title": "Challenge 4: Background Polling Battery Drain",
            "bullets": [
                "<b>Problem:</b> Continuous background polling drained laptop/mobile battery when the user switched browser tabs.",
                "<b>Solution:</b> Integrated document.visibilityState listener to suspend polling timers when inactive and refresh instantly on focus."
            ]
        }
    ], cols=2)

    # Slide 11 — Testing (renumbered from 10)
    add_card_slide(10, "Testing & Performance Benchmarks", "VERIFICATION & METRICS", [
        {
            "title": "Edge-Case Testing Scenarios",
            "bullets": [
                "<b>Search & Indexing Speed:</b> Instant filtering across 1,000+ Indian railway stations in under 10ms via memory-indexed trie lookup.",
                "<b>URL Hash Routing:</b> Seamless deep linking (#train/12050) allowing users to bookmark or share exact train live journeys directly.",
                "<b>Network Disconnection:</b> Disconnecting Wi-Fi triggers zero crashes; system transitions to cached station boards with an alert pill."
            ]
        },
        {
            "title": "Production Build Metrics",
            "bullets": [
                "<b>Total Gzipped JS Bundle:</b> 43.2 kB (includes store, API service, geometry calculations, and all UI views).",
                "<b>Total Gzipped CSS:</b> 8.6 kB (responsive design tokens, dark theme, and grid utilities).",
                "<b>Build Compilation Time:</b> 157 ms via Vite 6 / Rollup bundler.",
                "<b>Lighthouse Performance Score:</b> 98/100 for desktop performance, accessibility, and best practices."
            ]
        }
    ], cols=2)

    # Slide 12 — Learning Outcomes (renumbered from 11)
    add_card_slide(11, "Learning Outcomes & Conclusion", "TRAINING OUTCOMES", [
        {
            "title": "Key Technical Learnings",
            "bullets": [
                "<b>Reactive State Without Bloat:</b> Mastered building a robust central state store using pure JavaScript without heavy external libraries.",
                "<b>Applied GIS Mathematics:</b> Gained deep hands-on proficiency in geodesic algorithms, spherical trigonometry, and vector interpolation.",
                "<b>Production Dev Tooling:</b> Hands-on experience configuring Vite proxies, handling HTTP headers, and writing offline service workers.",
                "<b>Defensive API Engineering:</b> Built resilient frontend architecture capable of withstanding rate-limits and total network loss."
            ]
        },
        {
            "title": "Project Conclusion",
            "bullets": [
                "RailGaadi proves that modern vanilla web standards can deliver an exceptionally smooth, 60fps tracking experience.",
                "Successfully fulfilled all project milestones outlined in the 40-day Industrial Training Seminar curriculum.",
                "Established a solid technical foundation for future extensions, including crowd-sourced delay reporting and push notifications."
            ]
        }
    ], cols=2)

    # Slide 12: Thank You / Discussion
    def draw_slide_12(canvas, doc):
        canvas.saveState()
        canvas.setFillColor(C_NAVY)
        canvas.rect(0, 0, PAGE_WIDTH, PAGE_HEIGHT, fill=1, stroke=0)
        canvas.setFillColor(C_ACCENT)
        canvas.rect(0, 0, 0.35 * inch, PAGE_HEIGHT, fill=1, stroke=0)
        
        canvas.setFillColor(C_LIGHT_BLUE)
        canvas.setFont("Helvetica-Bold", 12)
        canvas.drawString(1.0 * inch, PAGE_HEIGHT - 2.0 * inch, "INDUSTRIAL TRAINING SEMINAR (40 DAYS DURATION)")
        
        canvas.setFillColor(C_WHITE)
        canvas.setFont("Helvetica-Bold", 36)
        canvas.drawString(1.0 * inch, PAGE_HEIGHT - 2.7 * inch, "Thank You!")
        
        canvas.setFont("Helvetica", 18)
        canvas.setFillColor(colors.HexColor("#cbd5e1"))
        canvas.drawString(1.0 * inch, PAGE_HEIGHT - 3.2 * inch, "Questions & Open Discussion")
        
        card_y = 1.6 * inch
        card_w = 5.4 * inch
        card_h = 1.9 * inch
        
        canvas.setFillColor(colors.HexColor("#1e293b"))
        canvas.setStrokeColor(colors.HexColor("#334155"))
        canvas.roundRect(1.0 * inch, card_y, card_w, card_h, 8, fill=1, stroke=1)
        canvas.setFillColor(C_LIGHT_BLUE)
        canvas.setFont("Helvetica-Bold", 11)
        canvas.drawString(1.25 * inch, card_y + card_h - 0.35 * inch, "GUIDED & EVALUATED BY:")
        canvas.setFillColor(C_WHITE)
        canvas.setFont("Helvetica-Bold", 12)
        canvas.drawString(1.25 * inch, card_y + card_h - 0.7 * inch, "Dr. Gajendra Singh Rajawat, Ms. Priya Shekhawat")
        canvas.setFont("Helvetica", 10)
        canvas.setFillColor(colors.HexColor("#cbd5e1"))
        canvas.drawString(1.25 * inch, card_y + card_h - 0.95 * inch, "Faculty Coordinators – Industrial Training Seminar")
        canvas.drawString(1.25 * inch, card_y + card_h - 1.2 * inch, "Department of Information Technology, PCE Jaipur")

        right_x = 6.8 * inch
        canvas.setFillColor(colors.HexColor("#1e293b"))
        canvas.setStrokeColor(colors.HexColor("#334155"))
        canvas.roundRect(right_x, card_y, card_w, card_h, 8, fill=1, stroke=1)
        canvas.setFillColor(C_LIGHT_BLUE)
        canvas.setFont("Helvetica-Bold", 11)
        canvas.drawString(right_x + 0.25 * inch, card_y + card_h - 0.35 * inch, "PRESENTER:")
        canvas.setFillColor(C_WHITE)
        canvas.setFont("Helvetica-Bold", 12)
        canvas.drawString(right_x + 0.25 * inch, card_y + card_h - 0.7 * inch, "Nikhil Kumar (PCE23IT034)")
        canvas.setFont("Helvetica", 10)
        canvas.setFillColor(colors.HexColor("#cbd5e1"))
        canvas.drawString(right_x + 0.25 * inch, card_y + card_h - 0.95 * inch, "Bachelor of Technology — VI Semester")
        canvas.drawString(right_x + 0.25 * inch, card_y + card_h - 1.2 * inch, "Department of Information Technology, PCE Jaipur")

        canvas.setFillColor(colors.HexColor("#94a3b8"))
        canvas.setFont("Helvetica", 9)
        canvas.drawString(1.0 * inch, 0.7 * inch, "Poornima College of Engineering, Jaipur  •  Department of IT  •  Session 2026–2027")
        canvas.drawRightString(PAGE_WIDTH - 1.0 * inch, 0.7 * inch, "Slide 12 of 12")
        canvas.restoreState()

    titles = [
        "", # Slide 1 custom
        "Introduction & Problem Statement",
        "Pre-requisites & Development Environment",
        "System Architecture & Decoupled Layers",
        "Training Progress: Requirements & Foundation",
        "Training Progress: Spatial Map & Live Polling",
        "Training Progress: Station Board, Charts & Polish",
        "Core Application Modules & Features",
        "Key Code Implementations & Algorithms",
        "Technical Challenges & Solutions",
        "Testing & Performance Benchmarks",
        "Learning Outcomes & Conclusion",
        "" # Slide 13 custom
    ]

    cats = [
        "",
        "PROJECT OVERVIEW & CONTEXT",
        "TECHNICAL PREREQUISITES",
        "SYSTEM DESIGN & DATA PIPELINE",
        "40-DAY TRAINING SCHEDULE (WEEKS 1 & 2)",
        "40-DAY TRAINING SCHEDULE (WEEKS 3 & 4)",
        "40-DAY TRAINING SCHEDULE (WEEKS 5 & 6)",
        "IMPLEMENTATION HIGHLIGHTS",
        "",  # Slide 9 — handled by custom draw
        "ENGINEERING CHALLENGES",
        "VERIFICATION & METRICS",
        "TRAINING OUTCOMES",
        ""   # Slide 13 — Thank You custom
    ]

    def on_page(canvas, doc):
        idx = canvas._pageNumber - 1
        if idx == 0:
            draw_slide_1(canvas, doc)
        elif idx == 8:
            draw_slide_9_code(canvas, doc)
        elif idx == 12:
            draw_slide_12(canvas, doc)
        else:
            draw_header_and_footer(canvas, doc, titles[idx], cats[idx])

    doc.build(story, onFirstPage=on_page, onLaterPages=on_page)
    print("PDF Presentation successfully generated:", output_path)

if __name__ == '__main__':
    base_dir = os.path.dirname(os.path.dirname(__file__))
    out_file = os.path.join(base_dir, "RailGaadi_ITS_Presentation_Slides.pdf")
    build_pdf(out_file)
