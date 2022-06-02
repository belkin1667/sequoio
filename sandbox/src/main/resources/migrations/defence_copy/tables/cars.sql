--sequoio-migration-file

--migration belkinmike:create_cars_table
CREATE TABLE cars (
      id BIGINT PRIMARY KEY,
      licence_plate TEXT NOT NULL,
      model TEXT NOT NULL
);

--migration belkinmike:create_cars_comments run:onchange #ticket:MAGISTRALDEV-83
COMMENT ON TABLE cars IS 'Автомобили';
COMMENT ON COLUMN cars.id IS 'Идентификатор автомобиля';
COMMENT ON COLUMN cars.model IS 'Модель автомобиля';