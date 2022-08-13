--
-- File generated with SQLiteStudio v3.2.1 on Mo Feb 22 04:02:12 2021
--
-- Text encoding used: UTF-8
--
-- Table: Adresse
CREATE TABLE Adresse (
    Adresse_id INTEGER PRIMARY KEY
                       NOT NULL
                       UNIQUE
                       COLLATE NOCASE,
    Hausnummer INTEGER NOT NULL
                       CHECK (Hausnummer > 0),
    Strasse    VARCHAR NOT NULL
                       CHECK (Strasse GLOB [replace](hex(zeroblob(length(Strasse) ) ), '00', '[a-zA-Z .-]') AND 
                              Strasse GLOB '[a-zA-z]*'),
    Stadt      VARCHAR NOT NULL
                       CHECK (Stadt GLOB [replace](hex(zeroblob(length(Stadt) ) ), '00', '[a-zA-Z]') ),
    PLZ        VARCHAR NOT NULL
                       CHECK (PLZ GLOB [replace](hex(zeroblob(length(PLZ) ) ), '00', '[0-9]') ) 
);


-- Table: Artikel
CREATE TABLE Artikel (
    ISBN              VARCHAR  NOT NULL
                               PRIMARY KEY
                               UNIQUE
                               CHECK (ISBN GLOB [replace](hex(zeroblob(length(ISBN) ) ), '00', '[0-9-]') ) 
                               COLLATE NOCASE,
    Bezeichnung       VARCHAR  NOT NULL
                               CHECK (Bezeichnung GLOB [replace](hex(zeroblob(length(Bezeichnung) ) ), '00', '[a-zA-Z]') ),
    Beschreibung      TEXT     NOT NULL
                               CHECK (Beschreibung GLOB [replace](hex(zeroblob(length(Beschreibung) ) ), '00', '[a-zA-Z0-9 ()_*?!%&;:.,#"]') AND 
                                      Beschreibung GLOB '*[a-zA-Z]*'),
    Coverbild         BLOB     CHECK (hex(Coverbild) GLOB '89504E470D0A1A0A*' OR 
                                      hex(Coverbild) = NULL),
    Erscheinungsdatum DATETIME NOT NULL
                               CHECK (Erscheinungsdatum IS strftime('%Y-%m-%d %H:%M:%S', Erscheinungsdatum) ),
    Medium            VARCHAR  REFERENCES Medium (Medium) ON DELETE CASCADE
                                                          ON UPDATE CASCADE
                               NOT NULL
);


-- Table: Ausleihe
CREATE TABLE Ausleihe (
    Ausleihe_id   INTEGER  NOT NULL
                           PRIMARY KEY
                           UNIQUE
                           COLLATE NOCASE,
    Ausleihbeginn DATETIME NOT NULL
                           CHECK (Ausleihbeginn IS strftime('%Y-%m-%d %H:%M:%S', Ausleihbeginn) ),
    Ausleihende   DATETIME NOT NULL
                           CHECK (Ausleihende IS strftime('%Y-%m-%d %H:%M:%S', Ausleihende) ),
    Zurueckgegeben        BOOLEAN  NOT NULL,
    Email         VARCHAR  NOT NULL
                           COLLATE NOCASE
                           REFERENCES Kunde (Email) ON DELETE CASCADE
                                                    ON UPDATE CASCADE,
    Exemplar_id   INTEGER  NOT NULL
                           REFERENCES Exemplar (Exemplar_id) ON DELETE CASCADE
                                                             ON UPDATE CASCADE
);


-- Table: Autor
CREATE TABLE Autor (
    Autor_id INTEGER NOT NULL
                     PRIMARY KEY
                     UNIQUE
                     COLLATE NOCASE,
    Vorname  VARCHAR NOT NULL
                     CHECK (Vorname GLOB [replace](hex(zeroblob(length(Vorname) ) ), '00', '[a-zA-Z]') ),
    Nachname VARCHAR NOT NULL
                     CHECK (Nachname GLOB [replace](hex(zeroblob(length(Nachname) ) ), '00', '[a-zA-Z]') ) 
);


-- Table: Bibliothekar
CREATE TABLE Bibliothekar (
    Email         VARCHAR REFERENCES Nutzer (Email) ON DELETE CASCADE
                                                    ON UPDATE CASCADE
                          PRIMARY KEY
                          NOT NULL
                          UNIQUE
                          COLLATE NOCASE,
    Telefonnummer VARCHAR NOT NULL
                          CHECK (Telefonnummer GLOB [replace](hex(zeroblob(length(Telefonnummer) ) ), '00', '[0-9]') ) 
                          UNIQUE
                          COLLATE NOCASE
);

-- Table: empfiehlt
CREATE TABLE empfiehlt (
    ISBN_1 VARCHAR NOT NULL
                   REFERENCES Artikel (ISBN) ON DELETE CASCADE
                                             ON UPDATE CASCADE
                   COLLATE NOCASE,
    ISBN_2 VARCHAR NOT NULL
                   REFERENCES Artikel (ISBN) ON DELETE CASCADE
                                             ON UPDATE CASCADE
                   COLLATE NOCASE,
    PRIMARY KEY (
        ISBN_1,
        ISBN_2
    ),
    UNIQUE (
        ISBN_1,
        ISBN_2
    )
);


-- Table: Exemplar
CREATE TABLE Exemplar (
    Exemplar_id       INTEGER NOT NULL
                              PRIMARY KEY
                              UNIQUE
                              COLLATE NOCASE,
    Anschaffungspreis DOUBLE  NOT NULL
                              CHECK (Anschaffungspreis GLOB '*.[0-9][0-9]' OR 
                                     Anschaffungspreis GLOB '*.[0-9]' OR 
                                     Anschaffungspreis NOT LIKE '%.%' AND 
                                     Anschaffungspreis >= 0),
    Standort_id       INTEGER REFERENCES Standort (Standort_id) ON DELETE CASCADE
                                                                ON UPDATE CASCADE
                              NOT NULL,
    ISBN              VARCHAR NOT NULL
                              COLLATE NOCASE
                              REFERENCES Artikel (ISBN) ON DELETE CASCADE
                                                        ON UPDATE CASCADE
);


-- Table: gehoert
CREATE TABLE gehoert (
    Bezeichnung VARCHAR NOT NULL
                        COLLATE NOCASE
                        REFERENCES Genre (Bezeichnung) ON DELETE CASCADE
                                                       ON UPDATE CASCADE,
    ISBN        VARCHAR NOT NULL
                        COLLATE NOCASE
                        REFERENCES Artikel (ISBN) ON DELETE CASCADE
                                                  ON UPDATE CASCADE,
    PRIMARY KEY (
        Bezeichnung,
        ISBN
    ),
    UNIQUE (
        Bezeichnung,
        ISBN
    )
);



-- Table: Genre
CREATE TABLE Genre (
    Bezeichnung VARCHAR PRIMARY KEY
                      NOT NULL
                      UNIQUE
                      CHECK (Bezeichnung GLOB [replace](hex(zeroblob(length(Bezeichnung) ) ), '00', '[a-zA-Z]') ) 
                      COLLATE NOCASE
);


-- Table: Kunde
CREATE TABLE Kunde (
    Email      VARCHAR NOT NULL
                       PRIMARY KEY
                       UNIQUE
                       COLLATE NOCASE
                       REFERENCES Nutzer (Email) ON DELETE CASCADE
                                                 ON UPDATE CASCADE,
    Guthaben   DOUBLE  NOT NULL
                       CHECK (Guthaben GLOB '*.[0-9][0-9]' OR 
                              Guthaben GLOB '*.[0-9]' OR 
                              Guthaben NOT LIKE '%.%'),
    Befreiung  BOOLEAN NOT NULL,
    Adresse_id INTEGER NOT NULL
                       REFERENCES Adresse (Adresse_id) ON DELETE CASCADE
                                                       ON UPDATE CASCADE
);


-- Table: Medium
CREATE TABLE Medium (
    Medium VARCHAR PRIMARY KEY
                 NOT NULL
                 UNIQUE
                 COLLATE NOCASE
                 CHECK (Medium LIKE 'Hardcover' OR 
                        Medium LIKE 'CD' OR 
                        Medium LIKE 'Softcover' OR 
                        Medium LIKE 'DVD') 
);


-- Table: Nutzer
CREATE TABLE Nutzer (
    Email        VARCHAR  PRIMARY KEY
                          NOT NULL
                          UNIQUE
                          COLLATE NOCASE,
    Name         VARCHAR  NOT NULL
                          CHECK (Name GLOB [replace](hex(zeroblob(length(Name) ) ), '00', '[a-zA-Z]') ),
    Nachname     VARCHAR  NOT NULL
                          CHECK (Nachname GLOB [replace](hex(zeroblob(length(Nachname) ) ), '00', '[a-zA-Z]') ),
    Geburtsdatum DATETIME NOT NULL
                          CHECK (Geburtsdatum IS strftime('%Y-%m-%d %H:%M:%S', Geburtsdatum) ),
    Passwort     VARCHAR  NOT NULL
                          CHECK (Passwort GLOB [replace](hex(zeroblob(length(Passwort) ) ), '00', '[a-zA-Z0-9!?"=<>;+:/-)_(]') AND 
                                 length(Passwort) >= 4 AND 
                                 Passwort GLOB '*[A-Z]*' AND 
                                 Passwort GLOB '*[0-9]*' AND 
                                 Passwort NOT GLOB '*[0-9][0-9]*') 
);


-- Table: Standort
CREATE TABLE Standort (
    Standort_id INTEGER PRIMARY KEY
                        NOT NULL
                        UNIQUE
                        COLLATE NOCASE,
    Etage       INTEGER NOT NULL,
    Regalnummer INTEGER NOT NULL
                        CHECK (Regalnummer > 0) 
);


-- Table: verfasst
CREATE TABLE verfasst (
    ISBN     VARCHAR NOT NULL
                     COLLATE NOCASE
                     REFERENCES Artikel (ISBN) ON DELETE CASCADE
                                               ON UPDATE CASCADE,
    Autor_id INTEGER NOT NULL
                     COLLATE NOCASE
                     REFERENCES Autor (Autor_id) ON DELETE CASCADE
                                                 ON UPDATE CASCADE,
    PRIMARY KEY (
        ISBN,
        Autor_id
    ),
    UNIQUE (
        ISBN,
        Autor_id
    )
);


-- Trigger: Adresse_Update
CREATE TRIGGER Adresse_Update
        BEFORE UPDATE
            ON Adresse
BEGIN
    SELECT CASE WHEN (
                         SELECT COUNT( * ) 
                           FROM Kunde
                          WHERE OLD.Adresse_id = Kunde.Adresse_id
                     )
>=             2 THEN RAISE(ABORT, "Addresse kann nicht geändert werden! (Mehr als ein Nutzer)") END;
END;


-- Trigger: Ausleihe_Beginn_Ende_Check
CREATE TRIGGER Ausleihe_Beginn_Ende_Check
        BEFORE INSERT
            ON Ausleihe
BEGIN
    SELECT CASE WHEN NEW.Ausleihbeginn > NEW.Ausleihende THEN RAISE(ABORT, "Ausleihbeginn ist aelter als Ausleihende!") END;
END;


-- Trigger: AusleiheCheck
CREATE TRIGGER AusleiheCheck
        BEFORE INSERT
            ON Ausleihe
BEGIN
    SELECT CASE WHEN NEW.Exemplar_id IN (
                   SELECT Exemplar_id
                     FROM Ausleihe
                    WHERE Ausleihe.Zurueckgegeben = 0 AND
							   New.Zurueckgegeben = 0
               )
           THEN RAISE(ABORT, "Wurde schon ausgeliehen!") END;
END;


-- Trigger: Email_Formatierung
CREATE TRIGGER Email_Formatierung
        BEFORE INSERT
            ON Nutzer
BEGIN
    SELECT CASE WHEN NEW.Email NOT LIKE '%_@_%._%' OR 
                     NEW.Email GLOB '[^a-zA-Z0-9]*@*' OR 
                     NEW.Email GLOB '^*@*[^a-zA-Z0-9]*.*' OR 
                     NEW.Email GLOB '*.*[^a-zA-Z]*' THEN RAISE(ABORT, "Email falsches Format!") END;
END;


-- Trigger: Exemplar_Ausleih_Anzahl
CREATE TRIGGER Exemplar_Ausleih_Anzahl
        BEFORE INSERT
            ON Ausleihe
BEGIN
    SELECT CASE WHEN (
                         SELECT COUNT( * ) 
                           FROM Ausleihe
                          WHERE Ausleihe.Zurueckgegeben = 0 AND 
                                Ausleihe.Email = NEW.Email
                     )
>=             5 THEN RAISE(ABORT, "Zu viele ausgeliehen!") END;
END;


-- Trigger: Telefonnummer_Format
CREATE TRIGGER Telefonnummer_Format
        BEFORE INSERT
            ON Bibliothekar
BEGIN
    SELECT CASE WHEN NEW.Telefonnummer NOT LIKE '%0049_%' THEN RAISE(ABORT, "Telefonnummer falsches Format!") END;
END;


-- Trigger: CheckUpdate
CREATE TRIGGER CheckUpdate
        BEFORE UPDATE
            ON Ausleihe
BEGIN
    SELECT CASE WHEN NEW.Zurueckgegeben = 0 AND 
                     old.Exemplar_id IN (
                   SELECT Exemplar_id
                     FROM Ausleihe
                    WHERE Zurueckgegeben = 0
               )
           THEN RAISE(ABORT, "Ist noch verleiht") END;
END;


PRAGMA foreign_keys = on;
