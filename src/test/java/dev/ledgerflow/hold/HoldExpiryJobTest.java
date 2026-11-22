package dev.ledgerflow.hold;

import dev.ledgerflow.account.AccountService;
import dev.ledgerflow.common.BusinessRuleException;
import dev.ledgerflow.journal.LedgerService;
import dev.ledgerflow.journal.PostEntryCommand;
import dev.ledgerflow.support.TestAccounts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static dev.ledgerflow.journal.PostingLine.credit;
import static dev.ledgerflow.journal.PostingLine.debit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class HoldExpiryJobTest {

    @Autowired
    HoldExpiryJob job;
    @Autowired
    HoldService holds;
    @Autowired
    AccountService accounts;
    @Autowired
    LedgerService ledger;

    @Test
    void expiresDueHoldsAndReleasesFunds() {
        TestAccounts t = new TestAccounts(accounts);
        String cash = t.asset("cash");
        String wallet = t.wallet("wallet");
        String merchant = t.wallet("merchant");
        ledger.post(new PostEntryCommand("fund", null, "INR", List.of(debit(cash, 1_000), credit(wallet, 1_000))));
        Hold shortHold = holds.place(wallet, 300, "short", Duration.ofSeconds(1));
        Hold longHold = holds.place(wallet, 200, "long", Duration.ofHours(1));

        int expired = job.expireDue(Instant.now().plusSeconds(60));

        assertThat(expired).isGreaterThanOrEqualTo(1);
        assertThat(holds.get(shortHold.getId()).getStatus()).isEqualTo(HoldStatus.EXPIRED);
        assertThat(holds.get(longHold.getId()).getStatus()).isEqualTo(HoldStatus.PENDING);
        assertThat(accounts.getByCode(wallet).availableMinor()).isEqualTo(800);
        assertThatThrownBy(() -> holds.capture(shortHold.getId(), 300, merchant))
                .isInstanceOf(BusinessRuleException.class);
    }
}
