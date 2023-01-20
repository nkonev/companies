CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- like git branch
CREATE TABLE branch(
    id uuid PRIMARY KEY default uuid_generate_v4(),
    name varchar(255),
    owner_id uuid -- null means 'system branch', for example 'main'
);

CREATE TABLE company_snapshots(
    id uuid PRIMARY KEY default uuid_generate_v4(),
    name varchar(255),
    bank_account varchar(255),
    estimated_size int,

    -- git fields
    modified_at TIMESTAMP,
    branch_id uuid REFERENCES branch(id),
    message   text not null
);

CREATE TABLE company_branch(
    id uuid PRIMARY KEY default uuid_generate_v4(),
    branch_id uuid REFERENCES branch(id) ON DELETE CASCADE,
    company_id uuid REFERENCES company(id) ON DELETE CASCADE
);

insert into branch(id, name) values
    ('00000000-0000-0000-0000-000000000000', 'main');
