-- Flyway V3: Add customer_id FK to beer_order
-- Nullable for backward compatibility

ALTER TABLE beer_order ADD COLUMN IF NOT EXISTS customer_id INT;

-- Add foreign key constraint
ALTER TABLE beer_order ADD CONSTRAINT fk_beer_order_customer
    FOREIGN KEY (customer_id) REFERENCES customer(id);

CREATE INDEX IF NOT EXISTS ix_beer_order_customer ON beer_order(customer_id);
