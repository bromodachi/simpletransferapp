DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS  accounts;

CREATE TABLE accounts(
    -- for simplicity, we're just going to use an id.
    -- Don't do this in production.
    id BIGSERIAL NOT NULL PRIMARY KEY,
    --  The user id
    user_id BIGINT NOT NULL,
    balance BIGINT NOT NULL,
    -- TODO: Don't leave a default hard code
    currency CHAR(3) NOT NULL DEFAULT 'USD',
    created_at BIGINT NOT NULL DEFAULT extract(epoch from now()) * 1000,
    updated_at BIGINT NOT NULL DEFAULT extract(epoch from now()) * 1000,
    -- depends on the spec but we won't allow negative balances.
    constraint balance_non_negative check ( balance >= 0 )
);

CREATE TABLE transactions(
     id BIGSERIAL NOT NULL PRIMARY KEY,
     idempotency_key VARCHAR(255) NOT NULL,
     from_account_id BIGINT NOT NULL REFERENCES accounts(id),
     to_account_id BIGINT NOT NULL REFERENCES accounts(id),
     amount BIGINT NOT NULL,
    -- TODO: Add currency table
     currency CHAR(3) NOT NULL,
     created_at BIGINT NOT NULL DEFAULT extract(epoch from now()) * 1000,
     updated_at BIGINT NOT NULL DEFAULT extract(epoch from now()) * 1000
);

CREATE INDEX index_on_transactions_from_account_id ON transactions (from_account_id);
CREATE INDEX index_on_transactions_to_account_id ON transactions (to_account_id);
CREATE UNIQUE INDEX uni_index_on_idempotency_key ON transactions (idempotency_key);
