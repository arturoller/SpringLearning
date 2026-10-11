-- Skrypt z celowym bledem w srodku: pierwsza i trzecia instrukcja sa poprawne, druga nie.
CREATE TABLE legacy_a (id BIGINT PRIMARY KEY);
CREATE TABLE legacy_oops (id BIGINT PRIMARY KEY, id BIGINT);
CREATE TABLE legacy_b (id BIGINT PRIMARY KEY);
