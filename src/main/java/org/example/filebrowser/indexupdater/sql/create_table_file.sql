create table file (
    id BIGSERIAL primary key,
    name VARCHAR(300) not null,
    extension VARCHAR(300),
    path VARCHAR(300) unique not null,
    file_creation_time TIMESTAMP not null,
    file_last_modified_time TIMESTAMP not null,
    size BIGINT not null,

    read_access BOOLEAN not null,
    checksum CHAR(64),
    content TEXT,

    last_scan_id BIGINT not null,

    created_at TIMESTAMP DEFAULT current_timestamp,
    updated_at TIMESTAMP DEFAULT current_timestamp
);

alter table file add column ts tsvector
    generated always as (
        setweight(to_tsvector('english', coalesce(name, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(content, '')), 'B')
        ) stored;

create index ts_idx on file using gin (ts);