"""Resolve typed positions into effective linear exposures for VaR.

Options need delta-adjusted exposure instead of raw premium (market_value),
because the option premium dramatically understates the actual directional risk.
Other position types pass through unchanged — their market_value is already
a reasonable linear exposure measure.
"""

from kinetix_risk.models import OptionPosition, PositionRisk


def resolve_positions(positions: list[PositionRisk]) -> list[PositionRisk]:
    """Convert typed positions into effective linear exposures for VaR."""
    resolved = []
    for pos in positions:
        if isinstance(pos, OptionPosition) and pos.spot_price > 0 and pos.implied_vol > 0:
            from kinetix_risk.black_scholes import bs_delta
            delta = bs_delta(pos)
            effective_mv = delta * pos.quantity * pos.spot_price * pos.contract_multiplier
            resolved.append(PositionRisk(
                instrument_id=pos.instrument_id,
                asset_class=pos.asset_class,
                market_value=effective_mv,
                currency=pos.currency,
            ))
        else:
            resolved.append(pos)
    return resolved
