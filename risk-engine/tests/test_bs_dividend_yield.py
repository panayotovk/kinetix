"""Tests for Black-Scholes Merton extension with continuous dividend yield."""

import math

import pytest
from scipy.stats import norm

from kinetix_risk.black_scholes import bs_delta, bs_gamma, bs_price, bs_vega
from kinetix_risk.models import OptionPosition, OptionType


def _call_with_div(q: float = 0.0) -> OptionPosition:
    return OptionPosition(
        instrument_id="TEST-C",
        underlying_id="TEST",
        option_type=OptionType.CALL,
        strike=100.0,
        expiry_days=365,
        spot_price=100.0,
        implied_vol=0.20,
        risk_free_rate=0.05,
        dividend_yield=q,
    )


def _put_with_div(q: float = 0.0) -> OptionPosition:
    return OptionPosition(
        instrument_id="TEST-P",
        underlying_id="TEST",
        option_type=OptionType.PUT,
        strike=100.0,
        expiry_days=365,
        spot_price=100.0,
        implied_vol=0.20,
        risk_free_rate=0.05,
        dividend_yield=q,
    )


@pytest.mark.unit
class TestBSMertonPrice:
    def test_zero_dividend_matches_standard_bs(self):
        """With q=0, Merton reduces to standard Black-Scholes."""
        call = _call_with_div(q=0.0)
        # Standard BS ATM call with S=100, K=100, T=1, r=0.05, vol=0.20
        d1 = (math.log(1.0) + (0.05 + 0.5 * 0.04) * 1.0) / 0.20
        d2 = d1 - 0.20
        expected = 100.0 * norm.cdf(d1) - 100.0 * math.exp(-0.05) * norm.cdf(d2)
        assert bs_price(call) == pytest.approx(expected, rel=1e-10)

    def test_dividend_reduces_call_price(self):
        """Higher dividend yield should reduce call option price."""
        call_no_div = bs_price(_call_with_div(q=0.0))
        call_with_div = bs_price(_call_with_div(q=0.03))
        assert call_with_div < call_no_div

    def test_dividend_increases_put_price(self):
        """Higher dividend yield should increase put option price."""
        put_no_div = bs_price(_put_with_div(q=0.0))
        put_with_div = bs_price(_put_with_div(q=0.03))
        assert put_with_div > put_no_div

    def test_known_merton_call_price(self):
        """Verify against known Merton call price for S=100, K=100, T=1, r=0.05, q=0.02, vol=0.20."""
        call = _call_with_div(q=0.02)
        r, q, T, vol = 0.05, 0.02, 1.0, 0.20
        d1 = (math.log(1.0) + (r - q + 0.5 * vol**2) * T) / (vol * math.sqrt(T))
        d2 = d1 - vol * math.sqrt(T)
        expected = 100.0 * math.exp(-q * T) * norm.cdf(d1) - 100.0 * math.exp(-r * T) * norm.cdf(d2)
        assert bs_price(call) == pytest.approx(expected, rel=1e-10)

    def test_put_call_parity_with_dividends(self):
        """C - P = S*exp(-qT) - K*exp(-rT) for Merton model."""
        q = 0.03
        call = bs_price(_call_with_div(q=q))
        put = bs_price(_put_with_div(q=q))
        S, K, r, T = 100.0, 100.0, 0.05, 1.0
        parity = S * math.exp(-q * T) - K * math.exp(-r * T)
        assert (call - put) == pytest.approx(parity, rel=1e-8)


@pytest.mark.unit
class TestBSMertonDelta:
    def test_zero_dividend_call_delta(self):
        call = _call_with_div(q=0.0)
        d1 = (math.log(1.0) + (0.05 + 0.5 * 0.04) * 1.0) / 0.20
        expected = float(norm.cdf(d1))
        assert bs_delta(call) == pytest.approx(expected, rel=1e-10)

    def test_dividend_reduces_call_delta(self):
        delta_no_div = bs_delta(_call_with_div(q=0.0))
        delta_with_div = bs_delta(_call_with_div(q=0.03))
        assert delta_with_div < delta_no_div

    def test_put_delta_is_negative(self):
        delta = bs_delta(_put_with_div(q=0.02))
        assert delta < 0


@pytest.mark.unit
class TestBSMertonGamma:
    def test_gamma_positive_with_dividend(self):
        gamma = bs_gamma(_call_with_div(q=0.02))
        assert gamma > 0

    def test_dividend_affects_gamma(self):
        gamma_no_div = bs_gamma(_call_with_div(q=0.0))
        gamma_with_div = bs_gamma(_call_with_div(q=0.03))
        # Both positive, but values differ
        assert gamma_no_div > 0
        assert gamma_with_div > 0
        assert gamma_no_div != pytest.approx(gamma_with_div, rel=1e-4)


@pytest.mark.unit
class TestBSMertonVega:
    def test_vega_positive_with_dividend(self):
        vega = bs_vega(_call_with_div(q=0.02))
        assert vega > 0
