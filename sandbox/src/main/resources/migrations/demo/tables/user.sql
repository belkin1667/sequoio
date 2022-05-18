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