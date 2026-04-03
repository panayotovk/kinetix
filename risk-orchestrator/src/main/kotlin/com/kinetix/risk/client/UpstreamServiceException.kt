package com.kinetix.risk.client

class UpstreamServiceException(val statusCode: Int, message: String) : RuntimeException(message)
