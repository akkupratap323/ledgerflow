package dev.ledgerflow.journal;

import dev.ledgerflow.account.AccountService;
import dev.ledgerflow.common.BusinessRuleException;
import dev.ledgerflow.support.TestAccounts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static dev.ledgerflow.journal.PostingLine.credit;
import static dev.ledgerflow.journal.PostingLine.debit;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ConcurrentPostingTest {

    @Autowired
    LedgerService ledger;
    @Autowired
    AccountService accountService;

    @Test
    void parallelSpendsNeverOverdrawTheWallet() throws Exception {
        TestAccounts t = new TestAccounts(accountService);
        String cash = t.asset("cash");
        String wallet = t.wallet("wallet");
        String merchant = t.wallet("merchant");
        ledger.post(new PostEntryCommand("fund", null, "INR", List.of(debit(cash, 1_000), credit(wallet, 1_000))));

        int attempts = 40; // 40 spends of 100 against a balance of 1,000: exactly 10 may succeed
        AtomicInteger rejected = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(10);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                try {
                    ledger.post(new PostEntryCommand("spend", null, "INR",
                            List.of(debit(wallet, 100), credit(merchant, 100))));
                } catch (BusinessRuleException e) {
                    rejected.incrementAndGet();
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(accountService.getByCode(wallet).getBalanceMinor()).isZero();
        assertThat(accountService.getByCode(merchant).getBalanceMinor()).isEqualTo(1_000);
        assertThat(rejected.get()).isEqualTo(attempts - 10);
    }
}
