-- Skrypt „platformowy” lekcji Jdbc03SqlInit. Boot czyta go tylko wtedy, gdy ustawisz spring.sql.init.platform=jdbc03platform
-- i NIE podasz własnych schema-locations (wtedy szuka schema.sql oraz schema-NAZWAPLATFORMY.sql w korzeniu classpath).
-- W prawdziwym projekcie nazwa platformy to np. h2, postgresql, mysql; tu jest unikalna, żeby plik nie dotknął innych lekcji.
CREATE TABLE platform_marker (id INT PRIMARY KEY);
