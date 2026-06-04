# UIAutomator Session Metrics - 2026-06-01

Session folder: `.tmp/uiautomator-bridge/20260601-172758`

## Corpus

- Total XML frames collected: 430
- XML parse errors after hierarchy cleanup: 0
- Complete offer frames recognized by current core extractor: 133
- Unique complete offer fingerprints recognized by current core extractor: 41
- Non-card or incomplete frames: 297
- Complete-card frame rate in the session: 30.93%

## Raw UI Signals

- Frames with primary price text: 133
- Frames with pickup time/km line: 133
- Frames with raw trip time/km line: 131
- Frames with action button text: 145
- Frames with primary price but incomplete card text: 2

## Unique Offer Mix

- Product labels in selected complete offers: UberX dominant, Priority present.
- Action labels: `Aceitar` and `Selecionar`.
- Priority/bonus pattern present: `+R$ ... incluido para prioridade`.
- Main fare must ignore bonus lines and use the price above trip blocks.

## Geometry Patterns

- Card semantic top range: 724 to 1060 px.
- Card semantic bottom range: 1411 to 1600 px.
- Price to pickup gap: 102 to 174 px.
- Pickup to trip gap: 42 to 147 px.
- Trip to button gap: 70 to 226 px.
- Common x positions on 720 px screen: price/card left around 56 px, trip text around 112 px, button right around 664 px.

## Business Metrics From Unique Complete Offers

- Fare range: R$ 7.00 to R$ 38.59.
- Average fare: R$ 18.95.
- Total distance range: 2.2 km to 34.0 km.
- Average total distance: 13.8 km.
- Total time range: 8 to 61 minutes.
- Average total time: 27.2 minutes.
- R$/km range: 1.09 to 3.71.
- Average R$/km: 1.55.

## Engineering Interpretation

- The 430 XMLs are frame-level evidence; many are repeated frames or no-card/map frames.
- The first regression set used 39 selected complete card XMLs to avoid repeating identical frames.
- The full corpus is now also covered by unit tests with 430 XMLs, preserving the session-level metrics.
- The current extractor recognizes more than the quick PowerShell classifier because it can group fragmented lines semantically.
