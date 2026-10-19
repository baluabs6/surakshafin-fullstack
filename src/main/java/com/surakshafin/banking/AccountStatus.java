package com.surakshafin.banking;

/** FROZEN blocks debits (withdrawals, transfers out, FD booking) but still allows incoming credits. */
public enum AccountStatus { ACTIVE, FROZEN }
