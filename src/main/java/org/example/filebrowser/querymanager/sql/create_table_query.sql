create table query(
    id BIGSERIAL primary key,
    query VARCHAR(300) UNIQUE not null,

    -- rank queries based on last used time
    last_used_at TIMESTAMP DEFAULT current_timestamp
);