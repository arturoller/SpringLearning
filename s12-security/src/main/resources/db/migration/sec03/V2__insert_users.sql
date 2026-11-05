-- Konta TYLKO DO NAUKI. Hasło każdego konta to: haslo-testowe.
-- Hash poniżej wygenerował PasswordEncoderFactories.createDelegatingPasswordEncoder() (BCrypt, koszt 10).
INSERT INTO app_users (username, password_hash, roles, enabled) VALUES
    ('anna',  '{bcrypt}$2a$10$77As/SkJTa3v4v5vtpmjdu3CMcS.yrU6NJvET7MhmMNwgYBwTIb86', 'CUSTOMER', TRUE),
    ('ewa',   '{bcrypt}$2a$10$77As/SkJTa3v4v5vtpmjdu3CMcS.yrU6NJvET7MhmMNwgYBwTIb86', 'EMPLOYEE', TRUE),
    ('admin', '{bcrypt}$2a$10$77As/SkJTa3v4v5vtpmjdu3CMcS.yrU6NJvET7MhmMNwgYBwTIb86', 'ADMIN',    TRUE),
    -- konto wyłączone (enabled = FALSE): poprawne hasło i tak nie wpuści (sekcja 4)
    ('jan',   '{bcrypt}$2a$10$77As/SkJTa3v4v5vtpmjdu3CMcS.yrU6NJvET7MhmMNwgYBwTIb86', 'CUSTOMER', FALSE),
    -- "stare" konto z hasłem zapisanym jawnie ({noop}) — po pierwszym logowaniu Spring sam przepisze je na BCrypt (sekcja 4)
    ('stary', '{noop}haslo-testowe',                                                     'CUSTOMER', TRUE);
