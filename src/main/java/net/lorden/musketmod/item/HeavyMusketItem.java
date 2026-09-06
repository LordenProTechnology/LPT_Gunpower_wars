package net.lorden.musketmod.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class HeavyMusketItem extends Item {
    public static final int CHARGE_TIME = 150; // 7.5 sekundy bazowego ładowania (zwiększone o 50%)
    public static final int RELOAD_COOLDOWN = 20;
    public static final int POST_SHOT_COOLDOWN = 40;

    private static final UUID SLOW_MODIFIER_UUID = UUID.fromString("71074360-6b00-11ec-90d6-0242ac120003");
    private final Multimap<Attribute, AttributeModifier> defaultModifiers;

    public HeavyMusketItem(Properties properties) {
        super(properties);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(SLOW_MODIFIER_UUID, "Heavy weapon penalty", -0.25, AttributeModifier.Operation.MULTIPLY_TOTAL));
        this.defaultModifiers = builder.build();
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.defaultModifiers : super.getDefaultAttributeModifiers(slot);
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
            tag.putBoolean("UsingCartridge", hasCartridge);
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(itemstack);
        } else {
            if (level.isClientSide) {
                if (!hasRamrod) {
                    player.displayClientMessage(Component.literal("Musisz trzymac pobojczyk w lewej rece!"), true);
                } else {
                    player.displayClientMessage(Component.literal("Brak amunicji do ciezkiego muszkietu!"), true);
                }
            }
            return InteractionResultHolder.fail(itemstack);
        }
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int count) {
        if (entity instanceof Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 255, false, false, false));

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

                ItemStack offhandStack = player.getOffhandItem();
                if (offhandStack.is(ModItems.RAMROD.get())) {
                    offhandStack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(InteractionHand.OFF_HAND));
                }

                player.containerMenu.broadcastChanges();

                setLoaded(stack, true);
                nbt.remove("PullTicks");
                nbt.remove("UsingCartridge");
                player.getCooldowns().addCooldown(this, RELOAD_COOLDOWN);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS, 1.0F, 0.7F);
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
            // 40.0F obrażeń bazowych, 80% omijania pancerza (32 nieblokowalne obrażenia - instakill w full diax secie)
            MusketBulletEntity bullet = new MusketBulletEntity(level, player, 40.0F, 0.80F);
            bullet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 8.0F, 0.0F);
            bullet.pickup = AbstractArrow.Pickup.DISALLOWED;
            level.addFreshEntity(bullet);

            stack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(player.getUsedItemHand()));

            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.2F, 0.4F);
            spawnParticles((ServerLevel) level, player);

            player.getCooldowns().addCooldown(this, POST_SHOT_COOLDOWN);

            // Zwiększony odrzut przy tak potężnym strzale
            Vec3 look = player.getLookAngle();
            player.push(-look.x * 1.5, 0.2, -look.z * 1.5);
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
        double px = player.getX() + look.x * 2.0;
        double py = player.getEyeY() + look.y * 2.0;
        double pz = player.getZ() + look.z * 2.0;
        for (int i = 0; i < 30; i++) {
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, px, py, pz, 1, 0.15, 0.15, 0.15, 0.05);
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