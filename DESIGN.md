---
name: Deep Focus Study System
colors:
  surface: '#f8f9fa'
  surface-dim: '#d9dadb'
  surface-bright: '#f8f9fa'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f3f4f5'
  surface-container: '#edeeef'
  surface-container-high: '#e7e8e9'
  surface-container-highest: '#e1e3e4'
  on-surface: '#191c1d'
  on-surface-variant: '#414846'
  inverse-surface: '#2e3132'
  inverse-on-surface: '#f0f1f2'
  outline: '#717976'
  outline-variant: '#c1c8c4'
  surface-tint: '#43655c'
  primary: '#01261f'
  on-primary: '#ffffff'
  primary-container: '#1a3c34'
  on-primary-container: '#83a69c'
  inverse-primary: '#aacec3'
  secondary: '#526600'
  on-secondary: '#ffffff'
  secondary-container: '#c8f323'
  on-secondary-container: '#576c00'
  tertiary: '#361812'
  on-tertiary: '#ffffff'
  tertiary-container: '#4f2d26'
  on-tertiary-container: '#c4948a'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#c5eadf'
  primary-fixed-dim: '#aacec3'
  on-primary-fixed: '#00201a'
  on-primary-fixed-variant: '#2b4d44'
  secondary-fixed: '#c8f323'
  secondary-fixed-dim: '#aed500'
  on-secondary-fixed: '#171e00'
  on-secondary-fixed-variant: '#3d4d00'
  tertiary-fixed: '#ffdad3'
  tertiary-fixed-dim: '#efbab0'
  on-tertiary-fixed: '#30130d'
  on-tertiary-fixed-variant: '#623d36'
  background: '#f8f9fa'
  on-background: '#191c1d'
  surface-variant: '#e1e3e4'
typography:
  h1:
    fontFamily: Manrope
    fontSize: 40px
    fontWeight: '700'
    lineHeight: '1.2'
    letterSpacing: -0.02em
  h2:
    fontFamily: Manrope
    fontSize: 32px
    fontWeight: '600'
    lineHeight: '1.3'
    letterSpacing: -0.01em
  h3:
    fontFamily: Manrope
    fontSize: 24px
    fontWeight: '600'
    lineHeight: '1.4'
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: '1.6'
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: '1.6'
  label-sm:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '600'
    lineHeight: '1'
    letterSpacing: 0.05em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  unit: 8px
  container-max: 1200px
  gutter: 24px
  margin-mobile: 16px
  margin-desktop: 48px
---

## Brand & Style

The design system is engineered for cognitive endurance and academic flow. It targets students and lifelong learners who require a digital environment that minimizes "visual noise" to maximize mental clarity. The brand personality is scholarly yet contemporary—reliable, quiet, and encouraging.

The aesthetic follows a **Minimalist-Modern** approach. It utilizes expansive whitespace (or "dark space") to separate disparate tasks, ensuring the user is never overwhelmed by a dense interface. While the structure is disciplined and grid-based, the use of soft shadows and generous corner radii prevents the UI from feeling cold or institutional, creating a welcoming digital "study nook."

## Colors

The palette is anchored by **Forest Deep**, a primary green chosen for its psychological associations with concentration and reduced eye strain. This color should be used for primary navigation, headers, and key interactive states.

To contrast this grounding base, the system employs two energetic accents:
- **Electric Lime:** Used exclusively for "Active" states and progress completion to provide a rewarding visual "pop."
- **Vivid Orange:** Reserved for timers, deadlines, and high-priority alerts.

The system supports a dual-mode strategy. In light mode, surfaces are off-white to reduce glare. In dark mode, the background shifts to a deep charcoal (rather than pure black) to maintain soft contrast ratios that facilitate long-reading sessions.

## Typography

This design system utilizes **Manrope** for headings to provide a modern, slightly geometric character that feels premium and structured. For all functional text, body copy, and inputs, **Inter** is used due to its exceptional tall x-height and readability at small scales.

The typographic scale emphasizes a clear hierarchy; headings are bold and tight, while body copy is given generous line height (1.6) to prevent line-tracking fatigue during long reading sessions. Labels use a slightly increased letter-spacing and uppercase styling to distinguish them clearly from interactive body text.

## Layout & Spacing

The layout philosophy is built on a **Fluid Grid** with a strict 8px spacing power-of-two scale. This ensures all elements align to a predictable rhythm. 

For the main dashboard, a 12-column grid is utilized with wide gutters (24px) to create "breathing room" between study widgets. Content-heavy views (like lesson readers) should transition to a constrained "Focus Width" of 720px, centered on the screen, to prevent excessive eye scanning. Padding within cards and containers should be generous (min 24px) to maintain the minimalist intent.

## Elevation & Depth

Visual hierarchy is conveyed through **Tonal Layers** supplemented by **Ambient Shadows**. 

- **Level 0 (Background):** The base canvas color.
- **Level 1 (Cards/Surface):** Elevated by a very soft, highly diffused shadow (Blur: 20px, Y: 4px, Opacity: 4% in light mode).
- **Level 2 (Active/Floating):** Used for modals or active study timers. These use a more pronounced shadow with a slight tint of the Primary color to ground them in the environment.

Transitions between elevations should be subtle; the design avoids heavy borders, preferring to use slight shifts in surface luminance to define boundaries.

## Shapes

The design system adopts a **Rounded** aesthetic. Standard components like buttons and input fields use a 0.5rem (8px) radius. Larger containers, such as study cards or progress modules, utilize `rounded-lg` (16px) or `rounded-xl` (24px) to create a friendly, organic feel. 

Progress bars and tags should use a fully pill-shaped radius to distinguish them as functional status indicators rather than structural containers.

## Components

- **Buttons:** Primary buttons are solid "Forest Deep" with white text. Secondary buttons use a ghost style with a 1px border. Interactions (hover/active) should be subtle shifts in color value, never abrupt.
- **Progress Bars:** These are high-visibility components. The track is a low-opacity version of the accent color, while the fill is "Electric Lime." For "Warning" states or "Time Remaining," use "Vivid Orange."
- **Cards:** Cards should have no border, using only the Level 1 elevation shadow to define their shape. They are the primary container for study sets and course modules.
- **Input Fields:** Use a subtle background fill (neutral-darker) rather than a heavy border. The focus state is indicated by a 2px "Forest Deep" bottom-border or ring.
- **Study Timer:** A specialized component featuring large-scale Manrope numbers. It should utilize the "Vivid Orange" accent when the timer is active to create a sense of focused urgency.
- **Chips/Tags:** Small, pill-shaped elements used for categorizing subjects. Use low-saturation background tints of the primary green to keep them secondary in the visual field.