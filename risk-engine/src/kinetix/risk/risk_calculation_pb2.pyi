import datetime

from kinetix.common import types_pb2 as _types_pb2
from google.protobuf import timestamp_pb2 as _timestamp_pb2
from google.protobuf.internal import containers as _containers
from google.protobuf.internal import enum_type_wrapper as _enum_type_wrapper
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from collections.abc import Iterable as _Iterable, Mapping as _Mapping
from typing import ClassVar as _ClassVar, Optional as _Optional, Union as _Union

DESCRIPTOR: _descriptor.FileDescriptor

class RiskCalculationType(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    RISK_CALCULATION_TYPE_UNSPECIFIED: _ClassVar[RiskCalculationType]
    HISTORICAL: _ClassVar[RiskCalculationType]
    PARAMETRIC: _ClassVar[RiskCalculationType]
    MONTE_CARLO: _ClassVar[RiskCalculationType]

class ConfidenceLevel(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    CONFIDENCE_LEVEL_UNSPECIFIED: _ClassVar[ConfidenceLevel]
    CL_95: _ClassVar[ConfidenceLevel]
    CL_99: _ClassVar[ConfidenceLevel]
    CL_975: _ClassVar[ConfidenceLevel]

class MarketDataType(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    MARKET_DATA_TYPE_UNSPECIFIED: _ClassVar[MarketDataType]
    SPOT_PRICE: _ClassVar[MarketDataType]
    HISTORICAL_PRICES: _ClassVar[MarketDataType]
    VOLATILITY_SURFACE: _ClassVar[MarketDataType]
    YIELD_CURVE: _ClassVar[MarketDataType]
    RISK_FREE_RATE: _ClassVar[MarketDataType]
    DIVIDEND_YIELD: _ClassVar[MarketDataType]
    CREDIT_SPREAD: _ClassVar[MarketDataType]
    FORWARD_CURVE: _ClassVar[MarketDataType]
    CORRELATION_MATRIX: _ClassVar[MarketDataType]

class ValuationOutput(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    VALUATION_OUTPUT_UNSPECIFIED: _ClassVar[ValuationOutput]
    VAR: _ClassVar[ValuationOutput]
    EXPECTED_SHORTFALL: _ClassVar[ValuationOutput]
    GREEKS: _ClassVar[ValuationOutput]
    PV: _ClassVar[ValuationOutput]

class FactorType(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    FACTOR_TYPE_UNSPECIFIED: _ClassVar[FactorType]
    FACTOR_EQUITY_BETA: _ClassVar[FactorType]
    FACTOR_RATES_DURATION: _ClassVar[FactorType]
    FACTOR_CREDIT_SPREAD: _ClassVar[FactorType]
    FACTOR_FX_DELTA: _ClassVar[FactorType]
    FACTOR_VOL_EXPOSURE: _ClassVar[FactorType]

class LoadingMethod(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    LOADING_METHOD_UNSPECIFIED: _ClassVar[LoadingMethod]
    LOADING_OLS_REGRESSION: _ClassVar[LoadingMethod]
    LOADING_ANALYTICAL: _ClassVar[LoadingMethod]
    LOADING_MANUAL: _ClassVar[LoadingMethod]
RISK_CALCULATION_TYPE_UNSPECIFIED: RiskCalculationType
HISTORICAL: RiskCalculationType
PARAMETRIC: RiskCalculationType
MONTE_CARLO: RiskCalculationType
CONFIDENCE_LEVEL_UNSPECIFIED: ConfidenceLevel
CL_95: ConfidenceLevel
CL_99: ConfidenceLevel
CL_975: ConfidenceLevel
MARKET_DATA_TYPE_UNSPECIFIED: MarketDataType
SPOT_PRICE: MarketDataType
HISTORICAL_PRICES: MarketDataType
VOLATILITY_SURFACE: MarketDataType
YIELD_CURVE: MarketDataType
RISK_FREE_RATE: MarketDataType
DIVIDEND_YIELD: MarketDataType
CREDIT_SPREAD: MarketDataType
FORWARD_CURVE: MarketDataType
CORRELATION_MATRIX: MarketDataType
VALUATION_OUTPUT_UNSPECIFIED: ValuationOutput
VAR: ValuationOutput
EXPECTED_SHORTFALL: ValuationOutput
GREEKS: ValuationOutput
PV: ValuationOutput
FACTOR_TYPE_UNSPECIFIED: FactorType
FACTOR_EQUITY_BETA: FactorType
FACTOR_RATES_DURATION: FactorType
FACTOR_CREDIT_SPREAD: FactorType
FACTOR_FX_DELTA: FactorType
FACTOR_VOL_EXPOSURE: FactorType
LOADING_METHOD_UNSPECIFIED: LoadingMethod
LOADING_OLS_REGRESSION: LoadingMethod
LOADING_ANALYTICAL: LoadingMethod
LOADING_MANUAL: LoadingMethod

class TimeSeriesPoint(_message.Message):
    __slots__ = ("timestamp", "value")
    TIMESTAMP_FIELD_NUMBER: _ClassVar[int]
    VALUE_FIELD_NUMBER: _ClassVar[int]
    timestamp: _timestamp_pb2.Timestamp
    value: float
    def __init__(self, timestamp: _Optional[_Union[datetime.datetime, _timestamp_pb2.Timestamp, _Mapping]] = ..., value: _Optional[float] = ...) -> None: ...

class TimeSeries(_message.Message):
    __slots__ = ("points",)
    POINTS_FIELD_NUMBER: _ClassVar[int]
    points: _containers.RepeatedCompositeFieldContainer[TimeSeriesPoint]
    def __init__(self, points: _Optional[_Iterable[_Union[TimeSeriesPoint, _Mapping]]] = ...) -> None: ...

class Matrix(_message.Message):
    __slots__ = ("rows", "cols", "values", "labels")
    ROWS_FIELD_NUMBER: _ClassVar[int]
    COLS_FIELD_NUMBER: _ClassVar[int]
    VALUES_FIELD_NUMBER: _ClassVar[int]
    LABELS_FIELD_NUMBER: _ClassVar[int]
    rows: int
    cols: int
    values: _containers.RepeatedScalarFieldContainer[float]
    labels: _containers.RepeatedScalarFieldContainer[str]
    def __init__(self, rows: _Optional[int] = ..., cols: _Optional[int] = ..., values: _Optional[_Iterable[float]] = ..., labels: _Optional[_Iterable[str]] = ...) -> None: ...

class CurvePoint(_message.Message):
    __slots__ = ("tenor", "value")
    TENOR_FIELD_NUMBER: _ClassVar[int]
    VALUE_FIELD_NUMBER: _ClassVar[int]
    tenor: str
    value: float
    def __init__(self, tenor: _Optional[str] = ..., value: _Optional[float] = ...) -> None: ...

class Curve(_message.Message):
    __slots__ = ("points",)
    POINTS_FIELD_NUMBER: _ClassVar[int]
    points: _containers.RepeatedCompositeFieldContainer[CurvePoint]
    def __init__(self, points: _Optional[_Iterable[_Union[CurvePoint, _Mapping]]] = ...) -> None: ...

class MarketDataValue(_message.Message):
    __slots__ = ("data_type", "instrument_id", "asset_class", "scalar", "time_series", "matrix", "curve")
    DATA_TYPE_FIELD_NUMBER: _ClassVar[int]
    INSTRUMENT_ID_FIELD_NUMBER: _ClassVar[int]
    ASSET_CLASS_FIELD_NUMBER: _ClassVar[int]
    SCALAR_FIELD_NUMBER: _ClassVar[int]
    TIME_SERIES_FIELD_NUMBER: _ClassVar[int]
    MATRIX_FIELD_NUMBER: _ClassVar[int]
    CURVE_FIELD_NUMBER: _ClassVar[int]
    data_type: MarketDataType
    instrument_id: str
    asset_class: str
    scalar: float
    time_series: TimeSeries
    matrix: Matrix
    curve: Curve
    def __init__(self, data_type: _Optional[_Union[MarketDataType, str]] = ..., instrument_id: _Optional[str] = ..., asset_class: _Optional[str] = ..., scalar: _Optional[float] = ..., time_series: _Optional[_Union[TimeSeries, _Mapping]] = ..., matrix: _Optional[_Union[Matrix, _Mapping]] = ..., curve: _Optional[_Union[Curve, _Mapping]] = ...) -> None: ...

class VaRRequest(_message.Message):
    __slots__ = ("book_id", "calculation_type", "confidence_level", "time_horizon_days", "num_simulations", "positions", "market_data")
    BOOK_ID_FIELD_NUMBER: _ClassVar[int]
    CALCULATION_TYPE_FIELD_NUMBER: _ClassVar[int]
    CONFIDENCE_LEVEL_FIELD_NUMBER: _ClassVar[int]
    TIME_HORIZON_DAYS_FIELD_NUMBER: _ClassVar[int]
    NUM_SIMULATIONS_FIELD_NUMBER: _ClassVar[int]
    POSITIONS_FIELD_NUMBER: _ClassVar[int]
    MARKET_DATA_FIELD_NUMBER: _ClassVar[int]
    book_id: _types_pb2.BookId
    calculation_type: RiskCalculationType
    confidence_level: ConfidenceLevel
    time_horizon_days: int
    num_simulations: int
    positions: _containers.RepeatedCompositeFieldContainer[_types_pb2.Position]
    market_data: _containers.RepeatedCompositeFieldContainer[MarketDataValue]
    def __init__(self, book_id: _Optional[_Union[_types_pb2.BookId, _Mapping]] = ..., calculation_type: _Optional[_Union[RiskCalculationType, str]] = ..., confidence_level: _Optional[_Union[ConfidenceLevel, str]] = ..., time_horizon_days: _Optional[int] = ..., num_simulations: _Optional[int] = ..., positions: _Optional[_Iterable[_Union[_types_pb2.Position, _Mapping]]] = ..., market_data: _Optional[_Iterable[_Union[MarketDataValue, _Mapping]]] = ...) -> None: ...

class VaRResponse(_message.Message):
    __slots__ = ("book_id", "calculation_type", "confidence_level", "var_value", "expected_shortfall", "component_breakdown", "calculated_at")
    BOOK_ID_FIELD_NUMBER: _ClassVar[int]
    CALCULATION_TYPE_FIELD_NUMBER: _ClassVar[int]
    CONFIDENCE_LEVEL_FIELD_NUMBER: _ClassVar[int]
    VAR_VALUE_FIELD_NUMBER: _ClassVar[int]
    EXPECTED_SHORTFALL_FIELD_NUMBER: _ClassVar[int]
    COMPONENT_BREAKDOWN_FIELD_NUMBER: _ClassVar[int]
    CALCULATED_AT_FIELD_NUMBER: _ClassVar[int]
    book_id: _types_pb2.BookId
    calculation_type: RiskCalculationType
    confidence_level: ConfidenceLevel
    var_value: float
    expected_shortfall: float
    component_breakdown: _containers.RepeatedCompositeFieldContainer[VaRComponentBreakdown]
    calculated_at: _timestamp_pb2.Timestamp
    def __init__(self, book_id: _Optional[_Union[_types_pb2.BookId, _Mapping]] = ..., calculation_type: _Optional[_Union[RiskCalculationType, str]] = ..., confidence_level: _Optional[_Union[ConfidenceLevel, str]] = ..., var_value: _Optional[float] = ..., expected_shortfall: _Optional[float] = ..., component_breakdown: _Optional[_Iterable[_Union[VaRComponentBreakdown, _Mapping]]] = ..., calculated_at: _Optional[_Union[datetime.datetime, _timestamp_pb2.Timestamp, _Mapping]] = ...) -> None: ...

class VaRComponentBreakdown(_message.Message):
    __slots__ = ("asset_class", "var_contribution", "percentage_of_total")
    ASSET_CLASS_FIELD_NUMBER: _ClassVar[int]
    VAR_CONTRIBUTION_FIELD_NUMBER: _ClassVar[int]
    PERCENTAGE_OF_TOTAL_FIELD_NUMBER: _ClassVar[int]
    asset_class: _types_pb2.AssetClass
    var_contribution: float
    percentage_of_total: float
    def __init__(self, asset_class: _Optional[_Union[_types_pb2.AssetClass, str]] = ..., var_contribution: _Optional[float] = ..., percentage_of_total: _Optional[float] = ...) -> None: ...

class ValuationRequest(_message.Message):
    __slots__ = ("book_id", "calculation_type", "confidence_level", "time_horizon_days", "num_simulations", "positions", "market_data", "requested_outputs", "monte_carlo_seed")
    BOOK_ID_FIELD_NUMBER: _ClassVar[int]
    CALCULATION_TYPE_FIELD_NUMBER: _ClassVar[int]
    CONFIDENCE_LEVEL_FIELD_NUMBER: _ClassVar[int]
    TIME_HORIZON_DAYS_FIELD_NUMBER: _ClassVar[int]
    NUM_SIMULATIONS_FIELD_NUMBER: _ClassVar[int]
    POSITIONS_FIELD_NUMBER: _ClassVar[int]
    MARKET_DATA_FIELD_NUMBER: _ClassVar[int]
    REQUESTED_OUTPUTS_FIELD_NUMBER: _ClassVar[int]
    MONTE_CARLO_SEED_FIELD_NUMBER: _ClassVar[int]
    book_id: _types_pb2.BookId
    calculation_type: RiskCalculationType
    confidence_level: ConfidenceLevel
    time_horizon_days: int
    num_simulations: int
    positions: _containers.RepeatedCompositeFieldContainer[_types_pb2.Position]
    market_data: _containers.RepeatedCompositeFieldContainer[MarketDataValue]
    requested_outputs: _containers.RepeatedScalarFieldContainer[ValuationOutput]
    monte_carlo_seed: int
    def __init__(self, book_id: _Optional[_Union[_types_pb2.BookId, _Mapping]] = ..., calculation_type: _Optional[_Union[RiskCalculationType, str]] = ..., confidence_level: _Optional[_Union[ConfidenceLevel, str]] = ..., time_horizon_days: _Optional[int] = ..., num_simulations: _Optional[int] = ..., positions: _Optional[_Iterable[_Union[_types_pb2.Position, _Mapping]]] = ..., market_data: _Optional[_Iterable[_Union[MarketDataValue, _Mapping]]] = ..., requested_outputs: _Optional[_Iterable[_Union[ValuationOutput, str]]] = ..., monte_carlo_seed: _Optional[int] = ...) -> None: ...

class GreeksSummary(_message.Message):
    __slots__ = ("asset_class_greeks", "theta", "rho")
    ASSET_CLASS_GREEKS_FIELD_NUMBER: _ClassVar[int]
    THETA_FIELD_NUMBER: _ClassVar[int]
    RHO_FIELD_NUMBER: _ClassVar[int]
    asset_class_greeks: _containers.RepeatedCompositeFieldContainer[GreekValues]
    theta: float
    rho: float
    def __init__(self, asset_class_greeks: _Optional[_Iterable[_Union[GreekValues, _Mapping]]] = ..., theta: _Optional[float] = ..., rho: _Optional[float] = ...) -> None: ...

class GreekValues(_message.Message):
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

class ValuationResponse(_message.Message):
    __slots__ = ("book_id", "calculation_type", "confidence_level", "var_value", "expected_shortfall", "component_breakdown", "calculated_at", "greeks", "computed_outputs", "pv_value", "model_version", "monte_carlo_seed")
    BOOK_ID_FIELD_NUMBER: _ClassVar[int]
    CALCULATION_TYPE_FIELD_NUMBER: _ClassVar[int]
    CONFIDENCE_LEVEL_FIELD_NUMBER: _ClassVar[int]
    VAR_VALUE_FIELD_NUMBER: _ClassVar[int]
    EXPECTED_SHORTFALL_FIELD_NUMBER: _ClassVar[int]
    COMPONENT_BREAKDOWN_FIELD_NUMBER: _ClassVar[int]
    CALCULATED_AT_FIELD_NUMBER: _ClassVar[int]
    GREEKS_FIELD_NUMBER: _ClassVar[int]
    COMPUTED_OUTPUTS_FIELD_NUMBER: _ClassVar[int]
    PV_VALUE_FIELD_NUMBER: _ClassVar[int]
    MODEL_VERSION_FIELD_NUMBER: _ClassVar[int]
    MONTE_CARLO_SEED_FIELD_NUMBER: _ClassVar[int]
    book_id: _types_pb2.BookId
    calculation_type: RiskCalculationType
    confidence_level: ConfidenceLevel
    var_value: float
    expected_shortfall: float
    component_breakdown: _containers.RepeatedCompositeFieldContainer[VaRComponentBreakdown]
    calculated_at: _timestamp_pb2.Timestamp
    greeks: GreeksSummary
    computed_outputs: _containers.RepeatedScalarFieldContainer[ValuationOutput]
    pv_value: float
    model_version: str
    monte_carlo_seed: int
    def __init__(self, book_id: _Optional[_Union[_types_pb2.BookId, _Mapping]] = ..., calculation_type: _Optional[_Union[RiskCalculationType, str]] = ..., confidence_level: _Optional[_Union[ConfidenceLevel, str]] = ..., var_value: _Optional[float] = ..., expected_shortfall: _Optional[float] = ..., component_breakdown: _Optional[_Iterable[_Union[VaRComponentBreakdown, _Mapping]]] = ..., calculated_at: _Optional[_Union[datetime.datetime, _timestamp_pb2.Timestamp, _Mapping]] = ..., greeks: _Optional[_Union[GreeksSummary, _Mapping]] = ..., computed_outputs: _Optional[_Iterable[_Union[ValuationOutput, str]]] = ..., pv_value: _Optional[float] = ..., model_version: _Optional[str] = ..., monte_carlo_seed: _Optional[int] = ...) -> None: ...

class CrossBookVaRRequest(_message.Message):
    __slots__ = ("book_ids", "calculation_type", "confidence_level", "time_horizon_days", "num_simulations", "positions", "market_data", "requested_outputs", "monte_carlo_seed", "portfolio_group_id")
    BOOK_IDS_FIELD_NUMBER: _ClassVar[int]
    CALCULATION_TYPE_FIELD_NUMBER: _ClassVar[int]
    CONFIDENCE_LEVEL_FIELD_NUMBER: _ClassVar[int]
    TIME_HORIZON_DAYS_FIELD_NUMBER: _ClassVar[int]
    NUM_SIMULATIONS_FIELD_NUMBER: _ClassVar[int]
    POSITIONS_FIELD_NUMBER: _ClassVar[int]
    MARKET_DATA_FIELD_NUMBER: _ClassVar[int]
    REQUESTED_OUTPUTS_FIELD_NUMBER: _ClassVar[int]
    MONTE_CARLO_SEED_FIELD_NUMBER: _ClassVar[int]
    PORTFOLIO_GROUP_ID_FIELD_NUMBER: _ClassVar[int]
    book_ids: _containers.RepeatedCompositeFieldContainer[_types_pb2.BookId]
    calculation_type: RiskCalculationType
    confidence_level: ConfidenceLevel
    time_horizon_days: int
    num_simulations: int
    positions: _containers.RepeatedCompositeFieldContainer[_types_pb2.Position]
    market_data: _containers.RepeatedCompositeFieldContainer[MarketDataValue]
    requested_outputs: _containers.RepeatedScalarFieldContainer[ValuationOutput]
    monte_carlo_seed: int
    portfolio_group_id: str
    def __init__(self, book_ids: _Optional[_Iterable[_Union[_types_pb2.BookId, _Mapping]]] = ..., calculation_type: _Optional[_Union[RiskCalculationType, str]] = ..., confidence_level: _Optional[_Union[ConfidenceLevel, str]] = ..., time_horizon_days: _Optional[int] = ..., num_simulations: _Optional[int] = ..., positions: _Optional[_Iterable[_Union[_types_pb2.Position, _Mapping]]] = ..., market_data: _Optional[_Iterable[_Union[MarketDataValue, _Mapping]]] = ..., requested_outputs: _Optional[_Iterable[_Union[ValuationOutput, str]]] = ..., monte_carlo_seed: _Optional[int] = ..., portfolio_group_id: _Optional[str] = ...) -> None: ...

class BookVaRContribution(_message.Message):
    __slots__ = ("book_id", "var_contribution", "percentage_of_total", "standalone_var", "diversification_benefit", "marginal_var", "incremental_var")
    BOOK_ID_FIELD_NUMBER: _ClassVar[int]
    VAR_CONTRIBUTION_FIELD_NUMBER: _ClassVar[int]
    PERCENTAGE_OF_TOTAL_FIELD_NUMBER: _ClassVar[int]
    STANDALONE_VAR_FIELD_NUMBER: _ClassVar[int]
    DIVERSIFICATION_BENEFIT_FIELD_NUMBER: _ClassVar[int]
    MARGINAL_VAR_FIELD_NUMBER: _ClassVar[int]
    INCREMENTAL_VAR_FIELD_NUMBER: _ClassVar[int]
    book_id: _types_pb2.BookId
    var_contribution: float
    percentage_of_total: float
    standalone_var: float
    diversification_benefit: float
    marginal_var: float
    incremental_var: float
    def __init__(self, book_id: _Optional[_Union[_types_pb2.BookId, _Mapping]] = ..., var_contribution: _Optional[float] = ..., percentage_of_total: _Optional[float] = ..., standalone_var: _Optional[float] = ..., diversification_benefit: _Optional[float] = ..., marginal_var: _Optional[float] = ..., incremental_var: _Optional[float] = ...) -> None: ...

class CrossBookVaRResponse(_message.Message):
    __slots__ = ("portfolio_group_id", "book_ids", "calculation_type", "confidence_level", "var_value", "expected_shortfall", "component_breakdown", "book_contributions", "total_standalone_var", "diversification_benefit", "calculated_at", "model_version", "monte_carlo_seed")
    PORTFOLIO_GROUP_ID_FIELD_NUMBER: _ClassVar[int]
    BOOK_IDS_FIELD_NUMBER: _ClassVar[int]
    CALCULATION_TYPE_FIELD_NUMBER: _ClassVar[int]
    CONFIDENCE_LEVEL_FIELD_NUMBER: _ClassVar[int]
    VAR_VALUE_FIELD_NUMBER: _ClassVar[int]
    EXPECTED_SHORTFALL_FIELD_NUMBER: _ClassVar[int]
    COMPONENT_BREAKDOWN_FIELD_NUMBER: _ClassVar[int]
    BOOK_CONTRIBUTIONS_FIELD_NUMBER: _ClassVar[int]
    TOTAL_STANDALONE_VAR_FIELD_NUMBER: _ClassVar[int]
    DIVERSIFICATION_BENEFIT_FIELD_NUMBER: _ClassVar[int]
    CALCULATED_AT_FIELD_NUMBER: _ClassVar[int]
    MODEL_VERSION_FIELD_NUMBER: _ClassVar[int]
    MONTE_CARLO_SEED_FIELD_NUMBER: _ClassVar[int]
    portfolio_group_id: str
    book_ids: _containers.RepeatedCompositeFieldContainer[_types_pb2.BookId]
    calculation_type: RiskCalculationType
    confidence_level: ConfidenceLevel
    var_value: float
    expected_shortfall: float
    component_breakdown: _containers.RepeatedCompositeFieldContainer[VaRComponentBreakdown]
    book_contributions: _containers.RepeatedCompositeFieldContainer[BookVaRContribution]
    total_standalone_var: float
    diversification_benefit: float
    calculated_at: _timestamp_pb2.Timestamp
    model_version: str
    monte_carlo_seed: int
    def __init__(self, portfolio_group_id: _Optional[str] = ..., book_ids: _Optional[_Iterable[_Union[_types_pb2.BookId, _Mapping]]] = ..., calculation_type: _Optional[_Union[RiskCalculationType, str]] = ..., confidence_level: _Optional[_Union[ConfidenceLevel, str]] = ..., var_value: _Optional[float] = ..., expected_shortfall: _Optional[float] = ..., component_breakdown: _Optional[_Iterable[_Union[VaRComponentBreakdown, _Mapping]]] = ..., book_contributions: _Optional[_Iterable[_Union[BookVaRContribution, _Mapping]]] = ..., total_standalone_var: _Optional[float] = ..., diversification_benefit: _Optional[float] = ..., calculated_at: _Optional[_Union[datetime.datetime, _timestamp_pb2.Timestamp, _Mapping]] = ..., model_version: _Optional[str] = ..., monte_carlo_seed: _Optional[int] = ...) -> None: ...

class PositionLoadingInput(_message.Message):
    __slots__ = ("instrument_id", "asset_class", "market_value", "instrument_returns")
    INSTRUMENT_ID_FIELD_NUMBER: _ClassVar[int]
    ASSET_CLASS_FIELD_NUMBER: _ClassVar[int]
    MARKET_VALUE_FIELD_NUMBER: _ClassVar[int]
    INSTRUMENT_RETURNS_FIELD_NUMBER: _ClassVar[int]
    instrument_id: str
    asset_class: str
    market_value: float
    instrument_returns: _containers.RepeatedScalarFieldContainer[float]
    def __init__(self, instrument_id: _Optional[str] = ..., asset_class: _Optional[str] = ..., market_value: _Optional[float] = ..., instrument_returns: _Optional[_Iterable[float]] = ...) -> None: ...

class FactorReturnSeries(_message.Message):
    __slots__ = ("factor", "returns")
    FACTOR_FIELD_NUMBER: _ClassVar[int]
    RETURNS_FIELD_NUMBER: _ClassVar[int]
    factor: FactorType
    returns: _containers.RepeatedScalarFieldContainer[float]
    def __init__(self, factor: _Optional[_Union[FactorType, str]] = ..., returns: _Optional[_Iterable[float]] = ...) -> None: ...

class FactorDecompositionRequest(_message.Message):
    __slots__ = ("book_id", "positions", "factor_returns", "total_var", "decomposition_date", "job_id")
    BOOK_ID_FIELD_NUMBER: _ClassVar[int]
    POSITIONS_FIELD_NUMBER: _ClassVar[int]
    FACTOR_RETURNS_FIELD_NUMBER: _ClassVar[int]
    TOTAL_VAR_FIELD_NUMBER: _ClassVar[int]
    DECOMPOSITION_DATE_FIELD_NUMBER: _ClassVar[int]
    JOB_ID_FIELD_NUMBER: _ClassVar[int]
    book_id: str
    positions: _containers.RepeatedCompositeFieldContainer[PositionLoadingInput]
    factor_returns: _containers.RepeatedCompositeFieldContainer[FactorReturnSeries]
    total_var: float
    decomposition_date: str
    job_id: str
    def __init__(self, book_id: _Optional[str] = ..., positions: _Optional[_Iterable[_Union[PositionLoadingInput, _Mapping]]] = ..., factor_returns: _Optional[_Iterable[_Union[FactorReturnSeries, _Mapping]]] = ..., total_var: _Optional[float] = ..., decomposition_date: _Optional[str] = ..., job_id: _Optional[str] = ...) -> None: ...

class FactorContribution(_message.Message):
    __slots__ = ("factor", "factor_exposure", "factor_var", "pnl_attribution", "pct_of_total_var")
    FACTOR_FIELD_NUMBER: _ClassVar[int]
    FACTOR_EXPOSURE_FIELD_NUMBER: _ClassVar[int]
    FACTOR_VAR_FIELD_NUMBER: _ClassVar[int]
    PNL_ATTRIBUTION_FIELD_NUMBER: _ClassVar[int]
    PCT_OF_TOTAL_VAR_FIELD_NUMBER: _ClassVar[int]
    factor: FactorType
    factor_exposure: float
    factor_var: float
    pnl_attribution: float
    pct_of_total_var: float
    def __init__(self, factor: _Optional[_Union[FactorType, str]] = ..., factor_exposure: _Optional[float] = ..., factor_var: _Optional[float] = ..., pnl_attribution: _Optional[float] = ..., pct_of_total_var: _Optional[float] = ...) -> None: ...

class InstrumentLoadingResult(_message.Message):
    __slots__ = ("instrument_id", "factor", "loading", "r_squared", "has_r_squared", "method")
    INSTRUMENT_ID_FIELD_NUMBER: _ClassVar[int]
    FACTOR_FIELD_NUMBER: _ClassVar[int]
    LOADING_FIELD_NUMBER: _ClassVar[int]
    R_SQUARED_FIELD_NUMBER: _ClassVar[int]
    HAS_R_SQUARED_FIELD_NUMBER: _ClassVar[int]
    METHOD_FIELD_NUMBER: _ClassVar[int]
    instrument_id: str
    factor: FactorType
    loading: float
    r_squared: float
    has_r_squared: bool
    method: LoadingMethod
    def __init__(self, instrument_id: _Optional[str] = ..., factor: _Optional[_Union[FactorType, str]] = ..., loading: _Optional[float] = ..., r_squared: _Optional[float] = ..., has_r_squared: bool = ..., method: _Optional[_Union[LoadingMethod, str]] = ...) -> None: ...

class FactorDecompositionResponse(_message.Message):
    __slots__ = ("book_id", "decomposition_date", "total_var", "systematic_var", "idiosyncratic_var", "r_squared", "factor_contributions", "loadings", "job_id")
    BOOK_ID_FIELD_NUMBER: _ClassVar[int]
    DECOMPOSITION_DATE_FIELD_NUMBER: _ClassVar[int]
    TOTAL_VAR_FIELD_NUMBER: _ClassVar[int]
    SYSTEMATIC_VAR_FIELD_NUMBER: _ClassVar[int]
    IDIOSYNCRATIC_VAR_FIELD_NUMBER: _ClassVar[int]
    R_SQUARED_FIELD_NUMBER: _ClassVar[int]
    FACTOR_CONTRIBUTIONS_FIELD_NUMBER: _ClassVar[int]
    LOADINGS_FIELD_NUMBER: _ClassVar[int]
    JOB_ID_FIELD_NUMBER: _ClassVar[int]
    book_id: str
    decomposition_date: str
    total_var: float
    systematic_var: float
    idiosyncratic_var: float
    r_squared: float
    factor_contributions: _containers.RepeatedCompositeFieldContainer[FactorContribution]
    loadings: _containers.RepeatedCompositeFieldContainer[InstrumentLoadingResult]
    job_id: str
    def __init__(self, book_id: _Optional[str] = ..., decomposition_date: _Optional[str] = ..., total_var: _Optional[float] = ..., systematic_var: _Optional[float] = ..., idiosyncratic_var: _Optional[float] = ..., r_squared: _Optional[float] = ..., factor_contributions: _Optional[_Iterable[_Union[FactorContribution, _Mapping]]] = ..., loadings: _Optional[_Iterable[_Union[InstrumentLoadingResult, _Mapping]]] = ..., job_id: _Optional[str] = ...) -> None: ...
