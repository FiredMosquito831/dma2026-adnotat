/*
 * ======================================================================================
 * FIȘIER: IMovieListener.java — CONTRACTUL DE CLICK PE RÂND
 * ======================================================================================
 * CE ESTE ACEST FIȘIER:
 *   O interfață cu o singură metodă. Spune ce trebuie să știe să facă cineva care
 *   vrea să fie anunțat când utilizatorul atinge un rând din listă.
 *
 * DE CE EXISTĂ:
 *   `RecyclerView` nu are `setOnItemClickListener`. Click-ul se leagă manual, pe
 *   `itemView`, în `ViewHolder` (38.2) — iar de acolo trebuie să ajungă undeva.
 *   Interfața este acel „undeva", fără ca adaptorul să știe cine este destinatarul.
 *
 * DE CE DEPINDE:
 *   Doar de `Movie` (tipul parametrului). Nu depinde de Android, de `View`,
 *   de `Context` și de nicio clasă din aplicație.
 *
 * CINE DEPINDE DE EL:
 *   - `MovieAdapter` — are un câmp `IMovieListener` și îl apelează la click;
 *   - `MainActivity` — îl implementează și se pasează pe sine adaptorului.
 *
 * CE NU FACE:
 *   Nu știe ce se întâmplă după click, nu pornește activități, nu ține poziția.
 *   Este doar o semnătură.
 *
 * CE S-AR RUPE FĂRĂ EL:
 *   Adaptorul ar trebui să primească direct un `MainActivity` — adică exact cuplarea
 *   pe care interfața o evită. Iar dacă ai schimba semnătura metodei, cele două
 *   locuri care depind de ea (adaptorul și activitatea) ar refuza să compileze.
 * ======================================================================================
 */

// Pachetul `adapter`, nu `activities`: contractul aparține adaptorului, care îl cere.
//   MainActivity îl importă de aici. Direcția dependenței: activities -> adapter -> model.
package com.example.group1098.adapter;

// `Movie` este tipul transportat de eveniment — nu `Object`, nu `View`.
import com.example.group1098.model.Movie;

/*
 * ======================================================================================
 * BLOC DE TEORIE: INTERFAȚA CA CALLBACK (INVERSION OF CONTROL)
 * ======================================================================================
 *
 * 1. **Problema.** Un rând de listă este o vedere inertă: știe să se deseneze, nu știe
 *    ce înseamnă „atingerea mea". Cineva trebuie să decidă ce se întâmplă la click —
 *    iar acel cineva este ACTIVITATEA, nu adaptorul.
 *
 * 2. **Soluția naivă.** Adaptorul ar putea primi direct `MainActivity` și ar putea
 *    construi el `Intent`-ul. Ar funcționa, dar adaptorul ar depinde de trei lucruri
 *    care nu au legătură cu desenatul unui card: clasa `ItemActivity`, constantele
 *    `MOVIE_EXTRA` / `POSITION_EXTRA` și un `Context` pentru `startActivity`.
 *
 * 3. **Soluția cu interfață.** Adaptorul depinde de UN SINGUR nume: `IMovieListener`.
 *    Nu știe cine îl implementează și nici ce se întâmplă după apel. Activitatea se
 *    pasează pe sine (`this`) și își asumă comportamentul.
 *
 * 4. **Inversion of control.** Nu adaptorul cheamă activitatea „pe nume"; el cheamă o
 *    metodă dintr-un contract, iar activitatea a decis să răspundă la el. Controlul
 *    asupra a „ce se întâmplă" este inversat față de fluxul normal de apeluri.
 *
 * 5. **Consecința practică la examen.** Pentru cerința „leagă click-ul pe rând de
 *    pornirea altei activități", răspunsul are exact trei piese:
 *      1) interfața (acest fișier),
 *      2) apelul din ViewHolder (`listener.onMovieClick(...)`),
 *      3) `implements` pe activitate + `new MovieAdapter(movies, this)`.
 * ======================================================================================
 */
// `public interface`: public = vizibil din orice pachet; interface = contract fără
//   implementare. Metodele sunt implicit `public abstract`.
public interface IMovieListener {

    // ==================================================================================
    // METODA: onMovieClick — SINGURA CERINȚĂ A CONTRACTULUI
    // ==================================================================================
    // CE FACE: definește ce primește cineva care vrea să afle de un click pe rând.
    // CUM: nu are corp — fiecare implementare decide ce face (aici: pornește ecranul).
    // RISC: `Movie` poate fi null dacă lista ar conține un element null; `position`
    //   este poziția în listă, valabilă doar dacă lista nu s-a schimbat între timp.
    // REF: apelul din `MovieAdapter.MovieViewHolder` și implementarea din MainActivity.
    // ==================================================================================
    void onMovieClick(Movie movie, int position);
}
