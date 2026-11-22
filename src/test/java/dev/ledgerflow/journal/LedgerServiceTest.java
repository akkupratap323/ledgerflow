package dev.ledgerflow.journal;

import dev.ledgerflow.account.AccountService;
import dev.ledgerflow.common.BusinessRuleException;
import dev.ledgerflow.common.NotFoundException;
import dev.ledgerflow.support.TestAccounts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static dev.ledgerflow.journal.PostingLine.credit;
import static dev.ledgerflow.journal.PostingLine.debit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class LedgerServiceTest {

    @Autowired
    LedgerService ledger;
    @Autowired
    AccountService accountService;

    String cash;
    String alice;
    String bob;
    String fees;

    @BeforeEach
    void setUp() {
        TestAccounts t = new TestAccounts(accountService);
        cash = t.asset("cash");
        alice = t.wallet("alice");
        bob = t.wallet("bob");
        fees = t.revenue("fees");
    }

    @Test
    void topUpIncreasesBothAssetAndWallet() {
        ledger.post(new PostEntryCommand("topup-1", "Top up", "INR",
                List.of(debit(cash, 10_000), credit(alice, 10_000))));

        assertThat(balance(cash)).isEqualTo(10_000);
        assertThat(balance(alice)).isEqualTo(10_000);
    }

    @Test
    void paymentWithFeeSplitsAcrossThreeAccounts() {
        ledger.post(new PostEntryCommand("topup", null, "INR", List.of(debit(cash, 10_000), credit(alice, 10_000))));

        JournalEntry entry = ledger.post(new PostEntryCommand("pay-1", "Alice pays Bob", "INR",
                List.of(debit(alice, 5_000), credit(bob, 4_900), credit(fees, 100))));

        assertThat(balance(alice)).isEqualTo(5_000);
        assertThat(balance(bob)).isEqualTo(4_900);
        assertThat(balance(fees)).isEqualTo(100);
        assertThat(ledger.get(entry.getId()).getPostings()).hasSize(3);
    }

    @Test
    void overdraftIsRejectedAndNothingIsWritten() {
        ledger.post(new PostEntryCommand("topup", null, "INR", List.of(debit(cash, 1_000), credit(alice, 1_000))));

        assertThatThrownBy(() -> ledger.post(new PostEntryCommand("pay", null, "INR",
                List.of(debit(alice, 5_000), credit(bob, 5_000)))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("insufficient funds");

        assertThat(balance(alice)).isEqualTo(1_000);
        assertThat(balance(bob)).isZero();
    }

    @Test
    void currencyMismatchIsRejected() {
        assertThatThrownBy(() -> ledger.post(new PostEntryCommand("x", null, "USD",
                List.of(debit(cash, 100), credit(alice, 100)))))
                .hasMessageContaining("is in INR");
    }

    @Test
    void unknownAccountIsRejected() {
        assertThatThrownBy(() -> ledger.post(new PostEntryCommand("x", null, "INR",
                List.of(debit(cash, 100), credit("wallet:ghost", 100)))))
                .isInstanceOf(NotFoundException.class);
    }

    private long balance(String code) {
        return accountService.getByCode(code).getBalanceMinor();
    }
}
