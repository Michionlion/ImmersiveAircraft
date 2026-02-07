# ImmersiveAircraft 1.21.11 Migration Log (Fabric-first)

## Scope and goals

- Baseline: `1.21.1` branch -> Minecraft `1.21.11`
- Priority: Fabric first
- Secondary: keep NeoForge aligned as much as possible
- Compatibility choices:
  - Keep REI
  - Keep JEI (restored for 1.21.11)
  - Drop Ad Astra integration for this migration

## Reference anchors

- Fabric 1.21.11 notes: <https://fabricmc.net/2025/12/05/12111.html>
- NeoForge 21.11 release: <https://neoforged.net/news/21.11release/>
- NeoForge 1.21.11 primer: <https://raw.githubusercontent.com/neoforged/.github/main/primers/1.21.11/index.md>
- NeoForge mod file docs: <https://docs.neoforged.net/docs/gettingstarted/modfiles>
- Minecraft 1.21.11 release notes: <https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-11>

## Current status snapshot

### Done (tooling/dependency/metadata)

- Updated version matrix in `gradle.properties` to 1.21.11 line.
- Updated Loom plugin in root `build.gradle` to `1.13.467`.
- Kept Loom on `1.13.467` and enabled `loom.ignoreDependencyLoomVersionValidation=true` in `gradle.properties`:
  - reason: JEI 1.21.11 artifacts are built with Fabric Loom `1.14.5`, while Architectury Loom `1.14.x` plugin marker is not currently published in available plugin repos.
- Removed JEI and Ad Astra deps from Gradle files (`common`, `fabric`, `neoforge`) in the initial pass, then restored JEI once 1.21.11 artifacts were confirmed.
- Fabric metadata updates in `fabric.mod.json`:
  - restored JEI plugin entrypoint (`jei_mod_plugin`) after initial removal pass
  - removed Ad Astra break rule
  - set loader floor to `>=0.18.4`
  - added Minecraft dep `~1.21.11`
- NeoForge metadata updates:
  - `neoforge.mods.toml` moved to modern dependency `type = "required"`
  - `pack.mcmeta` moved to min/max format `[94, 1]`
- Removed JEI integration class:
  - deleted `common/src/main/java/immersive_aircraft/combat/JEICCombat.java`

### Reintroduced (JEI 1.21.11)

- JEI availability decision updated: JEI support restored for this migration milestone.
- Dependency updates:
  - `gradle.properties`: added `jei_version=27.4.0.15`.
  - `build.gradle`: added `https://maven.blamejared.com/` repository.
  - `common/build.gradle`: `modCompileOnlyApi("mezz.jei:jei-${minecraft_version}-common-api:${jei_version}")`.
  - `fabric/build.gradle`: `modCompileOnlyApi("mezz.jei:jei-${minecraft_version}-fabric-api:${jei_version}")` + dev runtime JEI jar.
  - `neoforge/build.gradle`: `modCompileOnlyApi("mezz.jei:jei-${minecraft_version}-neoforge-api:${jei_version}")` + dev runtime JEI jar.
- Fabric metadata + plugin surface:
  - `fabric.mod.json`: restored `jei_mod_plugin` entrypoint for `immersive_aircraft.combat.JEICCombat`.
  - `fabric.mod.json`: added JEI suggestion bound `jei >=27.4.0.15`.
- Code surface:
  - restored `common/src/main/java/immersive_aircraft/combat/JEICCombat.java`.
  - updated JEI plugin UID type to 1.21.11 mappings (`Identifier`).

### Done (code migration + compile closure)

- Identifier migration:
  - broad `ResourceLocation` -> `Identifier` conversion across common/fabric/neoforge
  - packet codec migration to `readIdentifier` / `writeIdentifier`
- Entity + serialization migration:
  - converted vehicle save/load stack to `ValueInput`/`ValueOutput`:
    - `VehicleEntity`
    - `DyeableVehicleEntity`
    - `InventoryVehicleEntity`
    - `EngineVehicle`
  - migrated inventory serialization to `ContainerHelper.saveAllItems/loadAllItems`
  - updated bubble column and interpolation overrides for 1.21.11 signatures
  - updated entity builder registration to `ResourceKey<EntityType<?>>`
  - updated projectile/bomb entity creation APIs (`EntitySpawnReason`)
- Data loader migration:
  - `VehicleDataLoader` and `UpgradeDataLoader` now use generic `SimpleJsonResourceReloadListener<JsonElement>` with `ExtraCodecs.JSON` + `FileToIdConverter`
  - updated registry lookups to `getValue`/`getOptional` style
- Item/UI/input API migration:
  - tooltip APIs updated to `TooltipDisplay` + `Consumer<Component>` in item classes
  - keybinding category migration to `KeyMapping.Category`
  - client camera/tick delta API updates (`Camera.position()`, `Minecraft#getDeltaTracker`)
- Rendering/GUI API migration:
  - moved RenderType utility usage to `RenderTypes` where needed
  - updated `VehicleScreen` to `RenderPipelines.GUI_TEXTURED` blits and deferred tooltip API
  - removed direct `RenderSystem` color calls in overlays
- Mixin target migration:
  - updated `AbstractClientPlayerMixin` FOV target descriptor
  - updated `GuiMixin` target to `Gui#render(GuiGraphics, DeltaTracker)`
  - removed obsolete `EntityRenderDispatcherMixin` class + config entry
- Fabric-specific migration:
  - replaced removed Fabric `FuelRegistry` API with vanilla `FuelValues`
  - updated `JsonDataLoaderWrapper` to new `PreparableReloadListener#reload` signature
- NeoForge-specific migration:
  - migrated reload listener event usage to `AddServerReloadListenersEvent`
  - updated payload handler wiring from removed `DirectionalPayloadHandler` to `IPayloadHandler`
  - switched client send-to-server path to `ClientPacketDistributor.sendToServer`
  - updated `EventBusSubscriber` annotations (removed deprecated `bus = ...` usage)
  - updated NeoForge fuel registry path to use new burn time signature (`FuelValues`)

## Build validation state

### Passing

- `./gradlew :common:compileJava`
- `./gradlew :fabric:compileJava`
- `./gradlew :neoforge:compileJava`
- `./gradlew build`
- `JAVA_TOOL_OPTIONS='-Dfabric-tag-conventions-v2.missingTagTranslationWarning=VERBOSE' ./gradlew :fabric:runServer` now resolves/loads successfully (halts at expected `eula.txt` gate in dev run dir)
- `./gradlew :fabric:runClient` (startup + integrated world join on 1.21.11)
- `./gradlew :neoforge:runClient` (startup + integrated world join on 1.21.11 after networking fix)

### Runtime stabilization log (Fabric, 2026-02-07)

- Crash: `fabric/run/crash-reports/crash-2026-02-07_00.01.44-client.txt`
  - Root cause: `CobaltFuelRegistryImpl` initialized `FuelValues` too early (`Tags not bound`).
  - Fix:
    - `fabric/.../CobaltFuelRegistryImpl.java`: lazy fuel value init with guarded fallback.
    - `neoforge/.../CobaltFuelRegistryImpl.java`: same lazy init for parity.
- Crash: `fabric/run/crash-reports/crash-2026-02-07_00.06.48-client.txt`
  - Root cause: item registration in 1.21.11 requires item id on `Item.Properties`.
  - Fix:
    - `common/.../Items.java`: added `ThreadLocal<ResourceKey<Item>>` context and `baseProps().setId(...)` wiring during registration.
- Crash: `fabric/run/crash-reports/crash-2026-02-07_00.08.06-client.txt`
  - Root cause: obsolete `LivingEntityRenderer` mixin signature after render-state migration.
  - Fix:
    - Removed `common/.../mixin/client/LivingEntityRendererMixin.java`.
    - Removed its entry from `common/src/main/resources/immersive_aircraft.mixins.json`.
- Crash: `fabric/run/crash-reports/crash-2026-02-07_00.09.15-client.txt`
  - Root cause: `CameraMixin#setup` descriptor mismatch (`BlockGetter` vs `Level`).
  - Fix:
    - `common/.../mixin/client/CameraMixin.java`: updated injected method signature to `setup(Level, Entity, boolean, boolean, float, CallbackInfo)`.
- Runtime functional regression (non-crash): all IA shaped recipes failed to parse on reload.
  - Root cause: 1.21.11 recipe key syntax now expects string keys (`"namespace:item"` / `"#namespace:tag"`), not legacy ingredient objects/arrays.
  - Fix:
    - Normalized all files in `common/src/main/resources/data/immersive_aircraft/recipe/*.json` to new key string format.
  - Verification:
    - `fabric/run/logs/latest.log` no longer contains `Couldn't parse data file` entries for IA recipes.
- Runtime warning follow-up: untranslated item tag names from Fabric tag conventions.
  - Added translation keys in `common/src/main/resources/assets/immersive_aircraft/lang/en_us.json`:
    - `tag.item.immersive_aircraft.aircraft`
    - `tag.item.immersive_aircraft.weapons`
    - `tag.item.immersive_aircraft.upgrades`
  - Verified tag/key parity against all IA item tag files under `common/src/main/resources/data/immersive_aircraft/tags/item`.
  - Note: prior warning instances in `fabric/run/logs/latest.log` were from older runs before these keys were added.
- Runtime visual migration pass:
  - Added submit-pipeline bridge for existing BB-model render code:
    - `common/.../renderer/utils/SubmitCollectorBufferSource.java`
    - `common/.../renderer/VehicleEntityRenderer.java` now uses `VehicleRenderState` + submit/extract path.
  - Restored projectile renderers from placeholders to active submit rendering:
    - `common/.../renderer/bullet/BulletEntityRenderer.java`
    - `common/.../renderer/bullet/TinyTNTRenderer.java`
  - Added 1.21.11 item model definition files (new required `assets/.../items/*.json` layer):
    - generated `common/src/main/resources/assets/immersive_aircraft/items/*.json` for all IA items, each targeting existing `immersive_aircraft:item/<id>` models.
  - Validation:
    - `./gradlew :common:compileJava :fabric:compileJava` passes after these changes.
    - Fabric client starts cleanly with no new startup crashes.
    - user-confirmed in-game: item icons and entity rendering restored.
- Fabric metadata audit fix:
  - `fabric/src/main/resources/fabric.mod.json` pinned to concrete Fabric-side bounds:
    - `fabricloader >=0.18.4`
    - `fabric-api >=0.141.3+1.21.11`
    - `cloth-config >=21.11.151`
  - Optional integrations pinned via `suggests`:
    - `modmenu >=17.0.0-beta.2`
    - `jei >=27.4.0.15`
    - `roughlyenoughitems >=21.11.814`
  - Reason: align runtime metadata with actual API usage and resolved/tested dependency line.
- Fabric dependency regression fix:
  - `gradle.properties` aligned to `cloth_version=21.11.151` to match resolved runtime artifacts.
  - Prevents dev/runtime loader mismatch seen when requiring `>=21.11.153` while classpath resolved `21.11.151`.
- Fabric consistency audit:
  - Verified item registration vs assets parity:
    - 27 registered items
    - 27 `assets/immersive_aircraft/items/*.json`
    - 27 `assets/immersive_aircraft/models/item/*.json`
    - no missing or extra entries.
  - Build validation after audit fix:
    - `./gradlew :fabric:compileJava :fabric:processResources`
    - `./gradlew build`
  - Runtime log status (`fabric/run/logs/latest.log`):
    - recipes load and world join successful
    - no IA crash/error signatures
    - historical `TranslationConventionLogWarnings` entries were from pre-translation-key runs.

### Runtime stabilization log (NeoForge, 2026-02-07)

- Crash: `neoforge/run/crash-reports/crash-2026-02-07_00.47.34-client.txt` / `..._00.52.53-client.txt`
  - Root cause: client reload listeners were registered too early during construct/init, before client `ResourceManager` setup was complete.
  - Fix:
    - `neoforge/.../ClientNeoForge.java`: moved loader registration to `AddClientReloadListenersEvent` and removed early `Minecraft.getInstance()` access.
- Runtime network failure on join (non-fatal but broke IA payload handling):
  - Log source: `neoforge/run/logs/latest.log` (`immersive_aircraft:vehicle_upgrades`, `immersive_aircraft:aircraft_data`).
  - Root cause: `PayloadRegistrar#playBidirectional` arguments were passed as `(clientHandler, serverHandler)`; NeoForge expects `(serverHandler, clientHandler)`.
  - Fix:
    - `neoforge/.../cobalt/network/NetworkHandlerImpl.java`:
      - corrected `playBidirectional` argument order
      - added explicit one-sided registration paths (`playToServer` / `playToClient`) when one side is null
      - guarded server handler cast with `instanceof ServerPlayer`.
  - Verification:
    - `./gradlew :neoforge:runClient` reaches integrated world join with no `ClassCastException` for IA payloads.
    - no new crash report generated after this patch.
- Current non-blocking NeoForge warnings:
  - REI `OnlyInWarningsHandler` log spam (third-party warning, not IA crash).
  - intermittent `Received passengers for unknown entity` during join (needs follow-up during full NeoForge gameplay smoke).
  - JEI/Architectury-Loom interop warnings during dependency remap (non-fatal with current build; monitor on future Loom/plugin updates).

### Important functional caveat (must be revisited)

- Renderer pipeline is now running on the 1.21.11 submit path and visuals are restored in Fabric runtime tests.
- Residual technical risk:
  - vehicle rendering currently bridges legacy `MultiBufferSource` logic through a submit-collector adapter; this is functional but should eventually be replaced with fully native submit-node rendering for maintainability/performance.

## Next work items (in order)

1. NeoForge runtime smoke pass:
   - run gameplay parity checklist (vehicle spawn/use, icons/models, weapons/projectiles/trails, UI overlays, REI interactions)
   - investigate whether intermittent `Received passengers for unknown entity` is benign or indicates IA mount sync issue
2. Regression cleanup:
   - re-check mixin injections at runtime (camera/FOV/HUD)
   - re-check any temporary visual compromises and remove placeholders
3. Fabric final verification pass:
   - re-run runtime smoke on current tree and confirm no new `TranslationConventionLogWarnings` entries on fresh logs
   - validate JEI + REI side-by-side behavior in dev runtime

## Live execution log

- [x] Tooling/dependency baseline moved to 1.21.11 line
- [x] Initial JEI removal path completed and then superseded by JEI restoration; Ad Astra remains removed
- [x] Metadata updates for Fabric + NeoForge applied
- [x] Identifier and packet method migration started
- [x] Entity + serialization contract migration complete
- [x] Fabric compile green
- [x] NeoForge compile green
- [x] Full build green
- [x] Fabric client startup + world join stable
- [x] Fabric gameplay smoke checklist complete (user verified visuals + controls)
- [ ] Runtime smoke complete (NeoForge)
- [x] Crash sequence from `00.01.44` to `00.09.15` triaged and fixed
- [x] Recipe format migration for 1.21.11 completed
- [x] Renderer behavior restored for Fabric runtime
- [x] Fabric dependency metadata corrected (pinned loader/API/cloth + optional modmenu/REI bounds)
- [x] NeoForge startup crash fixed (client reload listener timing)
- [x] NeoForge packet direction bug fixed (bidirectional handler order)
- [x] JEI integration restored for 1.21.11 (deps + plugin entrypoint + compat class)
- [x] Fabric cloth requirement/runtime mismatch fixed (`21.11.151` aligned across metadata + Gradle properties)
- [x] IA item-tag translation keys added and mapped to current tag files
