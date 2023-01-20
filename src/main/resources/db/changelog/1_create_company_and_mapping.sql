CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- get drafts = get commits ? by companyId
-- get transactions = get branches by userId

-- like git branch
CREATE TABLE branch(
    id uuid PRIMARY KEY default uuid_generate_v4(),
    name varchar(255),
    owner_id uuid -- null means 'system branch', for example 'main'
);

create table commits(
    id UUID primary key default uuid_generate_v4(),
    branch_id uuid REFERENCES branch(id) ON DELETE CASCADE
);

CREATE TABLE bank(
    id uuid PRIMARY KEY default uuid_generate_v4(),
    name varchar(255),
    bank_account varchar(255),
    estimated_size int,

    -- git fields
    modified_at TIMESTAMP,
    message
);

CREATE TABLE company(
    id uuid PRIMARY KEY default uuid_generate_v4(),
    name varchar(255),
    bank_account varchar(255),
    estimated_size int,
    bank_id REFERENCES bank(id),

    -- git fields
    modified_at TIMESTAMP,
    message   text
);


CREATE TABLE company_branch(
    id uuid PRIMARY KEY default uuid_generate_v4(),
    branch_id uuid REFERENCES branch(id) ON DELETE CASCADE,
    company_id uuid REFERENCES company_snapshots(id) ON DELETE CASCADE
);

insert into branch(id, name) values
    ('00000000-0000-0000-0000-000000000000', 'main');
