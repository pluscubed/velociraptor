package com.pluscubed.velociraptor.billing;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClient.SkuType;

import java.util.Arrays;
import java.util.List;

/**
 * Static fields and methods useful for billing
 */
public final class BillingConstants {
    // SKUs for one-time donations
    public static final String SKU_D1 = "badge_1";
    public static final String SKU_D3 = "badge_2";
    public static final String SKU_D5 = "badge_3";
    public static final String SKU_D10 = "badge_4";
    public static final String SKU_D20 = "badge_5";

    // SKU for subscriptions
    public static final String SKU_TOMTOM = "sub_tomtom";
    public static final String SKU_HERE = "sub_here";
    //SKU for donation subscriptions
    public static final String SKU_D1_MONTHLY = "sub_1";
    public static final String SKU_D3_MONTHLY = "sub_2";

    private static final String[] IN_APP_SKUS = {SKU_D1, SKU_D3, SKU_D5, SKU_D10, SKU_D20};
    private static final String[] SUBSCRIPTIONS_SKUS = {SKU_TOMTOM, SKU_HERE, SKU_D1_MONTHLY, SKU_D3_MONTHLY};

    private BillingConstants() {
    }

    /**
     * Returns the list of all SKUs for the billing type specified
     */
    public static final List<String> getSkuList(@BillingClient.SkuType String billingType) {
        return (billingType.equals(SkuType.INAPP)) ? Arrays.asList(IN_APP_SKUS)
                : Arrays.asList(SUBSCRIPTIONS_SKUS);
    }
}