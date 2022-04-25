--sequoio-migration-file

--migration belkinmike:add_age_to_users runAfter:create_users_table
ALTER TABLE users
    ADD COLUMN age BIGINT;

--migration belkinmike:create_users_table runAfter:add_age_to_users run:always runAfter:add_comments_to_users_table run:never run:onchange
CREATE TABLE users(
    id BIGINT PRIMARY KEY,
    name TEXT NOT NULL
);

--migration belkinmike:add_comments_to_users_table
COMMENT ON TABLE users IS 'Таблица пользователей';
COMMENT ON COLUMN users.id IS 'Идентификатор пользователя';