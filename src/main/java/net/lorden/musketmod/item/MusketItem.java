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

public class MusketItem extends Item {
    public static final int CHARGE_TIME = 60;
    public static final int RELOAD_COOLDOWN = 12;
    public static final int POST_SHOT_COOLDOWN = 20;

    public MusketItem(Properties properties) {
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
        boolean hasCartridge = player.getInventory().contains(new ItemStack(ModItems.PAPER_CARTRIDGE.get()));
        boolean hasPowderAndBall = player.getInventory().contains(new ItemStack(Items.GUNPOWDER))
                && player.getInventory().contains(new ItemStack(ModItems.MUSKET_BALL.get()));

        if (hasRamrod && (hasCartridge || hasPowderAndBall)) {
            CompoundTag tag = itemstack.getOrCreateTag();
            tag.putBoolean("IsAiming", false);
            // Kartusz ma pierwszeństwo – skraca ładowanie o połowę
            tag.putBoolean("UsingCartridge", hasCartridge);
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(itemstack);
        } else {
            if (level.isClientSide) {
                if (!hasRamrod) {
                    player.displayClientMessage(Component.literal("Musisz trzymac pobojczyk w lewej rece!"), true);
                } else {
                    player.displayClientMessage(Component.literal("Brak kartusza lub prochu i kul!"), true);
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

            boolean usingCartridge = nbt.getBoolean("UsingCartridge");
            int requiredChargeTime = usingCartridge ? (CHARGE_TIME / 2) : CHARGE_TIME;

            if (!isLoaded(stack) && !nbt.getBoolean("IsAiming")) {
                nbt.putInt("PullTicks", usedDuration);
            }

            if (!level.isClientSide && !isLoaded(stack) && !nbt.getBoolean("IsAiming") && usedDuration >= requiredChargeTime) {
                if (usingCartridge) {
                    consumeItem(player, ModItems.PAPER_CARTRIDGE.get());
                } else {
                    consumeItem(player, Items.GUNPOWDER);
                    consumeItem(player, ModItems.MUSKET_BALL.get());
                }

                // Pobojczyk w lewej ręce traci 1 punkt wytrzymałości
                ItemStack offhandStack = player.getOffhandItem();
                if (offhandStack.is(ModItems.RAMROD.get())) {
                    offhandStack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(InteractionHand.OFF_HAND));
                }

                player.containerMenu.broadcastChanges();

                setLoaded(stack, true);
                nbt.remove("PullTicks");
                nbt.remove("UsingCartridge");
                player.getCooldowns().addCooldown(this, RELOAD_COOLDOWN);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS, 1.0F, 1.0F);
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
        nbt.remove("UsingCartridge");
        nbt.putBoolean("IsAiming", false);
    }

    private void shoot(Level level, Player player, ItemStack stack) {
        if (!level.isClientSide) {
            MusketBulletEntity bullet = new MusketBulletEntity(level, player);
            bullet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 5.0F, 1.5F);
            bullet.pickup = AbstractArrow.Pickup.DISALLOWED;
            level.addFreshEntity(bullet);

            stack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(player.getUsedItemHand()));

            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.6F, 0.6F);
            spawnParticles((ServerLevel) level, player);

            player.getCooldowns().addCooldown(this, POST_SHOT_COOLDOWN);

            Vec3 look = player.getLookAngle();
            player.push(-look.x * 0.8, 0.1, -look.z * 0.8);
        }
    }

    private void consumeItem(Player player, Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                stack.shrink(1);
                break;
            }
        }
    }

    private void spawnParticles(ServerLevel level, Player player) {
        Vec3 look = player.getLookAngle();
        double px = player.getX() + look.x * 1.5;
        double py = player.getEyeY() + look.y * 1.5;
        double pz = player.getZ() + look.z * 1.5;
        for (int i = 0; i < 15; i++) {
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, px, py, pz, 1, 0.1, 0.1, 0.1, 0.03);
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
            int maxTime = stack.getTag().getBoolean("UsingCartridge") ? (CHARGE_TIME / 2) : CHARGE_TIME;
            return Math.min(13, Math.round((float) stack.getTag().getInt("PullTicks") * 13.0F / (float) maxTime));
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