create table file (
    id BIGSERIAL primary key,
    name VARCHAR(300) not null,
    extension VARCHAR(300),
    path VARCHAR(300) unique not null,
    file_creation_time TIMESTAMP not null,
    file_last_modified_time TIMESTAMP not null,
    file_last_accessed_time TIMESTAMP not null,
    size BIGINT not null,

    read_access BOOLEAN not null,
    checksum CHAR(64),

    score DOUBLE PRECISION not null,

    type VARCHAR(64) not null,

    last_scan_id BIGINT not null,

    created_at TIMESTAMP DEFAULT current_timestamp,
    updated_at TIMESTAMP DEFAULT current_timestamp
);

create index file_creation_time on file (file_creation_time);
create index file_last_modified_time on file (file_last_modified_time);
create index file_last_accessed_time on file (file_last_accessed_time);
create index size on file (size);

CREATE TABLE text_file (
    file_id BIGINT PRIMARY KEY,
    content TEXT,

    CONSTRAINT file_id_fk FOREIGN KEY (file_id)
        REFERENCES file(id) ON DELETE CASCADE
);

alter table text_file add column ts tsvector
    generated always as (
            to_tsvector('simple', coalesce(content, ''))
        ) stored;

create index ts_idx on text_file using gin (ts);

CREATE TABLE image_file (
    file_id BIGINT PRIMARY KEY,
    color VARCHAR(64),

    CONSTRAINT file_id_fk FOREIGN KEY (file_id)
        REFERENCES file(id) ON DELETE CASCADE
);

