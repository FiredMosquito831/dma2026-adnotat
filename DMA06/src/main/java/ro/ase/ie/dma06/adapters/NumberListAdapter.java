/*
 * ======================================================================================
 * FIȘIER: NumberListAdapter.java
 * ======================================================================================
 * LOCAȚIE:  DMA06/src/main/java/ro/ase/ie/dma06/adapters/NumberListAdapter.java
 * PACHET:   ro.ase.ie.adapters        ← fără `dma06`, ca și `data`
 * ROL:      adaptează List<Integer> la rândul cu două coloane (item_number.xml),
 *           pentru un ListView. Este echivalentul manual al lui ArrayAdapter.
 *
 * STRUCTURA FIȘIERULUI (4 părți):
 *   1. ViewHolder            — 4 linii: două referințe la TextView
 *   2. câmpuri + constructor — 8 linii: lista și inflater-ul
 *   3. evidență              — 3 metode: getCount, getItem, getItemId
 *   4. getView               — 22 de linii: creare SAU refolosire + umplere
 *
 * CE TREBUIE SĂ ȘTII ÎNAINTE:
 *   - BaseAdapter lasă 4 metode abstracte; restul le implementează el.
 *   - `convertView` e rândul reciclat de ListView; `null` = nu există niciunul liber.
 *   - `setTag` / `getTag` lipesc ViewHolder-ul de rând. Fără ele, NPE la primul scroll.
 *   - `findViewById` se cheamă o singură dată per rând, nu la fiecare afișare.
 * ======================================================================================
 */

// ======================================================================================
// BLOCUL 1 — PACHETUL ȘI IMPORTURILE
// ======================================================================================
// Pachetul declarat e ro.ase.ie.adapters, deși folderul e ro/ase/ie/dma06/adapters/.
// Aceeași situație ca la NumberData: compilează, dar importurile trebuie să folosească
// pachetul declarat.
//
// Importurile se împart în trei grupuri:
//   android.*   — framework: Context, LayoutInflater, View, ViewGroup, BaseAdapter, TextView
//   java.util   — List
//   ro.ase.*    — R (resursele)
// ======================================================================================

package ro.ase.ie.adapters;   // pachetul declarat nu conține `dma06`, deși folderul da

import android.content.Context;      // necesar doar pentru LayoutInflater
import android.view.LayoutInflater;  // transformă XML în obiecte View
import android.view.View;            // rândul (umflat sau reciclat)
import android.view.ViewGroup;       // părintele rândului: ListView-ul
import android.widget.BaseAdapter;   // clasa de bază pentru adaptoare proprii
import android.widget.TextView;      // cele două coloane

import java.util.List;               // interfața pentru lista de numere

import ro.ase.ie.dma06.R;            // resursele aplicației

// ======================================================================================
// THEORY BLOCK: BASEADAPTER ȘI TIPARUL VIEWHOLDER
// ======================================================================================
/*
 * 1. CE ESTE BASEADAPTER
 *    -   O clasă abstractă din android.widget care implementează ListAdapter și
 *        SpinnerAdapter.
 *    -   Implementează tot ce ține de observatori, tipuri de view și enabled state:
 *        isEmpty(), areAllItemsEnabled(), isEnabled(int), getItemViewType(int),
 *        getViewTypeCount(), hasStableIds(), registerDataSetObserver().
 *    -   Lasă abstracte EXACT patru metode:
 *          getCount()                                                  câte elemente sunt
 *          getItem(int position)                                       obiectul de la poziție
 *          getItemId(int position)                                     id-ul stabil
 *          getView(int position, View convertView, ViewGroup parent)   rândul
 *
 * 2. CÂND E CHEMATA FIECARE METODĂ
 *    getCount()   — la fiecare layout al listei, la calculul barei de scroll, după
 *                   notifyDataSetChanged. Este prima întrebare pusă vreodată.
 *    getItem()    — NU e chemată de ListView ca să deseneze. E chemată de
 *                   AdapterView.getItemAtPosition(), de performItemClick() și de
 *                   codul tău (inclusiv din getView, ca aici).
 *    getItemId()  — la click (performItemClick primește id-ul), la getSelectedItemId().
 *                   Cu hasStableIds() == true, ListView păstrează selecția între
 *                   reîncărcări.
 *    getView()    — pentru fiecare rând vizibil, la fiecare layout, la fiecare pas de
 *                   scroll, după fiecare notifyDataSetChanged.
 *
 * 3. TIPARUL VIEWHOLDER
 *    Problema: findViewById parcurge arborele. Pe un rând cu 2 view-uri e ieftin;
 *    pe 10 rânduri × 2 view-uri × fiecare scroll, începe să conteze.
 *    Soluția: cauți o singură dată, la crearea rândului, și lipești rezultatul de rând
 *    cu setTag(). La refolosire, îl iei înapoi cu getTag().
 *
 *    Fără holder:                          Cu holder:
 *    ┌──────────────────────────┐          ┌──────────────────────────┐
 *    │ rând (View)              │          │ rând (View)              │
 *    │  ├─ tvDecimal            │          │  ├─ tvDecimal            │
 *    │  └─ tvBinary             │          │  ├─ tvBinary             │
 *    │  (căutate la fiecare     │          │  └─ tag → ViewHolder     │
 *    │   afișare)               │          │      ├─ tvDecimal ───────┘
 *    └──────────────────────────┘          │      └─ tvBinary
 *                                          └──────────────────────────┘
 *
 * 4. DE CE `static`
 *    O clasă interioară non-statică ține o referință implicită la instanța exterioară
 *    (`this$0`). ViewHolder-ul e atașat de un View, iar View-ul poate trăi mai mult
 *    decât adapterul (ListView îl ține în RecycleBin). `static` elimină referința.
 *    În plus, un holder non-static nu poate fi construit fără un adapter — ceea ce
 *    încurcă orice test.
 *
 * 5. CICLUL UNUI RÂND
 *    creare:     inflate → findViewById × 2 → new ViewHolder → setTag → umplere
 *    refolosire: getTag → umplere
 *    ieșire:     rândul intră în RecycleBin, cu tag-ul lui cu tot
 *
 * 6. GRAFUL DE OBIECTE
 *
 *    TwoColumnActivity
 *      └── NumberListAdapter            (câmp local; trăiește cât activitatea)
 *            ├── List<Integer>          (REFERINȚĂ la lista din NumberData)
 *            │     └── 100 × Integer
 *            ├── LayoutInflater         (unul singur, refolosit)
 *            └── getView() creează:
 *                  ViewHolder           (lipit de rând cu setTag)
 *                    ├── tvDecimal ──┐
 *                    └── tvBinary  ──┤
 *                                    ▼
 *                  View (LinearLayout-ul din item_number.xml)
 *                    ├── TextView tvDecimal
 *                    └── TextView tvBinary
 *
 *    Săgețile merg într-un singur sens: activitatea ține adapterul, adapterul ține
 *    lista și creează holder-e, holder-ul ține view-urile. Nicio săgeată nu se
 *    întoarce — de aceea holder-ul poate fi `static`.
 */

/** Step 2: fills a two-column row for a ListView. */
public class NumberListAdapter extends BaseAdapter {   // extinde scheletul, nu ArrayAdapter

    // ==================================================================================
    // BLOCUL 2 — CLASA INTERIOARĂ ViewHolder
    // ==================================================================================
    // Teorie:  cutia care ține referințele la view-urile rândului, ca să nu le mai cauți.
    // Logică:  două câmpuri, atât. Nu are metode, nu are logică, nu are constructor.
    // Risc:    dacă nu e `static`, ține o referință la adapter (`this$0`) și prelungește
    //          viața acestuia cât timp rândul e în RecycleBin.
    // Ref:     curs 4 (ListView și adaptoare).
    // ==================================================================================
    private static class ViewHolder {
        TextView tvDecimal;   // referința la coloana 1
        TextView tvBinary;    // referința la coloana 2
    }

    // De ce e o clasă interioară: cele două TextView-uri trebuie să călătorească
    // împreună. Un holder e o cutie cu două referințe, atârnată de rând cu setTag.
    // Fără el, ai avea două variabile separate și niciun loc unde să le păstrezi
    // între apeluri.
    //
    // De ce `private`: doar acest adapter îl folosește. Nu apare în nicio semnătură
    // publică. (La NumberRecyclerAdapter e `static class` fără private, pentru că
    // tipul apare în semnătura publică a adapterului.)
    //
    // De ce câmpurile NU sunt `final`: sunt atribuite DUPĂ `new ViewHolder()`, în
    // getView. Nu pot fi final pentru că nu sunt inițializate în constructor. Exact
    // asta repară versiunea RecyclerView: mută findViewById în constructorul
    // holder-ului și le poate face final.

    // ==================================================================================
    // BLOCUL 3 — CÂMPURILE ȘI CONSTRUCTORUL
    // ==================================================================================
    // Teorie:  două referințe finale: lista și inflater-ul.
    // Logică:  ambele se stabilesc o singură dată, în constructor.
    // Risc:    dacă ai copia lista (`new ArrayList<>(numbers)`), ai avea două liste și
    //          modificările din afară nu s-ar mai vedea.
    // Ref:     capitolul 12.
    // ==================================================================================
    private final List<Integer> numbers;          // sursa de date, ținută prin referință
    private final LayoutInflater layoutInflater;  // un singur inflater, refolosit

    public NumberListAdapter(Context context, List<Integer> numbers) {
        this.layoutInflater = LayoutInflater.from(context);  // îl obținem o dată, nu la fiecare rând
        this.numbers = numbers;                              // NU copiem lista
    }

    // `private final List<Integer> numbers` — referința nu se schimbă niciodată.
    // Conținutul listei s-ar putea schimba, dar adapterul arată mereu spre aceeași listă.
    //
    // `private final LayoutInflater layoutInflater` — LayoutInflater.from(context) face
    // o căutare internă. O facem O DATĂ, în constructor, nu la fiecare rând. În plus,
    // getView nu mai are nevoie de Context.
    //
    // `this.numbers = numbers` — nu se copiază lista. Adapterul primește referința la
    // lista construită de NumberData. Dacă ai modifica lista din afară, adapterul ar
    // vedea modificarea — dar nu ar redesena nimic fără notifyDataSetChanged().
    //
    // De ce constructorul nu validează nimic: nu are ce. `context` null ar crăpa oricum
    // la LayoutInflater.from, iar o listă null ar crăpa la primul getCount().

    // ==================================================================================
    // BLOCUL 4 — CELE TREI METODE DE EVIDENȚĂ
    // ==================================================================================
    // Trei metode scurte, fiecare răspunde la o întrebare a lui ListView. Niciuna nu
    // desenează nimic.
    // ==================================================================================

    // ----------------------------------------------------------------------------------
    // METODA 1 — getCount
    // ----------------------------------------------------------------------------------
    // Teorie:  prima întrebare a lui ListView: câte rânduri există?
    // Logică:  dimensiunea listei.
    // Risc:    dacă întorci 0, lista e goală; dacă întorci mai mult decât size(),
    //          getView ajunge la o poziție invalidă → IndexOutOfBoundsException.
    // Ref:     capitolul 12.
    // ----------------------------------------------------------------------------------
    @Override
    public int getCount() {
        return numbers.size();      // câte rânduri: exact elementele listei
    }

    // ----------------------------------------------------------------------------------
    // METODA 2 — getItem
    // ----------------------------------------------------------------------------------
    // Teorie:  obiectul de la poziția dată. NU e chemată de ListView pentru desenare.
    // Logică:  numbers.get(position), cu verificarea de bounds făcută de ArrayList.
    // Risc:    poziție invalidă → IndexOutOfBoundsException; element null → NPE la
    //          unboxing în getView.
    // Ref:     capitolul 12.
    // ----------------------------------------------------------------------------------
    @Override
    public Integer getItem(int position) {
        return numbers.get(position);   // obiectul de la poziția dată
    }

    // ----------------------------------------------------------------------------------
    // METODA 3 — getItemId
    // ----------------------------------------------------------------------------------
    // Teorie:  identitatea stabilă a elementului, folosită la click și la selecție.
    // Logică:  numerele sunt unice, deci numărul însuși e un id valid.
    // Risc:    într-o listă cu duplicate, două rânduri ar avea același id și selecția
    //          ar sări între ele.
    // Ref:     capitolul 12.
    // ----------------------------------------------------------------------------------
    @Override
    public long getItemId(int position) {
        // Each number is unique, so the number itself can be the ID
        return numbers.get(position);   // Integer → int → long (widening)
    }

    // getCount() — primul lucru pe care îl întreabă ListView. Determină câte rânduri
    // există și cât de lungă e bara de scroll. Dacă întorci 0, lista e goală.
    //
    // getItem(position) — obiectul de la poziția dată. ListView îl cheamă când cineva
    // cere elementul de la o poziție (getItemAtPosition), iar adapterul îl cheamă
    // singur în getView.
    //
    // getItemId(position) — identitatea elementului. numbers.get(position) întoarce
    // Integer, care se unboxează la int și apoi se extinde la long (widening,
    // conversie implicită sigură). Pentru că numerele 1–100 sunt unice, numărul
    // însuși e un ID valid. Într-o listă cu duplicate (două filme cu același titlu)
    // ai avea nevoie de un câmp id separat.
    //
    // Când e folosit getItemId: ListView îl cheamă când construiește argumentele unui
    // click. Identitatea stabilă (hasStableIds() == true) ar permite widget-ului să
    // păstreze selecția între reîncărcări; aici e false implicit, dar metoda trebuie
    // oricum implementată — e abstractă în BaseAdapter.
    //
    // @Override pe toate trei — metodele vin din Adapter / BaseAdapter. Fără adnotare
    // ar fi tot override, dar compilatorul nu ți-ar semnala o semnătură greșită.
    //
    // De ce getItem întoarce Integer și nu int: semnătura din Adapter e
    // `Object getItem(int)`. NumberListAdapter o restrânge la Integer — allowed,
    // pentru că Integer e subtip al lui Object (return type covariance). Câștigul:
    // getView nu mai are nevoie de cast.
    //
    // TABELUL CELOR PATRU METODE:
    //
    //   Metodă          Cine o cheamă                        Când
    //   getCount()      ListView.onMeasure/layoutChildren    la fiecare layout
    //   getItem(int)    AdapterView.getItemAtPosition        la cerere / din getView
    //   getItemId(int)  AdapterView.getSelectedItemId        la click / selecție
    //   getView(...)    AbsListView.obtainView               pentru fiecare rând vizibil

    // ==================================================================================
    // BLOCUL 5 — getView, INIMA ADAPTERULUI
    // ==================================================================================
    // Teorie:  singura metodă care produce un View. Are două ramuri: creare și refolosire.
    // Logică:  1. dacă convertView e null → inflate + findViewById + setTag
    //          2. altfel → getTag
    //          3. umple ambele TextView-uri
    //          4. returnează convertView
    // Risc:    fără setTag, getTag întoarce null și prima linie care atinge holder-ul
    //          dă NullPointerException. Lista merge până la primul scroll.
    // Ref:     capitolul 12 (secțiunea despre reciclare).
    // ==================================================================================
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;                       // declarat înainte de if, folosit după

        if (convertView == null) {
            // New row: inflate it and store its views
            convertView = layoutInflater.inflate(R.layout.item_number, parent, false);
            holder = new ViewHolder();                                      // cutia goală
            holder.tvDecimal = convertView.findViewById(R.id.tvDecimal);    // căutare 1
            holder.tvBinary = convertView.findViewById(R.id.tvBinary);      // căutare 2
            convertView.setTag(holder);                                     // lipește cutia de rând
        } else {
            // Reused row: get its stored views
            holder = (ViewHolder) convertView.getTag();                     // ia cutia înapoi
        }

        int number = getItem(position);                                     // Integer → int
        holder.tvDecimal.setText(String.valueOf(number));                   // coloana 1: „42"
        holder.tvBinary.setText(Integer.toBinaryString(number));            // coloana 2: „101010"

        return convertView;                                                 // ACELAȘI rând, umplut
    }

    // ----------------------------------------------------------------------------------
    // SEMNĂTURA — ce înseamnă fiecare parametru
    // ----------------------------------------------------------------------------------
    //
    //   position      al câtelea rând din listă (0…99). NU e poziția pe ecran.
    //                 La primul ecran: 0…9. După scroll: 10, 11, …
    //   convertView   un rând RECICLAT de ListView, sau null dacă nu există niciunul
    //                 liber. E un View, nu un ViewHolder — de aici setTag/getTag.
    //   parent        ListView-ul care cere rândul. Nu îl adaugi tu în arbore;
    //                 ListView o face după ce getView întoarce. Îți trebuie doar
    //                 pentru inflate, ca să se calculeze LayoutParams-ul corect.
    //
    // ----------------------------------------------------------------------------------
    // CELE DOUĂ RAMURI
    // ----------------------------------------------------------------------------------
    //
    //   convertView == null   →  rând NOU:       inflate + findViewById + setTag
    //   convertView != null   →  rând REFOLOSIT: getTag, fără nicio căutare
    //
    // La pornire, ListView cere primele ~10 rânduri cu convertView == null — nu are
    // ce recicla. Apoi, la fiecare scroll, rândul care iese din ecran intră în
    // RecycleBin și e dat înapoi la următorul apel:
    //
    //   getView(0,  null, listView)    → null → inflate + findViewById + setTag
    //   getView(1,  null, listView)    → null → inflate + findViewById + setTag
    //   ...
    //   getView(9,  null, listView)    → null → inflate + findViewById + setTag
    //         ── derulezi o poziție ──
    //   getView(10, rândul0, listView) → != null → getTag + setText
    //
    // De ce primele apeluri au convertView == null: pentru că RecycleBin-ul e gol.
    // Nu există niciun rând care să fi ieșit din ecran, pentru că nu ai derulat încă.
    //
    // De ce getView(10, rândul0, ...) și nu getView(10, rândul9, ...): ListView ia
    // rândul din capul listei de reciclare, nu neapărat ultimul ieșit. Ordinea exactă
    // ține de implementarea RecycleBin; pentru tine contează doar că UN rând reciclat
    // vine, nu care anume.
    //
    // ----------------------------------------------------------------------------------
    // convertView == null / setTag / getTag, PAS CU PAS
    // ----------------------------------------------------------------------------------
    //
    // PASUL 1 — primul apel, convertView == null:
    //
    //   ListView nu are niciun rând liber
    //     → getView(0, null, parent)
    //         convertView = layoutInflater.inflate(item_number, parent, false)
    //             ├─ creează LinearLayout-ul din XML
    //             ├─ creează cele două TextView-uri
    //             └─ NU îl atașează de parent (al treilea argument = false)
    //         holder = new ViewHolder()                    ← obiect gol, două câmpuri null
    //         holder.tvDecimal = convertView.findViewById(R.id.tvDecimal)
    //         holder.tvBinary  = convertView.findViewById(R.id.tvBinary)
    //         convertView.setTag(holder)                   ← rândul ține acum holder-ul
    //         setText × 2
    //         return convertView
    //
    // PASUL 2 — al doilea apel pentru același rând, după scroll:
    //
    //   rândul 0 iese din ecran
    //     → ListView îl scoate din arbore și îl pune în RecycleBin
    //     → getView(10, rândul0, parent)
    //         convertView = rândul0                        ← NU e null
    //         holder = (ViewHolder) convertView.getTag()   ← exact holder-ul de la pasul 1
    //         setText × 2                                  ← doar textul se schimbă
    //         return convertView
    //
    // PASUL 3 — ce se întâmplă cu tag-ul la reciclare: nimic. setTag nu e resetat de
    // ListView, nu e atins de removeView, nu e curățat de RecycleBin. Rândul pleacă
    // în pool cu holder-ul lipit și se întoarce cu el. De aceea funcționează: tag-ul
    // e al rândului, nu al poziției.
    //
    // Ce conține holder-ul între apeluri: aceleași două referințe la aceleași
    // TextView-uri. Rândul nu se reconstruiește, deci nici view-urile nu se schimbă.
    // Se schimbă doar textul din ele.
    //
    // De ce setTag e chemat doar pe ramura null: pentru că doar acolo se creează
    // holder-ul. Pe ramura else, holder-ul există deja, lipit de rând. Dacă ai chema
    // setTag și pe else, ai crea un holder nou la fiecare afișare — exact ce
    // încercai să eviți.
    //
    // ----------------------------------------------------------------------------------
    // DANGER! — exact NPE-ul când lipsește setTag
    // ----------------------------------------------------------------------------------
    //
    // Dacă scoți linia `convertView.setTag(holder);`, codul compilează. Lista se
    // afișează corect la pornire — primele 10 rânduri trec prin ramura null, unde
    // holder-ul e construit local. La primul scroll, însă:
    //
    //   holder = (ViewHolder) convertView.getTag();   // getTag() → null
    //                                                 // cast-ul (ViewHolder) null → null
    //   ...
    //   holder.tvDecimal.setText(...);                // ← aici crapă
    //
    // De ce cast-ul nu aruncă: un cast aplicat lui null întoarce null. (ViewHolder) null
    // e legal. ClassCastException apare doar când obiectul există și nu e de tipul
    // cerut. De aceea excepția nu vine de la linia cu cast, ci de la prima linie care
    // ATINGE holder-ul.
    //
    // Excepția exactă, pe ART:
    //
    //   java.lang.NullPointerException: Attempt to read from field
    //   'android.widget.TextView ro.ase.ie.adapters.NumberListAdapter$ViewHolder.tvDecimal'
    //   on a null object reference
    //       at ro.ase.ie.adapters.NumberListAdapter.getView(NumberListAdapter.java:47)
    //
    // De ce „read from field" și nu „invoke virtual method": pentru că prima operație
    // e citirea câmpului holder.tvDecimal, nu apelul setText. Dacă holder-ul ar
    // exista, dar tvDecimal ar fi null (de exemplu ai uitat un findViewById), atunci
    // ai primi varianta cu metoda:
    //
    //   java.lang.NullPointerException: Attempt to invoke virtual method
    //   'void android.widget.TextView.setText(java.lang.CharSequence)'
    //   on a null object reference
    //
    // Varianta cu tag greșit: dacă ai pune convertView.setTag("rând"), cast-ul ar
    // arunca imediat:
    //
    //   java.lang.ClassCastException: java.lang.String cannot be cast to
    //   ro.ase.ie.adapters.NumberListAdapter$ViewHolder
    //       at ro.ase.ie.adapters.NumberListAdapter.getView(NumberListAdapter.java:44)
    //
    // Fără setTag: lista funcționează până la primul scroll. Apoi crapă cu
    // NullPointerException. E cea mai frecventă greșeală la ListView cu adapter
    // propriu — și singura care trece de compilare, de primul ecran și de demo-ul
    // făcut fără să derulezi.
    //
    // Cum o depanezi în 30 de secunde: dacă lista se vede corect la deschidere și
    // crapă la scroll, e aproape sigur setTag lipsă sau pus pe ramura greșită.
    //
    // ----------------------------------------------------------------------------------
    // inflate — de ce (layout, parent, false)
    // ----------------------------------------------------------------------------------
    //
    // De ce parent: ca să se genereze LayoutParams-ul corect pentru rând. Fără el,
    // inflate nu știe ce parametri să creeze, iar match_parent din XML nu se poate
    // interpreta. LayoutParams e clasa care spune părintelui cum să așeze copilul;
    // LinearLayout are LinearLayout.LayoutParams, ListView are AbsListView.LayoutParams.
    //
    // De ce false: înseamnă NU atașa acum. ListView va atașa rândul când îi vine
    // rândul. Cu true, inflate ar returna PĂRINTELE, nu rândul — și ai primi un
    // ListView în loc de un rând, cu ClassCastException la findViewById.
    //
    // Ce se întâmplă dacă pui null în loc de parent: inflate nu are de unde lua
    // LayoutParams-ul, deci rândul primește LayoutParams implicite. Pe un rând cu
    // match_parent înălțime, rezultatul ar fi un rând de înălțime zero sau greșită.
    // Se vede imediat: rândurile se suprapun sau dispar.
    //
    // findViewById de două ori — doar pe rândul nou. Căutarea parcurge arborele de
    // view-uri; pe un rând cu două TextView-uri e ieftină, dar înmulțită cu 100 de
    // rânduri și cu fiecare scroll devine vizibilă.
    //
    // setTag(holder) — View.setTag(Object) e un câmp liber pe orice View. Nu are
    // legătură cu afișarea; e doar un loc unde atârni un obiect. Aici lipim
    // holder-ul de rândul lui.
    //
    // getTag() — întoarce Object, deci e nevoie de cast. Cast-ul NU e verificat de
    // compilator: dacă ai pus altceva în tag, ClassCastException la runtime; dacă
    // ai uitat setTag, getTag() întoarce null și primești NullPointerException la
    // prima linie care atinge holder-ul.
    //
    // ----------------------------------------------------------------------------------
    // PARTEA DE UMPLERE
    // ----------------------------------------------------------------------------------
    //
    //   int number = getItem(position);                    // Integer → int (unboxing)
    //   holder.tvDecimal.setText(String.valueOf(number));  // „42"
    //   holder.tvBinary.setText(Integer.toBinaryString(number));  // „101010"
    //
    // int number = getItem(position) — getItem întoarce Integer, atribuirea îl
    // unboxează. Dacă lista ar conține null, aici ai avea NullPointerException.
    // Aici nu se întâmplă: NumberData produce 1–100.
    //
    // String.valueOf(number) — echivalent cu Integer.toString(number). Mai explicit
    // decât number + "", care creează un StringBuilder în plus.
    //
    // Integer.toBinaryString(number) — reprezentarea binară fără zerouri inițiale și
    // fără prefix: 1 → "1", 100 → "1100100". De aceea coloana a doua nu e aliniată
    // frumos la stânga, iar item_number.xml o aliniază la dreapta și îi pune font
    // monospațiat.
    //
    // return convertView — întoarce exact rândul primit, acum umplut. Nu construiești
    // un View nou la fiecare apel; asta e toată economia.
    //
    // De ce nu return holder.tvDecimal sau altceva: ListView are nevoie de rândul
    // întreg, nu de un copil. Rândul e LinearLayout-ul cu două TextView-uri.
    //
    // ----------------------------------------------------------------------------------
    // CICLUL DE RECICLARE, CU CE ȚINE convertView ÎN FIECARE MOMENT
    // ----------------------------------------------------------------------------------
    //
    //   ECRAN (10 rânduri vizibile)                RecycleBin (pool-ul ListView-ului)
    //   ┌───────────────────────────────┐
    //   │ rând 0   poziția 0   tag=H0   │
    //   │ rând 1   poziția 1   tag=H1   │
    //   │ rând 2   poziția 2   tag=H2   │
    //   │ ...                           │
    //   │ rând 9   poziția 9   tag=H9   │
    //   └───────────────────────────────┘
    //
    //   ① DERULEZI SUS cu un rând
    //      rând 0 iese pe sus
    //        → ListView.removeView(rând0)
    //        → RecycleBin.add(rând0)          ← rând0 păstrează tag=H0
    //      ListView cere rândul pentru poziția 10:
    //        getView(10, rând0, parent)
    //          convertView = rând0             ← obiect View, NU null
    //          getTag()    = H0                ← holder-ul creat la poziția 0
    //          H0.tvDecimal.setText("11")
    //          H0.tvBinary.setText("1011")
    //          return rând0
    //
    //   ② ECRANUL ARATĂ ACUM
    //   ┌───────────────────────────────┐
    //   │ rând 1   poziția 1   tag=H1   │
    //   │ rând 2   poziția 2   tag=H2   │
    //   │ ...                           │
    //   │ rând 9   poziția 9   tag=H9   │
    //   │ rând 0   poziția 10  tag=H0   │   ← același View, alt text
    //   └───────────────────────────────┘
    //
    //   ③ DERULEZI JOS înapoi
    //      rând 0 iese pe jos → RecycleBin
    //      getView(0, rând0, parent)
    //          getTag() = H0
    //          setText("1"), setText("1")   ← se rescrie ce era la început
    //
    // Ce trebuie să reții din diagramă: convertView nu e „un rând oarecare", e un
    // obiect View anume, cu tag-ul lui. Poziția nu călătorește cu rândul; textul da.
    // De aceea setText se face ÎNTOTDEAUNA, pe ambele ramuri — dacă ai sări peste
    // umplere pe ramura else, rândul ar afișa textul vechi.
    //
    // Câte rânduri există în total: aproximativ atâtea câte încap pe ecran, plus
    // unul-doi în buffer. Nu 100. Ăsta e tot câștigul: 10 obiecte View în loc de 100,
    // indiferent cât derulezi.
    //
    // Când se golește pool-ul: la setAdapter cu un adapter nou, la notifyDataSetChanged
    // (parțial), la distrugerea ListView-ului. Nu se golește la scroll — de asta
    // funcționează.
    //
    // ----------------------------------------------------------------------------------
    // CE S-AR ÎNTÂMPLA DACĂ AI IGNORA RECICLAREA
    // ----------------------------------------------------------------------------------
    //
    // Varianta greșită, pentru comparație — NU e în repo:
    //
    //   View row = layoutInflater.inflate(R.layout.item_number, parent, false);
    //   // ... findViewById, setText ...
    //   return row;
    //
    // -   100 de rânduri umflate pentru 100 de elemente, deși pe ecran încap ~10;
    // -   memoria crește cu fiecare scroll, pentru că fiecare rând rămas în arbore
    //     ține un View și două TextView;
    // -   ListView nu mai are ce recicla, deci nici convertView nu mai vine;
    // -   scroll-ul devine sacadat, pentru că fiecare cadru umflă XML.
    //
    // Codul ar fi CORECT — ar afișa aceleași numere — dar ar fi lent. Reciclarea e
    // optimizare, nu corectitudine. La 100 de rânduri diferența e mică; la 10.000
    // de rânduri e diferența dintre o aplicație care merge și una care nu.
    //
    // Cât costă inflate: parsarea XML-ului e cache-uită de Resources, dar construirea
    // obiectelor View nu. Pentru un rând cu două TextView-uri, vorbim de microsecunde.
    // Înmulțit cu 100 de rânduri și cu fiecare cadru de scroll, ajunge la milisecunde —
    // exact bugetul unui cadru de 60fps (16 ms).
    //
    // ==================================================================================
    // CE SE ÎNTÂMPLĂ DACĂ UIȚI UN findViewById
    // ==================================================================================
    //
    //   holder.tvDecimal = convertView.findViewById(R.id.tvDecimal);
    //   // holder.tvBinary lipsește
    //
    // Compilează. Lista se afișează. Coloana a doua e goală pe toate rândurile. La
    // umplere:
    //
    //   holder.tvBinary.setText(...)   →  NullPointerException:
    //     Attempt to invoke virtual method 'void android.widget.TextView.setText(...)'
    //     on a null object reference
    //
    // Diferența față de lipsa lui setTag: acolo holder-ul e null, aici holder-ul există,
    // dar un câmp al lui e null. Mesajul o spune: „invoke virtual method ... on a null
    // object reference" înseamnă că receptorul apelului e null, adică tvBinary.
    //
    // ==================================================================================
    // DE CE NU SE POT PUNE DATE ÎN HOLDER
    // ==================================================================================
    //
    // Ai putea fi tentat să scrii:
    //
    //   holder.number = getItem(position);   // ← greșit
    //
    // și să citești valoarea la click. Nu funcționează, pentru că holder-ul e REFOLOSIT:
    // între bind și click, rândul poate fi reciclat pentru altă poziție, iar valoarea
    // din holder ar fi a altui element. Holder-ul ține VIEW-URI, nu date. Datele se
    // pun în view-uri (setText), nu în holder.
    //
    // ==================================================================================
    // CUM SE COMPARĂ CU ARRAYADAPTER
    // ==================================================================================
    //
    //   ArrayAdapter                      NumberListAdapter
    //   ├── getCount() → size()           ├── getCount() → size()
    //   ├── getItem() → list.get()        ├── getItem() → list.get()
    //   ├── getItemId() → poziția         ├── getItemId() → numărul
    //   └── getView()                     └── getView()
    //         ├── inflate / reuse               ├── if (convertView == null)
    //         ├── findViewById(text1)           │     ├── inflate
    //         └── setText(toString())           │     ├── new ViewHolder
    //                                           │     ├── findViewById × 2
    //                                           │     └── setTag
    //                                           ├── else getTag
    //                                           └── setText × 2
    //
    // Aceleași patru metode, aceeași structură. Diferența e că a doua variantă îți dă
    // acces la convertView și la layout-ul rândului. Tot ce câștigi e control; tot ce
    // plătești e cod.
    //
    // ==================================================================================
    // GREȘELI FRECVENTE, ÎN ORDINEA FRECVENȚEI
    // ==================================================================================
    //
    //   1. Lipsește setTag → NPE la primul scroll.
    //   2. setTag pus pe ramura else → se creează un holder nou la fiecare afișare;
    //      nu crapă, dar pierzi toată economia.
    //   3. findViewById chemat la fiecare apel, în afara if-ului → funcționează, dar
    //      căutarea se face de 100 de ori mai des decât trebuie.
    //   4. inflate cu `true` în loc de `false` → ClassCastException sau rânduri lipite.
    //   5. getCount() întoarce numbers.size() + 1 → IndexOutOfBoundsException la ultimul.
    //   6. return holder.tvDecimal în loc de convertView → ListView primește un TextView
    //      în loc de rândul întreg; coloana a doua dispare.
}
