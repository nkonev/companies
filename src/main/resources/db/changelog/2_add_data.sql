-- https://dev.mysql.com/blog-archive/mysql-8-0-uuid-support/
INSERT INTO company(id, name)
VALUES
    (UUID_TO_BIN(uuid()), 'P-Company');

INSERT INTO company(id, name)
VALUES
    (unhex(replace(uuid(),'-','')), 'O-Company');

