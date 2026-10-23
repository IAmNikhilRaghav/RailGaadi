#!/usr/bin/env python3
"""
Generate the Official RailGaadi Presentation PDF deck (.pdf)
Matching the exact layout, dimensions, fonts, aspect ratio, college logos,
and white-background/black-text color scheme of the original Poornima ITS PPT.

Author: Nikhil Kumar (PCE23IT034)
Session: 2026-27
"""

import os
from reportlab.lib import colors
from reportlab.platypus import (
    BaseDocTemplate, PageTemplate, Frame, Paragraph, Spacer, Table, TableStyle, PageBreak, Image as RLImage, KeepTogether, NextPageTemplate
)
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import inch
from reportlab.pdfgen import canvas

SLIDE_WIDTH = 10.0 * inch
SLIDE_HEIGHT = 5.625 * inch

C_WHITE = colors.white
C_BLACK = colors.black
C_GRAY_LINE = colors.HexColor("#9E9E9E")

logo_slide1 = os.path.abspath("public/presentation_assets/slide1_logo.png")
logo_header = os.path.abspath("public/presentation_assets/slide_header_logo.png")

snap_map = os.path.abspath("public/presentation_assets/snap_map.png")
snap_board = os.path.abspath("public/presentation_assets/snap_board.png")
snap_analytics = os.path.abspath("public/presentation_assets/snap_analytics.png")
snap_terminal = os.path.abspath("public/presentation_assets/snap_terminal.png")

# Titles for each slide (index 0 is Cover, index 1 is Slide 2, etc.)
SLIDE_TITLES = [
    "", # Slide 1 (Cover)
    "CONTENT",
    "Training Platform",
    "Why I Selected This Platform",
    "Prerequisites of the Training",
    "Week Wise Schedule",
    "Week Wise Schedule",
    "Technology Used",
    "RailGaadi - Project Description",
    "RailGaadi - Project Description",
    "RailGaadi - Project Description",
    "Learning",
    "Snapshots of Project",
    "Important Code Snippets",
    "Future Scope",
    "Conclusion",
    "References"
]

class PresentationCanvas(canvas.Canvas):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.pages = []

    def showPage(self):
        self.pages.append(dict(self.__dict__))
        self._startPage()

    def save(self):
        for page in self.pages:
            self.__dict__.update(page)
            super().showPage()
        super().save()

def draw_cover_bg(c, doc):
    c.saveState()
    c.setFillColor(C_WHITE)
    c.rect(0, 0, SLIDE_WIDTH, SLIDE_HEIGHT, fill=1, stroke=0)
    c.restoreState()

def draw_slide_header_and_bg(c, doc):
    c.saveState()
    # 1. White Background
    c.setFillColor(C_WHITE)
    c.rect(0, 0, SLIDE_WIDTH, SLIDE_HEIGHT, fill=1, stroke=0)

    # 2. Slide Title
    p_num = c._pageNumber
    t_text = SLIDE_TITLES[p_num - 1] if p_num <= len(SLIDE_TITLES) else ""
    c.setFillColor(C_BLACK)
    c.setFont("Helvetica", 27)
    c.drawString(0.57 * inch, SLIDE_HEIGHT - 0.72 * inch, t_text)

    # 4. Horizontal Divider Line
    c.setStrokeColor(C_GRAY_LINE)
    c.setLineWidth(0.75)
    c.line(0, SLIDE_HEIGHT - 1.11 * inch, SLIDE_WIDTH, SLIDE_HEIGHT - 1.11 * inch)

    c.restoreState()

def build_pdf(output_path):
    doc = BaseDocTemplate(
        output_path,
        pagesize=(SLIDE_WIDTH, SLIDE_HEIGHT),
        leftMargin=0,
        rightMargin=0,
        topMargin=0,
        bottomMargin=0
    )

    # Cover frame takes nearly full height
    cover_frame = Frame(
        0.54 * inch, 0.20 * inch, 8.92 * inch, 5.25 * inch,
        id='cover_frame',
        topPadding=0, bottomPadding=0, leftPadding=0, rightPadding=0
    )

    # Normal slides frame starts below header line
    slide_frame = Frame(
        0.57 * inch, 0.25 * inch, 8.86 * inch, 4.20 * inch,
        id='slide_frame',
        topPadding=0, bottomPadding=0, leftPadding=0, rightPadding=0
    )

    template_cover = PageTemplate(id='Cover', frames=cover_frame, onPage=draw_cover_bg)
    template_slide = PageTemplate(id='Slide', frames=slide_frame, onPage=draw_slide_header_and_bg)

    doc.addPageTemplates([template_cover, template_slide])

    styles = getSampleStyleSheet()

    title_main_style = ParagraphStyle(
        'CoverMain',
        fontName='Helvetica',
        fontSize=24,
        leading=29,
        alignment=1, # Center
        textColor=C_BLACK
    )

    dept_style = ParagraphStyle(
        'CoverDept',
        fontName='Helvetica',
        fontSize=15,
        leading=20,
        alignment=1,
        textColor=C_BLACK,
        spaceBefore=3
    )

    sub_style = ParagraphStyle(
        'CoverSub',
        fontName='Helvetica',
        fontSize=12.5,
        leading=16,
        alignment=1,
        textColor=C_BLACK,
        spaceBefore=2
    )

    proj_style = ParagraphStyle(
        'CoverProj',
        fontName='Helvetica-Bold',
        fontSize=16.5,
        leading=21,
        alignment=1,
        textColor=C_BLACK,
        spaceBefore=3
    )

    bottom_label_style = ParagraphStyle(
        'CoverBottomLabel',
        fontName='Helvetica',
        fontSize=11,
        leading=15,
        textColor=C_BLACK
    )

    body_bullet_style = ParagraphStyle(
        'BodyBullet',
        fontName='Helvetica',
        fontSize=10.5,
        leading=15,
        textColor=C_BLACK,
        spaceBefore=6
    )

    content_item_style = ParagraphStyle(
        'ContentItem',
        fontName='Helvetica',
        fontSize=10.5,
        leading=14.5,
        textColor=C_BLACK,
        spaceBefore=2.5
    )

    sec_title_style = ParagraphStyle(
        'SecTitle',
        fontName='Helvetica-Bold',
        fontSize=13.5,
        leading=17,
        textColor=C_BLACK,
        spaceBefore=2,
        spaceAfter=5
    )

    code_head_style = ParagraphStyle(
        'CodeHead',
        fontName='Helvetica-Bold',
        fontSize=10,
        leading=13,
        textColor=C_BLACK,
        spaceBefore=2,
        spaceAfter=3
    )

    code_pre_style = ParagraphStyle(
        'CodePre',
        fontName='Courier',
        fontSize=7.2,
        leading=9.5,
        textColor=C_BLACK
    )

    story = []

    # =========================================================================
    # SLIDE 1: COVER SLIDE
    # =========================================================================
    story.append(Paragraph("<b>POORNIMA COLLEGE OF ENGINEERING</b>", title_main_style))
    story.append(Paragraph("DEPARTMENT OF INFORMATION TECHNOLOGY", dept_style))
    story.append(Spacer(1, 6))

    story.append(Paragraph("An", sub_style))
    story.append(Paragraph("INDUSTRIAL TRAINING SEMINAR PRESENTATION", sub_style))
    story.append(Paragraph("on", sub_style))
    story.append(Paragraph("<b>Software Development</b>", proj_style))
    story.append(Spacer(1, 8))

    if os.path.exists(logo_slide1):
        logo_w, logo_h = 1.25 * inch, 1.25 * inch
        story.append(KeepTogether([RLImage(logo_slide1, width=logo_w, height=logo_h)]))
    story.append(Spacer(1, 8))

    meta_table_data = [
        [
            Paragraph("Presented to:<br/><b>Mr. Gajendra Singh Rajawat</b><br/><b>Ms. Priya Shekhawat</b><br/>(Faculty Coordinators)", bottom_label_style),
            Paragraph("Session: 2026-27", ParagraphStyle('Sess', parent=bottom_label_style, alignment=1)),
            Paragraph("Presented By:<br/><b>Nikhil Kumar</b><br/><b>PCE23IT034</b>", ParagraphStyle('PresBy', parent=bottom_label_style, alignment=2))
        ]
    ]
    meta_table = Table(meta_table_data, colWidths=[3.2 * inch, 2.4 * inch, 3.2 * inch])
    meta_table.setStyle(TableStyle([
        ('VALIGN', (0,0), (-1,-1), 'BOTTOM'),
        ('LEFTPADDING', (0,0), (-1,-1), 0),
        ('RIGHTPADDING', (0,0), (-1,-1), 0),
        ('BOTTOMPADDING', (0,0), (-1,-1), 0),
        ('TOPPADDING', (0,0), (-1,-1), 0)
    ]))
    story.append(meta_table)

    # Switch to 'Slide' template for all following slides
    story.append(NextPageTemplate('Slide'))
    story.append(PageBreak())

    # =========================================================================
    # SLIDE 2: CONTENT
    # =========================================================================
    content_list = [
        "•  Training Platform",
        "•  Why you have selected this platform?",
        "•  Prerequisites of the Training",
        "•  Week wise schedule",
        "•  Technology Used",
        "•  Project Description",
        "&nbsp;&nbsp;&nbsp;&nbsp;–  Project Introduction",
        "&nbsp;&nbsp;&nbsp;&nbsp;–  Project Scope",
        "&nbsp;&nbsp;&nbsp;&nbsp;–  Module Assigned",
        "•  Learning",
        "•  Snapshots of Project",
        "•  Important Code Snippets",
        "•  Future Scope",
        "•  Conclusion",
        "•  References"
    ]
    for item in content_list:
        story.append(Paragraph(item, content_item_style))

    # =========================================================================
    # SLIDE 3: Training Platform
    # =========================================================================
    story.append(PageBreak())
    story.append(Paragraph("<b>About Sumit DigiTech Private Limited</b>", sec_title_style))
    story.append(Spacer(1, 2))
    s3_items = [
        "●  <b>Industry:</b> Information Technology & Digital Solutions | <b>Location:</b> India",
        "●  <b>Services:</b> Software Development, Web Development, Website Design, UI/UX Development, Digital Solutions",
        "●  <b>Focus Areas:</b> Modern Web Applications, Software Solutions, Digital Transformation, Technology-Driven Development",
        "●  <b>Training & Projects:</b> Practical software development projects and hands-on technical learning opportunities",
        "●  <b>Development Approach:</b> User-Focused Solutions • Quality Development • Modern Technologies • Practical Implementation",
        "●  <b>Core Values:</b> Innovation • Quality • Customer Satisfaction • Reliable & User-Friendly Solutions"
    ]
    for it in s3_items:
        story.append(Paragraph(it, body_bullet_style))

    # =========================================================================
    # SLIDE 4: Why I Selected This Platform
    # =========================================================================
    story.append(PageBreak())
    s4_items = [
        "●  <b>Practical Learning Opportunities –</b> Exposure to Software Development, Web Development, and real-time transit telemetry applications.",
        "●  <b>Professional Work Culture –</b> Supportive and learning-oriented environment focused on innovation and clean software architecture.",
        "●  <b>Real-World Experience –</b> Hands-on involvement in developing practical software projects, GIS cartography, and scalable web solutions.",
        "●  <b>Skill Development –</b> Improved technical knowledge, spatial mathematical algorithms, frontend performance, and problem-solving abilities.",
        "●  <b>Growth-Oriented –</b> Guidance from experienced professionals, continuous code reviews, and valuable teamwork experience.",
        "●  <b>Project-Based Learning –</b> Opportunity to design and build 'RailGaadi' — a live passenger train tracking and route intelligence web platform.",
        "●  <b>Future Relevance –</b> Technologies and development practices aligned with current and emerging industry trends in modern web standards."
    ]
    for it in s4_items:
        story.append(Paragraph(it, body_bullet_style))

    # =========================================================================
    # SLIDE 5: Prerequisites of the Training
    # =========================================================================
    story.append(PageBreak())
    s5_items = [
        "●  <b>Fundamentals of Web Technologies –</b> Strong understanding of semantic HTML5, modern CSS3, and JavaScript logic.",
        "●  <b>Frontend Development –</b> Practical knowledge of modern ES Modules, component-based layout design, and responsive user interfaces.",
        "●  <b>State Architecture & Asynchronous Logic –</b> Understanding event-driven Pub-Sub reactive patterns, Promises, and REST API communication.",
        "●  <b>Spatial Cartography & GIS Math –</b> Basic knowledge of geographical coordinate systems (Lat/Lon), Haversine spherical math, and vector maps.",
        "●  <b>API Integration & Reverse Proxies –</b> Practical exposure to transit APIs, OpenWeatherMap, Vite dev proxy, and CORS handling.",
        "●  <b>Version Control & Development Tools –</b> Using Git/GitHub, VS Code, npm, Vite bundler, and browser DevTools for debugging.",
        "●  <b>Problem-Solving & Soft Skills –</b> Debugging ability, logical thinking, communication, teamwork, and technical adaptability."
    ]
    for it in s5_items:
        story.append(Paragraph(it, body_bullet_style))

    # =========================================================================
    # SLIDE 6: Week Wise Schedule (Weeks 1 to 3)
    # =========================================================================
    story.append(PageBreak())
    s6_items = [
        "<b>Week 1:</b>",
        "●  Introduction to Software Development environment, toolchain setup, and project requirements.",
        "●  Setup Node.js runtime, Vite 6 bundler, and established modular project file architecture.",
        "●  Evaluated Indian Railways live transit APIs and analyzed rate limits and payload schemas.",
        "<b>Week 2:</b>",
        "●  Designed and developed responsive user interfaces and modern layout using CSS custom properties.",
        "●  Implemented the centralized Pub-Sub reactive state store (store.js) for decoupled view reactivity.",
        "●  Created mock JSON train schemas (12050 Gatimaan, 22436 Vande Bharat, 12952 Rajdhani) for offline testing.",
        "<b>Week 3:</b>",
        "●  Integrated MapLibre GL JS WebGL map engine with high-FPS vector basemap tiles.",
        "●  Developed geoService.js for Haversine distance calculations, bearing angles, and polyline interpolation.",
        "●  Built dynamic animated train markers with directional heading and interactive station popups."
    ]
    for it in s6_items:
        if it.startswith("<b>Week"):
            story.append(Paragraph(it, ParagraphStyle('WkHead', parent=sec_title_style, fontSize=11.5, leading=14, spaceBefore=6, spaceAfter=2)))
        else:
            story.append(Paragraph(it, ParagraphStyle('WkBul', parent=body_bullet_style, spaceBefore=2)))

    # =========================================================================
    # SLIDE 7: Week Wise Schedule (Weeks 4 to 6)
    # =========================================================================
    story.append(PageBreak())
    s7_items = [
        "<b>Week 4:</b>",
        "●  Configured Vite development reverse proxy to eliminate browser Cross-Origin Resource Sharing (CORS) blocks.",
        "●  Engineered resilient polling service (railradarService.js) with cache TTL and seamless local mock fallback.",
        "●  Integrated OpenWeatherMap and OpenTopography APIs for coordinate-level weather and SRTM terrain elevation.",
        "<b>Week 5:</b>",
        "●  Developed the Live Station Board module simulating digital railway junction split-flap departure/arrival displays.",
        "●  Built the HTML5 Canvas Real-Time Speed & Elevation Profile telemetry graph with Retina display scaling.",
        "●  Implemented dark mode semantic tokens, slide-in Favourites drawer, and native browser push notifications for delays.",
        "<b>Week 6:</b>",
        "●  Implemented Progressive Web Application (PWA) service worker (sw.js) and manifest for offline execution.",
        "●  Completed testing, debugging, and application optimization (achieved 98/100 Lighthouse performance score).",
        "●  Prepared project documentation, technical ITS report, and final presentation."
    ]
    for it in s7_items:
        if it.startswith("<b>Week"):
            story.append(Paragraph(it, ParagraphStyle('WkHead', parent=sec_title_style, fontSize=11.5, leading=14, spaceBefore=6, spaceAfter=2)))
        else:
            story.append(Paragraph(it, ParagraphStyle('WkBul', parent=body_bullet_style, spaceBefore=2)))

    # =========================================================================
    # SLIDE 8: Technology Used
    # =========================================================================
    story.append(PageBreak())
    s8_items = [
        "●  <b>Core Web Stack –</b> HTML5 Semantic Elements, Modern CSS3 Custom Properties, and Vanilla JavaScript (ES2022 Modules).",
        "●  <b>Vector Map & Spatial Rendering –</b> MapLibre GL JS 5.1 (hardware-accelerated WebGL vector cartography with CARTO basemaps).",
        "●  <b>Build Toolchain & Dev Server –</b> Vite 6 (ESM development server, Hot Module Replacement, and Rollup production bundler).",
        "●  <b>Telemetry & Canvas Visualization –</b> HTML5 2D Canvas API with devicePixelRatio scaling for Retina display clarity.",
        "●  <b>Transit & Environmental APIs –</b> RailRadar Transit API, OpenWeatherMap live weather API, and OpenTopography SRTM DEM.",
        "●  <b>State Architecture & PWA –</b> Custom reactive Pub-Sub Store (store.js), Service Worker (sw.js), and Web App Manifest.",
        "●  <b>Version Control & Development Tools –</b> Git, GitHub, Visual Studio Code, npm, and Google Chrome DevTools."
    ]
    for it in s8_items:
        story.append(Paragraph(it, body_bullet_style))

    # =========================================================================
    # SLIDE 9: Project Introduction
    # =========================================================================
    story.append(PageBreak())
    story.append(Paragraph("<b>Project Introduction</b>", sec_title_style))
    story.append(Spacer(1, 2))
    p9_1 = (
        "The <b>RailGaadi (Train Tracker) Web Application</b> is a high-performance, real-time transit intelligence and spatial passenger "
        "tracking platform designed to provide passengers with comprehensive, live situational awareness across the Indian Railway network. "
        "It transforms traditional static timetables into an interactive, multi-dimensional journey monitoring experience."
    )
    p9_2 = (
        "The application is developed as a <b>lightweight, framework-free web platform</b> utilizing modern ES Modules, HTML5 Canvas, and "
        "<b>MapLibre GL JS WebGL cartography</b>, integrated with live GPS telemetry, terrain elevation models, and micro-climate forecasts. "
        "It provides features such as <b>live dynamic train positioning, route gradient profiling, digital station departure boards, delay status alerts, "
        "and offline PWA capability</b>, offering travelers a seamless, reliable, and user-friendly tracking interface."
    )
    story.append(Paragraph(p9_1, ParagraphStyle('P9', parent=body_bullet_style, fontSize=10.5, leading=15.5, spaceBefore=4)))
    story.append(Paragraph(p9_2, ParagraphStyle('P9_2', parent=body_bullet_style, fontSize=10.5, leading=15.5, spaceBefore=8)))

    # =========================================================================
    # SLIDE 10: Project Scope
    # =========================================================================
    story.append(PageBreak())
    story.append(Paragraph("<b>Project Scope</b>", sec_title_style))
    story.append(Spacer(1, 2))
    s10_items = [
        "●  Build a responsive interface using <b>modern Vanilla JavaScript</b> with structured routing and reusable components.",
        "●  Enable users to search and track trains in real-time with <b>smooth spatial polyline rendering and live speed telemetry</b>.",
        "●  Implement an authenticated local reverse proxy to bypass <b>browser CORS constraints</b> and ensure reliable API communication.",
        "●  Integrate multi-source external APIs including <b>RailRadar transit telemetry, OpenWeatherMap conditions, and SRTM elevation</b>.",
        "●  Engineer defensive offline resilience through <b>exponential backoff retries, local cached fallbacks, and PWA service workers</b>.",
        "●  Develop interactive station intelligence features including <b>digital live arrival/departure flipboards and filterable delay analytics</b>."
    ]
    for it in s10_items:
        story.append(Paragraph(it, body_bullet_style))

    # =========================================================================
    # SLIDE 11: Module Assigned
    # =========================================================================
    story.append(PageBreak())
    story.append(Paragraph("<b>Module Assigned</b>", sec_title_style))
    story.append(Spacer(1, 2))
    s11_items = [
        "●  <b>Spatial Cartography & Trajectory Module –</b> Implemented MapLibre GL vector map integration, train heading interpolation, and station markers.",
        "●  <b>Reactive State Machine (store.js) –</b> Architected a centralized Pub-Sub state store coordinating telemetry updates across decoupled views.",
        "●  <b>Resilient Transit API Service (railradarService.js) –</b> Engineered network fetch mechanisms with TTL caching, CORS proxying, and automated mock failover.",
        "●  <b>Speed Telemetry & Elevation Canvas (analyticsView.js) –</b> Developed double-buffered HTML5 canvas graphics with Retina devicePixelRatio scaling.",
        "●  <b>Electronic Station Board (stationBoard.js) –</b> Created live digital split-flap display boards with arrival/departure filtering and real-time delay flags.",
        "●  <b>Offline PWA & Notification Engine (sw.js, notificationService.js) –</b> Implemented service worker caching and native browser push notifications for train delays."
    ]
    for it in s11_items:
        story.append(Paragraph(it, body_bullet_style))

    # =========================================================================
    # SLIDE 12: Learning
    # =========================================================================
    story.append(PageBreak())
    s12_items = [
        "●  Improved skills in <b>HTML5, CSS3, modern JavaScript (ES2022+), and modular architecture</b>.",
        "●  Gained deep practical experience in <b>GIS mathematics, spherical trigonometry (Haversine formula), and polyline interpolation</b>.",
        "●  Learned to use <b>Git & GitHub</b> for production version control, branching workflows, and collaboration.",
        "●  Understood <b>API integration and reverse proxying</b> for robust communication between frontend and upstream transit services.",
        "●  Gained comprehensive knowledge of <b>MapLibre GL WebGL mapping, HTML5 Canvas graphics, and Retina display scaling</b>.",
        "●  Mastered building <b>Progressive Web Applications (PWA)</b> with service worker offline caching and native push notifications.",
        "●  Enhanced <b>debugging, problem-solving, teamwork, and technical presentation skills</b>."
    ]
    for it in s12_items:
        story.append(Paragraph(it, body_bullet_style))

    # =========================================================================
    # SLIDE 13: Snapshots of Project
    # =========================================================================
    story.append(PageBreak())
    img_w, img_h = 4.30 * inch, 1.95 * inch
    snap_table_data = [
        [RLImage(snap_map, width=img_w, height=img_h), RLImage(snap_board, width=img_w, height=img_h)],
        [RLImage(snap_analytics, width=img_w, height=img_h), RLImage(snap_terminal, width=img_w, height=img_h)]
    ]
    snap_table = Table(snap_table_data, colWidths=[4.43 * inch, 4.43 * inch])
    snap_table.setStyle(TableStyle([
        ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
        ('ALIGN', (0,0), (-1,-1), 'CENTER'),
        ('LEFTPADDING', (0,0), (-1,-1), 0),
        ('RIGHTPADDING', (0,0), (-1,-1), 0),
        ('TOPPADDING', (0,0), (-1,-1), 2),
        ('BOTTOMPADDING', (0,0), (-1,-1), 2)
    ]))
    story.append(snap_table)

    # =========================================================================
    # SLIDE 14: Important Code Snippets
    # =========================================================================
    story.append(PageBreak())
    code_left = [
        Paragraph("<b>1. Spherical Haversine Distance (geoService.js)</b>", code_head_style),
        Paragraph(
            "static distanceKm(lat1, lon1, lat2, lon2) {<br/>"
            "&nbsp;&nbsp;const R = 6371; // Earth radius in km<br/>"
            "&nbsp;&nbsp;const dLat = (lat2 - lat1) * Math.PI / 180;<br/>"
            "&nbsp;&nbsp;const dLon = (lon2 - lon1) * Math.PI / 180;<br/>"
            "&nbsp;&nbsp;const a = Math.sin(dLat/2)**2 +<br/>"
            "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Math.cos(lat1*Math.PI/180) *<br/>"
            "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Math.cos(lat2*Math.PI/180) * Math.sin(dLon/2)**2;<br/>"
            "&nbsp;&nbsp;return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));<br/>"
            "}",
            code_pre_style
        ),
        Spacer(1, 8),
        Paragraph("<b>2. Pub/Sub Reactive State Store (store.js)</b>", code_head_style),
        Paragraph(
            "subscribe(listener) {<br/>"
            "&nbsp;&nbsp;this.listeners.add(listener);<br/>"
            "&nbsp;&nbsp;return () =&gt; this.listeners.delete(listener);<br/>"
            "}<br/>"
            "setState(updates) {<br/>"
            "&nbsp;&nbsp;this.state = { ...this.state, ...updates };<br/>"
            "&nbsp;&nbsp;const changed = Object.keys(updates);<br/>"
            "&nbsp;&nbsp;this.listeners.forEach(fn =&gt; fn(this.state, changed));<br/>"
            "}",
            code_pre_style
        )
    ]

    code_right = [
        Paragraph("<b>3. Retina Canvas Scaling (analyticsView.js)</b>", code_head_style),
        Paragraph(
            "const ctx = canvas.getContext('2d');<br/>"
            "const dpr = window.devicePixelRatio || 1;<br/>"
            "const rect = canvas.getBoundingClientRect();<br/>"
            "canvas.width = rect.width * dpr;<br/>"
            "canvas.height = rect.height * dpr;<br/>"
            "ctx.scale(dpr, dpr); // Crisp on 4K / Retina<br/>"
            "ctx.clearRect(0, 0, rect.width, rect.height);",
            code_pre_style
        ),
        Spacer(1, 8),
        Paragraph("<b>4. Reverse Proxy CORS Bypass (vite.config.ts)</b>", code_head_style),
        Paragraph(
            "defineConfig({<br/>"
            "&nbsp;&nbsp;server: {<br/>"
            "&nbsp;&nbsp;&nbsp;&nbsp;port: 8080,<br/>"
            "&nbsp;&nbsp;&nbsp;&nbsp;proxy: {<br/>"
            "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;'/api/railradar': {<br/>"
            "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;target: 'https://api.railradar.in/v1',<br/>"
            "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;changeOrigin: true,<br/>"
            "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;rewrite: p =&gt; p.replace(/^\\/api\\/railradar/, '')<br/>"
            "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;}<br/>"
            "&nbsp;&nbsp;&nbsp;&nbsp;}<br/>"
            "&nbsp;&nbsp;}<br/>"
            "});",
            code_pre_style
        )
    ]

    code_table = Table([[code_left, code_right]], colWidths=[4.43 * inch, 4.43 * inch])
    code_table.setStyle(TableStyle([
        ('VALIGN', (0,0), (-1,-1), 'TOP'),
        ('LEFTPADDING', (0,0), (-1,-1), 0),
        ('RIGHTPADDING', (0,0), (-1,-1), 0),
        ('TOPPADDING', (0,0), (-1,-1), 0),
        ('BOTTOMPADDING', (0,0), (-1,-1), 0)
    ]))
    story.append(code_table)

    # =========================================================================
    # SLIDE 15: Future Scope
    # =========================================================================
    story.append(PageBreak())
    s15_items = [
        "●  <b>Crowdsourced Live Tracking –</b> Allow on-board passengers to validate real-time GPS locations and coach occupancy.",
        "●  <b>AI Predictive Delay Modeling –</b> Integrate machine learning to predict junction bottlenecks and delay propagation across sectors.",
        "●  <b>Multi-Modal Transit Integration –</b> Connect Indian Railways schedules with metro lines and local buses for unified journey planning.",
        "●  <b>WebSocket & MQTT Streaming –</b> Upgrade polling intervals to low-latency bi-directional streams for sub-second updates.",
        "●  <b>Cross-Platform Mobile App –</b> Extend the web platform into native Android and iOS applications with offline background notifications.",
        "●  <b>Offline Vector Map Caching –</b> Pre-cache regional map boundaries locally so tracks and terrain render inside isolated tunnels."
    ]
    for it in s15_items:
        story.append(Paragraph(it, body_bullet_style))

    # =========================================================================
    # SLIDE 16: Conclusion
    # =========================================================================
    story.append(PageBreak())
    s16_items = [
        "●  Gained practical experience in <b>Software Development</b> through real-world project implementation.",
        "●  Strengthened knowledge of <b>HTML, CSS, modern JavaScript (ES Modules), MapLibre GL, and API integration</b>.",
        "●  Successfully developed the <b>RailGaadi (Train Tracker) Web Application</b> with live telemetry, elevation curves, and station boards.",
        "●  Learned to use professional tools and technologies like <b>Git, GitHub, VS Code, Vite, and PWA Service Workers</b> for development.",
        "●  Enhanced <b>teamwork, communication, problem-solving, and time management</b> in a professional environment.",
        "●  Overall, the internship boosted my confidence and prepared me for <b>future opportunities in full-stack web and software engineering</b>."
    ]
    for it in s16_items:
        story.append(Paragraph(it, body_bullet_style))

    # =========================================================================
    # SLIDE 17: References
    # =========================================================================
    story.append(PageBreak())
    s17_items = [
        "●  <b>Sumit DigiTech Private Limited –</b> Official Website and Training Resources",
        "●  <b>MapLibre GL JS Documentation –</b> https://maplibre.org/maplibre-gl-js/docs/",
        "●  <b>MDN Web Docs –</b> HTML, CSS, JavaScript, Canvas API & Service Workers",
        "●  <b>Vite Official Documentation –</b> Next Generation Frontend Tooling",
        "●  <b>OpenWeatherMap & OpenTopography –</b> Weather & SRTM Elevation APIs",
        "●  <b>Git & GitHub Guides –</b> Version Control & Repository Management",
        "<b>Other References</b>",
        "●  Tutorials & articles from platforms like <b>GeeksforGeeks, W3Schools, FreeCodeCamp</b>",
        "●  Guidance and mentorship provided by the <b>Sumit DigiTech team & Poornima Faculty Coordinators</b>"
    ]
    for it in s17_items:
        if it == "<b>Other References</b>":
            story.append(Paragraph(it, ParagraphStyle('OtherRef', parent=sec_title_style, fontSize=11.5, leading=15, spaceBefore=8, spaceAfter=2)))
        else:
            story.append(Paragraph(it, body_bullet_style))

    doc.build(story, canvasmaker=PresentationCanvas)
    print(f"Successfully generated PDF presentation deck at: {output_path}")

if __name__ == "__main__":
    out_pdf1 = "RailGaadi_ITS_Presentation_Slides.pdf"
    out_pdf2 = "RailGaadi_ITS_Presentation_Nikhil_Kumar.pdf"
    
    build_pdf(out_pdf1)
    build_pdf(os.path.join("public", out_pdf1))
    
    # Also write with exact matching name to PPTX file
    import shutil
    shutil.copyfile(out_pdf1, out_pdf2)
    shutil.copyfile(out_pdf1, os.path.join("public", out_pdf2))
    print(f"Successfully generated matching PDF presentation deck at: {out_pdf2}")
    print(f"Successfully generated matching PDF presentation deck at: public/{out_pdf2}")
