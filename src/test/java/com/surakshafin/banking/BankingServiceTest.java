package com.surakshafin.banking;

import com.surakshafin.common.BadRequestException;
import com.surakshafin.common.ConflictException;
import com.surakshafin.fraud.Dtos.ReportRequest;
import com.surakshafin.fraud.FraudService;
import com.surakshafin.identity.Dtos.KycLiteVerifyRequest;
import com.surakshafin.identity.Dtos.RegisterRequest;
import com.surakshafin.identity.IdentityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "surakshafin.jwt.secret=test-secret-test-secret-test-secret-123456")
class BankingServiceTest {

    private static final AtomicInteger PHONE_SEQ = new AtomicInteger(1000);

    @Autowired AccountService accounts;
    @Autowired TransferService transfers;
    @Autowired FixedDepositService fds;
    @Autowired IdentityService identity;
    @Autowired FraudService fraud;

    private Long newKycUser(String name) {
        String phone = "9" + String.format("%09d", PHONE_SEQ.incrementAndGet());
        Long id = identity.register(new RegisterRequest(phone, name, "Passw0rd123", "en")).profile().id();
        identity.verifyKycLite(id, new KycLiteVerifyRequest("ABCDE1234F"));
        return id;
    }

    private Dtos.AccountView fundedAccount(Long userId, String amount) {
        var acc = accounts.open(userId, new Dtos.OpenAccountRequest(AccountType.SAVINGS));
        return accounts.deposit(userId, acc.id(), new Dtos.AmountRequest(new BigDecimal(amount), null));
    }

    private Dtos.TransferRequest transferReq(Dtos.AccountView from, Dtos.AccountView to, String amount, String key, boolean ack) {
        return new Dtos.TransferRequest(from.id(), to.accountNumber(), to.ifsc(), new BigDecimal(amount),
                TransferMode.IMPS, "test", key, ack);
    }

    @Test
    void accountOpeningRequiresKyc() {
        String phone = "9" + String.format("%09d", PHONE_SEQ.incrementAndGet());
        Long id = identity.register(new RegisterRequest(phone, "No Kyc", "Passw0rd123", "en")).profile().id();
        assertThrows(BadRequestException.class, () -> accounts.open(id, new Dtos.OpenAccountRequest(AccountType.SAVINGS)));
    }

    @Test
    void transferMovesMoneyAndWritesBothLedgerSides() {
        Long alice = newKycUser("Alice Sharma");
        Long bob = newKycUser("Bob Rao");
        var a = fundedAccount(alice, "10000.00");
        var b = fundedAccount(bob, "500.00");
        transfers.addBeneficiary(alice, new Dtos.BeneficiaryRequest(b.accountNumber(), b.ifsc(), "Bob", false));

        transfers.transfer(alice, transferReq(a, b, "2500.50", UUID.randomUUID().toString(), false));

        assertEquals(0, new BigDecimal("7499.50").compareTo(accounts.get(alice, a.id()).balance()));
        assertEquals(0, new BigDecimal("3000.50").compareTo(accounts.get(bob, b.id()).balance()));
        var stmt = accounts.statement(alice, a.id(), null, null, 0, 20);
        assertEquals(EntryType.DEBIT, stmt.entries().get(0).type());
        assertEquals(0, new BigDecimal("7499.50").compareTo(stmt.entries().get(0).balanceAfter()));
    }

    @Test
    void retryWithSameIdempotencyKeyDoesNotMoveMoneyTwice() {
        Long alice = newKycUser("Alice Sharma");
        Long bob = newKycUser("Bob Rao");
        var a = fundedAccount(alice, "10000.00");
        var b = fundedAccount(bob, "1.00");
        transfers.addBeneficiary(alice, new Dtos.BeneficiaryRequest(b.accountNumber(), b.ifsc(), null, false));
        String key = UUID.randomUUID().toString();

        var first = transfers.transfer(alice, transferReq(a, b, "1000.00", key, false));
        var second = transfers.transfer(alice, transferReq(a, b, "1000.00", key, false));

        assertEquals(first.reference(), second.reference());
        assertEquals(0, new BigDecimal("9000.00").compareTo(accounts.get(alice, a.id()).balance()));
        assertThrows(ConflictException.class,
                () -> transfers.transfer(alice, transferReq(a, b, "1.00", key, false)));
    }

    @Test
    void insufficientBalanceIsRejectedAndNothingChanges() {
        Long alice = newKycUser("Alice Sharma");
        Long bob = newKycUser("Bob Rao");
        var a = fundedAccount(alice, "100.00");
        var b = fundedAccount(bob, "100.00");
        transfers.addBeneficiary(alice, new Dtos.BeneficiaryRequest(b.accountNumber(), b.ifsc(), null, false));

        assertThrows(BadRequestException.class,
                () -> transfers.transfer(alice, transferReq(a, b, "100.01", UUID.randomUUID().toString(), false)));
        assertEquals(0, new BigDecimal("100.00").compareTo(accounts.get(alice, a.id()).balance()));
        assertEquals(0, new BigDecimal("100.00").compareTo(accounts.get(bob, b.id()).balance()));
    }

    @Test
    void cannotSendToAnAccountThatIsNotABeneficiary() {
        Long alice = newKycUser("Alice Sharma");
        Long bob = newKycUser("Bob Rao");
        var a = fundedAccount(alice, "1000.00");
        var b = fundedAccount(bob, "1000.00");
        assertThrows(BadRequestException.class,
                () -> transfers.transfer(alice, transferReq(a, b, "10.00", UUID.randomUUID().toString(), false)));
    }

    @Test
    void reportedPayeeIsBlockedByFraudModule() {
        Long alice = newKycUser("Alice Sharma");
        Long bob = newKycUser("Bob Rao");
        var a = fundedAccount(alice, "1000.00");
        var b = fundedAccount(bob, "1000.00");
        transfers.addBeneficiary(alice, new Dtos.BeneficiaryRequest(b.accountNumber(), b.ifsc(), null, false));
        fraud.submitReport(newKycUser("Carol Iyer"), new ReportRequest("MULE_ACCOUNT", "Took my money",
                b.accountNumber() + "@" + b.ifsc().toUpperCase() + ".ifsc.npci", null));

        assertThrows(BadRequestException.class,
                () -> transfers.transfer(alice, transferReq(a, b, "10.00", UUID.randomUUID().toString(), true)));
        assertTrue(transfers.check(alice, new Dtos.TransferCheckRequest(a.id(), b.accountNumber(), b.ifsc(), new BigDecimal("10.00"))).blocked());
    }

    @Test
    void largeTransferToUntrustedPayeeNeedsAcknowledgement() {
        Long alice = newKycUser("Alice Sharma");
        Long bob = newKycUser("Bob Rao");
        var a = fundedAccount(alice, "50000.00");
        var b = fundedAccount(bob, "1.00");
        transfers.addBeneficiary(alice, new Dtos.BeneficiaryRequest(b.accountNumber(), b.ifsc(), null, false));

        assertThrows(ConflictException.class,
                () -> transfers.transfer(alice, transferReq(a, b, "6000.00", UUID.randomUUID().toString(), false)));
        assertDoesNotThrow(() -> transfers.transfer(alice, transferReq(a, b, "6000.00", UUID.randomUUID().toString(), true)));
    }

    @Test
    void ownAccountTransferNeedsNoBeneficiary() {
        Long alice = newKycUser("Alice Sharma");
        var a = fundedAccount(alice, "1000.00");
        var a2 = accounts.open(alice, new Dtos.OpenAccountRequest(AccountType.CURRENT));
        transfers.transfer(alice, transferReq(a, a2, "400.00", UUID.randomUUID().toString(), false));
        assertEquals(0, new BigDecimal("400.00").compareTo(accounts.get(alice, a2.id()).balance()));
    }

    @Test
    void rtgsEnforcesMinimumAndDailyLimitIsApplied() {
        Long alice = newKycUser("Alice Sharma");
        Long bob = newKycUser("Bob Rao");
        var a = fundedAccount(alice, "200000.00");
        var b = fundedAccount(bob, "1.00");
        transfers.addBeneficiary(alice, new Dtos.BeneficiaryRequest(b.accountNumber(), b.ifsc(), null, true));
        var rtgsTooSmall = new Dtos.TransferRequest(a.id(), b.accountNumber(), b.ifsc(), new BigDecimal("1000.00"),
                TransferMode.RTGS, null, UUID.randomUUID().toString(), true);
        assertThrows(BadRequestException.class, () -> transfers.transfer(alice, rtgsTooSmall));

        transfers.transfer(alice, transferReq(a, b, "90000.00", UUID.randomUUID().toString(), true));
        // default daily limit is 1,00,000, so a further 20,000 must be refused
        assertThrows(BadRequestException.class,
                () -> transfers.transfer(alice, transferReq(a, b, "20000.00", UUID.randomUUID().toString(), true)));
    }

    @Test
    void otherUsersCannotTouchMyAccount() {
        Long alice = newKycUser("Alice Sharma");
        Long mallory = newKycUser("Mallory Roy");
        var a = fundedAccount(alice, "1000.00");
        assertThrows(RuntimeException.class, () -> accounts.withdraw(mallory, a.id(), new Dtos.AmountRequest(new BigDecimal("10.00"), null)));
        assertThrows(RuntimeException.class, () -> accounts.statement(mallory, a.id(), null, null, 0, 20));
    }

    @Test
    void fixedDepositBookingAndPrematureClosure() {
        Long alice = newKycUser("Alice Sharma");
        var a = fundedAccount(alice, "50000.00");
        var fd = fds.book(alice, new Dtos.BookFdRequest(a.id(), new BigDecimal("20000.00"), 12));
        assertEquals(0, new BigDecimal("30000.00").compareTo(accounts.get(alice, a.id()).balance()));
        assertEquals(0, new BigDecimal("7.00").compareTo(fd.ratePercent()));
        assertEquals(FdStatus.ACTIVE, fd.status());

        var closed = fds.close(alice, fd.id());
        assertEquals(FdStatus.CLOSED_PREMATURE, closed.status());
        // closed on day 0, so no interest: principal comes straight back
        assertEquals(0, new BigDecimal("50000.00").compareTo(accounts.get(alice, a.id()).balance()));
        assertThrows(BadRequestException.class, () -> fds.close(alice, fd.id()));
    }

    @Test
    void fdMathMatchesQuarterlyCompounding() {
        assertEquals(0, new BigDecimal("107185.90")
                .compareTo(FixedDepositService.maturityAmount(new BigDecimal("100000.00"), new BigDecimal("7.00"), 12)));
    }

    @Test
    void emiMatchesKnownFigure() {
        var r = EmiCalculator.calculate(new BigDecimal("1000000"), new BigDecimal("8.5"), 240);
        assertEquals(0, new BigDecimal("8678.23").compareTo(r.emi()));
        assertEquals(0, BigDecimal.ZERO.compareTo(r.schedule().get(239).balance()));
    }

    @Test
    void csvStatementListsEntriesOldestFirst() {
        Long alice = newKycUser("Alice Sharma");
        var a = accounts.open(alice, new Dtos.OpenAccountRequest(AccountType.SAVINGS));
        accounts.deposit(alice, a.id(), new Dtos.AmountRequest(new BigDecimal("10.00"), "first"));
        accounts.withdraw(alice, a.id(), new Dtos.AmountRequest(new BigDecimal("4.00"), "second"));
        String csv = accounts.statementCsv(alice, a.id(), null, null);
        assertTrue(csv.startsWith("Date,Reference,Description"));
        assertTrue(csv.indexOf("first") < csv.indexOf("second"), csv);
    }
}
