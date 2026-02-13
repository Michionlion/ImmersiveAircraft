# ImmersiveAircraft 1.21.11 Migration (Current Final State)

This file tracks the migration end state and active follow-ups.

## Final decisions

- Target Minecraft: `1.21.11`.
- Fabric is release-critical; NeoForge is kept in parity as closely as possible.
- Mapping strategy: layered Mojang mappings + Parchment `1.21.11:2025.12.20`.
- Integrations:
  - Keep JEI support.
  - Keep REI support.
  - Drop Ad Astra integration for this milestone.
- Keep current warship sound behavior unchanged.
- Keep root Loom at `1.13.467` with `loom.ignoreDependencyLoomVersionValidation=true`.

## Version matrix (pinned)

- `minecraft_version=1.21.11`
- `fabric_loader_version=0.18.4`
- `fabric_api_version=0.141.3+1.21.11`
- `cloth_version=21.11.153`
- `mod_menu_version=17.0.0-beta.2`
- `neoforge_version=21.11.38-beta`
- `neoforge_min_version=21.11.0-beta`
- `jei_version=27.4.0.15`
- `rei_version=21.11.814`

## Metadata minimums

- `fabric.mod.json`
  - `fabricloader >=0.17.3`
  - `minecraft ~1.21.11`
  - `fabric-api >=0.135.1+1.21.11`
  - `cloth-config >=21.11.150`
  - suggested (recommended): `modmenu >=17.0.0-beta.2`, `jei >=27.4.0.15`, `roughlyenoughitems >=21.11.814`
  - `jei_mod_plugin` and `rei_client` entrypoints both enabled.
- `neoforge.mods.toml`
  - modern dependency schema (`type = "required"`).
  - minecraft range: `[${minecraft_version},1.21.12)`.
  - neoforge range: `[${neoforge_min_version},)`.
- NeoForge `pack.mcmeta` uses 1.21.11 pack format object (`min_format/max_format [94, 1]`).

## Implemented migration work

### Build/toolchain/dependencies

- Migrated project to MC `1.21.11` and updated Fabric/NeoForge dependency lines.
- Restored and updated JEI integration for Fabric + NeoForge + common API.
- Removed Ad Astra dependency and runtime coupling from the migration target.
- Kept REI support active.
- Split dependency strategy:
  - compile/dev pins track current 1.21.11-line patch releases.
  - metadata minimums stay at first supported 1.21.11-line versions for compatibility.

### API/mapping migration

- Applied 1.21.11 mapping/API updates across common + platform code:
  - `ResourceLocation` to `Identifier` where required.
  - packet codec migration to identifier read/write APIs.
  - GameRule access and other signature updates required by 1.21.11 mappings.

### Client/server separation rework

- Introduced environment split in `common` module via Loom (`splitEnvironmentSourceSets`).
- Added `common/src/client/java` source set and moved client-only code there:
  - renderers, HUD/UI, keybindings, client mixins, client network handler, REI/JEI client plugin classes, config UI integration.
- Added `ClientRegistration` (client-only abstraction) for entity renderer registration.
- Removed client renderer registration method from shared `Registration` API.
- Updated Fabric and NeoForge client bootstrap to install `ClientRegistration` implementation before renderer bootstrap.
- Removed direct client-class references from `common/src/main` by adding hooks in `Main`:
  - key state/name getter hooks used by vehicle controls.
  - text wrap hook used by item tooltip/description logic.
- Removed dependency from main code on client-only utility paths.

## Validation status

Successful in current tree:

- `./gradlew :common:compileJava`
- `./gradlew :fabric:compileJava`
- `./gradlew :neoforge:compileJava`
- `./gradlew build`
- `./gradlew clean :common:compileJava :fabric:compileJava :neoforge:compileJava build`
- `./gradlew :fabric:runClient` (timed run; client stayed up to timeout with full render/audio/resource initialization and no crash report)
- `./gradlew :neoforge:runClient` (timed run; client stayed up to timeout with full render/audio/resource initialization and no crash report)
- Manual gameplay smoke (user-verified): Fabric + NeoForge both load to world, allow movement, and pass basic in-world checks.

Compatibility probes also passed:

- `./gradlew :common:compileJava :fabric:compileJava :neoforge:compileJava -Pfabric_loader_version=0.17.3 -Pneoforge_version=21.11.0-beta`

## Known issues / follow-up

- Fabric warning remains for untranslated item tag keys if any new tags are introduced without lang entries (current core IA tag keys are translated).
- NeoForge still needs deeper parity checks for edge-case systems (network corner cases, advanced weapon/upgrade interactions).
- Loom emits expected warnings when dependencies were built against newer Loom versions.
- Rendering currently works through the submit-collector bridge path; a fully native submit-node rewrite remains optional future cleanup.

## Next high-priority checks

1. Run targeted multiplayer/dedicated-server sync checks (mounting/passenger updates, vehicle state replication).
2. Run focused JEI/REI compatibility verification on both loaders (recipe/category parity and UI overlap behavior).
3. Optional: continue native submit-node renderer cleanup to replace adapter bridge paths.
