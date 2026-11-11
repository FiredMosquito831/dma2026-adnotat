/*
 * ======================================================================================
 * FIȘIER: NumberRecyclerAdapter.java
 * ======================================================================================
 * LOCAȚIE:  DMA06/src/main/java/ro/ase/ie/dma06/adapters/NumberRecyclerAdapter.java
 * PACHET:   ro.ase.ie.adapters
 * ROL:      adaptează List<Integer> la ACELAȘI rând cu două coloane (item_number.xml),
 *           dar pentru un RecyclerView. ViewHolder-ul e obligatoriu, iar getView e
 *           despărțit în două metode.
 *
 * STRUCTURA FIȘIERULUI (5 părți):
 *   1. ViewHolder            — 10 linii: câmpuri final + constructor cu findViewById
 *   2. câmp + constructor    — 5 linii: doar lista, fără Context
 *   3. onCreateViewHolder    — 6 linii: creează rândul și holder-ul
 *   4. onBindViewHolder      — 6 linii: umple holder-ul
 *   5. getItemCount          — 3 linii: câte elemente sunt
 *
 * CE TREBUIE SĂ ȘTII ÎNAINTE:
 *   - Trei metode abstracte obligatorii: onCreateViewHolder, onBindViewHolder,
 *     getItemCount. Nu există getView.
 *   - Parametrul generic <NumberViewHolder> elimină toate cast-urile.
 *   - `super(itemView)` e obligatoriu în constructorul holder-ului.
 *   - Nu există `convertView`: RecyclerView decide singur ce holder refolosește.
 * ======================================================================================
 */

// ======================================================================================
// BLOCUL 1 — PACHETUL ȘI IMPORTURILE
// ======================================================================================
// Față de NumberListAdapter au dispărut Context, BaseAdapter și inflater-ul ca câmp;
// au apărut NonNull și RecyclerView. Importul `android.view.View` rămâne: e tipul
// parametrului din constructorul holder-ului.
// ======================================================================================

package ro.ase.ie.adapters;   // același pachet ca NumberListAdapter

import android.view.LayoutInflater;   // pentru inflate, în onCreateViewHolder
import android.view.View;             // itemView-ul holder-ului
import android.view.ViewGroup;        // parent-ul din onCreateViewHolder
import android.widget.TextView;       // cele două coloane

import androidx.annotation.NonNull;                 // contract: parametru / rezultat care nu e null
import androidx.recyclerview.widget.RecyclerView;  // clasa de bază și ViewHolder-ul ei

import java.util.List;

import ro.ase.ie.dma06.R;

// ======================================================================================
// THEORY BLOCK: RECYCLERVIEW.ADAPTER
// ======================================================================================
/*
 * 1. CE ESTE
 *    -   Clasa abstractă care leagă datele de RecyclerView.
 *    -   Are TREI metode abstracte:
 *          onCreateViewHolder(ViewGroup parent, int viewType) → ViewHolder
 *          onBindViewHolder(ViewHolder holder, int position) → void
 *          getItemCount()                                    → int
 *    -   Toate celelalte au implementări implicite.
 *
 * 2. DE CE NU ARE getView
 *    -   Pentru că RecyclerView nu cere niciodată un View. Cere un ViewHolder.
 *    -   Un ViewHolder e o cutie cu un View înăuntru (itemView) plus referințe la
 *        copiii lui. RecyclerView ține holder-e, nu view-uri.
 *    -   De ce: ca să poată refolosi un holder FĂRĂ să știe ce e în el. Pentru asta
 *        are nevoie de un tip stabil (holder-ul), nu de un View oarecare.
 *
 * 3. DE CE TREI METODE ȘI NU UNA
 *    -   Crearea (inflate + findViewById) e scumpă și rară.
 *    -   Umplerea (setText) e ieftină și frecventă.
 *    -   Separându-le, RecyclerView poate chema doar a doua, de mii de ori, fără să
 *        plătească prima.
 *    -   getItemCount e a treia pentru că RecyclerView trebuie să știe câte poziții
 *        există ÎNAINTE de a cere vreun holder.
 *
 * 4. CE SE ÎNTÂMPLĂ CU CELELALTE METODE
 *    getItemId(int)             → NO_ID (-1) implicit; se activează cu setHasStableIds(true)
 *    getItemViewType(int)       → 0 implicit; pentru rânduri de tipuri diferite
 *    onViewRecycled(holder)     → gol; bun pentru anulat un download de imagine
 *    onViewAttachedToWindow     → gol; holder-ul intră în ecran
 *    onViewDetachedFromWindow   → gol; holder-ul iese din ecran
 *    onAttachedToRecyclerView   → gol; adapterul e legat de un RecyclerView
 *    onDetachedFromRecyclerView → gol; adapterul e dezlegat
 *
 * 5. CINE CHEAMĂ CE
 *    RecyclerView + LayoutManager decid:
 *      - câți holder-e încap pe ecran
 *      - care holder se refolosește pentru ce poziție
 *      - când e nevoie de un holder nou
 *    Adapterul doar răspunde: „iată un holder nou" / „am umplut holder-ul".
 *
 * 6. DE CE E CLASĂ ABSTRACTĂ ȘI NU INTERFAȚĂ
 *    Pentru că are stare: lista de observatori (AdapterDataObservable), referința la
 *    RecyclerView și implementările implicite ale metodelor opționale. O interfață nu
 *    poate ține stare.
 *
 * DIAGRAMA CELOR TREI METODE:
 *
 *   RecyclerView                         Adapter
 *   ── getItemCount() ──────────────────▶ 100
 *   ── „am nevoie de un holder" ────────▶ onCreateViewHolder(parent, 0)
 *                                            └─ inflate + new NumberViewHolder
 *   ── „umple-l pentru poziția 7" ──────▶ onBindViewHolder(holder, 7)
 *                                            └─ setText × 2
 *   ── „holder-ul a ieșit din ecran" ───▶ (îl pune în pool; adapterul nu e întrebat)
 */

/**
 * Step 3: fills the same row for a RecyclerView.
 * The ViewHolder is required here, and getView is split into two methods.
 */
public class NumberRecyclerAdapter
        extends RecyclerView.Adapter<NumberRecyclerAdapter.NumberViewHolder> {
        //                       ↑ tipul holder-ului, ca parametru generic

    // ==================================================================================
    // BLOCUL 2 — CLASA INTERIOARĂ NumberViewHolder
    // ==================================================================================
    // Teorie:  obligatorie; RecyclerView.Adapter e parametrizat cu ea.
    // Logică:  constructorul primește itemView, cheamă super(itemView) și caută copiii.
    // Risc:    fără `super(itemView)`, RecyclerView nu are ce recicla — holder-ul nu are
    //          un View asociat.
    // Ref:     capitolul 12.
    // ==================================================================================
    static class NumberViewHolder extends RecyclerView.ViewHolder {
        final TextView tvDecimal;   // final: atribuit în constructor, nu se mai schimbă
        final TextView tvBinary;    // final: atribuit în constructor, nu se mai schimbă

        NumberViewHolder(@NonNull View itemView) {
            super(itemView);                                        // obligatoriu: salvează rândul
            tvDecimal = itemView.findViewById(R.id.tvDecimal);      // căutare 1, o dată per holder
            tvBinary = itemView.findViewById(R.id.tvBinary);        // căutare 2, o dată per holder
        }
    }

    // Parametrul generic — RecyclerView.Adapter<NumberRecyclerAdapter.NumberViewHolder>
    // declară CE TIP DE HOLDER produce adapterul. De aici încolo, toate metodele
    // lucrează cu NumberViewHolder, nu cu RecyclerView.ViewHolder: onCreateViewHolder
    // îl returnează, onBindViewHolder îl primește. NICIO cast nicăieri — asta e
    // câștigul față de getTag().
    //
    // De ce NumberRecyclerAdapter.NumberViewHolder și nu doar NumberViewHolder: în
    // declarația `extends` nu ești încă în interiorul clasei, deci numele clasei
    // interioare trebuie calificat. În restul fișierului poți scrie doar NumberViewHolder.
    //
    // super(itemView) — obligatoriu. RecyclerView.ViewHolder nu are constructor fără
    // parametri, deci compilatorul te obligă să îl chemi. El salvează rândul în itemView.
    //
    // findViewById în constructorul holder-ului — asta e jumătatea de CREARE. Rulează
    // o singură dată per holder, nu la fiecare afișare.
    //
    // Câmpurile final — pot fi final pentru că sunt atribuite în constructor. La
    // NumberListAdapter nu puteau, pentru că erau atribuite după `new ViewHolder()`.
    // Diferența asta mică e exact motivul pentru care RecyclerView a mutat căutarea
    // în constructor.
    //
    // static class NumberViewHolder — din nou static, din aceleași două motive: fără
    // referință implicită la adapter și fără dependență de o instanță a adapterului.
    // Aici e `static class` fără `private`: tipul apare în semnătura publică a clasei
    // (RecyclerView.Adapter<...NumberViewHolder>), deci nu are rost să fie ascuns.
    // La NumberListAdapter holder-ul e `private static class`, pentru că nu apare în
    // nicio semnătură publică.
    //
    // @NonNull pe itemView — contract: RecyclerView nu îți dă niciodată null. Adnotarea
    // nu schimbă runtime-ul; e citită de lint și de Android Studio.

    // ==================================================================================
    // BLOCUL 3 — CÂMPUL ȘI CONSTRUCTORUL
    // ==================================================================================
    // Teorie:  primește doar lista; Context-ul se ia din `parent` la creare.
    // Logică:  this.numbers = numbers.
    // Risc:    dacă ai cere și Context, ai avea un parametru în plus care nu e folosit
    //          nicăieri în clasă.
    // Ref:     capitolul 12.
    // ==================================================================================
    private final List<Integer> numbers;   // sursa de date, prin referință

    public NumberRecyclerAdapter(List<Integer> numbers) {
        this.numbers = numbers;   // NU copiem lista
    }

    // Constructorul adapterului primește doar lista — spre deosebire de
    // NumberListAdapter, nu are nevoie de Context. Contextul se ia la nevoie din
    // parent.getContext(), în onCreateViewHolder.
    //
    // De ce nu are LayoutInflater ca câmp: pentru că îl poate lua din `parent` la
    // fiecare creare, iar creările sunt rare. Un câmp ar fi o optimizare prematură —
    // și ar cere Context în constructor.

    // ==================================================================================
    // BLOCUL 4 — onCreateViewHolder, JUMĂTATEA DE CREARE
    // ==================================================================================
    // Teorie:  creează un holder NOU: umflă rândul și construiește cutia.
    // Logică:  1. LayoutInflater.from(parent.getContext())
    //          2. inflate(item_number, parent, false)
    //          3. new NumberViewHolder(view)
    // Risc:    dacă uiți `false`, inflate întoarce părintele, nu rândul.
    // Ref:     capitolul 12.
    // ==================================================================================
    @NonNull
    @Override
    public NumberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Runs only when a new row is needed
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_number, parent, false);   // ACELAȘI layout ca treapta 2
        return new NumberViewHolder(view);                       // holder-ul, nu view-ul
    }

    // Când rulează: doar când RecyclerView are nevoie de un holder NOU — pool-ul de
    // reciclare e gol. La pornire, pentru primele rânduri vizibile; apoi din ce în ce
    // mai rar.
    //
    // `parent` — RecyclerView-ul. Din el se ia Context-ul pentru inflater și el
    // furnizează LayoutParams-ul.
    //
    // `viewType` — aici e mereu 0, pentru că nu suprascriem getItemViewType. Dacă ai
    // avea două feluri de rânduri (antet + element), ai returna 0/1 din getItemViewType
    // și ai face switch aici, ca să umfli layout-ul potrivit.
    //
    // LayoutInflater.from(parent.getContext()) — nu mai e nevoie de un câmp
    // layoutInflater, pentru că `parent` cară contextul cu el.
    //
    // .inflate(R.layout.item_number, parent, false) — exact același apel ca la ListView:
    // același layout, aceiași parametri, același false.
    //
    // return new NumberViewHolder(view) — întoarce HOLDER-UL, nu view-ul. RecyclerView
    // are nevoie de holder ca să știe ce a creat și să îl poată recicla.
    //
    // @NonNull — contract: parent nu e null, rezultatul nu e null. Lint-ul verifică.
    //
    // De ce metoda nu întoarce View: pentru că RecyclerView nu lucrează cu View-uri.
    // Lucrează cu holder-e, care știu ce copii au înăuntru. Un View returnat ar trebui
    // re-inspectat la fiecare refolosire — exact ce ViewHolder evită.

    // ==================================================================================
    // BLOCUL 5 — onBindViewHolder ȘI getItemCount
    // ==================================================================================
    // Teorie:  umple un holder existent cu datele de la poziția dată.
    // Logică:  1. numbers.get(position)  2. setText × 2
    // Risc:    dacă faci `findViewById` aici, ai pierdut tot câștigul holder-ului.
    // Ref:     capitolul 12.
    // ==================================================================================
    @Override
    public void onBindViewHolder(@NonNull NumberViewHolder holder, int position) {
        // Runs each time a row shows a number
        int number = numbers.get(position);                          // Integer → int
        holder.tvDecimal.setText(String.valueOf(number));            // coloana 1
        holder.tvBinary.setText(Integer.toBinaryString(number));     // coloana 2
    }

    // ----------------------------------------------------------------------------------
    // METODA 3 — getItemCount
    // ----------------------------------------------------------------------------------
    // Teorie:  înlocuiește getCount() din BaseAdapter.
    // Logică:  dimensiunea listei.
    // Risc:    dacă minte, onBindViewHolder primește o poziție invalidă.
    // Ref:     capitolul 12.
    // ----------------------------------------------------------------------------------
    @Override
    public int getItemCount() {
        return numbers.size();   // 100
    }

    // Când rulează onBindViewHolder: de fiecare dată când un rând ARATĂ un element —
    // la prima afișare, la fiecare refolosire după scroll, după notifyItemChanged,
    // după o animație.
    //
    // Nu există convertView — holder-ul vine gata creat. Nu există
    // `if (convertView == null)`, pentru că decizia a fost luată deja de RecyclerView.
    //
    // Nu există findViewById — referințele sunt în holder, luate la creare.
    //
    // numbers.get(position) direct — adapterul nu are getItem(position). Dacă ai nevoie
    // de el (de exemplu pentru un click), îl adaugi ca metodă proprie.
    //
    // getItemCount() înlocuiește getCount() — același rol: câte elemente are lista.
    //
    // Dacă getItemCount() minte: dacă întoarce mai mult decât numbers.size(),
    // onBindViewHolder ajunge la numbers.get(position) cu o poziție invalidă →
    // IndexOutOfBoundsException. Dacă întoarce mai puțin, ultimele elemente pur și
    // simplu nu apar.
    //
    // De ce onBindViewHolder întoarce void: pentru că holder-ul e deja la RecyclerView.
    // Nu trebuie returnat nimic — metoda doar modifică un obiect pe care RecyclerView
    // îl are deja. La getView, returnarea era necesară pentru că ListView nu avea altă
    // cale să afle ce view să adauge.
    //
    // Ce se întâmplă dacă apeși pe un rând: nimic. Nu există listener. Într-o aplicație
    // reală, ai pune un OnClickListener pe holder.itemView chiar în onBindViewHolder —
    // și ai avea grijă să folosești holder.getAdapterPosition(), nu `position`, pentru
    // că poziția se poate schimba între bind și click.
    //
    // ==================================================================================
    // GETVIEW()  vs  onCreateViewHolder() + onBindViewHolder() — ACEEAȘI IMAGINE, DESPICATĂ
    // ==================================================================================
    //
    // ┌─────────────────────────────────────────────────────────────────────────────────┐
    // │                          LISTVIEW: O SINGURĂ METODĂ                             │
    // │                                                                                 │
    // │   getView(position, convertView, parent)                                        │
    // │   ├─────────────────────────────────────────────────────────────────────────┐   │
    // │   │ PARTEA A — CREARE                                      doar dacă null   │   │
    // │   │   if (convertView == null) {                                           │   │
    // │   │       convertView = inflate(...)          ← XML → obiecte              │   │
    // │   │       holder = new ViewHolder()            ← cutia goală               │   │
    // │   │       holder.tvDecimal = findViewById(...) ← căutare                   │   │
    // │   │       holder.tvBinary  = findViewById(...) ← căutare                   │   │
    // │   │       convertView.setTag(holder)           ← lipește cutia             │   │
    // │   │   }                                                                    │   │
    // │   ├─────────────────────────────────────────────────────────────────────────┤   │
    // │   │ PARTEA B — UMPLERE                                    la FIECARE apel  │   │
    // │   │   else { holder = getTag() }              ← ia cutia                   │   │
    // │   │   holder.tvDecimal.setText(...)                                        │   │
    // │   │   holder.tvBinary.setText(...)                                         │   │
    // │   │   return convertView                                                   │   │
    // │   └─────────────────────────────────────────────────────────────────────────┘   │
    // │                                                                                 │
    // │   ↑ cele două părți sunt în ACEEAȘI metodă, separate doar de un `if`            │
    // └─────────────────────────────────────────────────────────────────────────────────┘
    //
    //                                     ↓  despicarea  ↓
    //
    // ┌─────────────────────────────────────────────────────────────────────────────────┐
    // │                       RECYCLERVIEW: DOUĂ METODE SEPARATE                        │
    // │                                                                                 │
    // │   onCreateViewHolder(parent, viewType)      onBindViewHolder(holder, position)  │
    // │   ├──────────────────────────────────┐      ├──────────────────────────────────┤
    // │   │ PARTEA A — CREARE                │      │ PARTEA B — UMPLERE               │
    // │   │   View view = inflate(...)       │      │   int number = numbers.get(pos)  │
    // │   │   return new NumberViewHolder(   │      │   holder.tvDecimal.setText(...)  │
    // │   │       view)                      │      │   holder.tvBinary.setText(...)   │
    // │   │       ├─ super(itemView)         │      │   (void — nu returnează nimic)   │
    // │   │       ├─ findViewById × 2        │      │                                  │
    // │   │       └─ câmpuri final           │      │                                  │
    // │   └──────────────────────────────────┘      └──────────────────────────────────┘
    // │                                                                                 │
    // │   CINE DECIDE CARE SE CHEAMĂ:                                                   │
    // │     pool gol            → onCreateViewHolder + onBindViewHolder                 │
    // │     pool are un holder  → doar onBindViewHolder                                 │
    // │                                                                                 │
    // │   CINE ȚINE LOCUL LUI convertView: pool-ul intern (RecycledViewPool)            │
    // │   CINE ȚINE LOCUL LUI setTag/getTag: tipul generic al adapterului               │
    // └─────────────────────────────────────────────────────────────────────────────────┘
    //
    // Citește diagrama de sus în jos, apoi de jos în sus. Sus: o metodă, două ramuri,
    // un `if` care le desparte. Jos: două metode, fiecare cu o singură ramură, iar
    // decizia mutată în framework. ACEEAȘI MUNCĂ, ÎMPĂRȚITĂ ALTFEL.
    //
    // CE SE CÂȘTIGĂ CONCRET:
    //   reumplere fără creare   pool-ul dă un holder gata făcut; doar onBindViewHolder
    //   animații                RecyclerView știe ce holder s-a mutat
    //   actualizări parțiale    notifyItemChanged(5) re-leagă doar poziția 5
    //   cod tipat               holder-ul e parametru, nu Object dintr-un tag
    //   testabilitate           poți testa onBindViewHolder cu un holder construit manual
    //
    // CE SE PIERDE: două metode în loc de una, un parametru generic în plus,
    // super(itemView) obligatoriu. Pentru o listă de 100 de numere, câștigul e mic.
    // Pentru o listă de 10.000 de imagini, e diferența dintre 60fps și 20fps.
    //
    // ==================================================================================
    // CE SE ÎNTÂMPLĂ DACĂ UIȚI super(itemView)
    // ==================================================================================
    //
    //   NumberViewHolder(@NonNull View itemView) {
    //       // super(itemView);   ← scos
    //       tvDecimal = itemView.findViewById(R.id.tvDecimal);
    //   }
    //
    // Nu compilează: RecyclerView.ViewHolder nu are constructor fără parametri, iar
    // compilatorul încearcă să cheme super() implicit. Erora e la compilare, nu la
    // runtime — deci o prinzi imediat:
    //
    //   error: constructor ViewHolder in class ViewHolder cannot be applied to given types;
    //     required: View
    //     found: no arguments
    //
    // Dacă ai chema super(null), ar compila, dar itemView ar fi null și RecyclerView ar
    // crăpa la primul layout, când încearcă să adauge holder-ul în arbore.
    //
    // ==================================================================================
    // RECYCLEDVIEWPOOL — CE ȚINE ȘI CÂND SE GOLEȘTE
    // ==================================================================================
    //
    //   RecycledViewPool
    //     ├── SparseArray<ScrapData> pe viewType
    //     │     ├── viewType 0 → listă de holder-e (max 5 implicit)
    //     │     └── viewType 1 → listă de holder-e
    //     └── fiecare holder ține itemView + referințele din constructor
    //
    // Implicit, pool-ul păstrează maximum 5 holder-e per tip. Restul sunt lăsate să
    // fie colectate. La o listă cu 10 rânduri vizibile, 5 în pool sunt de ajuns.
    //
    // Se golește la setAdapter cu alt adapter, la setRecycledViewPool(null) și la
    // distrugerea RecyclerView-ului. Nu se golește la scroll.
    //
    // ==================================================================================
    // CUM SE ADAUGĂ UN CLICK PE RÂND
    // ==================================================================================
    //
    //   @Override
    //   public void onBindViewHolder(@NonNull NumberViewHolder holder, int position) {
    //       int number = numbers.get(position);
    //       holder.tvDecimal.setText(String.valueOf(number));
    //       holder.tvBinary.setText(Integer.toBinaryString(number));
    //       holder.itemView.setOnClickListener(v ->
    //               Toast.makeText(v.getContext(),
    //                       "Poziția " + holder.getAdapterPosition(),
    //                       Toast.LENGTH_SHORT).show());
    //   }
    //
    // De ce getAdapterPosition() și nu `position`: între momentul bind-ului și momentul
    // click-ului, lista se poate schimba (un element șters, unul adăugat). `position`
    // e valoarea de la bind, care poate fi veche; getAdapterPosition() întoarce poziția
    // curentă. În codul din repo nu există click, deci nu există nici problema.
    //
    // ==================================================================================
    // DE CE HOLDER-UL NU ARE NEVOIE DE ADAPTER
    // ==================================================================================
    //
    // Un ViewHolder ține itemView și doi copii. Nu are nevoie de listă, de poziție sau
    // de adapter ca să existe. De aceea poate fi `static`: nu depinde de nicio instanță
    // a clasei exterioare. Toată informația vine ca parametru în onBindViewHolder.
    //
    // La NumberListAdapter, holder-ul e construit în getView și umplut imediat, tot
    // acolo. Aici e construit în onCreateViewHolder și umplut în altă metodă, cu date
    // primite ca parametru. Diferența de design se vede în semnătura constructorului.
}
