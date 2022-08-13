- Ist-Beziehung zwischen Nutzer, Bib. und Kunde:
Hier habe ich eine Ist-Beziehung gewählt, da der Nutzer entweder ein Bib. oder ein Kunde sein muss und andersherum muss ein Bib. sowie 
der Kunde ein Nutzer sein (ein Kunde ist hier nur dann ein Kunde wenn diese wirklich Bücher ausleiht bzw. ausgeliehen hatte).
Des Weiteren erben so Bib und Kunde die selben Attritbute die sie gemeinsam haben.

- Telefonnr. von Bib:
Ist hier kein Primärschlüssel, da die E-Mail durch die Ist-Beziehung ein Primärschlüssel wird.

- Befreiung beim Kunden:
hier wird festgelegt ob der Kunde vom Jahresbeitrag befreit wird oder nicht.

- Status beim leiht:
Status heißt nur ob es zurückgeben wurde oder noch ausgeliehen ist.

- Coverbild (PNG) bei Artikel:
Ist optional, da ein Coverbild nicht immer verfügbar ist.

- ISBN und Medium bei Artikel und Medium:
Schwierig war die Überlegung ob die ISBN eine Aussage über Medium macht, nach einer Recherche im Internet war es nicht wirklich eindeutig wehalb
ich hier das Medium als Entität erstellt habe um somit zu erkenne in welcher Form ein Artikel verfügbar ist.
Die Kardinalität zwischen Aritkel und Medium, wir können kein Artikel haben ohne dass dieser Artikel einem Medium zugeordnet ist. Andersherum kann 
ein Medium mehreren Artikel zugeordnet werden oder auch gar keinem.

-Regal Nr. bei Standort:
Man könnte Regal Nr. als Primschlüsel ansehen allerdings ergibt es weniger Sinn, da pro Etage die Regal Nr. neu bestimmt wird und somit 
ist die Regal Nr mehrfach da.

-Bezeichnung bei Genre:
Bzeichnung ist ein Primschlüssel, da jede Bezeichnung des Genres untereinander einzigartig ist.


