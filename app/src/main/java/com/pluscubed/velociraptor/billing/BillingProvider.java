package com.pluscubed.velociraptor.billing;


/**
 * An interface that provides an access to BillingLibrary methods
 */
public interface BillingProvider {
    BillingManager getBillingManager();

    boolean isPremiumPurchased();

    boolean isGoldMonthlySubscribed();

    boolean isTankFull();

    boolean isGoldYearlySubscribed();
}