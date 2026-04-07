# ADR-0017: Hash-Chained Audit Trail

## Status
Accepted

## Context
Financial regulations require an immutable, tamper-evident audit trail for all trade events. A simple append-only table can be modified after the fact by anyone with database access. We need a mechanism to detect unauthorized changes.

## Decision
Implement a hash-chained audit trail in the audit-service. Each audit event record includes a `record_hash` (SHA-256 of the event data concatenated with the previous record's hash) and a `previous_hash` (link to the prior record), forming a blockchain-like chain.

`AuditHasher` (`audit-service/.../persistence/AuditHasher.kt`) provides:
- `computeHash(event, previousHash)` — SHA-256 of all event fields (receivedAt, tradeId, bookId, instrumentId, assetClass, side, quantity, priceAmount, priceCurrency, tradedAt, userId, userRole, eventType) concatenated with the previous hash

**Note (updated 2026-04-07):** Field names updated to reflect the portfolio→book rename (V34). The field `portfolioId` was renamed to `bookId` in the hash input — existing chains use the new name.
- `verifyChain(events)` — validates the entire chain from genesis
- `verifyChainIncremental(events, startingPreviousHash)` — validates a segment of the chain, enabling pagination

A REST endpoint (`/api/v1/audit/verify`) allows on-demand chain verification.

## Consequences

### Positive
- Tamper detection: any modification to a historical record breaks the chain from that point forward
- Incremental verification supports large audit trails without loading the entire history
- SHA-256 is a well-understood, collision-resistant hash function
- Audit data is stored as VARCHAR (not typed columns) to preserve values exactly as received

### Negative
- Sequential hash chaining means audit events must be inserted in order — no parallel writes
- Verification is O(n) for the full chain; incremental mode mitigates this for routine checks
- Chain cannot self-heal — if a record is corrupted, verification fails permanently from that point

### Alternatives Considered
- **Append-only table with database constraints**: Simpler, but database administrators can still modify records without detection.
- **External blockchain (Hyperledger)**: Provides decentralised immutability, but adds significant infrastructure complexity for an internal audit trail.
- **WORM storage**: Write-once storage at the filesystem level. Effective but less granular — we need per-record verification, not per-file.
