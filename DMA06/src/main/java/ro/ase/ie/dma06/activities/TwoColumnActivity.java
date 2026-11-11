/*
 * ======================================================================================
 * FIȘIER: TwoColumnActivity.java
 * ======================================================================================
 * LOCAȚIE:  DMA06/src/main/java/ro/ase/ie/dma06/activities/TwoColumnActivity.java
 * PACHET:   ro.ase.ie.dma06.activities
 * ROL:      treapta 2. Același ListView ca treapta 1, dar cu un adapter propriu care
 *           umple două coloane.
 *
 * STRUCTURA FIȘIERULUI (2 părți):
 *   1. importuri   — 7 linii
 *   2. onCreate    — 8 linii: layout, findViewById, adapter, setAdapter
 *
 * CE TREBUIE SĂ ȘTII ÎNAINTE:
 *   - Id-ul `lvNumbers` e ACELAȘI ca în treapta 1, dar în alt layout. Nu e conflict.
 *   - Activitatea nu știe ce e într-un rând: nu atinge tvDecimal / tvBinary.
 *   - Diferența față de treapta 1 e o singură clasă: NumberListAdapter.
 * ======================================================================================
 */

// ======================================================================================
// BLOCUL 1 — PACHETUL ȘI IMPORTURILE
// ======================================================================================
// Față de SingleColumnActivity, importul `android.widget.ArrayAdapter` a dispărut și
// a apărut `ro.ase.ie.adapters.NumberListAdapter`. Restul e identic.
// ======================================================================================

package ro.ase.ie.dma06.activities;      // pachetul ecranelor

import android.os.Bundle;                // parametrul lui onCreate
import android.widget.ListView;          // ACELAȘI widget ca treapta 1

import androidx.appcompat.app.AppCompatActivity;

import ro.ase.ie.adapters.NumberListAdapter;   // adapterul propriu, din pachetul `adapters`
import ro.ase.ie.data.NumberData;             // aceleași date ca la treapta 1
import ro.ase.ie.dma06.R;

// ======================================================================================
// THEORY BLOCK: DE LA ARRAYADAPTER LA BASEADAPTER
// ======================================================================================
/*
 * 1. CE RĂMÂNE IDENTIC
 *    -   Widget-ul: ListView.
 *    -   Datele: NumberData.getNumbers().
 *    -   Ciclul: onCreate → setContentView → findViewById → new Adapter → setAdapter.
 *    -   Reciclarea: tot ListView o face; doar că acum o scrii tu.
 *
 * 2. CE SE SCHIMBĂ
 *    -   Adapterul: ArrayAdapter (SDK) → NumberListAdapter (al tău).
 *    -   Layout-ul rândului: android.R.layout.simple_list_item_1 → item_number.xml.
 *    -   Numărul de coloane: 1 → 2.
 *    -   Cine cheamă findViewById: SDK-ul → codul tău, o dată per rând.
 *
 * 3. CE TREBUIE SĂ FACĂ ACTIVITATEA ÎN PLUS
 *    -   Nimic, în afară de a pasa `this` (Context) adapterului propriu.
 *    -   ArrayAdapter lua Context-ul prin constructor; NumberListAdapter la fel.
 *    -   Deci diferența se vede doar în tipul variabilei.
 *
 * 4. CE NU TREBUIE SĂ FACĂ ACTIVITATEA
 *    -   Să cunoască id-urile din rând (tvDecimal, tvBinary).
 *    -   Să știe câte coloane are rândul.
 *    -   Să atingă convertView, ViewHolder sau reciclarea.
 *    Orice astfel de linie ar fi o scurgere de responsabilitate: activitatea ar trebui
 *    schimbată când se schimbă rândul.
 *
 * DIAGRAMA RESPONSABILITĂȚILOR:
 *
 *   TwoColumnActivity                 NumberListAdapter
 *   ├── setContentView(layout)        ├── getCount()   → 100
 *   ├── findViewById(lvNumbers)       ├── getItem(n)
 *   ├── new NumberListAdapter(        ├── getItemId(n)
 *   │       this,                     └── getView(n, convertView, parent)
 *   │       NumberData.getNumbers())        ├── inflate / getTag
 *   └── setAdapter(adapter)                 ├── findViewById × 2
 *                                           └── setText × 2
 *
 * 5. DE CE ID-URILE NU SE CIOCNESC
 *    R.id.lvNumbers e o singură constantă int, generată o dată pentru toate layout-urile.
 *    Valoarea e aceeași, iar findViewById o caută în arborele atașat activității
 *    curente. Dacă ai avea două ListView-uri cu același id ÎN ACELAȘI layout, primul
 *    găsit ar fi returnat — și al doilea ar fi inaccesibil.
 */

/**
 * Step 2: two columns in a ListView.
 * A custom adapter fills each column of the row layout.
 */
public class TwoColumnActivity extends AppCompatActivity {

    // ==================================================================================
    // METODA 1 — onCreate
    // ==================================================================================
    // Teorie:  același ciclu de viață; se schimbă doar clasa adapterului.
    // Logică:  1. super  2. setContentView  3. findViewById  4. new NumberListAdapter
    //          5. setAdapter
    // Risc:    `this` trebuie să fie Context; dacă ai pasa altceva, LayoutInflater.from
    //          aruncă.
    // Ref:     capitolul 12.
    // ==================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);                    // 1. ciclul de viață
        setContentView(R.layout.activity_two_column);          // 2. include antetul + ListView

        ListView lvNumbers = findViewById(R.id.lvNumbers);     // 3. ACELAȘI id ca în treapta 1

        NumberListAdapter adapter = new NumberListAdapter(this, NumberData.getNumbers());
        //                                ↑ Context                  ↑ List<Integer> cu 100
        lvNumbers.setAdapter(adapter);                         // 4. leagă lista de widget
    }

    // Același id, alt layout — lvNumbers apare și în activity_single_column.xml, și în
    // activity_two_column.xml. Nu e o problemă: id-urile sunt unice PER LAYOUT, nu global.
    // findViewById caută doar în arborele layout-ului curent, deci găsește ListView-ul
    // din ecranul curent.
    //
    // `this` ca Context — adapterul are nevoie de el pentru LayoutInflater. Aici e
    // singurul loc în care activitatea atinge detaliul de inflate.
    //
    // Activitatea nu știe ce e într-un rând — nu există findViewById(R.id.tvDecimal)
    // aici. Cele două coloane sunt treaba adapterului. Dacă redenumești tvDecimal în
    // XML, se schimbă doar NumberListAdapter.
    //
    // Comparație cu treapta 1: aceeași structură, patru linii, doar clasa adapterului
    // diferită. Lecția: activitatea aproape nu se schimbă; adapterul e locul unde se
    // întâmplă totul.
    //
    // Ce s-ar întâmpla dacă ai pasa `null` ca Context: LayoutInflater.from(null) aruncă
    // NullPointerException în constructorul adapterului, deci la
    // new NumberListAdapter(null, ...). Erora apare devreme, în onCreate, nu la primul
    // rând — bine pentru depanare.
    //
    // ==================================================================================
    // DE CE ACTIVITATEA NU ARE NEVOIE DE `implements`
    // ==================================================================================
    //
    // Nu e nevoie de nicio interfață pentru că activitatea nu primește evenimente de la
    // adapter. Nu există „onItemBound" sau „onRowCreated". Adapterul e un obiect pasiv:
    // ListView îl întreabă, el răspunde. Activitatea doar îl construiește și îl predă.
    //
    // Dacă ai vrea să reacționezi la un click pe rând, ai avea două variante:
    //   1. lvNumbers.setOnItemClickListener(...)  → în activitate, cu poziția
    //   2. un listener pus pe rând, în getView     → în adapter
    // Aici nu e nevoie de niciuna: rândurile nu sunt clickabile.
    //
    // ==================================================================================
    // CE S-AR ÎNTÂMPLA DACĂ AI PUNE ADAPTERUL ÎNTR-UN CÂMP STATIC
    // ==================================================================================
    //
    //   private static NumberListAdapter adapter;   // ← greșit
    //
    // Adapterul ține lista și, prin holder-e, view-urile. Un câmp static ar ține
    // adapterul în viață după ce activitatea e distrusă, iar view-urile din holder-e ar
    // ține arborele vechi. Rezultatul: o scurgere de memorie care crește la fiecare
    // rotire. Adapterul e întotdeauna un câmp de instanță (sau o variabilă locală, ca
    // aici) — niciodată static.
}
