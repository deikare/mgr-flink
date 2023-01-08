# Notatki do pracy
* implementacja w Flinku
  * najpierw drzewa przy użyciu `valueState<Tree>`,
  * potem zrównoleglanie z wykorzystaniem Flinka
* klasyfikator w liściu drzewa
  * najpierw statystycznie obliczana klasa większościowa
  * potem np klasyfikator bayesowski
* podział atrybutów na ciągłe i dyskretne
  * na początku same ciągłe, bo dyskretne = ciągłe + eps
  * rozpoznawanie wartości atrybutu do podziału na początku przy wykorzystaniu prostej statystyki najczęściej wystąpującego
  * potem z wykorzystaniem B-drzewa z książki Gamy
* robić start N próbek (na przykład 100, 1000), podczas których tylko uczymy klasyfikator
* funkcja heurystyki
  * najpierw stała dla wszystkich node'ów
  * potem zależna od typu node'a
* tau dobierane eksperymentalnie
* w przypadku poprawnego trafienia robić statystykę trafień
* produkcja danych
  * na początku z pliku .csv
  * potem z Kafki
* do sprawozdania
  * opisać cel pracy
  * opisać Flinka
  * opisać algorytm
  * opisać eksperymenty