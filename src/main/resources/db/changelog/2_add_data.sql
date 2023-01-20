-- https://dev.mysql.com/blog-archive/mysql-8-0-uuid-support/
INSERT INTO company(id, name)
VALUES
    (uuid_generate_v4(), 'P-Company');

INSERT INTO company(id, name)
VALUES
    (uuid_generate_v4(), 'O-Company');

