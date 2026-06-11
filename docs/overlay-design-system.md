# CalcMot Overlay Design System

Status: implemented as native Jetpack Compose tokens and overlay components.

## Tokens

- Colors: `CalcMotColors.Bad`, `Warning`, `Good`, `Great`, translucent overlay surfaces and text colors.
- Opacity: `CalcMotOpacity` keeps the overlay between 82% and 90% perceived opacity.
- Typography: compact styles for the quality badge, primary value, secondary metrics and impact copy.
- Spacing, shape and elevation: centralized in `CalcMotSpacing`, `CalcMotShape` and `CalcMotElevation`.

## Overlay States

- `RUIM`: red accent and badge for offers below goals.
- `ATENÇÃO`: amber accent and badge for mixed offers.
- `BOA`: green accent and badge for offers inside goals.
- `ÓTIMA`: premium purple accent and badge for offers at least 20% above both km and hour goals.

The overlay does not depend only on color: every state has a visible text badge.

## Components

- `CalcMotOverlayContainer`: translucent dark surface, premium border, compact padding and drag handle.
- `OfferQualityBadge`: short state label with strong contrast.
- `MetricRow` and `OverlayMetricSummary`: R$/km first, then R$/hora and total time.
- `FinancialImpactBlock`: optional two-line plus block, shown only when the user enables impact on overlay.
- `GoalStatusPill`: compact positive/negative impact marker inside the impact block.
- `OverlayDragHandle`: subtle indicator that the overlay can be moved.

## QA Checklist

- Current capture pipeline, `OfferTreeExtractor` and overlay state machine are unchanged.
- Impact block is optional and never replaces R$/km, R$/hora or duration.
- `ÓTIMA` uses premium purple and is visually distinct from `BOA`.
- The overlay keeps a compact translucent surface so the Uber card remains visible.
- `OverlayManager` still logs `visibleLatencyMs`; field validation should keep it at or below 300 ms.
