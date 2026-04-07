# ADR-0019: Official EOD/SOD Labeling with Promotion Governance

## Status
Accepted

## Context
Multiple VaR calculations run throughout the day (ad hoc, intraday, pre-close). Regulators and risk managers need to distinguish the "official" end-of-day (EOD) result from ad hoc runs. This official result feeds regulatory reporting and daily P&L attribution.

## Decision
Introduce `RunLabel` to classify each valuation run and `EodPromotionService` to govern the promotion of a completed run to "Official EOD" status.

**Run labels:** `ADHOC`, `SOD`, `INTRADAY`, `OVERNIGHT`, `PRE_CLOSE`, `OFFICIAL_EOD`, `SUPERSEDED_EOD`

**Promotion governance (`EodPromotionService`):**
- Only `COMPLETED` runs can be promoted
- A run cannot be promoted twice (`AlreadyPromoted` exception)
- The promoter cannot be the same person who triggered the run (four-eyes principle, `SelfPromotion` exception)
- Promoting a new run for the same book/date automatically supersedes the previous Official EOD
- Promotion emits an `OfficialEodPromotedEvent` (Kafka) and a `EodPromotedAuditEvent` (risk audit topic)
- Demotion is supported for corrections (`demoteFromOfficialEod`)
- Requires `PROMOTE_EOD_RUN` permission (granted to `RISK_MANAGER` and `ADMIN` roles)

## Consequences

### Positive
- Clear audit trail of which run was blessed as official and by whom
- Four-eyes principle prevents a single user from both running and approving a calculation
- Supersession logic ensures at most one Official EOD per book per date
- Events enable downstream systems (regulatory reporting, dashboards) to react to promotions

### Negative
- Adds operational ceremony — someone must explicitly promote the EOD run each day
- Supersession could surprise users if they promote without realising a prior EOD exists
- Demotion capability requires careful access control to prevent misuse

### Alternatives Considered
- **Automatic EOD**: The last run of the day is automatically marked as official. Simpler, but no human review — a bad late-day run would become the official result.
- **Time-window based**: Runs within a specific window (e.g., 16:00-16:30) are candidates. Too rigid — late market data or reruns would be excluded.
- **No official designation**: All runs are equal. Fails regulatory reporting requirements that demand a single authoritative daily result.

**Note (updated 2026-04-07):** Terminology updated to reflect the portfolio→book rename (V34). References to "portfolio" in the governance context now use "book".
