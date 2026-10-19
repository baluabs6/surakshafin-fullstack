package com.surakshafin.banking;

import java.math.BigDecimal;

/** Per-rail limits modelled on the RBI rules: IMPS capped at 5 lakh, RTGS floor of 2 lakh. */
public enum TransferMode {
    IMPS(BigDecimal.ONE, new BigDecimal("500000")),
    NEFT(BigDecimal.ONE, null),
    RTGS(new BigDecimal("200000"), null);

    private final BigDecimal min;
    private final BigDecimal max;

    TransferMode(BigDecimal min, BigDecimal max) {
        this.min = min;
        this.max = max;
    }

    public BigDecimal min() { return min; }
    public BigDecimal max() { return max; }
}
