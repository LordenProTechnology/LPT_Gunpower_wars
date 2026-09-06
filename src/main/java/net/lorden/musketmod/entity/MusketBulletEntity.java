package net.lorden.musketmod.entity;

import net.lorden.musketmod.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public class MusketBulletEntity extends AbstractArrow implements ItemSupplier {
    private int ticksInGround = 0;

    public MusketBulletEntity(EntityType<? extends AbstractArrow> type, Level level) {
        super(type, level);
        this.pickup = Pickup.DISALLOWED;
    }

    public MusketBulletEntity(Level level, LivingEntity shooter) {
        super(ModEntities.MUSKET_BULLET.get(), shooter, level);
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
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        this.playSound(SoundEvents.ARMOR_EQUIP_IRON, 0.7F, 1.4F);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}