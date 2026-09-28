CREATE TABLE payments (
    id           VARCHAR(36)  NOT NULL PRIMARY KEY,
    merchant_id  VARCHAR(255) NOT NULL,
    amount_minor BIGINT       NOT NULL,
    currency     VARCHAR(3)   NOT NULL,
    recorded_at  TIMESTAMP    NOT NULL
);

CREATE INDEX idx_payments_merchant_id ON payments (merchant_id);
