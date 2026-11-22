package dev.ledgerflow.report;

import dev.ledgerflow.account.AccountService;
import dev.ledgerflow.account.AccountType;
import dev.ledgerflow.journal.LedgerService;
import dev.ledgerflow.journal.PostEntryCommand;
import dev.ledgerflow.support.TestAccounts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static dev.ledgerflow.journal.PostingLine.credit;
import static dev.ledgerflow.journal.PostingLine.debit;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TrialBalanceTest {

    @Autowired
    TrialBalanceService service;
    @Autowired
    AccountService accounts;
    @Autowired
    LedgerService ledger;

    @Test
    void ledgerStaysBalancedAcrossAccountTypes() {
        TestAccounts t = new TestAccounts(accounts);
        String cash = t.asset("cash");
        String wallet = t.wallet("wallet");
        String fees = t.revenue("fees");
        String hosting = t.open("hosting", AccountType.EXPENSE, true);
        String capital = t.open("capital", AccountType.EQUITY, false);

        ledger.post(new PostEntryCommand("seed", null, "INR", List.of(debit(cash, 50_000), credit(capital, 50_000))));
        ledger.post(new PostEntryCommand("t", null, "INR", List.of(debit(cash, 5_000), credit(wallet, 5_000))));
        ledger.post(new PostEntryCommand("f", null, "INR", List.of(debit(wallet, 100), credit(fees, 100))));
        ledger.post(new PostEntryCommand("h", null, "INR", List.of(debit(hosting, 2_000), credit(cash, 2_000))));

        TrialBalance inr = service.trialBalance().stream()
                .filter(tb -> tb.currency().equals("INR")).findFirst().orElseThrow();

        assertThat(inr.balanced()).isTrue();
        assertThat(inr.debitTotal()).isEqualTo(inr.creditTotal());
        assertThat(inr.totalsByType()).containsKeys(AccountType.ASSET, AccountType.EXPENSE,
                AccountType.LIABILITY, AccountType.EQUITY, AccountType.REVENUE);
    }
}
