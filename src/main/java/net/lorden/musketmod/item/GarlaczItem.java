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
    public static final int CHARGE_TIME = 130; // Krótszy czas ładowania niż arkebuz (~6.5 sekundy)
    public static final int POST_SHOT_COOLDOWN = 30; // Chwilowy cooldown po wystrzale
    public static final int PELLETS_COUNT = 8; // Ilość śrucin w jednej salwie

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

        // 1. Strzał lub celowanie, gdy załadowany
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

        // 2. Warunki ładowania: Pobojczyk w 2. ręce + proch w prochownicy na pasku + ŚRUT (lead_shot) w ekwipunku
        boolean hasRamrod = player.getOffhandItem().is(ModItems.RAMROD.get());
        ItemStack flask = ArkebuzItem.getPowderFlaskFromHotbar(player);
        boolean hasFlaskPowder = flask != null && PowderFlaskItem.getPowderCount(flask) > 0;
        boolean hasLeadShot = player.getInventory().contains(new ItemStack(ModItems.LEAD_SHOT.get()));

        if (hasRamrod && hasFlaskPowder && hasLeadShot) {
            tag.putBoolean("IsAiming", false);
            tag.putBoolean("IsLoading", true);
            tag.putBoolean("ReadyToAim", false);
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        } else {
            if (level.isClientSide) {
                if (!hasRamrod) {
                    player.displayClientMessage(Component.translatable("message.musketmod.need_ramrod"), true);
                } else if (!hasFlaskPowder) {
                    player.displayClientMessage(Component.translatable("message.musketmod.no_ammo"), true);
                } else {
                    player.displayClientMessage(Component.translatable("message.musketmod.need_lead_shot"), true);
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
                    // Pobranie 1 porcji prochu
                    ItemStack flaskStack = ArkebuzItem.getPowderFlaskFromHotbar(player);
                    if (flaskStack != null) {
                        PowderFlaskItem.consumePowder(flaskStack, 1);
                    }
                    // Zużycie 1 sztuki śrutu (lead_shot)
                    consumeItem(player, ModItems.LEAD_SHOT.get());

                    // Uszkodzenie pobojczyka
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

            // Wystrzał salwy śrutu (mechanika shotguna)
            for (int i = 0; i < PELLETS_COUNT; i++) {
                // Obrażenia: 9.0F na śrucinę, mniejsza grawitacja/rozmiar
                MusketBulletEntity pellet = new MusketBulletEntity(level, player, 9.0F, 0.15F);
                // Prędkość: 3.2F (spada szybciej), Inaccuracy: 12.0F (duży rozrzut kielicha garłacza)
                pellet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.2F, 12.0F);
                pellet.pickup = AbstractArrow.Pickup.DISALLOWED;
                level.addFreshEntity(pellet);
            }

            stack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(player.getUsedItemHand()));

            // Głośniejszy, basowy wybuch z kielicha
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.2F, 0.5F);
            spawnParticles((ServerLevel) level, player);

            player.getCooldowns().addCooldown(this, POST_SHOT_COOLDOWN);

            // Bardzo mocny odrzut w tył
            Vec3 look = player.getLookAngle();
            player.push(-look.x * 1.4, 0.20, -look.z * 1.4);
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
        double px = player.getX() + look.x * 1.1;
        double py = player.getEyeY() + look.y * 1.1;
        double pz = player.getZ() + look.z * 1.1;
        for (int i = 0; i < 30; i++) {
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, px, py, pz, 1, 0.25, 0.25, 0.25, 0.06);
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