import datetime

from kinetix.common import types_pb2 as _types_pb2
from kinetix.risk import risk_calculation_pb2 as _risk_calculation_pb2
from google.protobuf import timestamp_pb2 as _timestamp_pb2
from google.protobuf.internal import containers as _containers
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from collections.abc import Iterable as _Iterable, Mapping as _Mapping
from typing import ClassVar as _ClassVar, Optional as _Optional, Union as _Union

DESCRIPTOR: _descriptor.FileDescriptor

class StressTestRequest(_message.Message):
    __slots__ = ("book_id", "scenario_name", "calculation_type", "confidence_level", "time_horizon_days", "positions", "vol_shocks", "price_shocks", "description")
    class VolShocksEntry(_message.Message):
        __slots__ = ("key", "value")
        KEY_FIELD_NUMBER: _ClassVar[int]
        VALUE_FIELD_NUMBER: _ClassVar[int]
        key: str
        value: float
        def __init__(self, key: _Optional[str] = ..., value: _Optional[float] = ...) -> None: ...
    class PriceShocksEntry(_message.Message):
        __slots__ = ("key", "value")
        KEY_FIELD_NUMBER: _ClassVar[int]
        VALUE_FIELD_NUMBER: _ClassVar[int]
        key: str
        value: float
        def __init__(self, key: _Optional[str] = ..., value: _Optional[float] = ...) -> None: ...
    BOOK_ID_FIELD_NUMBER: _ClassVar[int]
    SCENARIO_NAME_FIELD_NUMBER: _ClassVar[int]
    CALCULATION_TYPE_FIELD_NUMBER: _ClassVar[int]
    CONFIDENCE_LEVEL_FIELD_NUMBER: _ClassVar[int]
    TIME_HORIZON_DAYS_FIELD_NUMBER: _ClassVar[int]
    POSITIONS_FIELD_NUMBER: _ClassVar[int]
    VOL_SHOCKS_FIELD_NUMBER: _ClassVar[int]
    PRICE_SHOCKS_FIELD_NUMBER: _ClassVar[int]
    DESCRIPTION_FIELD_NUMBER: _ClassVar[int]
    book_id: _types_pb2.BookId
    scenario_name: str
    calculation_type: _risk_calculation_pb2.RiskCalculationType
    confidence_level: _risk_calculation_pb2.ConfidenceLevel
    time_horizon_days: int
    positions: _containers.RepeatedCompositeFieldContainer[_types_pb2.Position]
    vol_shocks: _containers.ScalarMap[str, float]
    price_shocks: _containers.ScalarMap[str, float]
    description: str
    def __init__(self, book_id: _Optional[_Union[_types_pb2.BookId, _Mapping]] = ..., scenario_name: _Optional[str] = ..., calculation_type: _Optional[_Union[_risk_calculation_pb2.RiskCalculationType, str]] = ..., confidence_level: _Optional[_Union[_risk_calculation_pb2.ConfidenceLevel, str]] = ..., time_horizon_days: _Optional[int] = ..., positions: _Optional[_Iterable[_Union[_types_pb2.Position, _Mapping]]] = ..., vol_shocks: _Optional[_Mapping[str, float]] = ..., price_shocks: _Optional[_Mapping[str, float]] = ..., description: _Optional[str] = ...) -> None: ...

class AssetClassImpact(_message.Message):
    __slots__ = ("asset_class", "base_exposure", "stressed_exposure", "pnl_impact")
    ASSET_CLASS_FIELD_NUMBER: _ClassVar[int]
    BASE_EXPOSURE_FIELD_NUMBER: _ClassVar[int]
    STRESSED_EXPOSURE_FIELD_NUMBER: _ClassVar[int]
    PNL_IMPACT_FIELD_NUMBER: _ClassVar[int]
    asset_class: _types_pb2.AssetClass
    base_exposure: float
    stressed_exposure: float
    pnl_impact: float
    def __init__(self, asset_class: _Optional[_Union[_types_pb2.AssetClass, str]] = ..., base_exposure: _Optional[float] = ..., stressed_exposure: _Optional[float] = ..., pnl_impact: _Optional[float] = ...) -> None: ...

class PositionStressImpact(_message.Message):
    __slots__ = ("instrument_id", "asset_class", "base_market_value", "stressed_market_value", "pnl_impact", "percentage_of_total")
    INSTRUMENT_ID_FIELD_NUMBER: _ClassVar[int]
    ASSET_CLASS_FIELD_NUMBER: _ClassVar[int]
    BASE_MARKET_VALUE_FIELD_NUMBER: _ClassVar[int]
    STRESSED_MARKET_VALUE_FIELD_NUMBER: _ClassVar[int]
    PNL_IMPACT_FIELD_NUMBER: _ClassVar[int]
    PERCENTAGE_OF_TOTAL_FIELD_NUMBER: _ClassVar[int]
    instrument_id: str
    asset_class: _types_pb2.AssetClass
    base_market_value: float
    stressed_market_value: float
    pnl_impact: float
    percentage_of_total: float
    def __init__(self, instrument_id: _Optional[str] = ..., asset_class: _Optional[_Union[_types_pb2.AssetClass, str]] = ..., base_market_value: _Optional[float] = ..., stressed_market_value: _Optional[float] = ..., pnl_impact: _Optional[float] = ..., percentage_of_total: _Optional[float] = ...) -> None: ...

class StressTestResponse(_message.Message):
    __slots__ = ("scenario_name", "base_var", "stressed_var", "pnl_impact", "asset_class_impacts", "calculated_at", "position_impacts")
    SCENARIO_NAME_FIELD_NUMBER: _ClassVar[int]
    BASE_VAR_FIELD_NUMBER: _ClassVar[int]
    STRESSED_VAR_FIELD_NUMBER: _ClassVar[int]
    PNL_IMPACT_FIELD_NUMBER: _ClassVar[int]
    ASSET_CLASS_IMPACTS_FIELD_NUMBER: _ClassVar[int]
    CALCULATED_AT_FIELD_NUMBER: _ClassVar[int]
    POSITION_IMPACTS_FIELD_NUMBER: _ClassVar[int]
    scenario_name: str
    base_var: float
    stressed_var: float
    pnl_impact: float
    asset_class_impacts: _containers.RepeatedCompositeFieldContainer[AssetClassImpact]
    calculated_at: _timestamp_pb2.Timestamp
    position_impacts: _containers.RepeatedCompositeFieldContainer[PositionStressImpact]
    def __init__(self, scenario_name: _Optional[str] = ..., base_var: _Optional[float] = ..., stressed_var: _Optional[float] = ..., pnl_impact: _Optional[float] = ..., asset_class_impacts: _Optional[_Iterable[_Union[AssetClassImpact, _Mapping]]] = ..., calculated_at: _Optional[_Union[datetime.datetime, _timestamp_pb2.Timestamp, _Mapping]] = ..., position_impacts: _Optional[_Iterable[_Union[PositionStressImpact, _Mapping]]] = ...) -> None: ...

class ListScenariosRequest(_message.Message):
    __slots__ = ()
    def __init__(self) -> None: ...

class ListScenariosResponse(_message.Message):
    __slots__ = ("scenario_names",)
    SCENARIO_NAMES_FIELD_NUMBER: _ClassVar[int]
    scenario_names: _containers.RepeatedScalarFieldContainer[str]
    def __init__(self, scenario_names: _Optional[_Iterable[str]] = ...) -> None: ...

class GreeksRequest(_message.Message):
    __slots__ = ("book_id", "calculation_type", "confidence_level", "time_horizon_days", "positions")
    BOOK_ID_FIELD_NUMBER: _ClassVar[int]
    CALCULATION_TYPE_FIELD_NUMBER: _ClassVar[int]
    CONFIDENCE_LEVEL_FIELD_NUMBER: _ClassVar[int]
    TIME_HORIZON_DAYS_FIELD_NUMBER: _ClassVar[int]
    POSITIONS_FIELD_NUMBER: _ClassVar[int]
    book_id: _types_pb2.BookId
    calculation_type: _risk_calculation_pb2.RiskCalculationType
    confidence_level: _risk_calculation_pb2.ConfidenceLevel
    time_horizon_days: int
    positions: _containers.RepeatedCompositeFieldContainer[_types_pb2.Position]
    def __init__(self, book_id: _Optional[_Union[_types_pb2.BookId, _Mapping]] = ..., calculation_type: _Optional[_Union[_risk_calculation_pb2.RiskCalculationType, str]] = ..., confidence_level: _Optional[_Union[_risk_calculation_pb2.ConfidenceLevel, str]] = ..., time_horizon_days: _Optional[int] = ..., positions: _Optional[_Iterable[_Union[_types_pb2.Position, _Mapping]]] = ...) -> None: ...

class StressGreekValues(_message.Message):
    __slots__ = ("asset_class", "delta", "gamma", "vega")
    ASSET_CLASS_FIELD_NUMBER: _ClassVar[int]
    DELTA_FIELD_NUMBER: _ClassVar[int]
    GAMMA_FIELD_NUMBER: _ClassVar[int]
    VEGA_FIELD_NUMBER: _ClassVar[int]
    asset_class: _types_pb2.AssetClass
    delta: float
    gamma: float
    vega: float
    def __init__(self, asset_class: _Optional[_Union[_types_pb2.AssetClass, str]] = ..., delta: _Optional[float] = ..., gamma: _Optional[float] = ..., vega: _Optional[float] = ...) -> None: ...

class GreeksResponse(_message.Message):
    __slots__ = ("book_id", "asset_class_greeks", "theta", "rho", "calculated_at")
    BOOK_ID_FIELD_NUMBER: _ClassVar[int]
    ASSET_CLASS_GREEKS_FIELD_NUMBER: _ClassVar[int]
    THETA_FIELD_NUMBER: _ClassVar[int]
    RHO_FIELD_NUMBER: _ClassVar[int]
    CALCULATED_AT_FIELD_NUMBER: _ClassVar[int]
    book_id: str
    asset_class_greeks: _containers.RepeatedCompositeFieldContainer[StressGreekValues]
    theta: float
    rho: float
    calculated_at: _timestamp_pb2.Timestamp
    def __init__(self, book_id: _Optional[str] = ..., asset_class_greeks: _Optional[_Iterable[_Union[StressGreekValues, _Mapping]]] = ..., theta: _Optional[float] = ..., rho: _Optional[float] = ..., calculated_at: _Optional[_Union[datetime.datetime, _timestamp_pb2.Timestamp, _Mapping]] = ...) -> None: ...

class InstrumentDailyReturns(_message.Message):
    __slots__ = ("instrument_id", "daily_returns")
    INSTRUMENT_ID_FIELD_NUMBER: _ClassVar[int]
    DAILY_RETURNS_FIELD_NUMBER: _ClassVar[int]
    instrument_id: str
    daily_returns: _containers.RepeatedScalarFieldContainer[float]
    def __init__(self, instrument_id: _Optional[str] = ..., daily_returns: _Optional[_Iterable[float]] = ...) -> None: ...

class HistoricalReplayRequest(_message.Message):
    __slots__ = ("scenario_name", "positions", "instrument_returns", "window_start", "window_end")
    SCENARIO_NAME_FIELD_NUMBER: _ClassVar[int]
    POSITIONS_FIELD_NUMBER: _ClassVar[int]
    INSTRUMENT_RETURNS_FIELD_NUMBER: _ClassVar[int]
    WINDOW_START_FIELD_NUMBER: _ClassVar[int]
    WINDOW_END_FIELD_NUMBER: _ClassVar[int]
    scenario_name: str
    positions: _containers.RepeatedCompositeFieldContainer[_types_pb2.Position]
    instrument_returns: _containers.RepeatedCompositeFieldContainer[InstrumentDailyReturns]
    window_start: str
    window_end: str
    def __init__(self, scenario_name: _Optional[str] = ..., positions: _Optional[_Iterable[_Union[_types_pb2.Position, _Mapping]]] = ..., instrument_returns: _Optional[_Iterable[_Union[InstrumentDailyReturns, _Mapping]]] = ..., window_start: _Optional[str] = ..., window_end: _Optional[str] = ...) -> None: ...

class PositionReplayImpact(_message.Message):
    __slots__ = ("instrument_id", "asset_class", "market_value", "pnl_impact", "daily_pnl", "proxy_used")
    INSTRUMENT_ID_FIELD_NUMBER: _ClassVar[int]
    ASSET_CLASS_FIELD_NUMBER: _ClassVar[int]
    MARKET_VALUE_FIELD_NUMBER: _ClassVar[int]
    PNL_IMPACT_FIELD_NUMBER: _ClassVar[int]
    DAILY_PNL_FIELD_NUMBER: _ClassVar[int]
    PROXY_USED_FIELD_NUMBER: _ClassVar[int]
    instrument_id: str
    asset_class: _types_pb2.AssetClass
    market_value: float
    pnl_impact: float
    daily_pnl: _containers.RepeatedScalarFieldContainer[float]
    proxy_used: bool
    def __init__(self, instrument_id: _Optional[str] = ..., asset_class: _Optional[_Union[_types_pb2.AssetClass, str]] = ..., market_value: _Optional[float] = ..., pnl_impact: _Optional[float] = ..., daily_pnl: _Optional[_Iterable[float]] = ..., proxy_used: bool = ...) -> None: ...

class HistoricalReplayResponse(_message.Message):
    __slots__ = ("scenario_name", "total_pnl_impact", "position_impacts", "window_start", "window_end", "calculated_at")
    SCENARIO_NAME_FIELD_NUMBER: _ClassVar[int]
    TOTAL_PNL_IMPACT_FIELD_NUMBER: _ClassVar[int]
    POSITION_IMPACTS_FIELD_NUMBER: _ClassVar[int]
    WINDOW_START_FIELD_NUMBER: _ClassVar[int]
    WINDOW_END_FIELD_NUMBER: _ClassVar[int]
    CALCULATED_AT_FIELD_NUMBER: _ClassVar[int]
    scenario_name: str
    total_pnl_impact: float
    position_impacts: _containers.RepeatedCompositeFieldContainer[PositionReplayImpact]
    window_start: str
    window_end: str
    calculated_at: _timestamp_pb2.Timestamp
    def __init__(self, scenario_name: _Optional[str] = ..., total_pnl_impact: _Optional[float] = ..., position_impacts: _Optional[_Iterable[_Union[PositionReplayImpact, _Mapping]]] = ..., window_start: _Optional[str] = ..., window_end: _Optional[str] = ..., calculated_at: _Optional[_Union[datetime.datetime, _timestamp_pb2.Timestamp, _Mapping]] = ...) -> None: ...

class ReverseStressRequest(_message.Message):
    __slots__ = ("positions", "target_loss", "max_shock")
    POSITIONS_FIELD_NUMBER: _ClassVar[int]
    TARGET_LOSS_FIELD_NUMBER: _ClassVar[int]
    MAX_SHOCK_FIELD_NUMBER: _ClassVar[int]
    positions: _containers.RepeatedCompositeFieldContainer[_types_pb2.Position]
    target_loss: float
    max_shock: float
    def __init__(self, positions: _Optional[_Iterable[_Union[_types_pb2.Position, _Mapping]]] = ..., target_loss: _Optional[float] = ..., max_shock: _Optional[float] = ...) -> None: ...

class InstrumentShock(_message.Message):
    __slots__ = ("instrument_id", "shock")
    INSTRUMENT_ID_FIELD_NUMBER: _ClassVar[int]
    SHOCK_FIELD_NUMBER: _ClassVar[int]
    instrument_id: str
    shock: float
    def __init__(self, instrument_id: _Optional[str] = ..., shock: _Optional[float] = ...) -> None: ...

class ReverseStressResponse(_message.Message):
    __slots__ = ("shocks", "achieved_loss", "target_loss", "converged", "calculated_at")
    SHOCKS_FIELD_NUMBER: _ClassVar[int]
    ACHIEVED_LOSS_FIELD_NUMBER: _ClassVar[int]
    TARGET_LOSS_FIELD_NUMBER: _ClassVar[int]
    CONVERGED_FIELD_NUMBER: _ClassVar[int]
    CALCULATED_AT_FIELD_NUMBER: _ClassVar[int]
    shocks: _containers.RepeatedCompositeFieldContainer[InstrumentShock]
    achieved_loss: float
    target_loss: float
    converged: bool
    calculated_at: _timestamp_pb2.Timestamp
    def __init__(self, shocks: _Optional[_Iterable[_Union[InstrumentShock, _Mapping]]] = ..., achieved_loss: _Optional[float] = ..., target_loss: _Optional[float] = ..., converged: bool = ..., calculated_at: _Optional[_Union[datetime.datetime, _timestamp_pb2.Timestamp, _Mapping]] = ...) -> None: ...
