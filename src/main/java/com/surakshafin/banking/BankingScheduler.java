package com.surakshafin.banking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BankingScheduler {

    private static final Logger log = LoggerFactory.getLogger(BankingScheduler.class);

    private final FixedDepositService fixedDepositService;

    public BankingScheduler(FixedDepositService fixedDepositService) {
        this.fixedDepositService = fixedDepositService;
    }

    /** Pays out every fixed deposit that has reached maturity, shortly after midnight IST. */
    @Scheduled(cron = "0 5 0 * * *", zone = "${surakshafin.banking.timezone:Asia/Kolkata}")
    public void settleMaturedDeposits() {
        for (Long id : fixedDepositService.dueForSettlement()) {
            try {
                fixedDepositService.settleMatured(id);
            } catch (RuntimeException e) {
                log.error("Could not settle fixed deposit {}", id, e);
            }
        }
    }
}
