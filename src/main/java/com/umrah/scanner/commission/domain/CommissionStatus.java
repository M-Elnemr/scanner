package com.umrah.scanner.commission.domain;

/** Where a company's commission debt to the platform stands. */
public enum CommissionStatus {

    /** Owed: the lead is fully paid but the company has not settled with the platform yet. */
    PENDING,

    /** The company states it has paid; awaiting admin confirmation. */
    REPORTED,

    /** Admin has confirmed receipt. Only now can the customer's cashback be paid out. */
    CONFIRMED,

    /**
     * The lead behind this debt was cancelled. The row is kept rather than deleted so a settled or
     * reported payment stays explainable after the fact, but nothing further is owed on it.
     */
    CANCELLED
}
