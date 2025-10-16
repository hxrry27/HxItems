# HxItems

item editing plugin for purpur servers, formed from my love of similar ones such as EssentialsX lore function and RHSignItem. designed to work with unicode based resource packs in mind also, whilst supporting mini message and legacy formats

## Features

- **Rename Items** - custom names with full formatting support
- **Edit Lore** - line-by-line lore editing
- **Sign Items** - permanent signatures stored in database
- **Auto-formatting** - removes italic and purple lore code by default, supports minimessage, hex colors, and legacy codes
- **SQLite Storage** - persistent signature database
- **Unicode Support** - resource pack compatible
- **Version Agnostic** - works on 1.21.7, 1.21.8, 1.21.9, 1.21.10+

## Commands

### `/rename <name>`
rename the item you're holding.
- **Aliases:** `/renameitem`, `/itemrename`
- **Permission:** `hxitems.rename`
- **Examples:**
  - `/rename <gradient:#ff0000:#0000ff>Cool Sword` - gradient name
  - `/rename &#00ff88Mint Tool` - hex color
  - `/rename &6Golden Pick` - legacy color
  - `/rename clear` - remove name

### `/lore <set|add|remove|clear> [line] [text]`
edit item lore.
- **Aliases:** `/itemlore`, `/editlore`
- **Permission:** `hxitems.lore`
- **Examples:**
  - `/lore set 1 <red>First line` - set line 1
  - `/lore add <gray>New last line` - add to end
  - `/lore remove 2` - remove line 2
  - `/lore clear` - remove all lore

### `/sign [message]`
sign the item you're holding.
- **Aliases:** `/signitem`, `/itemsign`
- **Permission:** `hxitems.sign`
- **Examples:**
  - `/sign My first build` - sign with message
  - `/sign` - sign without message
  - `/sign clear` - remove signature

### `/hxitems <reload|info>`
admin commands.
- **Aliases:** `/hxi`
- **Permission:** `hxitems.admin`
- **Subcommands:**
  - `reload` - reload configuration
  - `info` - show plugin info and stats

## Configuration

```yaml
# config.yml
database:
  type: sqlite
  file: items.db

rename:
  max-length: 64
  allow-formatting: true
  allow-unicode: true
  strip-italic: true

lore:
  max-lines: 20
  max-line-length: 100
  allow-formatting: true
  allow-unicode: true
  strip-italic: true

signatures:
  enabled: true
  max-message-length: 100
  show-timestamp: true
  format: "&7Signed by &f{player}&7: &o{message}"

formatting:
  default-prefix: "<!italic>"
  legacy-colors: true
  minimessage: true
  hex-colors: true
  unicode:
    enabled: true
    allowed-ranges:
      - "\uE000-\uF8FF"
```

## Formatting Guide

### MiniMessage
- `<red>text` - named colors
- `<#00ff88>text` - hex colors
- `<gradient:#ff0000:#0000ff>text` - gradients
- `<bold>text` - bold
- `<!italic>text` - remove italic (auto-applied)

### Legacy Codes
- `&c` - red
- `&6` - gold
- `&b` - aqua
- `&l` - bold

### Hex Colors
- `&#RRGGBB` - Hex format (e.g., `&#00ff88`)

## Permissions

- `hxitems.*` - all permissions
- `hxitems.rename` - rename items (default: true)
- `hxitems.lore` - edit lore (default: true)
- `hxitems.sign` - sign items (default: true)
- `hxitems.admin` - admin commands (default: op)

## Dependencies

- **HxCore** (v1.0.0-SNAPSHOT) - Required
- **Paper/Purpur** (1.21.7+) - Required

## Future Plans

- visual GUI editor? (`/itemedit`)
- possibly utilising AnvilGUI integration for text input
- preview system (before/after comparison)
- bulk lore editing
- import/export item templates

## Author

**hxrry27** 