--sequoio-migration-file

--migration belkinmike:create_users_table
CREATE TABLE users (
       id BIGINT PRIMARY KEY,
       first_name TEXT NOT NULL,
       second_name TEXT NOT NULL
);

--migration belkinmike:create_users_first_name_idx transactional:false
DROP INDEX IF EXISTS users_first_name_idx;
CREATE INDEX CONCURRENTLY IF NOT EXISTS users_first_name_idx on users (first_name);

--migration belkinmike:add_car_id_to_users_table runAfter:create_cars_table
ALTER TABLE users
    ADD COLUMN car_id BIGINT REFERENCES cars (id);