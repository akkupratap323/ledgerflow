package dev.ledgerflow.account;

import dev.ledgerflow.common.ConflictException;
import dev.ledgerflow.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accounts;

    public AccountService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @Transactional
    public Account open(String code, String name, AccountType type, String currency, boolean allowNegative) {
        if (accounts.existsByCode(code)) {
            throw new ConflictException("account_exists", "Account with code " + code + " already exists");
        }
        return accounts.save(new Account(code, name, type, currency, allowNegative));
    }

    @Transactional(readOnly = true)
    public Account get(UUID id) {
        return accounts.findById(id).orElseThrow(() -> new NotFoundException("Account", id));
    }

    @Transactional(readOnly = true)
    public Account getByCode(String code) {
        return accounts.findByCode(code).orElseThrow(() -> new NotFoundException("Account", code));
    }

    @Transactional
    public Account freeze(UUID id) {
        Account account = lock(id);
        account.freeze();
        return account;
    }

    @Transactional
    public Account unfreeze(UUID id) {
        Account account = lock(id);
        account.unfreeze();
        return account;
    }

    @Transactional
    public Account close(UUID id) {
        Account account = lock(id);
        account.close();
        return account;
    }

    private Account lock(UUID id) {
        return accounts.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Account", id));
    }

    @Transactional(readOnly = true)
    public List<Account> list() {
        return accounts.findAll();
    }
}
