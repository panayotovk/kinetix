# ADR-0018: Run Reproducibility via Manifests

## Status
Accepted

## Context
Risk managers and auditors need to understand exactly what inputs produced a given VaR number. Without a snapshot of inputs, a risk result cannot be independently verified or reproduced. Regulatory frameworks (FRTB, Basel) require model reproducibility.

## Decision
Capture a `RunManifest` for every risk calculation run. The manifest records all inputs and outputs needed to reproduce the result:

**Input capture (before calculation):**
- `jobId`, `bookId`, `valuationDate`
- `calculationType`, `confidenceLevel`, `timeHorizonDays`, `numSimulations`
- `monteCarloSeed` — deterministic seed for MC runs (0 = unseeded)
- `positionCount`, `positionDigest` — SHA-256 hash of serialized positions
- `marketDataDigest` — SHA-256 hash of all market data inputs
- `inputDigest` — combined hash of positions + market data + parameters

**Output capture (after calculation):**
- `modelVersion` — risk-engine version string (e.g., "0.1.0-abc1234")
- `varValue`, `expectedShortfall`
- `outputDigest` — SHA-256 hash of the full result
- `status` — `CAPTURED` → `COMPLETED` or `FAILED`

The `RunManifestCapture` service (`risk-orchestrator`) is integrated into `VaRCalculationService` as an optional collaborator, called between position fetch and valuation phases.

## Consequences

### Positive
- Any risk result can be independently verified by replaying the manifest's inputs through the same model version
- Input digests enable quick comparison: same inputs + same model = same outputs (deterministic MC with seed)
- Audit trail links a VaR number to the exact market data and positions used
- Manifest capture is optional (null-safe) — does not block calculation if capture fails

### Negative
- Additional storage for manifest records per run
- Digest computation adds latency (SHA-256 of serialized positions and market data)
- True reproducibility requires archiving the exact model version, not just the version string

### Alternatives Considered
- **Log-based reconstruction**: Parse logs to reconstruct inputs. Fragile, incomplete, and difficult to verify programmatically.
- **Full input archival**: Store the complete position and market data snapshots per run. More thorough but significantly more storage. Digests provide a compromise — verify sameness without storing duplicates.
- **No manifest**: Rely on the VaR result alone. Fails regulatory reproducibility requirements.
