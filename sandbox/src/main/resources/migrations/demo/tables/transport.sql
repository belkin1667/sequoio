--sequoio-migration-file

--migration belkinmike:create_cars_table
CREATE TABLE cars (
   id BIGINT PRIMARY KEY,
   licence_plate TEXT NOT NULL
);