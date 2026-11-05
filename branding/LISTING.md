# BarelyPowered listing kit: CurseForge

Everything needed to fill in the CurseForge project form. Files are in `branding/icon/` and `branding/banners/`.

## Project fields

| Field | CurseForge |
|---|---|
| Project ID | not created yet (Casey creates the project; record the id here and in `.github/scripts/publish_curseforge.py`) |
| Name | BarelyPowered |
| Slug / URL | `barelypowered` |
| Mod id | `barelypowered` |
| Icon / logo | `icon/icon_512.png` (square, at least 400x400) |
| Class | Mods |
| Main category | Technology, or the closest energy category the form offers |
| Loaders | Forge and NeoForge |
| Game versions | Minecraft 1.21.1, 26.1.2, 26.2 and 26.3, one jar per Minecraft version; set on each uploaded file |
| Environment | Client and Server (the upload script sends both) |
| License | MIT |
| Source | https://github.com/ItMeCasey/BarelyPowered |
| Issues | https://github.com/ItMeCasey/BarelyPowered/issues |
| Wiki | https://itmecasey.com/wiki/barelypowered/ (not deployed yet; add the link once it is live) |

### One-line description

> Generators only: combustion, lava, solar, wind, water and geothermal power for Forge and NeoForge that output Forge Energy, so any cable mod can carry it. No cables, no machines.

Use it for the CurseForge summary (under 250 characters), social posts and the top of the description.

### Gallery

| File | Use |
|---|---|
| `banners/features.png` (1920x1080) | First gallery image: the seven generators |
| `banners/output.png` (1920x1080) | How output is worked out (solar, wind, water curves), second gallery image |
| `banners/social.png` (1280x640) | GitHub social preview (repo Settings > Social preview) |
| `banners/banner.png` (800x150, saved at 2x) | Top of the description |
| `banners/h_*.png` (800x46, saved at 2x) | Section headers inside the description (features, generators, config, compat, faq) |

The panels and charts in these images are illustrations drawn for the branding; the generator faces are the mod's own textures. Add real in-game screenshots to the gallery from the tour frames in `docs/frames/` once they are picked.

### Description

The CurseForge description is in [`curseforge/DESCRIPTION.md`](../curseforge/DESCRIPTION.md). Paste it with the description type set to **Markdown**. Its images load from GitHub, so `branding/` has to be pushed first. It has no Install section on purpose, and its Tested line says only what was run.

## Checklist

1. Push `branding/` to GitHub so the description images load.
2. Create the CurseForge project with `icon_512.png` as the logo, then record the project id above and in the upload script, and add the `CURSEFORGE_TOKEN` repository secret.
3. Set `banners/social.png` as the GitHub social preview.
4. Update the Tested line in the description at release.
5. `icon/barelypowered_logo.png` is the jar's mod list logo (already in the jar as `barelypowered_logo.png`).
