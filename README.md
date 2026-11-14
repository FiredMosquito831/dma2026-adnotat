# dma2026 — cod adnotat

Proiectul **DMA2026** de la curs, cu **53 de fișiere adnotate** — codul comentat linie cu linie, ca să poți citi și înțelege orice fișier direct în Android Studio.

> **Sursa:** [`mihaidoinea/dma2026`](https://github.com/mihaidoinea/dma2026), commitul `d719ffc`.
> Codul este **identic, byte cu byte**. Singura diferență față de repo-ul original sunt liniile de comentariu.

## Ce e nou — 14.11.2026

Folderul **`G1098-14.11.2026(week7-full)/`** conține versiunea **actualizată** a aplicației, adusă pe 14 noiembrie: `RecyclerView`, adaptor propriu cu `ViewHolder`, callback de click prin interfață `IMovieListener` și un ecran nou (`PlaceCiocanul`).

E un **proiect separat**, cu `settings.gradle.kts` propriu (`include(":app")`) — nu face parte din cele 8 module. Îl deschizi separat:

*File → Open* → alegi folderul `G1098-14.11.2026(week7-full)` (nu rădăcina repo-ului).

Pachetul lui e `com.example.group1098`, exact cum a fost exportat.

## Două limbi, două branch-uri

| Branch | Comentariile |
|---|---|
| **`main`** (default) | în **română** |
| **`english`** | în **engleză** |
| **`archive-2026-11-11`** | versiunea din 11.11, **fără** folderul nou (RO) |
| **`archive-2026-11-11-en`** | versiunea din 11.11, **fără** folderul nou (EN) |

Aceleași fișiere, același cod, doar comentariile diferă. Schimbi branch-ul și Android Studio reîncarcă:

```bash
git checkout english     # comentarii în engleză
git checkout main        # comentarii în română
```

## Ce conține

Opt module, exact ca în repo-ul profesorului. În fiecare, fișierele principale sunt adnotate:

| Modul | Fișiere adnotate | Ce este |
|---|---|---|
| `G1097` | 6 | aplicația Movies — varianta de referință |
| `G1096` | 6 | aceeași aplicație, altă variantă |
| `G1098` | 6 | aceeași aplicație, altă variantă |
| `G1099` | 6 | aceeași aplicație, altă variantă |
| `DMA02` | 4 | `Application` și ciclul de viață |
| `DMA03` | 8 | navigare, drawer, meniuri |
| `DMA05` | 3 | ViewBinding |
| `DMA06` | 14 | listele, în trei trepte |

Fișierele care nu sunt adnotate (resursele secundare, testele, configurațiile) sunt **identice cu repo-ul original**.

## Cum îl deschizi

**Din Android Studio:** *File → New → Project from Version Control* → `https://github.com/FiredMosquito831/dma2026-adnotat.git`

**Din terminal:**
```bash
git clone https://github.com/FiredMosquito831/dma2026-adnotat.git
```

## Cum rulezi un modul

Repo-ul are **8 module** într-un singur proiect Gradle. Nu deschizi câte un proiect pentru fiecare — alegi modulul din configurația de run:

1. În bara de sus, în stânga butonului **Run ▶**, e un dropdown cu configurații.
2. Dacă nu apare nimic: **Add Configuration** (sau `Edit Configurations…`).
3. **+** → **Android App** → la **Module** alegi, de exemplu, `dma2026.G1097.main`.
4. **Apply**, apoi **Run ▶**.

Repetă pentru fiecare modul pe care vrei să îl rulezi. Android Studio ține minte configurațiile.

## Ce îți trebuie

- **Android Studio** — orice versiune recentă
- **JDK 17+** — vine cu Android Studio
- **Android SDK 37** — din *SDK Manager* dacă lipsește
- Un **emulator cu API 33+** sau un telefon conectat

## De unde vin comentariile

Fiecare fișier adnotat corespunde unui capitol din site-ul de pregătire, unde e explicat bloc cu bloc, cu diagrame și cu „de ce"-ul din spate:

**https://firedmosquito831.github.io/g1097-movies-walkthrough/**

| Modul | Capitol |
|---|---|
| `G1097` | [22 — Codul adnotat](https://firedmosquito831.github.io/g1097-movies-walkthrough/cap-22.html) |
| `G1096` | [29 — Codul G1096 adnotat](https://firedmosquito831.github.io/g1097-movies-walkthrough/cap-29.html) |
| `G1098` | [30 — Codul G1098 adnotat](https://firedmosquito831.github.io/g1097-movies-walkthrough/cap-30.html) |
| `G1099` | [31 — Codul G1099 adnotat](https://firedmosquito831.github.io/g1097-movies-walkthrough/cap-31.html) |
| `DMA02` | [25 — Codul DMA02 adnotat](https://firedmosquito831.github.io/g1097-movies-walkthrough/cap-25.html) |
| `DMA03` | [26 — Codul DMA03 adnotat](https://firedmosquito831.github.io/g1097-movies-walkthrough/cap-26.html) |
| `DMA05` | [27 — Codul DMA05 adnotat](https://firedmosquito831.github.io/g1097-movies-walkthrough/cap-27.html) |
| `DMA06` | [28 — Codul DMA06 adnotat](https://firedmosquito831.github.io/g1097-movies-walkthrough/cap-28.html) |

## Licență și sursă

Codul aparține cursului **Dezvoltare aplicații mobile** (ASE). Acest repo doar adaugă comentarii peste el, pentru studiu. Versiunea fără comentarii: [`mihaidoinea/dma2026`](https://github.com/mihaidoinea/dma2026).

---

by FiredMosquito831
