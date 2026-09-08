package net.lorden.musketmod.item;

import net.minecraft.ChatFormatting;
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

import java.util.List;

public class PowderFlaskItem extends Item {
    public static final int MAX_POWDER = 128;

    public PowderFlaskItem(Properties properties) {
        super(properties.stacksTo(1));
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

    // Logika wkładania/wyjmowania w ekwipunku (jak Bundle)
    @Override
    public boolean overrideStackedOnOther(ItemStack flask, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) return false;
        ItemStack other = slot.getItem();

        // PPM na slocie z prochem -> wciąga proch do prochownicy
        if (other.is(Items.GUNPOWDER)) {
            int current = getPowderCount(flask);
            int space = MAX_POWDER - current;
            if (space > 0) {
                int toTake = Math.min(space, other.getCount());
                setPowderCount(flask, current + toTake);
                other.shrink(toTake);
                return true;
            }
        }
        // PPM na pustym slocie -> wyrzuca stack prochu do slotu
        else if (other.isEmpty()) {
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

        // Trzymamy proch kursorem i klikamy PPM na prochownicę
        if (other.is(Items.GUNPOWDER)) {
            int current = getPowderCount(flask);
            int space = MAX_POWDER - current;
            if (space > 0) {
                int toTake = Math.min(space, other.getCount());
                setPowderCount(flask, current + toTake);
                other.shrink(toTake);
                return true;
            }
        }
        // Trzymamy pusty kursor i klikamy PPM na prochownicę -> wyciągamy proch na kursor
        else if (other.isEmpty()) {
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
        return 0x555555; // Ciemnoszary pasek napełnienia
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        int count = getPowderCount(stack);
        tooltip.add(Component.translatable("tooltip.musketmod.powder_flask", count, MAX_POWDER).withStyle(ChatFormatting.GRAY));
    }
}