# ADR-0030: Contextual Filter Dropdowns

## Status
Accepted

## Context
The UI has several filter dropdowns (e.g. "All Types" for instrument types) that show a static list of all possible values regardless of what data is present. This creates noise — a rates-only book shows 13 instrument type options when only 2-3 are relevant — and includes legacy aliases (`FUTURES`, `COMMODITY`) that never match real data because the components use `Object.keys(INSTRUMENT_TYPE_COLORS)` instead of the curated `INSTRUMENT_TYPE_OPTIONS` list.

The `useBookSelector` hook already derives its options from live API data, establishing a precedent for data-driven filter options.

## Decision
Filter dropdowns for **data-driven types** (instrument types in PositionGrid and TradeBlotter) derive their options from the current dataset, showing only types that are actually present, with counts (e.g. "Cash Equity (42)").

**Closed enums** (scenario types in ScenarioLibraryGrid, trade sides BUY/SELL) remain static — they are small, fixed vocabularies where confirming "zero exist" has value.

**Creation forms** (WhatIfPanel instrument type selector) always show the full domain list, since the user may be creating something that does not yet exist in the dataset.

### Rules
1. Options are derived from the **unfiltered** dataset to avoid cascading filter confusion (selecting one filter must not cause options to disappear from another).
2. Options are sorted in **canonical domain order** (using `INSTRUMENT_TYPE_OPTIONS` from `instrumentTypes.ts`), not alphabetically or by count.
3. When a selected filter value disappears from the dataset (e.g. on book switch), the filter **auto-resets** to "All" with an inline notice.
4. The dropdown is **suppressed** when only one type exists in the dataset.

## Consequences

### Positive
- Eliminates dead options that can never produce results, reducing cognitive noise
- Counts provide at-a-glance distribution information without requiring a separate chart
- Fixes the existing bug where legacy aliases appeared as filter options
- Consistent with the `useBookSelector` pattern already in the codebase

### Negative
- Filter options now depend on data, introducing a stale-selection edge case that requires the auto-reset mechanism
- Slightly more complex component logic (a `useMemo` + `useEffect` per filterable component)

### Alternatives Considered
- **Shared `useFilterOptions` hook**: Rejected — the derivation is a single `useMemo` expression; a hook adds indirection without value.
- **Show unavailable types as greyed-out/disabled**: Rejected — disabled options in native `<select>` elements are poorly supported by screen readers, and showing types that don't exist implies a broader universe that isn't there.
- **Make ScenarioLibraryGrid contextual too**: Rejected — it's a closed 3-member enum where hiding an option prevents users from confirming "there are none of this type."
