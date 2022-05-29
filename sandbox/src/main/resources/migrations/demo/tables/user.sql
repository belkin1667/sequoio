--sequoio-migration-file

--migration belkinmike:create_users_table
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    firstName TEXT NOT NULL,
    secondName TEXT NOT NULL
);

--migration belkinmike:add_car_id_to_users_table runAfter:create_cars_table
ALTER TABLE users
    ADD COLUMN car_id BIGINT REFERENCES cars (id);

--migration belkinmike:add_external_id_to_users_table
ALTER TABLE users
    ADD COLUMN external_id TEXT;

--migration belkinmike:add_users_external_id_idx transactional:false
DROP INDEX IF EXISTS users_external_id_idx;
CREATE INDEX IF NOT EXISTS users_external_id_idx on users (external_id);

--migration belkinmike:add_users_car_id_idx transactional:false
DROP INDEX IF EXISTS users_car_id_idx;
CREATE INDEX CONCURRENTLY IF NOT EXISTS users_car_id_idx on users (external_id);