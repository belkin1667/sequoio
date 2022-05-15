--sequoio-migration-file

--migration belkinmike:create_users #TICKET:SEQUOIO-123 #LOL:KEK
CREATE TABLE users(
    id BIGINT PRIMARY KEY,
    name TEXT NOT NULL
);