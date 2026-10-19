-- Drugi skrypt schematu: widok zależy od tabeli employee, więc MUSI wykonać się po schema.sql.
CREATE VIEW it_employee AS
    SELECT id, name, salary FROM employee WHERE department = 'IT';
