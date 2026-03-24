import datetime

from google.protobuf import timestamp_pb2 as _timestamp_pb2
from google.protobuf.internal import containers as _containers
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from collections.abc import Iterable as _Iterable, Mapping as _Mapping
from typing import ClassVar as _ClassVar, Optional as _Optional, Union as _Union

DESCRIPTOR: _descriptor.FileDescriptor

class ExposureProfile(_message.Message):
    __slots__ = ("tenor", "tenor_years", "expected_exposure", "pfe_95", "pfe_99")
    TENOR_FIELD_NUMBER: _ClassVar[int]
    TENOR_YEARS_FIELD_NUMBER: _ClassVar[int]
    EXPECTED_EXPOSURE_FIELD_NUMBER: _ClassVar[int]
    PFE_95_FIELD_NUMBER: _ClassVar[int]
    PFE_99_FIELD_NUMBER: _ClassVar[int]
    tenor: str
    tenor_years: float
    expected_exposure: float
    pfe_95: float
    pfe_99: float
    def __init__(self, tenor: _Optional[str] = ..., tenor_years: _Optional[float] = ..., expected_exposure: _Optional[float] = ..., pfe_95: _Optional[float] = ..., pfe_99: _Optional[float] = ...) -> None: ...

class NettingSetExposure(_message.Message):
    __slots__ = ("netting_set_id", "agreement_type", "gross_exposure", "net_exposure", "netting_benefit", "netting_benefit_pct", "position_count")
    NETTING_SET_ID_FIELD_NUMBER: _ClassVar[int]
    AGREEMENT_TYPE_FIELD_NUMBER: _ClassVar[int]
    GROSS_EXPOSURE_FIELD_NUMBER: _ClassVar[int]
    NET_EXPOSURE_FIELD_NUMBER: _ClassVar[int]
    NETTING_BENEFIT_FIELD_NUMBER: _ClassVar[int]
    NETTING_BENEFIT_PCT_FIELD_NUMBER: _ClassVar[int]
    POSITION_COUNT_FIELD_NUMBER: _ClassVar[int]
    netting_set_id: str
    agreement_type: str
    gross_exposure: float
    net_exposure: float
    netting_benefit: float
    netting_benefit_pct: float
    position_count: int
    def __init__(self, netting_set_id: _Optional[str] = ..., agreement_type: _Optional[str] = ..., gross_exposure: _Optional[float] = ..., net_exposure: _Optional[float] = ..., netting_benefit: _Optional[float] = ..., netting_benefit_pct: _Optional[float] = ..., position_count: _Optional[int] = ...) -> None: ...

class WrongWayRiskFlag(_message.Message):
    __slots__ = ("instrument_id", "counterparty_sector", "position_sector", "exposure", "message")
    INSTRUMENT_ID_FIELD_NUMBER: _ClassVar[int]
    COUNTERPARTY_SECTOR_FIELD_NUMBER: _ClassVar[int]
    POSITION_SECTOR_FIELD_NUMBER: _ClassVar[int]
    EXPOSURE_FIELD_NUMBER: _ClassVar[int]
    MESSAGE_FIELD_NUMBER: _ClassVar[int]
    instrument_id: str
    counterparty_sector: str
    position_sector: str
    exposure: float
    message: str
    def __init__(self, instrument_id: _Optional[str] = ..., counterparty_sector: _Optional[str] = ..., position_sector: _Optional[str] = ..., exposure: _Optional[float] = ..., message: _Optional[str] = ...) -> None: ...

class PFEPositionInput(_message.Message):
    __slots__ = ("instrument_id", "market_value", "asset_class", "volatility", "sector")
    INSTRUMENT_ID_FIELD_NUMBER: _ClassVar[int]
    MARKET_VALUE_FIELD_NUMBER: _ClassVar[int]
    ASSET_CLASS_FIELD_NUMBER: _ClassVar[int]
    VOLATILITY_FIELD_NUMBER: _ClassVar[int]
    SECTOR_FIELD_NUMBER: _ClassVar[int]
    instrument_id: str
    market_value: float
    asset_class: str
    volatility: float
    sector: str
    def __init__(self, instrument_id: _Optional[str] = ..., market_value: _Optional[float] = ..., asset_class: _Optional[str] = ..., volatility: _Optional[float] = ..., sector: _Optional[str] = ...) -> None: ...

class CalculatePFERequest(_message.Message):
    __slots__ = ("counterparty_id", "netting_set_id", "agreement_type", "positions", "num_simulations", "seed", "correlation_matrix")
    COUNTERPARTY_ID_FIELD_NUMBER: _ClassVar[int]
    NETTING_SET_ID_FIELD_NUMBER: _ClassVar[int]
    AGREEMENT_TYPE_FIELD_NUMBER: _ClassVar[int]
    POSITIONS_FIELD_NUMBER: _ClassVar[int]
    NUM_SIMULATIONS_FIELD_NUMBER: _ClassVar[int]
    SEED_FIELD_NUMBER: _ClassVar[int]
    CORRELATION_MATRIX_FIELD_NUMBER: _ClassVar[int]
    counterparty_id: str
    netting_set_id: str
    agreement_type: str
    positions: _containers.RepeatedCompositeFieldContainer[PFEPositionInput]
    num_simulations: int
    seed: int
    correlation_matrix: _containers.RepeatedScalarFieldContainer[float]
    def __init__(self, counterparty_id: _Optional[str] = ..., netting_set_id: _Optional[str] = ..., agreement_type: _Optional[str] = ..., positions: _Optional[_Iterable[_Union[PFEPositionInput, _Mapping]]] = ..., num_simulations: _Optional[int] = ..., seed: _Optional[int] = ..., correlation_matrix: _Optional[_Iterable[float]] = ...) -> None: ...

class CalculatePFEResponse(_message.Message):
    __slots__ = ("counterparty_id", "netting_set_id", "gross_exposure", "net_exposure", "exposure_profile", "calculated_at")
    COUNTERPARTY_ID_FIELD_NUMBER: _ClassVar[int]
    NETTING_SET_ID_FIELD_NUMBER: _ClassVar[int]
    GROSS_EXPOSURE_FIELD_NUMBER: _ClassVar[int]
    NET_EXPOSURE_FIELD_NUMBER: _ClassVar[int]
    EXPOSURE_PROFILE_FIELD_NUMBER: _ClassVar[int]
    CALCULATED_AT_FIELD_NUMBER: _ClassVar[int]
    counterparty_id: str
    netting_set_id: str
    gross_exposure: float
    net_exposure: float
    exposure_profile: _containers.RepeatedCompositeFieldContainer[ExposureProfile]
    calculated_at: _timestamp_pb2.Timestamp
    def __init__(self, counterparty_id: _Optional[str] = ..., netting_set_id: _Optional[str] = ..., gross_exposure: _Optional[float] = ..., net_exposure: _Optional[float] = ..., exposure_profile: _Optional[_Iterable[_Union[ExposureProfile, _Mapping]]] = ..., calculated_at: _Optional[_Union[datetime.datetime, _timestamp_pb2.Timestamp, _Mapping]] = ...) -> None: ...

class CalculateCVARequest(_message.Message):
    __slots__ = ("counterparty_id", "exposure_profile", "lgd", "pd_1y", "cds_spread_bps", "rating", "sector", "risk_free_rate")
    COUNTERPARTY_ID_FIELD_NUMBER: _ClassVar[int]
    EXPOSURE_PROFILE_FIELD_NUMBER: _ClassVar[int]
    LGD_FIELD_NUMBER: _ClassVar[int]
    PD_1Y_FIELD_NUMBER: _ClassVar[int]
    CDS_SPREAD_BPS_FIELD_NUMBER: _ClassVar[int]
    RATING_FIELD_NUMBER: _ClassVar[int]
    SECTOR_FIELD_NUMBER: _ClassVar[int]
    RISK_FREE_RATE_FIELD_NUMBER: _ClassVar[int]
    counterparty_id: str
    exposure_profile: _containers.RepeatedCompositeFieldContainer[ExposureProfile]
    lgd: float
    pd_1y: float
    cds_spread_bps: float
    rating: str
    sector: str
    risk_free_rate: float
    def __init__(self, counterparty_id: _Optional[str] = ..., exposure_profile: _Optional[_Iterable[_Union[ExposureProfile, _Mapping]]] = ..., lgd: _Optional[float] = ..., pd_1y: _Optional[float] = ..., cds_spread_bps: _Optional[float] = ..., rating: _Optional[str] = ..., sector: _Optional[str] = ..., risk_free_rate: _Optional[float] = ...) -> None: ...

class CalculateCVAResponse(_message.Message):
    __slots__ = ("counterparty_id", "cva", "is_estimated", "hazard_rate", "pd_1y", "calculated_at")
    COUNTERPARTY_ID_FIELD_NUMBER: _ClassVar[int]
    CVA_FIELD_NUMBER: _ClassVar[int]
    IS_ESTIMATED_FIELD_NUMBER: _ClassVar[int]
    HAZARD_RATE_FIELD_NUMBER: _ClassVar[int]
    PD_1Y_FIELD_NUMBER: _ClassVar[int]
    CALCULATED_AT_FIELD_NUMBER: _ClassVar[int]
    counterparty_id: str
    cva: float
    is_estimated: bool
    hazard_rate: float
    pd_1y: float
    calculated_at: _timestamp_pb2.Timestamp
    def __init__(self, counterparty_id: _Optional[str] = ..., cva: _Optional[float] = ..., is_estimated: bool = ..., hazard_rate: _Optional[float] = ..., pd_1y: _Optional[float] = ..., calculated_at: _Optional[_Union[datetime.datetime, _timestamp_pb2.Timestamp, _Mapping]] = ...) -> None: ...
