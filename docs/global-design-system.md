# CalcMot Global Design System

Status: implemented for the current internal app shell.

## Implemented

- Global tokens in `ui/design/tokens`: colors, typography, spacing, shape, elevation, opacity, motion and icon constants.
- Global theme in `ui/design/theme`: `CalcMotTheme`, dark-first color scheme and light-ready scheme.
- Base components in `ui/design/components`: scaffold, top bar, cards, buttons, switch row, text fields, number fields, banners, status badges, empty state, divider and bottom action bar.
- Domain components in `ui/design/domain`: permission status, goal preset, daily summary, financial impact summary, offer history item, service health and beta feedback card.
- Compose preview in `ui/design/previews`.
- Existing `MetricaTheme` now delegates to the global `CalcMotTheme` so current app entry points and tests keep working.
- Home, onboarding, finance/metas and privacy screens now use the global design system components.

## Guardrails

- `AccessibilityService`, `OfferTreeExtractor`, overlay state machine and capture pipeline were not changed.
- OCR, ML Kit, screenshots and UIAutomator production runtime were not introduced.
- Overlay-specific design system remains separate and compatible with the global system.
- Financial copy avoids guaranteed-savings language and uses goals/impact wording.

## QA Checklist

- Home presents app status, monitoring, daily summary placeholder and primary driver-app action.
- Onboarding explains accessibility with simple language and direct CTA.
- Finance screen exposes goal presets, custom goals, financial impact toggle and cost settings.
- Privacy screen follows the same shell and card styling.
- Dark theme is default; light theme tokens are prepared for future work.
- Unit tests and full Gradle build pass with JDK 17.
