-- Skrypt z błędem w środku: druga instrukcja odwołuje się do tabeli, której nie ma.
INSERT INTO employee (name, department, salary) VALUES ('Ewa Testowa', 'IT', 5000.00);
INSERT INTO nie_istnieje (x) VALUES (1);
INSERT INTO employee (name, department, salary) VALUES ('Jan Testowy', 'IT', 5100.00);
