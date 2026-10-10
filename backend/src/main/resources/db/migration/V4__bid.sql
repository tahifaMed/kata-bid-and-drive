-- The bid totals live on the auction row; version guards concurrent bids (ADR 0003).
ALTER TABLE auction
    ADD COLUMN current_bid       NUMERIC(12, 2) CHECK (current_bid > 0),
    ADD COLUMN bid_count         INTEGER NOT NULL DEFAULT 0 CHECK (bid_count >= 0),
    ADD COLUMN highest_bidder_id BIGINT REFERENCES app_user (id),
    ADD COLUMN version           BIGINT NOT NULL DEFAULT 0;

CREATE TABLE bid (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    auction_id BIGINT NOT NULL REFERENCES auction (id),
    bidder_id  BIGINT NOT NULL REFERENCES app_user (id),
    amount     NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    placed_at  TIMESTAMPTZ NOT NULL
);

-- Serves the newest-first history (spec 006).
CREATE INDEX bid_auction_newest ON bid (auction_id, placed_at DESC, id DESC);
