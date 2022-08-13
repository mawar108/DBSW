--
-- File generated with SQLiteStudio v3.2.1 on Mo Feb 22 04:02:43 2021
--
-- Text encoding used: UTF-8
--

--1
SELECT ISBN
  FROM (
           SELECT ISBN,
                  count( * ) AS Anzahl
             FROM Exemplar
            GROUP BY ISBN
       )
 WHERE Anzahl >= 5;

--2:
SELECT *
  FROM (
           SELECT *
             FROM KUNDE
            ORDER BY Guthaben DESC
            LIMIT 3
       )
 ORDER BY Guthaben ASC;
 
--3:
SELECT Email,
       Name
  FROM Nutzer
 WHERE Email IN (
           SELECT Email
             FROM Ausleihe
            WHERE Exemplar_id IN (
                      SELECT Exemplar_id
                        FROM Gehoert
                             JOIN
                             Exemplar USING (
                                 ISBN
                             )
                       WHERE Gehoert.Bezeichnung != 'Kriminalroman'
                  )
       )
AND 
       Nachname = 'Musk';
