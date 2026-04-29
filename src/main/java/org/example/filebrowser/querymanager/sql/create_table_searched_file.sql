create table searched_file(
    search_file_id BIGSERIAL primary key,
    file_id BIGINT UNIQUE not null,
    nr_searches BIGINT DEFAULT 1,

    constraint fk_file foreign key (file_id) references file(id) on delete cascade
);