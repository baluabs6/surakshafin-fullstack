package com.surakshafin.config;

import com.surakshafin.fraud.ScamPattern;
import com.surakshafin.fraud.ScamPatternRepository;
import com.surakshafin.literacy.LiteracyContent;
import com.surakshafin.literacy.LiteracyContentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DemoDataSeeder implements CommandLineRunner {

    private final ScamPatternRepository scamPatternRepository;
    private final LiteracyContentRepository literacyContentRepository;

    public DemoDataSeeder(ScamPatternRepository scamPatternRepository,
                           LiteracyContentRepository literacyContentRepository) {
        this.scamPatternRepository = scamPatternRepository;
        this.literacyContentRepository = literacyContentRepository;
    }

    @Override
    public void run(String... args) {
        if (scamPatternRepository.count() == 0) {
            scamPatternRepository.save(pattern(
                    "Fake QR code refund scam",
                    "Scammer poses as a buyer on a marketplace app, sends a QR code and claims scanning it will send you money. Scanning a QR code can only send money out, never receive it.",
                    "UPI_QR", "HIGH"));
            scamPatternRepository.save(pattern(
                    "Fake customer care number",
                    "Searching for a bank's customer care number online leads to a fraudulent number planted in reviews/listings. The 'agent' asks for OTP, PIN, or remote-access app installation.",
                    "FAKE_CUSTOMER_CARE", "CRITICAL"));
            scamPatternRepository.save(pattern(
                    "Instant loan app harassment",
                    "Unregulated loan apps offer instant credit with minimal KYC, then charge hidden fees and use contact-list access for harassment on delay.",
                    "LOAN_APP", "HIGH"));
            scamPatternRepository.save(pattern(
                    "Work-from-home task scam",
                    "Victims are asked to 'invest' small amounts to unlock earnings from simple online tasks; early small payouts build trust before larger amounts are stolen.",
                    "JOB_SCAM", "MEDIUM"));
            scamPatternRepository.save(pattern(
                    "KYC update phishing link",
                    "SMS/WhatsApp message claims your bank account or UPI will be blocked unless you 'update KYC' via a link that leads to a fake bank login page.",
                    "KYC_PHISHING", "CRITICAL"));
        }

        if (literacyContentRepository.count() == 0) {
            literacyContentRepository.save(article(
                    "Never share your UPI PIN to receive money",
                    "Your UPI PIN is only ever needed to SEND money. No one — not a buyer, not a bank employee, not customer support — ever needs your PIN to send you a refund or a payment. If anyone asks for it for that reason, it is a scam. Hang up, and report it in the Fraud Protection section of this app.",
                    "UPI_SAFETY", "en"));
            literacyContentRepository.save(article(
                    "Read your BNPL agreement before you tap 'Pay Later'",
                    "Buy-Now-Pay-Later feels like a discount but is a loan. Missed instalments usually mean late fees plus an impact on your credit score, the same as missing a credit card bill. Track every BNPL purchase in the Spend Tracker so it doesn't quietly add up.",
                    "BNPL_RISKS", "en"));
            literacyContentRepository.save(article(
                    "Don't let your bank account become a 'mule' account",
                    "A mule account is a bank account used, often without the owner's full understanding, to move stolen money. Renting out your account or sharing your net-banking login for a fee — even a small one — can make you legally liable, not just a victim.",
                    "MULE_ACCOUNTS", "en"));
            literacyContentRepository.save(article(
                    "You have the right to escalate — for free",
                    "If your bank doesn't resolve a complaint within 30 days, you can escalate to the RBI Integrated Ombudsman at no cost, without a lawyer. The Grievance Routing tool in this app tells you exactly where your complaint should go and drafts it for you.",
                    "GRIEVANCE_RIGHTS", "en"));
        }
    }

    private static ScamPattern pattern(String title, String description, String category, String severity) {
        ScamPattern p = new ScamPattern();
        p.setTitle(title);
        p.setDescription(description);
        p.setCategory(category);
        p.setSeverity(severity);
        return p;
    }

    private static LiteracyContent article(String title, String body, String topic, String language) {
        LiteracyContent c = new LiteracyContent();
        c.setTitle(title);
        c.setBody(body);
        c.setTopic(topic);
        c.setLanguage(language);
        c.setFormat("ARTICLE");
        return c;
    }
}
