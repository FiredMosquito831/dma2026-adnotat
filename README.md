# dma2026 — annotated code

The **DMA2026** course project with **53 annotated files** — the code commented line by line, so you can read and understand any file directly in Android Studio.

> **Source:** [`mihaidoinea/dma2026`](https://github.com/mihaidoinea/dma2026), commit `d719ffc`.
> The code is **byte-identical**. The only difference from the original repo is the comment lines.

## Two languages, two branches

| Branch | The comments are in |
|---|---|
| **`main`** (default) | **Romanian** |
| **`english`** (this one) | **English** |

Same files, same code, only the comments differ. Switch the branch and Android Studio reloads:

```bash
git checkout english     # comments in English
git checkout main        # comments in Romanian
```

## What it contains

Eight modules, exactly like the professor's repo. In each, the main files are annotated:

| Module | Annotated files | What it is |
|---|---|---|
| `G1097` | 6 | the Movies app — the reference variant |
| `G1096` | 6 | the same app, another variant |
| `G1098` | 6 | the same app, another variant |
| `G1099` | 6 | the same app, another variant |
| `DMA02` | 4 | `Application` and the lifecycle |
| `DMA03` | 8 | navigation, drawer, menus |
| `DMA05` | 3 | ViewBinding |
| `DMA06` | 14 | the lists, in three steps |

Files that are not annotated (secondary resources, tests, build configuration) are **identical to the original repo**.

## How to open it

**From Android Studio:** *File → New → Project from Version Control* → `https://github.com/FiredMosquito831/dma2026-adnotat.git`, then `git checkout english`.

**From the terminal:**
```bash
git clone -b english https://github.com/FiredMosquito831/dma2026-adnotat.git
```

## How to run a module

The repo has **8 modules** in a single Gradle project. You do not open a separate project for each — you pick the module from the run configuration:

1. In the top bar, left of the **Run ▶** button, there is a configuration dropdown.
2. If nothing is there: **Add Configuration** (or `Edit Configurations…`).
3. **+** → **Android App** → under **Module** pick, for example, `dma2026.G1097.main`.
4. **Apply**, then **Run ▶**.

Repeat for each module you want to run. Android Studio remembers the configurations.

## What you need

- **Android Studio** — any recent version
- **JDK 17+** — ships with Android Studio
- **Android SDK 37** — from the *SDK Manager* if missing
- An **API 33+ emulator** or a connected phone

## Where the comments come from

Every annotated file corresponds to a chapter of the study site, where it is explained block by block, with diagrams and the "why" behind it:

**https://firedmosquito831.github.io/g1097-movies-walkthrough/en/**

| Module | Chapter |
|---|---|
| `G1097` | [22 — Annotated code](https://firedmosquito831.github.io/g1097-movies-walkthrough/en/cap-22.html) |
| `G1096` | [29 — G1096 code annotated](https://firedmosquito831.github.io/g1097-movies-walkthrough/en/cap-29.html) |
| `G1098` | [30 — G1098 code annotated](https://firedmosquito831.github.io/g1097-movies-walkthrough/en/cap-30.html) |
| `G1099` | [31 — G1099 code annotated](https://firedmosquito831.github.io/g1097-movies-walkthrough/en/cap-31.html) |
| `DMA02` | [25 — DMA02 code annotated](https://firedmosquito831.github.io/g1097-movies-walkthrough/en/cap-25.html) |
| `DMA03` | [26 — DMA03 code annotated](https://firedmosquito831.github.io/g1097-movies-walkthrough/en/cap-26.html) |
| `DMA05` | [27 — DMA05 code annotated](https://firedmosquito831.github.io/g1097-movies-walkthrough/en/cap-27.html) |
| `DMA06` | [28 — DMA06 code annotated](https://firedmosquito831.github.io/g1097-movies-walkthrough/en/cap-28.html) |

## Licence and source

The code belongs to the **Mobile Application Development** course (ASE). This repo only adds comments on top of it, for study. The version without comments: [`mihaidoinea/dma2026`](https://github.com/mihaidoinea/dma2026).

---

by FiredMosquito831
