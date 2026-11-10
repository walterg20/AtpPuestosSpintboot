---
name: Gestión Caminera Chaco
colors:
  surface: '#f8f9ff'
  surface-dim: '#cbdbf5'
  surface-bright: '#f8f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#eff4ff'
  surface-container: '#e5eeff'
  surface-container-high: '#dce9ff'
  surface-container-highest: '#d3e4fe'
  on-surface: '#0b1c30'
  on-surface-variant: '#43474e'
  inverse-surface: '#213145'
  inverse-on-surface: '#eaf1ff'
  outline: '#74777f'
  outline-variant: '#c4c6cf'
  surface-tint: '#485f82'
  primary: '#00152f'
  on-primary: '#ffffff'
  primary-container: '#0f2a4a'
  on-primary-container: '#7a92b7'
  inverse-primary: '#b0c8f0'
  secondary: '#006398'
  on-secondary: '#ffffff'
  secondary-container: '#5bb8fe'
  on-secondary-container: '#00476e'
  tertiary: '#00190e'
  on-tertiary: '#ffffff'
  tertiary-container: '#00301f'
  on-tertiary-container: '#24a375'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#d4e3ff'
  primary-fixed-dim: '#b0c8f0'
  on-primary-fixed: '#001c3a'
  on-primary-fixed-variant: '#304869'
  secondary-fixed: '#cce5ff'
  secondary-fixed-dim: '#93ccff'
  on-secondary-fixed: '#001d31'
  on-secondary-fixed-variant: '#004b73'
  tertiary-fixed: '#85f8c4'
  tertiary-fixed-dim: '#68dba9'
  on-tertiary-fixed: '#002114'
  on-tertiary-fixed-variant: '#005137'
  background: '#f8f9ff'
  on-background: '#0b1c30'
  surface-variant: '#d3e4fe'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  display-lg-mobile:
    fontFamily: Inter
    fontSize: 26px
    fontWeight: '700'
    lineHeight: 34px
    letterSpacing: -0.01em
  headline-lg:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.015em
  headline-md:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: -0.005em
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '600'
    lineHeight: 14px
    letterSpacing: 0.04em
  data-mono:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '500'
    lineHeight: 18px
    letterSpacing: -0.01em
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  gutter: 1.5rem
  gutter-sm: 1rem
  margin: 2rem
  margin-sm: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

The design system establishes a high-performance, authoritative, and clean administrative environment for provincial checkpoint monitoring and logistics. The aesthetic is contemporary enterprise civic software: institutional without being archaic, agile without compromising legal rigor. It directly serves field agents, fiscal auditors, and shift supervisors operating under ATP (Administración Tributaria Provincial) and ECOM Chaco.

The emotional target is clarity, unshakeable stability, and focused efficiency under shift conditions. High data density must coexist with immediate scannability. Structural decisions eliminate visual clutter, prioritizing operational statuses, fast transaction verification, vehicle processing times, and strict audit trails.

Visual style balances **Corporate / Modern** precision with crisp tactile micro-details: pure planar cards, razor-thin structural borders, low-glare surface treatments for varied lighting environments (from roadside toll booths to central auditing rooms), and deliberate semantic signaling.

## Colors

The palette draws from civic authority and operational clarity:

- **Primary (`#0f2a4a`)**: Deep institutional navy used for global navigation, structural headers, high-level session identity cards, and dominant brand anchoring. Represents state authority, compliance, and permanence.
- **Secondary (`#0284c7`)**: Vibrant digital cyan/blue. Directs user focus to interactive targets, active tabs, primary actions, progress meters, and dynamic filters.
- **Tertiary / Semantic Success (`#059669`)**: Forest emerald dedicated to operational validity, active booths (`Abierto`), verified vehicle transit manifests, and reconciled collections.
- **Neutral (`#64748b`)**: Slate gray providing balanced hierarchy for secondary data, column definitions, disabled states, and structural dividers.

### Supporting Semantics
- **Warning / Alert**: `#d97706` (Amber) for pending reviews, shift changeover countdowns, and flagged anomalies.
- **Destructive / Closed**: `#dc2626` (Red) for infractions and rejected transit permits; `#475569` (Slate Dark) for inactive/closed lanes (`Cerrado`).
- **Canvas & Surfaces**: Base canvas sits on `#f8fafc` (Slate 50), with inner workspace containers on `#f1f5f9` (Slate 100) and elevated cards on pristine `#ffffff`.
- **Text & Contrast**: High-contrast `#0f172a` (Slate 900) for primary records and numeric metrics, `#334155` for standard body copy.

## Typography

Inter is chosen across headlines, body copy, and UI metadata for its optical legibility in data-dense table layouts, high-speed input forms, and varying display calibrations.

- **Tabular Numerals**: Form tables, currency amounts (aranceles/multas), license plate numbers (patentes), and document numbers (CUIT/DNI) must enforce font feature settings `'tnum' 1` to ensure vertical column alignment across records.
- **Labels & Microcopy**: Form field headers, metadata chips, and session bar identifiers use `label-sm` or `label-md` with slight letter-spacing expansions to facilitate instantaneous identification during rapid field inspections.
- **Hierarchy Rules**: Page-level titles never exceed 32px on desktop, avoiding visual imbalance against dense data tables. Subsections maintain a clean 16px to 20px range.

## Layout & Spacing

The structural layout utilizes a fluid 12-column grid system capped at a maximum container width of `1600px` to maintain comfortable eye movement on ultrawide checkpoint monitors.

- **Desktop (>= 1024px)**: 12 columns, `1.5rem` (`24px`) gutters, `2rem` (`32px`) external margins. Supports multi-column operations: 3 columns for shift metadata and checkpoint quick stats, 9 columns for live transit logs and inspection queues.
- **Tablet (768px - 1023px)**: 8 columns, `1rem` (`16px`) gutters, `1.5rem` (`24px`) margins. Sidebar controls condense into expandable drawer overlays; data tables offer horizontal kinetic scrolling with sticky identifier columns.
- **Mobile (< 768px)**: 4 columns, `0.75rem` (`12px`) gutters, `1rem` (`16px`) margins. Cards unwrap into full-width vertical stacks. Critical verification actions pin to bottom sheets for one-thumb field interaction.

Component spacing strictly honors a 4px/8px incremental base: compact padding (`space-sm`) inside high-density table cells, and spacious rhythm (`space-lg` to `space-xl`) between distinct administrative sections.

## Elevation & Depth

Visual hierarchy employs a calibrated **Tonal Layer + Soft Ambient Shadow** approach to create clear visual planes without distraction:

- **Level 0 (Base Canvas)**: Background `#f8fafc`. Flush, matte, glare-resistant.
- **Level 1 (Data Cards & Tables)**: Background `#ffffff`, bordered by `1px solid #e2e8f0`. Shadow: `0 1px 3px 0 rgba(15, 42, 74, 0.05), 0 1px 2px -1px rgba(15, 42, 74, 0.04)`.
- **Level 2 (Active Focus & Dropdowns)**: Surfaces slightly raised during row selection or popover triggers. Shadow: `0 4px 6px -1px rgba(15, 42, 74, 0.08), 0 2px 4px -2px rgba(15, 42, 74, 0.04)`.
- **Level 3 (Modal Dialogs & Confirmation Overlays)**: Bordered with `#cbd5e1`. Shadow: `0 20px 25px -5px rgba(15, 42, 74, 0.12), 0 8px 10px -6px rgba(15, 42, 74, 0.08)`.
- **Level 4 (Top Navigation Bar)**: Deep Navy `#0f2a4a` anchored at top z-index, using an interior subtle divider `1px solid rgba(255, 255, 255, 0.1)` rather than high-contrast dropshadows to preserve modern elegance.

## Shapes

The design system utilizes **Soft** geometry (`roundedness: 1`), conveying enterprise precision, organizational discipline, and technical capability.

- **Base Radius (0.25rem / 4px)**: Data inputs, table search fields, small status chips, and contextual tooltips.
- **Intermediate Radius (0.5rem / 8px)**: Standard panels, summary metric cards, filter bars, action buttons, and modal dialogs.
- **Large Radius (0.75rem / 12px)**: Outer boundary layout cards and segmented master containers.
- **Pill (Fully Rounded)**: Reserved strictly for operational status indicators (e.g., `Abierto` / `Cerrado`), vehicle classification badges, and active session counters.

## Components

### Top Navigation Bar & Global Header
- **Background**: Deep Navy `#0f2a4a` with crisp white and cyan accents.
- **Branding**: Official institutional typography lockup with ATP Chaco and ECOM emblems.
- **Operational Session Bar**: A prominent, integrated metadata cluster featuring:
  - **Usuario**: `Sebastian Montiel` (with badge avatar and role tag).
  - **Puesto**: `LA VICUÑA` (with active station ping icon).
  - **Turno**: `20` (highlighted in subtle light slate pill).
  - **Comisión**: `13 oscar meza`.
  Divided by semi-transparent vertical rules (`rgba(255, 255, 255, 0.15)`).

### Status Badges & Chips
- **Abierto (Open)**: Background `#ecfdf5`, text `#065f46`, border `1px solid #a7f3d0`, preceded by a pulsed green dot.
- **Cerrado (Closed)**: Background `#f1f5f9`, text `#475569`, border `1px solid #cbd5e1`, static neutral indicator.
- **Demorado / En Revisión**: Background `#fffbeb`, text `#b45309`, border `1px solid #fde68a`.

### Action Buttons
- **Primary**: Background `#0284c7`, text `#ffffff`, hover `#0369a1`. Sharp 4px corners, 40px height for desktop tables, 44px for field touchscreen interfaces.
- **Institutional Primary**: Background `#0f2a4a`, text `#ffffff`, hover `#1e3a5f`.
- **Secondary / Ghost**: Background `transparent`, border `1px solid #cbd5e1`, text `#1e293b`, hover `#f8fafc`.

### Data Tables
- **Header**: `#f8fafc`, uppercase 11px bold text (`#475569`), bottom border `1px solid #e2e8f0`.
- **Rows**: Alternating subtle hover states (`#f8fafc`), height `48px`, border-bottom `1px solid #f1f5f9`.
- **Numeric Cells**: Right-aligned, formatted with monospace feature flag.
- **Action Cells**: Pinned right column with icon triggers (Inspect, Print Act, Issue Receipt).

### Form Controls & Input Fields
- Structured input fields with explicit labels positioned above or modern floating labels.
- Borders: `1px solid #cbd5e1`, background `#ffffff`, focus ring `2px solid #0284c7` with 0px offset.
- Fieldsets: Bordered sections with legend headings for grouping vehicle data, driver CUIT/licence validation, and cargo manifest inputs.

### Administrative Summary Cards
- White surface, `1px solid #e2e8f0` border, `8px` corner radius.
- Contains metric title (12px uppercase), large metric count (24px bold `#0f172a`), and secondary trend text (12px).

### Civic Footer
- Structured three-column layout grounded in `#0f172a` or light neutral tone with deep navy borders.
- Displays legal disclaimers, system version build, connectivity status (Online / Sync Mode), and institutional logos for ATP, Gobierno del Chaco, and ECOM.

## Referencia

- Proyecto Stitch: https://stitch.withgoogle.com/projects/6019790973415672987
- Pantallas descargadas (HTML): `openspec/features/99-stitch-design/screens/` (comisiones, turnos, carga-manual, formularios-actas, control-cargas, login).
- Tokens estructurados: `openspec/features/99-stitch-design/tokens.json`.