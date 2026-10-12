package net.lorden.musketmod.item;

import net.lorden.musketmod.client.renderer.PowderFlaskRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

public class PowderFlaskItem extends Item implements GeoItem {
    public static final int MAX_POWDER = 128;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public PowderFlaskItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private PowderFlaskRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new PowderFlaskRenderer();
                }
                return this.renderer;
            }
        });
    }

    public static int getPowderCount(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getInt("PowderCount") : 0;
    }

    public static void setPowderCount(ItemStack stack, int amount) {
        stack.getOrCreateTag().putInt("PowderCount", Mth.clamp(amount, 0, MAX_POWDER));
    }

    public static boolean consumePowder(ItemStack stack, int amount) {
        int current = getPowderCount(stack);
        if (current >= amount) {
            setPowderCount(stack, current - amount);
            return true;
        }
        return false;
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack flask, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) return false;
        ItemStack other = slot.getItem();

        if (other.is(Items.GUNPOWDER)) {
            int current = getPowderCount(flask);
            int space = MAX_POWDER - current;
            if (space > 0) {
                int toTake = Math.min(space, other.getCount());
                setPowderCount(flask, current + toTake);
                other.shrink(toTake);
                return true;
            }
        } else if (other.isEmpty()) {
            int current = getPowderCount(flask);
            if (current > 0) {
                int toDrop = Math.min(64, current);
                slot.set(new ItemStack(Items.GUNPOWDER, toDrop));
                setPowderCount(flask, current - toDrop);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack flask, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY || !slot.allowModification(player)) return false;

        if (other.is(Items.GUNPOWDER)) {
            int current = getPowderCount(flask);
            int space = MAX_POWDER - current;
            if (space > 0) {
                int toTake = Math.min(space, other.getCount());
                setPowderCount(flask, current + toTake);
                other.shrink(toTake);
                return true;
            }
        } else if (other.isEmpty()) {
            int current = getPowderCount(flask);
            if (current > 0) {
                int toTake = Math.min(64, current);
                access.set(new ItemStack(Items.GUNPOWDER, toTake));
                setPowderCount(flask, current - toTake);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getPowderCount(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * (float) getPowderCount(stack) / (float) MAX_POWDER);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x555555;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        int count = getPowderCount(stack);
        tooltip.add(Component.translatable("tooltip.musketmod.powder_flask", count, MAX_POWDER).withStyle(ChatFormatting.GRAY));
    }
}