-- Broken V7 script to simulate halfway failure
CREATE TABLE broken_table (
    id BIGINT PRIMARY KEY
);

-- Intentional syntax error
INVALID SQL SYNTAX CAUSING HALFWAY FAILURE;
