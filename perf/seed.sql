-- 20,000 payments for merchant MR-9001, so the settlement endpoint does realistic work.
-- Expected answer afterwards: {"merchantId":"MR-9001","amountOwedMinor":10581674}
-- (total 10,920,200 minus a 338,526 fee at 3.1%, fee truncated)
INSERT INTO payments (id, merchant_id, amount_minor, currency, recorded_at)
SELECT gen_random_uuid()::text,
       'MR-9001',
       (100 + (g % 900))::bigint,
       'GBP',
       now() - (g || ' seconds')::interval
FROM generate_series(1, 20000) AS g;
