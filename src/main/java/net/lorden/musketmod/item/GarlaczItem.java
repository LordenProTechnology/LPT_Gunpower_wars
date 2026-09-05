package net.lorden.musketmod.item;

import net.lorden.musketmod.entity.MusketBulletEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class GarlaczItem extends Item {
    public static final int CHARGE_TIME = 80;        // 4 sekundy ładowania
    public static final int RELOAD_COOLDOWN = 15;
    public static final int POST_SHOT_COOLDOWN = 30; // Dłuższy cooldown po salwie

    public GarlaczItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(itemstack);
        if (hand == InteractionHand.OFF_HAND) return InteractionResultHolder.pass(itemstack);

        if (isLoaded(itemstack)) {
            itemstack.getOrCreateTag().putBoolean("IsAiming", true);
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(itemstack);
        }

        boolean hasRamrod = player.getOffhandItem().is(ModItems.RAMROD.get());
        boolean hasGunpowder = player.getInventory().contains(new ItemStack(Items.GUNPOWDER));
        // Garłacz wymaga dedykowanego śrutu, brak obsługi kartusza
        boolean hasSrut = player.getInventory().contains(new ItemStack(ModItems.LEAD_SHOT.get()));

        if (hasRamrod && hasGunpowder && hasSrut) {
            itemstack.getOrCreateTag().putBoolean("IsAiming", false);
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(itemstack);
        } else {
            if (level.isClientSide) {
                if (!hasRamrod) {
                    player.displayClientMessage(Component.literal("Musisz trzymac pobojczyk w lewej rece!"), true);
                } else {
                    player.displayClientMessage(Component.literal("Garłacz wymaga prochu i śrutu ołowianego!"), true);
                }
            }
            return InteractionResultHolder.fail(itemstack);
        }
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int count) {
        if (entity instanceof Player player) {
            CompoundTag nbt = stack.getOrCreateTag();
            int usedDuration = this.getUseDuration(stack) - count;

            if (!isLoaded(stack) && !nbt.getBoolean("IsAiming")) {
                nbt.putInt("PullTicks", usedDuration);
            }

            if (!level.isClientSide && !isLoaded(stack) && !nbt.getBoolean("IsAiming") && usedDuration >= CHARGE_TIME) {
                consumeItem(player, Items.GUNPOWDER);
                consumeItem(player, ModItems.LEAD_SHOT.get());

                ItemStack offhandStack = player.getOffhandItem();
                if (offhandStack.is(ModItems.RAMROD.get())) {
                    offhandStack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(InteractionHand.OFF_HAND));
                }

                player.containerMenu.broadcastChanges();

                setLoaded(stack, true);
                nbt.remove("PullTicks");
                player.getCooldowns().addCooldown(this, RELOAD_COOLDOWN);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS, 1.0F, 0.8F);
                player.releaseUsingItem();
            }
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) return;
        CompoundTag nbt = stack.getOrCreateTag();
        if (isLoaded(stack) && nbt.getBoolean("IsAiming")) {
            shoot(level, player, stack);
            setLoaded(stack, false);
        }
        nbt.remove("PullTicks");
        nbt.putBoolean("IsAiming", false);
    }

    private void shoot(Level level, Player player, ItemStack stack) {
        if (!level.isClientSide) {
            // Wystrzelenie wiązki 7 śrucin o mniejszych obrażeniach (po 7 DMG każda) i dużym rozrzucie (8.0F)
            for (int i = 0; i < 7; i++) {
                MusketBulletEntity bullet = new MusketBulletEntity(level, player, 7.0F, 0.30F);
                bullet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.8F, 8.0F);
                bullet.pickup = AbstractArrow.Pickup.DISALLOWED;
                level.addFreshEntity(bullet);
            }

            stack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(player.getUsedItemHand()));

            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0F, 0.5F);
            spawnParticles((ServerLevel) level, player);

            player.getCooldowns().addCooldown(this, POST_SHOT_COOLDOWN);

            // Masywny odrzut w tył
            Vec3 look = player.getLookAngle();
            player.push(-look.x * 1.6, 0.2, -look.z * 1.6);
        }
    }

    private void consumeItem(Player player, Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(item)) {
                s.shrink(1);
                break;
            }
        }
    }

    private void spawnParticles(ServerLevel level, Player player) {
        Vec3 look = player.getLookAngle();
        double px = player.getX() + look.x * 1.2;
        double py = player.getEyeY() + look.y * 1.2;
        double pz = player.getZ() + look.z * 1.2;
        for (int i = 0; i < 25; i++) {
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, px, py, pz, 1, 0.2, 0.2, 0.2, 0.04);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        boolean isPulling = stack.hasTag() && stack.getTag().getInt("PullTicks") > 0 && !isLoaded(stack);
        return isPulling || stack.isDamaged();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().getInt("PullTicks") > 0 && !isLoaded(stack)) {
            return Math.min(13, Math.round((float) stack.getTag().getInt("PullTicks") * 13.0F / (float) CHARGE_TIME));
        }
        return Math.round(13.0F - (float) stack.getDamageValue() * 13.0F / (float) stack.getMaxDamage());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().getInt("PullTicks") > 0 && !isLoaded(stack)) {
            return 0xFFFFFF;
        }
        float f = Math.max(0.0F, ((float) stack.getMaxDamage() - (float) stack.getDamageValue()) / (float) stack.getMaxDamage());
        return Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
    }

    public static boolean isLoaded(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean("Loaded");
    }

    private static void setLoaded(ItemStack stack, boolean loaded) {
        stack.getOrCreateTag().putBoolean("Loaded", loaded);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }
}