package net.lorden.musketmod.entity;

import net.lorden.musketmod.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.network.NetworkHooks;

public class MusketBulletEntity extends AbstractArrow implements ItemSupplier {
    private float totalDamage = 20.0F;
    private float armorPenetration = 0.50F;
    private int ticksInGround = 0;

    public MusketBulletEntity(EntityType<? extends AbstractArrow> type, Level level) {
        super(type, level);
        this.pickup = Pickup.DISALLOWED;
    }

    // Stary konstruktor (dla Muszkietu i Arkebuza)
    public MusketBulletEntity(Level level, LivingEntity shooter) {
        super(ModEntities.MUSKET_BULLET.get(), shooter, level);
        this.pickup = Pickup.DISALLOWED;
    }

    // NOWY konstruktor (dla Garłacza i innych broni o zmiennych statystykach)
    public MusketBulletEntity(Level level, LivingEntity shooter, float damage, float armorPenetration) {
        super(ModEntities.MUSKET_BULLET.get(), shooter, level);
        this.totalDamage = damage;
        this.armorPenetration = armorPenetration;
        this.pickup = Pickup.DISALLOWED;
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(ModItems.MUSKET_BALL.get());
    }

    @Override
    protected ItemStack getPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.inGround && this.level().isClientSide()) {
            this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }
        if (this.inGround) {
            this.ticksInGround++;
            if (this.ticksInGround >= 180 && !this.level().isClientSide()) {
                this.discard();
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        // NIE wywołujemy super.onHitEntity(result), bo nadpisuje obrażenia i daje invulnerableTime!
        Entity target = result.getEntity();

        if (!this.level().isClientSide() && target instanceof LivingEntity livingTarget) {
            Entity owner = this.getOwner();

            float bypassedDamage = this.totalDamage * this.armorPenetration;
            float standardDamage = this.totalDamage * (1.0F - this.armorPenetration);

            // 1. Zwykłe obrażenia pocisku (pancerz je redukuje)
            if (standardDamage > 0) {
                DamageSource normalSource = this.damageSources().arrow(this, owner != null ? owner : this);
                livingTarget.hurt(normalSource, standardDamage);
            }

            // 2. Zerujemy czas nietykalności, by druga pula obrażeń nie została odrzucona
            livingTarget.invulnerableTime = 0;

            // 3. Obrażenia penetrujące pancerz (magia ignoruje pancerz)
            if (bypassedDamage > 0) {
                DamageSource bypassSource = this.damageSources().magic();
                livingTarget.hurt(bypassSource, bypassedDamage);
            }

            this.playSound(SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 1.2F);
            this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        this.playSound(SoundEvents.ARMOR_EQUIP_IRON, 0.7F, 1.4F);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}