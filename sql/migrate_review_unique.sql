ALTER TABLE review
  DROP COLUMN status,
  ADD UNIQUE KEY uk_review_order (order_id);
