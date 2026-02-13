package immersive_aircraft.item;

import immersive_aircraft.Main;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.List;
import java.util.function.Consumer;

public abstract class DescriptionItem extends Item {
    public DescriptionItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flags) {
        super.appendHoverText(stack, ctx, tooltipDisplay, tooltipAdder, flags);
        List<Component> wrapped = Main.textWrapper.wrap(Component.translatable(getDescriptionId() + ".description").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY), 180);
        wrapped.forEach(tooltipAdder);
    }
}
