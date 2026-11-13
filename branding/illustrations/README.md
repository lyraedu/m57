# M57 explainer illustration set

The owner requested a generous, cohesive visual set on 13 November 2026 and
explicitly approved the imagegen CLI/API fallback. `prompts.jsonl` defines sixteen
distinct scenes: attention, personal/family controls, optional learning detour,
focus, breaks, short-video choice, daily allowance, routines, browsing,
permissions, local history, privacy, commitment, goals, setup completion and
empty history. All text and actions remain native Android controls. The artwork
must contain no text, UI screenshots, logos or third-party app marks.

**Current state:** all sixteen scenes were generated with the approved CLI
fallback and visually reviewed together for palette, artifacts, text and marks.
Full PNG originals live in `source/`; full generation prompts are retained in
`prompts.jsonl`. The packaged sixteen 960×640 WebP illustrations total about
636 KiB. The [contact sheet](../../docs/assets/illustration-preview.png) records
the visual review, not phone acceptance. Original project illustrations and
derivatives use the project's AGPL-3.0-only terms.

To run in a network-enabled environment, create a local environment and install
the supported CLI dependencies:

```sh
python3 -m venv .venv-imagegen
.venv-imagegen/bin/python -m pip install openai pillow
```

Configure `OPENAI_API_KEY` locally without putting it in Git, terminal history
or chat, then run the installed imagegen skill's bundled CLI:

```sh
.venv-imagegen/bin/python /home/akshay/.codex/skills/.system/imagegen/scripts/image_gen.py generate-batch \
  --input branding/illustrations/prompts.jsonl \
  --out-dir output/imagegen/m57-illustrations \
  --concurrency 3 --max-attempts 1 --fail-fast
```

The approved fallback defaults to `gpt-image-2`, medium quality, 1536×1024 opaque
PNG. Keep approved generated originals in `branding/illustrations/source/`. Review all sixteen for
style consistency, misleading imagery, artifacts and accidental text before
packaging. Then:

```sh
python3 scripts/import-illustrations.py
```

The importer requires every scene, emits 960×640 WebP assets and a source/output
digest manifest, and never uses the network. Those local assets are consumed
by onboarding, personal pages, protected settings and sixteen feature guides.
Artwork decoding uses a worker and an 8MiB cache. It does not carry functional
meaning that is absent from accessible native text. No missing-image placeholder
or remote image download is shown in the application.

The importer defaults to `branding/illustrations/source/` and validates the
complete set before replacing packaged files.

Prompt changes should preserve the established palette and reference asset
geometry; these scenes must not redraw the M57 or Lyra mark. Update this status,
license/source metadata and implementation evidence after actual generation and
visual/device review.
