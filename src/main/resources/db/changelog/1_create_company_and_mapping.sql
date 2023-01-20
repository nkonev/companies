CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE company(
                        id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
                        name varchar(255),
                        bank_account varchar(255),
                        estimated_size int,
                        modified_at TIMESTAMP
);

CREATE TABLE mapping(
                        branch_id UUID PRIMARY KEY,
                        user_id UUID NOT NULL,
                        company_id UUID NOT NULL
);