package net.lorden.musketmod.item;

import net.lorden.musketmod.client.renderer.GarlaczRenderer;
import net.lorden.musketmod.entity.MusketBulletEntity;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
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
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class GarlaczItem extends Item implements GeoItem {
    public static final int CHARGE_TIME = 140; // 7.0 sekund ładowania (krótszy niż arkebuz)
    public static final int POST_SHOT_COOLDOWN = 25;
    public static final int PELLETS_COUNT = 7; // Ilość śrutu na jeden strzał

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ANIM_RELOAD = RawAnimation.begin().thenPlay("reload");
    private static final RawAnimation ANIM_AIM = RawAnimation.begin().thenLoop("aim");
    private static final RawAnimation ANIM_SHOOT = RawAnimation.begin().thenPlay("shoot");

    public GarlaczItem(Properties properties) {
        super(properties);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 2, event -> {
            ItemStack stack = event.getData(software.bernie.geckolib.constant.DataTickets.ITEMSTACK);
            if (stack != null && stack.hasTag()) {
                CompoundTag tag = stack.getTag();
                if (tag.getBoolean("IsLoading")) {
                    return event.setAndContinue(ANIM_RELOAD);
                }
                if (tag.getBoolean("IsAiming")) {
                    return event.setAndContinue(ANIM_AIM);
                }
            }
            return event.setAndContinue(ANIM_IDLE);
        }).triggerableAnim("shoot", ANIM_SHOOT));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private GarlaczRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new GarlaczRenderer();
                }
                return this.renderer;
            }
        });
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        if (hand == InteractionHand.OFF_HAND) return InteractionResultHolder.pass(stack);

        CompoundTag tag = stack.getOrCreateTag();

        if (isLoaded(stack)) {
            if (tag.getBoolean("IsAiming")) {
                shoot(level, player, stack);
                setLoaded(stack, false);
                tag.putBoolean("IsAiming", false);
                return InteractionResultHolder.consume(stack);
            } else {
                tag.putBoolean("IsAiming", true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARMOR_EQUIP_GENERIC, SoundSource.PLAYERS, 0.8F, 1.2F);
                return InteractionResultHolder.consume(stack);
            }
        }

        boolean hasRamrod = player.getOffhandItem().is(ModItems.RAMROD.get());
        ItemStack flask = ArkebuzItem.getPowderFlaskFromHotbar(player);
        boolean hasFlaskPowder = flask != null && PowderFlaskItem.getPowderCount(flask) > 0;
        boolean hasBall = player.getInventory().contains(new ItemStack(ModItems.MUSKET_BALL.get()));

        if (hasRamrod && hasFlaskPowder && hasBall) {
            tag.putBoolean("IsAiming", false);
            tag.putBoolean("IsLoading", true);
            tag.putBoolean("ReadyToAim", false);
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        } else {
            if (level.isClientSide) {
                if (!hasRamrod) {
                    player.displayClientMessage(Component.translatable("message.musketmod.need_ramrod"), true);
                } else {
                    player.displayClientMessage(Component.translatable("message.musketmod.no_ammo"), true);
                }
            }
            return InteractionResultHolder.fail(stack);
        }
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int count) {
        if (entity instanceof Player player) {
            CompoundTag nbt = stack.getOrCreateTag();
            int usedDuration = this.getUseDuration(stack) - count;

            if (nbt.getBoolean("IsLoading")) {
                nbt.putInt("PullTicks", usedDuration);

                if (usedDuration >= CHARGE_TIME && !nbt.getBoolean("ReadyToAim")) {
                    nbt.putBoolean("ReadyToAim", true);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            }
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) return;
        CompoundTag nbt = stack.getOrCreateTag();

        if (nbt.getBoolean("IsLoading")) {
            if (nbt.getBoolean("ReadyToAim")) {
                if (!level.isClientSide) {
                    ItemStack flaskStack = ArkebuzItem.getPowderFlaskFromHotbar(player);
                    if (flaskStack != null) {
                        PowderFlaskItem.consumePowder(flaskStack, 1);
                    }
                    consumeItem(player, ModItems.MUSKET_BALL.get());

                    ItemStack offhandStack = player.getOffhandItem();
                    if (offhandStack.is(ModItems.RAMROD.get())) {
                        offhandStack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(InteractionHand.OFF_HAND));
                    }

                    player.containerMenu.broadcastChanges();
                }

                setLoaded(stack, true);
                nbt.putBoolean("IsLoading", false);
                nbt.putBoolean("ReadyToAim", false);
                nbt.remove("PullTicks");

                nbt.putBoolean("IsAiming", true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARMOR_EQUIP_GENERIC, SoundSource.PLAYERS, 0.8F, 1.2F);

                player.getCooldowns().addCooldown(this, 6);
            } else {
                nbt.putBoolean("IsLoading", false);
                nbt.remove("PullTicks");
            }
        }
    }

    private void shoot(Level level, Player player, ItemStack stack) {
        if (!level.isClientSide) {
            triggerAnim(player, GeoItem.getOrAssignId(stack, (ServerLevel) level), "controller", "shoot");

            // Wystrzał chmary śrutu z dużym rozrzutem (inaccuracy 9.0F)
            for (int i = 0; i < PELLETS_COUNT; i++) {
                MusketBulletEntity bullet = new MusketBulletEntity(level, player, 14.0F, 0.15F);
                bullet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 4.0F, 9.0F);
                bullet.pickup = AbstractArrow.Pickup.DISALLOWED;
                level.addFreshEntity(bullet);
            }

            stack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(player.getUsedItemHand()));

            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0F, 0.5F);
            spawnParticles((ServerLevel) level, player);

            player.getCooldowns().addCooldown(this, POST_SHOT_COOLDOWN);

            // Silniejszy odrzut garłacza
            Vec3 look = player.getLookAngle();
            player.push(-look.x * 1.2, 0.15, -look.z * 1.2);
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
        double px = player.getX() + look.x * 1.2;
        double py = player.getEyeY() + look.y * 1.2;
        double pz = player.getZ() + look.z * 1.2;
        for (int i = 0; i < 25; i++) {
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, px, py, pz, 1, 0.2, 0.2, 0.2, 0.05);
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

    public static void setLoaded(ItemStack stack, boolean loaded) {
        stack.getOrCreateTag().putBoolean("Loaded", loaded);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }
}