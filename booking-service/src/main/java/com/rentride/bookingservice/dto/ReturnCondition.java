package com.rentride.bookingservice.dto;

/**
 * Physical and operational condition of a vehicle upon return inspection.
 */
public enum ReturnCondition {

    /**
     * Vehicle returned in standard acceptable operating condition without damage.
     */
    GOOD,

    /**
     * Vehicle returned with visible or structural damage requiring repair assessment.
     */
    DAMAGED,

    /**
     * Vehicle requires servicing, deep cleaning, or mechanical maintenance before re-listing.
     */
    REQUIRES_MAINTENANCE
}

