/*
 * ======================================================================================
 * FIȘIER: SingleColumnActivity.java
 * ======================================================================================
 * LOCAȚIE:  DMA06/src/main/java/ro/ase/ie/dma06/activities/SingleColumnActivity.java
 * PACHET:   ro.ase.ie.dma06.activities
 * ROL:      treapta 1. Un ListView, un ArrayAdapter, un TextView per rând.
 *           Nu scrie nicio linie de reciclare: ArrayAdapter o face în locul ei.
 *
 * STRUCTURA FIȘIERULUI (2 părți):
 *   1. importuri   — 6 linii
 *   2. onCreate    — 10 linii, dintre care 4 construiesc adapterul
 *
 * CE TREBUIE SĂ ȘTII ÎNAINTE:
 *   - `android.R.layout.simple_list_item_1` e un layout din SISTEM, nu din proiect.
 *   - `ArrayAdapter` cheamă `toString()` pe fiecare element. Pentru Integer = numărul.
 *   - `setAdapter` e tot ce trebuie: ListView cere singur getCount și getView.
 * ======================================================================================
 */

// ======================================================================================
// BLOCUL 1 — PACHETUL ȘI IMPORTURILE
// ======================================================================================
// Aici pachetul coincide cu folderul (activities/). Importurile sunt în trei grupuri:
// android.* (framework), androidx.* (bibliotecă împachetată) și ro.ase.* (codul tău).
// Separarea nu e obligatorie pentru compilator, dar face evident ce vine de unde.
// ======================================================================================

package ro.ase.ie.dma06.activities;      // pachetul ecranelor

import android.os.Bundle;                // parametrul lui onCreate
import android.widget.ArrayAdapter;      // adapterul gata făcut din SDK
import android.widget.ListView;          // widget-ul care afișează lista

import androidx.appcompat.app.AppCompatActivity;   // activitatea de bază

import ro.ase.ie.data.NumberData;        // pachetul declarat, nu folderul
import ro.ase.ie.dma06.R;                // resursele aplicației

// ======================================================================================
// THEORY BLOCK: LISTVIEW + ARRAYADAPTER
// ======================================================================================
/*
 * 1. CE FACE LISTVIEW
 *    -   Este un AdapterView: un widget care nu știe ce afișează, dar știe să afișeze.
 *    -   Cere unui Adapter: „câte elemente ai?" (getCount) și „dă-mi rândul pentru
 *        poziția N" (getView).
 *    -   Ține doar rândurile vizibile în arborele de view-uri. Restul nu există.
 *    -   Are un RecycleBin intern: rândurile care ies din ecran sunt păstrate pentru
 *        refolosire, pe tipuri (getItemViewType).
 *
 * 2. CE FACE ARRAYADAPTER
 *    -   Implementează getCount, getItem, getItemId și getView.
 *    -   getView: dacă convertView e null, umflă `resource`; altfel refolosește.
 *    -   Apoi ia elementul de la poziție și îi cheamă `toString()`, pe care îl pune
 *        într-un TextView.
 *    -   Deci: un singur text per rând. Atât.
 *
 * 3. CE TREBUIE SĂ CONȚINĂ `resource`
 *    -   Un TextView. Fie rădăcina însăși (cazul simple_list_item_1), fie un copil
 *        cu id-ul android.R.id.text1.
 *    -   Dacă lipsește, ArrayAdapter aruncă RuntimeException la primul rând:
 *        „You must supply a resource ID for a TextView".
 *
 * 4. UNDE SE OPREȘTE
 *    -   La al doilea TextView. ArrayAdapter nu are unde să pună a doua valoare.
 *    -   La formatare per coloană. toString() dă un singur șir; nu poți alinia
 *        coloana a doua la dreapta cu font monospațiat.
 *    -   La modele fără toString() suprascris: în listă apare Movie@a1b2c3.
 *
 * 5. RECICLAREA E ASCUNSĂ, NU LIPSĂ
 *    -   ArrayAdapter primește `convertView` și îl refolosește, exact ca un adapter
 *        propriu. Doar că tu nu vezi parametrul și nu poți pune un ViewHolder.
 *    -   Pentru un singur TextView, lipsa ViewHolder-ului nu costă: nu ai ce căuta.
 *
 * 6. CE COD SE EXECUTĂ LA FIECARE RÂND
 *    ListView.obtainView(position)
 *      └─ ArrayAdapter.getView(position, convertView, parent)
 *           ├─ convertView == null ?
 *           │     ├─ da  → inflater.inflate(simple_list_item_1, parent, false)
 *           │     └─ nu  → view = convertView
 *           ├─ text = (TextView) view        ← mFieldId == 0, rădăcina e TextView
 *           └─ text.setText(getItem(position).toString())
 *
 * DIAGRAMA:
 *
 *   ListView ──getCount()──▶ ArrayAdapter ──▶ List.size()
 *            ──getView(n)──▶ ArrayAdapter ──▶ convertView? inflate : reuse
 *                                          ──▶ getItem(n).toString()
 *                                          ──▶ textView.setText(...)
 */

/**
 * Step 1: one column.
 * ArrayAdapter is enough because each row shows one text value.
 */
public class SingleColumnActivity extends AppCompatActivity {   // o activitate, fără stare

    // ==================================================================================
    // METODA 1 — onCreate
    // ==================================================================================
    // Teorie:  ciclul de viață; aici se construiește tot ecranul.
    // Logică:  1. super  2. setContentView  3. findViewById  4. construiește adapterul
    //          5. setAdapter
    // Risc:    dacă uiți setAdapter, ecranul rămâne gol, fără nicio eroare.
    // Ref:     capitolul 12 (ListView vs RecyclerView).
    // ==================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);                       // 1. ciclul de viață al părintelui
        setContentView(R.layout.activity_single_column);          // 2. rădăcina layout-ului ESTE ListView-ul

        ListView lvNumbers = findViewById(R.id.lvNumbers);        // 3. singurul view din layout

        // ArrayAdapter calls toString() on each number and shows it in a TextView
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, NumberData.getNumbers());
        //      ↑ Context   ↑ layout din SISTEM            ↑ List<Integer> cu 100 de elemente
        lvNumbers.setAdapter(adapter);                            // 4. leagă lista de widget
    }

    // ArrayAdapter<Integer> — generic-ul trebuie să coincidă cu tipul listei. Lista e
    // List<Integer>, deci adapterul e ArrayAdapter<Integer>. Dacă ai pune
    // ArrayAdapter<String>, ai avea nevoie de List<String> — și ai pierde tipul numeric.
    //
    // new ArrayAdapter<>(...) cu diamant — tipul se deduce din variabila din stânga.
    //
    // `this` ca prim argument — e Context-ul. Adapterul are nevoie de el ca să obțină
    // LayoutInflater-ul cu care umflă rândul. În NumberListAdapter vei vedea exact
    // același lucru, doar că explicit: LayoutInflater.from(context).
    //
    // android.R.layout.simple_list_item_1 — „android." de la început e esențial.
    // R.layout.simple_list_item_1 nu există în proiect; android.R e pachetul de resurse
    // al framework-ului. Fișierul e livrat de sistem, nu de tine.
    //
    // NumberData.getNumbers() ca al treilea argument — se cheamă o singură dată, în
    // onCreate. Rezultatul e o listă de 100 de elemente, ținută de adapter prin
    // referință. Dacă ai chema getNumbers() din nou, ai avea o listă nouă, pe care
    // adapterul nu o vede.
    //
    // lvNumbers.setAdapter(adapter) — ultima linie și cea mai importantă. Înainte de ea,
    // ListView-ul e gol și nu știe că există date. La apel, ListView înregistrează un
    // DataSetObserver pe adapter și cere un layout; abia atunci încep getCount() și getView().
    //
    // Ordinea dintre findViewById și setAdapter nu contează — poți construi adapterul
    // înainte de a găsi lista. Ce contează e ca ambele să se întâmple după setContentView,
    // altfel findViewById întoarce null.
    //
    // ==================================================================================
    // CE FACE ARRAYADAPTER CU convertView
    // ==================================================================================
    //
    //   ArrayAdapter.getView(position, convertView, parent)
    //     ├─ convertView == null ?
    //     │     ├─ da  → view = inflater.inflate(mResource, parent, false)
    //     │     └─ nu  → view = convertView
    //     ├─ mFieldId == 0 ?
    //     │     ├─ da  → text = (TextView) view          ← rădăcina e TextView
    //     │     └─ nu  → text = view.findViewById(mFieldId)
    //     └─ text.setText(getItem(position).toString())
    //
    // Reciclarea e IDENTICĂ cu cea din NumberListAdapter. Diferența e că tu nu vezi
    // parametrul convertView și nu poți atârna un ViewHolder de rând. Pentru un singur
    // TextView, asta nu costă nimic — nu ai ce căuta.
    //
    // ==================================================================================
    // CE SE ÎNTÂMPLĂ DACĂ UIȚI setAdapter
    // ==================================================================================
    //
    //   ListView fără adapter
    //     ├─ getCount() nu e chemată niciodată
    //     ├─ getView() nu e chemată niciodată
    //     ├─ ecranul: alb, fără rânduri
    //     └─ nicio excepție, niciun mesaj în Logcat
    //
    // E aceeași clasă de eroare ca RecyclerView fără LayoutManager: o componentă validă,
    // într-o stare validă, care nu are ce afișa. Nu e un bug al framework-ului; e o
    // configurare incompletă.
    //
    // ==================================================================================
    // DE CE TREI LINII AJUNG AICI ȘI NU AJUNG LA TREAPTA 2
    // ==================================================================================
    //
    //   Treapta 1: 1 coloană  → 1 TextView  → ArrayAdapter știe
    //   Treapta 2: 2 coloane  → 2 TextView  → ArrayAdapter NU știe
    //   Treapta 3: 2 coloane  → 2 TextView  → RecyclerView.Adapter știe
    //
    // Regula nu e „ArrayAdapter e pentru liste simple". Regula e: ArrayAdapter știe
    // EXACT UN text per rând. Atât timp cât rândul tău e un text, e cel mai bun
    // instrument. Când rândul are structură, treci la BaseAdapter.
}
