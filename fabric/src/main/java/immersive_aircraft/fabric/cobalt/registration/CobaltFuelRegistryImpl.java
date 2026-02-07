package immersive_aircraft.fabric.cobalt.registration;

import immersive_aircraft.cobalt.registration.CobaltFuelRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.entity.FuelValues;

public class CobaltFuelRegistryImpl extends CobaltFuelRegistry {
    private static FuelValues fuelValues;

    public CobaltFuelRegistryImpl() {
        INSTANCE = this;
    }

    private static FuelValues getFuelValues() {
        if (fuelValues != null) {
            return fuelValues;
        }

        try {
            fuelValues = FuelValues.vanillaBurnTimes(
                    HolderLookup.Provider.create(BuiltInRegistries.REGISTRY.stream().map(registry -> (HolderLookup.RegistryLookup<?>) registry)),
                    FeatureFlags.DEFAULT_FLAGS
            );
            return fuelValues;
        } catch (IllegalStateException ignored) {
            // Can occur during early bootstrap before tags are bound.
            return null;
        }
    }

    @Override
    public int get(ItemStack stack) {
        FuelValues values = getFuelValues();
        return values == null ? 0 : values.burnDuration(stack);
    }
}
