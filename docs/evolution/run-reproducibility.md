# Run Reproducibility — Design Document

## Problem Statement

Regulators (FRTB MAR31.3, SR 11-7, PRA SS1/23, BCBS 239) require that any risk
calculation can be reproduced on demand — same inputs, same model, same result.
Today the platform records calculation outputs but does not capture immutable
snapshots of the inputs that produced them.

## Current Gaps

| Gap | Severity |
|-----|----------|
| Market data fetched live, not snapshotted per run | HIGH |
| Monte Carlo seed not passed through gRPC or stored | HIGH |
| Risk-engine model version not reported | HIGH |
| No SHA-256 input digest for tamper evidence | HIGH |
| Position snapshot missing quantities/costs | MEDIUM |
| Market data service retention is 2yr; regulatory need is 7yr | HIGH |

## Solution — RunManifest ("Tag with Pointers")

A **RunManifest** is a pointer document with a unique ID that links a valuation
job to every input used in the calculation. The manifest ID is stored on the
`ValuationJob`. Input data lives where the pointers point to.

```
ValuationJob ──manifestId──► RunManifest
                                ├── modelVersion (engine SHA, algorithm, params)
                                ├── monteCarloSeed (nullable)
                                ├── positionSnapshot (full positions in Postgres)
                                ├── marketDataSnapshots[] (content-addressable blobs)
                                │     ├── sha256 → spot prices
                                │     ├── sha256 → historical series
                                │     ├── sha256 → vol surface
                                │     ├── sha256 → yield curve
                                │     └── sha256 → correlation matrix
                                └── inputDigest (SHA-256 of all inputs combined)
```

### Content-Addressable Market Data Storage

Market data blobs are keyed by SHA-256 of their canonical JSON serialisation.
Two runs that use the same AAPL spot price ($183.47) produce the same hash and
share storage. This gives 15-30x deduplication for intraday runs.

Initial implementation stores blobs in PostgreSQL JSONB. A `MarketDataBlobStore`
interface allows migration to MinIO/S3 for production scale.

### Storage Estimates (with dedup)

- Per run raw: ~2 MB (positions + market data)
- With dedup: ~150 MB/day for 10 portfolios at 5-min intervals
- 7-year total: ~385 GB

## Implementation Plan

### Commit 1: Proto — add model_version and monte_carlo_seed
- Add `model_version` string to `ValuationResponse` (field 11)
- Add `monte_carlo_seed` int64 to `ValuationRequest` (field 9) and `ValuationResponse` (field 12)
- Backwards compatible — old clients ignore new fields

### Commit 2: Python risk-engine — populate model_version, wire seed
- `server.py`: read seed from request, pass through to MC calculations
- `converters.py`: populate `model_version` in response
- `portfolio_risk.py`: accept and forward seed parameter
- `valuation.py`: accept and forward seed parameter
- Add `__version__` module constant
- Dockerfile: inject `GIT_COMMIT_SHA` at build time

### Commit 3: Kotlin — read model_version and seed from gRPC
- `ValuationResultMapper`: read `modelVersion` and `monteCarloSeed` from proto
- `ValuationResult`: already has `modelVersion`, add `monteCarloSeed`
- `GrpcRiskEngineClient`: pass seed in request, read from response
- `VaRCalculationRequest`: add `monteCarloSeed` field

### Commit 4: Domain models
- `RunManifest` — the pointer document
- `MarketDataRef` — one entry per market data dependency
- `PositionSnapshotEntry` — one position in the snapshot
- `InputDigest` — utility for canonical hashing

### Commit 5: Persistence — V20 migration and repositories
- `run_manifests` table (7-year retention)
- `run_position_snapshots` table
- `run_market_data_blobs` table (content-addressable)
- `RunManifestRepository` interface + Exposed implementation
- `MarketDataBlobStore` interface + Postgres implementation
- Add `manifest_id` column to `valuation_jobs`

### Commit 6: Capture — wire manifest creation into calculation flow
- `RunManifestCapture` interface + `DefaultRunManifestCapture`
- Wire into `VaRCalculationService` after FETCH_MARKET_DATA step
- Seed generation for Monte Carlo runs
- `NoOpRunManifestCapture` for tests

### Commit 7: Replay — endpoint to reconstruct and re-run
- `ReplayService` — loads manifest, restores inputs, drives calculation
- `ReplayPositionProvider` — serves positions from snapshot
- Routes: `POST /api/v1/risk/runs/{jobId}/replay`
- Routes: `GET /api/v1/risk/runs/{jobId}/manifest`

### Commit 8: Audit integration
- Emit `RISK_RUN_MANIFEST_FROZEN` event into hash-chained audit trail
- Link EOD promotion events to manifest

## Regulatory Coverage

| Regulation | Requirement | How Addressed |
|------------|-------------|---------------|
| FRTB MAR31.3 | Verifiable and reproducible | Manifest + replay API |
| SR 11-7 | Input data quality evidence | Immutable position + market data snapshots |
| PRA SS1/23 | Records of inputs/outputs | 7-year manifest retention |
| BCBS 239 P2-P4 | Accurate, complete risk data | Content-addressable blobs with SHA-256 |
| EBA GL/2021/07 | Recalculation for any business day | Replay endpoint |
