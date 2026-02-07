package immersive_aircraft.neoforge;

import immersive_aircraft.Main;
import immersive_aircraft.Renderer;
import immersive_aircraft.WeaponRendererRegistry;
import immersive_aircraft.client.KeyBindings;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@Mod(value = Main.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(value = Dist.CLIENT, modid = Main.MOD_ID)
public final class ClientNeoForge {
    @SubscribeEvent
    public static void registerResourceLoaders(AddClientReloadListenersEvent event) {
        if (NeoForgeBusEvents.RESOURCE_REGISTRY != null) {
            NeoForgeBusEvents.RESOURCE_REGISTRY.getLoaderMap().forEach(event::addListener);
        }
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        Renderer.bootstrap();
        WeaponRendererRegistry.bootstrap();
    }

    @SubscribeEvent
    public static void onKeyRegister(RegisterKeyMappingsEvent event) {
        KeyBindings.list.forEach(event::register);
    }

}
