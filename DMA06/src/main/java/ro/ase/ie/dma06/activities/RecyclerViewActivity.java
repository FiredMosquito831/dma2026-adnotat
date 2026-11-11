/*
 * ======================================================================================
 * FIȘIER: RecyclerViewActivity.java
 * ======================================================================================
 * LOCAȚIE:  DMA06/src/main/java/ro/ase/ie/dma06/activities/RecyclerViewActivity.java
 * PACHET:   ro.ase.ie.dma06.activities
 * ROL:      treapta 3. Același rând, aceleași date, dar cu RecyclerView. Aici se vede
 *           ce ListView ținea înăuntru: LayoutManager, ItemDecoration, ItemAnimator.
 *
 * STRUCTURA FIȘIERULUI (2 părți):
 *   1. importuri   — 10 linii, dintre care 3 din androidx.recyclerview
 *   2. onCreate    — 12 linii: layout, RecyclerView, LayoutManager, decoration, adapter
 *
 * CE TREBUIE SĂ ȘTII ÎNAINTE:
 *   - Fără setLayoutManager, RecyclerView nu afișează NIMIC și nu dă eroare.
 *   - DividerItemDecoration ia orientarea ca `int`, nu ca obiect.
 *   - Adapterul nu primește Context: îl ia din `parent` la creare.
 * ======================================================================================
 */

// ======================================================================================
// BLOCUL 1 — PACHETUL ȘI IMPORTURILE
// ======================================================================================
// Trei importuri noi față de treapta 2, toate din androidx.recyclerview:
//   RecyclerView            widget-ul
//   LinearLayoutManager     aranjarea
//   DividerItemDecoration   separatorul
// ======================================================================================

package ro.ase.ie.dma06.activities;      // pachetul ecranelor

import android.os.Bundle;                // parametrul lui onCreate

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;   // linia dintre rânduri
import androidx.recyclerview.widget.LinearLayoutManager;    // aranjarea pe verticală
import androidx.recyclerview.widget.RecyclerView;

import ro.ase.ie.adapters.NumberRecyclerAdapter;
import ro.ase.ie.data.NumberData;
import ro.ase.ie.dma06.R;

// ======================================================================================
// THEORY BLOCK: CELE PATRU PIESE ALE UNUI RECYCLERVIEW
// ======================================================================================
/*
 * 1. ADAPTER — CONȚINUTUL
 *    Ce se afișează. Creează holder-e și le umple. Nu știe unde stau pe ecran.
 *
 * 2. LAYOUTMANAGER — ARANJAREA
 *    Unde stau. Câte rânduri încap, cât măsoară fiecare, cum se calculează scroll-ul.
 *    Fără el, RecyclerView nu are cine să îi spună ce să deseneze.
 *
 * 3. ITEMDECORATION — DECORAȚIUNEA
 *    Ce se desenează peste sau sub rânduri: separatori, margini, umbre.
 *    Spre deosebire de ListView, nu există `android:divider` în XML.
 *
 * 4. ITEMANIMATOR — ANIMAȚIA
 *    Cum se mișcă rândurile când datele se schimbă. Vine implicit (DefaultItemAnimator);
 *    nu îl configurezi aici.
 *
 * SEPARAREA:
 *
 *   ┌──────────────────────────────────────────────────────────────────────────────┐
 *   │                            RecyclerView                                      │
 *   │                                                                              │
 *   │   ┌──────────────┐   ┌──────────────────┐   ┌──────────────┐   ┌──────────┐  │
 *   │   │   Adapter    │   │  LayoutManager   │   │ItemDecoration│   │ Animator │  │
 *   │   │  (conținut)  │   │   (aranjare)     │   │ (separatori) │   │(mișcare) │  │
 *   │   └──────┬───────┘   └────────┬─────────┘   └──────┬───────┘   └────┬─────┘  │
 *   │          │                    │                    │                │        │
 *   │   ce se vede            unde se vede          ce se vede        cum se       │
 *   │   în fiecare rând       pe ecran              între rânduri     mișcă       │
 *   └──────────────────────────────────────────────────────────────────────────────┘
 *
 *   ListView avea toate patru înghesuite într-o singură clasă. De aceea nu puteai
 *   schimba aranjarea fără să schimbi widget-ul.
 *
 * CONSECINȚA PRACTICĂ:
 *   -   Schimbi LinearLayoutManager cu GridLayoutManager(2) → grilă, fără nicio
 *       modificare în adapter.
 *   -   Adaugi un ItemDecoration → separatori, fără să atingi adapterul.
 *   -   Schimbi adapterul → alt conținut, fără să atingi aranjarea.
 *
 * DE CE „PATRU” ȘI NU TREI:
 *   ItemAnimator există și lucrează, chiar dacă nu îl configurezi. Un RecyclerView fără
 *   setItemAnimator are DefaultItemAnimator, care animează adăugări, ștergeri și
 *   mutări. Dacă îl scoți cu setItemAnimator(null), animațiile dispar, dar lista
 *   funcționează.
 */

/**
 * Step 3: the same two columns in a RecyclerView.
 * RecyclerView needs a LayoutManager, and dividers are added separately.
 */
public class RecyclerViewActivity extends AppCompatActivity {

    // ==================================================================================
    // METODA 1 — onCreate
    // ==================================================================================
    // Teorie:  ciclul de viață; aici se asamblează cele patru piese.
    // Logică:  1. super  2. setContentView  3. findViewById
    //          4. LayoutManager + setLayoutManager
    //          5. DividerItemDecoration + addItemDecoration
    //          6. adapter + setAdapter
    // Risc:    fără setLayoutManager, ecranul rămâne gol, fără excepție.
    // Ref:     capitolul 12 și capitolul 23 (migrarea ListView → RecyclerView).
    // ==================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);                          // 1. ciclul de viață
        setContentView(R.layout.activity_recycler_view);             // 2. antet + RecyclerView

        RecyclerView rvNumbers = findViewById(R.id.rvNumbers);       // 3. singurul RecyclerView

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);   // 4. aranjare verticală
        rvNumbers.setLayoutManager(layoutManager);                          //    ← OBLIGATORIU
        rvNumbers.addItemDecoration(
                new DividerItemDecoration(this, layoutManager.getOrientation()));  // 5. separator
        //                                ↑ Context   ↑ orientarea, ca int

        NumberRecyclerAdapter adapter = new NumberRecyclerAdapter(NumberData.getNumbers());
        rvNumbers.setAdapter(adapter);                               // 6. conținutul
    }

    // Ordinea celor trei setări: setLayoutManager → addItemDecoration → setAdapter.
    // Ordinea nu e fatală între ele, dar setLayoutManager TREBUIE să fie înainte de
    // orice layout. Dacă îl uiți, RecyclerView nu desenează nimic, indiferent câte
    // decorări și adaptoare ai adăuga.
    //
    // LinearLayoutManager(this) — constructorul cu un singur argument înseamnă vertical.
    // Varianta completă: LinearLayoutManager(context, orientation, reverseLayout).
    // Aici orientation ar fi RecyclerView.VERTICAL (1), iar reverseLayout false.
    //
    // layoutManager.getOrientation() — întoarce RecyclerView.VERTICAL, adică 1.
    // DividerItemDecoration așteaptă propria constantă, DividerItemDecoration.VERTICAL,
    // care e tot 1. Sunt două constante diferite ca semnificație, dar cu aceleași valori
    // (0 = orizontal, 1 = vertical în ambele clase), deci linia funcționează.
    //
    // DANGER! Compilatorul nu poate verifica: ambele sunt int. Dacă ai schimba
    // LayoutManager-ul pe orizontală, getOrientation() ar întoarce 0, care e tot
    // DividerItemDecoration.HORIZONTAL — deci codul rămâne corect. Potrivirea e reală,
    // nu întâmplătoare, dar se bazează pe valori numerice, nu pe un tip comun.
    //
    // addItemDecoration înainte de setAdapter — ordinea dintre ele nu contează.
    // Decorarea se aplică la desenare, nu la setarea datelor.
    //
    // De ce rvNumbers e declarat RecyclerView și nu View: pentru că ai nevoie de
    // setLayoutManager, addItemDecoration și setAdapter — toate metode ale clasei.
    // findViewById e generic, deci deduce tipul din variabilă.
    //
    // De ce nu se cheamă notifyDataSetChanged(): datele sunt fixe, iar setAdapter
    // declanșează singur primul layout. notifyDataSetChanged() ar fi redundant și ar
    // cauza o reumplere inutilă.

    // ==================================================================================
    // CE FACE LAYOUTMANAGER LA FIECARE CADRU
    // ==================================================================================
    //
    //   RecyclerView.onLayout()
    //     └─ LayoutManager.onLayoutChildren(recycler, state)
    //          ├─ calculează câți copii încap în spațiul disponibil
    //          ├─ cere holder-e din pool pentru pozițiile vizibile
    //          ├─ cheamă adapter.onBindViewHolder(holder, position) pentru fiecare
    //          ├─ măsoară și poziționează fiecare rând (layoutDecorated)
    //          └─ aruncă în pool holder-ele care au ieșit din ecran
    //
    // GridLayoutManager — new GridLayoutManager(this, 2) aranjează în două coloane.
    // Adapterul rămâne neschimbat, item_number.xml rămâne neschimbat.
    //
    // StaggeredGridLayoutManager — coloane cu înălțimi diferite, ca un Pinterest.
    // La fel, fără modificări în adapter.
    //
    // setReverseLayout(true) — lista crește de jos în sus, ca un chat. Tot din
    // LayoutManager.
    //
    // ==================================================================================
    // CE SE ÎNTÂMPLĂ FĂRĂ setLayoutManager
    // ==================================================================================
    //
    //   Nimic. Și exact asta e problema.
    //
    //   RecyclerView fără LayoutManager
    //     ├─ setAdapter(adapter)        → succes, fără excepție
    //     ├─ addItemDecoration(...)     → succes, fără excepție
    //     ├─ onMeasure()                → măsoară 0 × 0
    //     ├─ onLayout()                 → Log.e: „No layout manager attached; skipping layout"
    //     └─ ecranul                    → gol
    //
    //   Mesajul exact din Logcat:
    //     E/RecyclerView: No layout manager attached; skipping layout
    //
    //   De ce nu aruncă excepție: pentru că RecyclerView e proiectat să funcționeze și
    //   fără conținut. Un RecyclerView gol e o stare validă (de exemplu înainte de
    //   încărcarea datelor). Framework-ul nu poate ști dacă ai uitat să setezi managerul
    //   sau dacă intenționat nu ai încă date.
    //
    //   De ce e cea mai urâtă eroare din capitol: compilează, rulează, nu crapă, dar
    //   ecranul e gol. Toate celelalte greșeli din capitol se văd imediat; asta se vede
    //   doar dacă știi să te uiți în Logcat.
    //
    //   Ordinea de diagnostic pentru un RecyclerView gol:
    //     1) ai setLayoutManager?
    //     2) ai setAdapter?
    //     3) getItemCount() întoarce mai mult de 0?
    //     4) rădăcina layout-ului are înălțime nenulă?
    //   În 90% din cazuri, răspunsul e la prima întrebare.

    // ==================================================================================
    // DIAGRAMA SEPARĂRII: CONȚINUT vs ARANJARE
    // ==================================================================================
    //
    // ┌─────────────────────────────────────────────────────────────────────────────────┐
    // │              CE SE AFIȘEAZĂ          vs.        CUM SE ARANJEAZĂ                │
    // │                                                                                 │
    // │   ADAPTER (conținut)                      LAYOUTMANAGER (aranjare)               │
    // │   ├── getItemCount() → 100                ├── câte rânduri încap                 │
    // │   ├── onCreateViewHolder → rândul         ├── unde stă fiecare rând               │
    // │   ├── onBindViewHolder → textul           ├── cât măsoară fiecare                 │
    // │   └── getItemViewType → tipul             ├── cum se calculează scroll-ul         │
    // │                                           └── ce holder se refolosește            │
    // │                                                                                 │
    // │   Nu știe:                                Nu știe:                              │
    // │   ├── câte rânduri încap                  ├── ce e într-un rând                   │
    // │   ├── unde stă rândul                     ├── ce date există                      │
    // │   └── cum se derulează                    └── câte elemente sunt                  │
    // │                                                                                 │
    // │   ─────────────────────────────────────────────────────────────────────────     │
    // │                                                                                 │
    // │   ACELEAȘI DATE + ACELAȘI ADAPTER:                                              │
    // │                                                                                 │
    // │   LinearLayoutManager      GridLayoutManager(2)        StaggeredGrid(2)          │
    // │   ┌───┐                    ┌───┬───┐                   ┌───┬───┐                 │
    // │   │ 1 │                    │ 1 │ 2 │                   │ 1 │ 2 │                 │
    // │   ├───┤                    ├───┼───┤                   │   ├───┤                 │
    // │   │ 2 │                    │ 3 │ 4 │                   │ 3 │ 4 │                 │
    // │   ├───┤                    ├───┼───┤                   ├───┼───┤                 │
    // │   │ 3 │                    │ 5 │ 6 │                   │ 5 │ 6 │                 │
    // │   └───┘                    └───┴───┘                   └───┴───┘                 │
    // │                                                                                 │
    // │   zero modificări în adapter, zero modificări în item_number.xml                │
    // └─────────────────────────────────────────────────────────────────────────────────┘
    //
    // De ce separarea e importantă la examen: întrebarea „ce trebuie să setăm la un
    // RecyclerView ca să afișeze ceva” are răspunsul „adapter + LayoutManager”. Dacă
    // răspunzi doar „adapter”, ai pierdut jumătate din punctaj — și, mai important,
    // ai pierdut exact diferența față de ListView.
    //
    // Ce înseamnă „conținut” și „aranjare” în practică: conținutul e ce se vede în
    // fiecare rând (numărul zecimal și cel binar). Aranjarea e unde se vede fiecare
    // rând (unul sub altul, în două coloane, invers). Sunt două întrebări independente,
    // deci două obiecte independente.

    // ==================================================================================
    // DIVIDERITEMDECORATION — SEPARATORUL
    // ==================================================================================
    //
    // ListView are android:divider în XML. RecyclerView nu are așa ceva. Liniile dintre
    // rânduri sunt desenate de un ItemDecoration, iar DividerItemDecoration e decorarea
    // gata făcută din bibliotecă.
    //
    // Cum funcționează, în trei pași:
    //   1. RecyclerView desenează rândurile (prin LayoutManager)
    //   2. ItemDecoration.onDraw()      → desenează SUB conținut (înainte de copii)
    //   3. ItemDecoration.onDrawOver()  → desenează PESTE conținut (după copii)
    //
    // DividerItemDecoration folosește onDraw și desenează o linie la marginea de jos a
    // fiecărui rând, cu excepția ultimului.
    //
    // layoutManager.getOrientation() — îi spune decorării dacă liniile sunt orizontale
    // sau verticale. Pentru o listă verticală, separatorul e o linie orizontală între
    // rânduri. Pentru o listă orizontală, ar fi o linie verticală între coloane.
    //
    // Ce se întâmplă dacă pui orientarea greșită: separatorul e desenat pe cealaltă
    // axă — adică linii verticale peste rânduri, sau linii orizontale suprapuse. Nu
    // crapă, doar arată greșit. E genul de bug care se vede din prima.
    //
    // De ce nu e în XML: pentru că RecyclerView nu are atribute proprii de separator.
    // Tot ce ține de desenare se face din cod, prin ItemDecoration. E prețul separării:
    // mai mult cod, mai mult control.
    //
    // ==================================================================================
    // LISTVIEW vs RECYCLERVIEW — DIFERENȚELE DIN ACEST FIȘIER
    // ==================================================================================
    //
    //   ListView (treptele 1 și 2)          RecyclerView (treapta 3)
    //   ├── aranjare internă, fixă          ├── setLayoutManager(...) explicit
    //   ├── android:divider în temă         ├── addItemDecoration(...) în cod
    //   ├── animații limitate               ├── ItemAnimator implicit
    //   ├── adapter cu getView              ├── adapter cu 3 metode
    //   └── reciclare internă               └── RecycledViewPool, tot intern
    //
    // Trei linii de configurare în plus, trei capabilități în plus. Nimic din fișierul
    // ăsta nu există în treptele 1 și 2 — și exact asta e lecția capitolului.
    //
    // ==================================================================================
    // CE SE ÎNTÂMPLĂ DACĂ PUI DECORAREA DUPĂ setAdapter
    // ==================================================================================
    //
    //   Funcționează. addItemDecoration nu depinde de adapter; decorarea se aplică la
    //   faza de desenare, care are loc oricum după layout. Ordinea recomandată
    //   (manager → decoration → adapter) e o convenție de citire, nu o cerință.
    //
    // Ce ESTE o cerință: setLayoutManager înainte ca RecyclerView să primească primul
    // layout. Dacă îl setezi după ce view-ul a fost deja măsurat, RecyclerView cere
    // un requestLayout() — de aceea, în practică, îl setezi în onCreate, înainte de
    // orice altceva.
    //
    // ==================================================================================
    // DE CE `this` CA PRIM ARGUMENT LA LinearLayoutManager ȘI LA DividerItemDecoration
    // ==================================================================================
    //
    // Ambele au nevoie de Context:
    //   - LinearLayoutManager îl folosește pentru Resources (dimensiuni, densitate);
    //   - DividerItemDecoration îl folosește pentru a obține drawable-ul de separator
    //     din tema curentă.
    //
    // Nu e același Context ca la adapter: adapterul nu primește niciunul, pentru că îl
    // ia din `parent` în onCreateViewHolder. Aici, în onCreate, nu ai încă un `parent`,
    // deci `this` e singura sursă.
}
