
CREATE TABLE company(
    id binary(16) DEFAULT (UUID_TO_BIN(uuid())) PRIMARY KEY,
    name varchar(255),
    bank_account varchar(255),
    estimated_size int,
    modified_at TIMESTAMP
);

CREATE TABLE mapping(
    branch_id binary(16) PRIMARY KEY,
    user_id binary(16) NOT NULL,
    company_id binary(16) NOT NULL
);

CREATE TABLE legal_entity(
    id binary(16) DEFAULT (UUID_TO_BIN(uuid())) PRIMARY KEY,
    name varchar(255),
    company_id binary(16) NOT NULL,
    modified_at TIMESTAMP,
    foreign key (company_id) references company(id)
);

create index le_name_idx on legal_entity(name);

CALL DOLT_ADD('.');
CALL DOLT_COMMIT('-m', 'Company and mapping is created', '--author', 'Liquibase Migrations <liquibase@example.com>');
