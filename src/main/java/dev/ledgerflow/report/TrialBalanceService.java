package dev.ledgerflow.report;

import dev.ledgerflow.account.Account;
import dev.ledgerflow.account.AccountRepository;
import dev.ledgerflow.account.AccountType;
import dev.ledgerflow.common.Side;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class TrialBalanceService {

    private final AccountRepository accounts;

    public TrialBalanceService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public List<TrialBalance> trialBalance() {
        Map<String, Map<AccountType, Long>> byCurrency = new TreeMap<>();
        for (Account a : accounts.findAll()) {
            byCurrency.computeIfAbsent(a.getCurrency(), c -> new EnumMap<>(AccountType.class))
                    .merge(a.getType(), a.getBalanceMinor(), Math::addExact);
        }
        List<TrialBalance> result = new ArrayList<>();
        byCurrency.forEach((currency, totals) -> {
            long debit = 0;
            long credit = 0;
            for (var e : totals.entrySet()) {
                if (e.getKey().normalSide() == Side.DEBIT) {
                    debit = Math.addExact(debit, e.getValue());
                } else {
                    credit = Math.addExact(credit, e.getValue());
                }
            }
            result.add(new TrialBalance(currency, totals, debit, credit, debit == credit));
        });
        return result;
    }
}
