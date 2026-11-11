/*
 * ======================================================================================
 * FIȘIER: NumberData.java
 * ======================================================================================
 * LOCAȚIE:  DMA06/src/main/java/ro/ase/ie/dma06/data/NumberData.java
 * PACHET:   ro.ase.ie.data          ← ATENȚIE: nu conține `dma06`, deși folderul da
 * ROL:      singura sursă de date a aplicației. Produce o listă cu numerele 1..100.
 *           Nu știe ce e un View, un Context, un Adapter sau o activitate.
 *
 * STRUCTURA FIȘIERULUI (3 părți):
 *   1. constante      — 2 linii: FIRST_NUMBER, LAST_NUMBER
 *   2. constructor    — privat și gol, cu un comentariu care explică de ce e gol
 *   3. getNumbers()   — 7 linii: buclă și return
 *
 * CE TREBUIE SĂ ȘTII ÎNAINTE:
 *   - `final` pe clasă + constructor `private` + metode `static` = clasă utilitară.
 *   - Metoda întoarce o listă NOUĂ la fiecare apel, nu una partajată.
 *   - Zero importuri `android.*`: clasa asta ar rula și pe desktop.
 * ======================================================================================
 */

// ======================================================================================
// BLOCUL 1 — PACHETUL ȘI IMPORTURILE
// ======================================================================================
// Pachetul declarat NU conține `dma06`, deși folderul este ro/ase/ie/dma06/data/.
// Nu e o greșeală de tipar: așa e în repo, și la fel stau lucrurile la cele două
// adaptoare, care declară ro.ase.ie.adapters.
//
// De ce compilează: Gradle dă compilatorului lista explicită de fișiere sursă, iar
// javac nu impune ca folderul să coincidă cu pachetul când fișierele sunt date direct.
// Potrivirea folder–pachet contează doar când compilatorul caută o clasă pe classpath.
//
// Consecința practică: importurile folosesc pachetul declarat, nu calea din folder.
//   import ro.ase.ie.data.NumberData;          ← corect, așa e în repo
//   import ro.ase.ie.dma06.data.NumberData;    ← greșit, clasa nu există acolo
//
// La runtime nu se schimbă nimic: ClassLoader încarcă după numele din pachet, nu după
// calea din folder. Folderele sunt doar organizare pe disc.
// ======================================================================================

package ro.ase.ie.data;          // ATENȚIE: pachetul declarat NU conține `dma06`

import java.util.ArrayList;      // implementarea concretă a listei
import java.util.List;           // interfața folosită în semnături

// Două importuri, ambele din java.util. Zero importuri android.*: clasa ar putea rula
// într-un test JUnit obișnuit, fără emulator. Separarea pe pachete nu e cosmetică —
// ea permite testarea sursei fără Android.

// ======================================================================================
// THEORY BLOCK: CLASA UTILITARĂ (UTILITY CLASS)
// ======================================================================================
/*
 * 1. CE ESTE
 *    O clasă care nu are stare și nu are instanțe. Toate metodele sunt `static`, deci
 *    se cheamă pe clasă: `NumberData.getNumbers()`, niciodată `new NumberData().get...`.
 *
 * 2. DE CE CONSTRUCTOR PRIVAT
 *    -   `private NumberData() {}` blochează `new NumberData()` din orice altă clasă.
 *    -   Fără el, compilatorul ar genera un constructor public implicit și oricine ar
 *        putea crea instanțe fără sens.
 *    -   Nu e o eroare să ai instanțe inutile; e zgomot. Iar zgomotul ascunde intenția.
 *    -   Constructorul privat mai blochează și moștenirea: o subclasă ar trebui să cheme
 *        `super()`, care e inaccesibil. De aceea `final` pe clasă e aproape redundant —
 *        dar îl scriem oricum, ca să fie explicit.
 *
 * 3. DE CE METODE STATIC
 *    -   Nu depind de nicio stare a instanței.
 *    -   Se încarcă o singură dată, cu clasa, nu cu fiecare obiect.
 *    -   Pot fi chemate din orice context, inclusiv dintr-un test JUnit fără Android.
 *
 * 4. ALTERNATIVE ȘI DE CE NU SUNT FOLOSITE AICI
 *    -   Singleton: ar impune o instanță partajată. Aici fiecare ecran vrea lista LUI.
 *    -   Interfață cu metode `default`: nu ai ce implementa — nu există contract, există
 *        o singură funcție.
 *    -   Clasă cu constructor public: ar fi permis `new NumberData()`, care nu are sens.
 *
 * 5. REGULA CARE SE VEDE ÎN COD
 *    `final class` + `private constructor` + `static methods` = aceeași combinație ca în
 *    java.lang.Math, java.util.Collections, java.util.Arrays. Nu e un truc de seminar;
 *    e tiparul din JDK.
 *
 * DIAGRAMA:
 *
 *   NumberData                    (clasa; nu se instanțiază niciodată)
 *   ├── FIRST_NUMBER = 1          static final
 *   ├── LAST_NUMBER  = 100        static final
 *   ├── NumberData()              private  ← blochează `new`
 *   └── getNumbers()              static   ← singurul punct de acces
 */

/** The data source shared by all three steps. */
public final class NumberData {  // final: nu poate fi extinsă; o clasă utilitară nu are ce moșteni

    // ==================================================================================
    // BLOCUL 2 — CONSTANTELE
    // ==================================================================================
    // private  — nimeni din afară nu are nevoie de ele
    // static   — aparțin clasei, nu instanței (și oricum nu există instanțe)
    // final    — nu se modifică niciodată; compilatorul poate înlocui valoarea direct
    //
    // De ce două constante și nu 100 scris direct: dacă vrei 1–1000, schimbi o singură
    // linie. Iar bucla devine citibilă: „de la FIRST_NUMBER până la LAST_NUMBER".
    // ==================================================================================
    private static final int FIRST_NUMBER = 1;    // primul număr din listă
    private static final int LAST_NUMBER = 100;   // ultimul număr, INCLUSIV

    // ==================================================================================
    // BLOCUL 3 — CONSTRUCTORUL PRIVAT
    // ==================================================================================
    // Teorie:  blochează instanțierea; clasa are doar metode statice.
    // Logică:  corpul e gol intenționat; comentariul documentează decizia.
    // Risc:    dacă îl scoți, compilatorul generează unul public implicit și `new`
    //          devine legal.
    // Ref:     curs 2 (clase și obiecte).
    // ==================================================================================
    private NumberData() {
        // This class only has static methods
    }

    // De ce constructorul e gol și nu aruncă excepție: unele clase utilitare scriu
    // `throw new AssertionError()` în constructor, ca să prindă apelurile prin reflecție.
    // Aici nu e nevoie: constructorul e privat, iar reflecția nu e un scenariu de laborator.
    //
    // Comentariul din interior — „This class only has static methods" — documentează
    // DE CE e gol. Un constructor privat gol fără explicație arată ca o scăpare.

    // ==================================================================================
    // BLOCUL 4 — getNumbers()
    // ==================================================================================
    // Teorie:  metodă statică ce construiește o colecție; nu atinge nicio stare.
    // Logică:  1. creează ArrayList  2. buclă de la 1 la 100  3. add  4. return
    // Risc:    dacă ai returna o listă partajată (câmp static), modificările dintr-un
    //          ecran ar apărea în toate celelalte.
    // Ref:     capitolul 04 (MainActivity - listă).
    // ==================================================================================
    public static List<Integer> getNumbers() {
        List<Integer> numbers = new ArrayList<>();     // 1. listă nouă la fiecare apel
        for (int number = FIRST_NUMBER; number <= LAST_NUMBER; number++) {   // 2. 1..100
            numbers.add(number);                       // 3. autoboxing: int → Integer
        }
        return numbers;                                // 4. 100 de elemente
    }

    // Listă nouă la fiecare apel — asta e esențial. Nu există un câmp static partajat.
    // Fiecare dintre cele trei activități cheamă getNumbers() în onCreate și primește
    // PROPRIA listă. Dacă ar fi un singleton, o modificare făcută într-un ecran ar
    // apărea în toate celelalte, fără să știi de unde vine.
    //
    // List în semnătură, ArrayList în implementare — returnezi interfața, construiești
    // implementarea. Cine primește lista nu depinde de ArrayList; poți schimba în
    // LinkedList fără să atingi nimic altceva. Regula se numește „programează la interfață".
    //
    // new ArrayList<>() cu diamant gol — tipul generic se deduce din context (Java 7+).
    //
    // `<=` și nu `<` — LAST_NUMBER e INCLUSIV. Bucla dă exact 100 de elemente: 1..100.
    // Cu `<` ai avea 99 — și s-ar vedea imediat în listă, pentru că ultimul rând ar fi 99.
    //
    // Autoboxing — numbers.add(number) primește un int, dar lista e List<Integer>.
    // Compilatorul transformă automat în Integer.valueOf(number). Pentru valorile
    // −128…127, Integer.valueOf întoarce instanțe din cache, deci pentru 1–100 nu se
    // creează 100 de obiecte noi.
    //
    // De ce Integer și nu int: genericele Java nu acceptă tipuri primitive. List<int>
    // nu compilează. De aici autoboxing-ul, de aici și Integer în getItem la adaptoare.
    //
    // ==================================================================================
    // CÂND SE ÎNCARCĂ CLASA
    // ==================================================================================
    //
    //   Primul apel NumberData.getNumbers()
    //     → ClassLoader încarcă NumberData
    //     → inițializează câmpurile statice: FIRST_NUMBER = 1, LAST_NUMBER = 100
    //     → abia apoi execută corpul metodei
    //
    // Inițializarea se face o singură dată per proces, nu la fiecare apel. De aceea
    // constantele pot fi citite fără nicio grijă de ordine: sunt gata înainte de prima
    // metodă.
    //
    // ==================================================================================
    // DE CE E UȘOR DE TESTAT
    // ==================================================================================
    //
    //   @Test
    //   public void getNumbers_returnsHundredElements() {
    //       List<Integer> numbers = NumberData.getNumbers();
    //       assertEquals(100, numbers.size());
    //       assertEquals(Integer.valueOf(1), numbers.get(0));
    //       assertEquals(Integer.valueOf(100), numbers.get(99));
    //   }
    //
    // Testul rulează pe JVM, fără emulator, pentru că în fișier nu apare niciun tip
    // android.*. Dacă NumberData ar fi primit un Context ca să citească resurse, testul
    // ar fi avut nevoie de Robolectric sau de un device. Separarea pe pachete nu e
    // cosmetică: ea decide dacă poți testa în 50 de milisecunde sau în 50 de secunde.
}
