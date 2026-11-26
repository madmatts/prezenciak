# Design hand-off (Claude Design → implementation)

Designs are made in **Claude Design** from the prompts in `designer-prompts.md`, one package per phase.
MVP is **light theme only**.

## Preferred: share the link
For each package, paste the **claude.ai link** of the design (and of the design system, once it exists) into the
Claude Code session. Claude reads the source directly (HTML/CSS, tokens, real copy). Nothing needs exporting.

## Fallback: export as HTML into the repo
If a link can't be read (permissions, closed project), export the design as **HTML (standalone / zip)** and unpack it to:

```
docs/design/exports/
  design-system/            tokens stylesheet + component overview
  package-1/                one HTML page per screen/state
  package-2/
  ...
```

Optionally add **PNG screenshots** next to the HTML (same name, `.png`) for visual comparison.

## Formats to avoid
PDF, PPTX, Canva or screenshot-only exports: they lose exact sizes, colors, spacing and text, so the
implementation would have to guess.

## Note
Standalone claude.ai/design closes on **2026-12-14**. Claude Design now lives inside Claude. Create new designs there,
or export anything made in the standalone app before that date.
