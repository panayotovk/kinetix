package com.kinetix.risk.service

class NoSodBaselineException(bookId: String) :
    RuntimeException("No SOD baseline exists for portfolio $bookId on today's date")
