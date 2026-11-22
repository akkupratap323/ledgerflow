package dev.ledgerflow.hold;

import dev.ledgerflow.account.Account;
import dev.ledgerflow.account.AccountService;
import dev.ledgerflow.common.BusinessRuleException;
import dev.ledgerflow.journal.LedgerService;
import dev.ledgerflow.journal.PostEntryCommand;
import dev.ledgerflow.support.TestAccounts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.List;

import static dev.ledgerflow.journal.PostingLine.credit;
import static dev.ledgerflow.journal.PostingLine.debit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class HoldServiceTest {

    @Autowired
    HoldService holds;
    @Autowired
    AccountService accounts;
    @Autowired
    LedgerService ledger;

    String wallet;
    String merchant;

    @BeforeEach
    void setUp() {
        TestAccounts t = new TestAccounts(accounts);
        String cash = t.asset("cash");
        wallet = t.wallet("wallet");
        merchant = t.wallet("merchant");
        ledger.post(new PostEntryCommand("fund", null, "INR", List.of(debit(cash, 1_000), credit(wallet, 1_000))));
    }

    @Test
    void holdReducesAvailableButNotBalance() {
        holds.place(wallet, 600, "order-1", Duration.ofMinutes(10));

        Account a = accounts.getByCode(wallet);
        assertThat(a.getBalanceMinor()).isEqualTo(1_000);
        assertThat(a.availableMinor()).isEqualTo(400);
    }

    @Test
    void heldFundsCannotBeSpentElsewhere() {
        holds.place(wallet, 600, "order-1", Duration.ofMinutes(10));

        assertThatThrownBy(() -> ledger.post(new PostEntryCommand("spend", null, "INR",
                List.of(debit(wallet, 500), credit(merchant, 500)))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("insufficient");
    }

    @Test
    void cannotHoldMoreThanAvailable() {
        holds.place(wallet, 600, "a", Duration.ofMinutes(10));

        assertThatThrownBy(() -> holds.place(wallet, 500, "b", Duration.ofMinutes(10)))
                .hasMessageContaining("insufficient available funds");
    }

    @Test
    void partialCaptureMovesMoneyAndReleasesTheRest() {
        Hold hold = holds.place(wallet, 600, "order-1", Duration.ofMinutes(10));

        Hold captured = holds.capture(hold.getId(), 450, merchant);

        assertThat(captured.getStatus()).isEqualTo(HoldStatus.CAPTURED);
        assertThat(captured.getCaptureEntryId()).isNotNull();
        Account a = accounts.getByCode(wallet);
        assertThat(a.getBalanceMinor()).isEqualTo(550);
        assertThat(a.getHeldMinor()).isZero();
        assertThat(accounts.getByCode(merchant).getBalanceMinor()).isEqualTo(450);
    }

    @Test
    void voidReleasesFunds() {
        Hold hold = holds.place(wallet, 600, "order-1", Duration.ofMinutes(10));

        holds.voidHold(hold.getId());

        assertThat(accounts.getByCode(wallet).availableMinor()).isEqualTo(1_000);
    }

    @Test
    void finishedHoldCannotBeCapturedOrVoidedAgain() {
        Hold hold = holds.place(wallet, 600, "order-1", Duration.ofMinutes(10));
        holds.capture(hold.getId(), 600, merchant);

        assertThatThrownBy(() -> holds.capture(hold.getId(), 600, merchant))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already captured");
        assertThatThrownBy(() -> holds.voidHold(hold.getId()))
                .hasMessageContaining("already captured");
        assertThat(accounts.getByCode(merchant).getBalanceMinor()).isEqualTo(600);
    }

    @Test
    void captureAboveHeldAmountIsRejected() {
        Hold hold = holds.place(wallet, 600, "order-1", Duration.ofMinutes(10));

        assertThatThrownBy(() -> holds.capture(hold.getId(), 601, merchant))
                .hasMessageContaining("between 1 and the held amount");
    }
}
