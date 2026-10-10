CREATE TABLE auction (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    seller_id      BIGINT NOT NULL REFERENCES app_user (id),
    starting_price NUMERIC(12, 2) NOT NULL CHECK (starting_price > 0),
    min_increment  NUMERIC(12, 2) NOT NULL CHECK (min_increment > 0),
    ends_at        TIMESTAMPTZ NOT NULL,
    status         VARCHAR(16) NOT NULL CHECK (status IN ('OPEN'))
);

-- A car belongs to exactly one auction (see spec 004, criterion 5).
CREATE TABLE car (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    auction_id  BIGINT NOT NULL UNIQUE REFERENCES auction (id),
    make        VARCHAR(60) NOT NULL,
    model       VARCHAR(60) NOT NULL,
    year        INTEGER NOT NULL,
    mileage_km  INTEGER NOT NULL CHECK (mileage_km >= 0),
    description VARCHAR(2000),
    image_url   VARCHAR(2048)
);
