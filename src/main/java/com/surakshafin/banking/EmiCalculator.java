package com.surakshafin.banking;

import com.surakshafin.common.BadRequestException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Reducing-balance EMI maths. Pure functions; no persistence. */
public final class EmiCalculator {

    private static final MathContext MC = new MathContext(30, RoundingMode.HALF_EVEN);
    private static final BigDecimal TWELVE_HUNDRED = BigDecimal.valueOf(1200);

    private EmiCalculator() {}

    public static Dtos.EmiResponse calculate(BigDecimal principal, BigDecimal annualRatePercent, int months) {
        if (principal == null || principal.compareTo(new BigDecimal("1000")) < 0
                || principal.compareTo(new BigDecimal("100000000")) > 0) {
            throw new BadRequestException("principal must be between 1,000 and 10,00,00,000");
        }
        if (annualRatePercent == null || annualRatePercent.signum() < 0 || annualRatePercent.compareTo(BigDecimal.valueOf(60)) > 0) {
            throw new BadRequestException("annualRatePercent must be between 0 and 60");
        }
        if (months < 1 || months > 360) {
            throw new BadRequestException("tenureMonths must be between 1 and 360");
        }

        BigDecimal p = principal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal r = annualRatePercent.divide(TWELVE_HUNDRED, MC); // monthly rate
        BigDecimal emi;
        if (r.signum() == 0) {
            emi = p.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        } else {
            BigDecimal growth = BigDecimal.ONE.add(r).pow(months, MC);
            emi = p.multiply(r, MC).multiply(growth, MC).divide(growth.subtract(BigDecimal.ONE), MC)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        List<Dtos.EmiInstallment> schedule = new ArrayList<>(months);
        BigDecimal balance = p;
        BigDecimal totalInterest = BigDecimal.ZERO;
        BigDecimal totalPayable = BigDecimal.ZERO;
        for (int i = 1; i <= months; i++) {
            BigDecimal interest = balance.multiply(r, MC).setScale(2, RoundingMode.HALF_UP);
            BigDecimal installment = emi;
            BigDecimal principalPart = installment.subtract(interest);
            // Final instalment clears whatever rounding left behind so the balance ends at exactly zero.
            if (i == months || principalPart.compareTo(balance) > 0) {
                principalPart = balance;
                installment = principalPart.add(interest);
            }
            balance = balance.subtract(principalPart);
            totalInterest = totalInterest.add(interest);
            totalPayable = totalPayable.add(installment);
            schedule.add(new Dtos.EmiInstallment(i, installment, principalPart, interest, balance));
        }
        return new Dtos.EmiResponse(p, annualRatePercent, months, emi, totalInterest, totalPayable, schedule);
    }
}
