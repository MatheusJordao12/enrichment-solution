package com.enrichmentsolution.mod.common.entity;

import com.enrichmentsolution.mod.common.entity.goal.RobotAutoRechargeGoal;
import com.enrichmentsolution.mod.common.entity.goal.RobotDefendOwnerTargetGoal;
import com.enrichmentsolution.mod.common.entity.goal.RobotFollowOwnerGoal;
import com.enrichmentsolution.mod.common.entity.goal.RobotJetpackFlyGoal;
import com.enrichmentsolution.mod.common.entity.goal.RobotMineStoneGoal;
import com.enrichmentsolution.mod.common.entity.goal.RobotPickupItemGoal;
import com.enrichmentsolution.mod.common.entity.goal.RobotStayHostileTargetGoal;
import com.enrichmentsolution.mod.common.entity.goal.RobotStayPatrolGoal;
import com.enrichmentsolution.mod.common.entity.robot.RobotAnimationController;
import com.enrichmentsolution.mod.common.entity.robot.RobotEnergyManager;
import com.enrichmentsolution.mod.common.entity.robot.RobotInventoryHelper;
import com.enrichmentsolution.mod.common.entity.robot.RobotPatrolState;
import com.enrichmentsolution.mod.common.item.RobotItem;
import com.enrichmentsolution.mod.common.menu.RobotMenu;
import com.enrichmentsolution.mod.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * Companion Robot entity with modular energy, inventory/scaffolding, patrol, and animation helpers.
 */
public class RobotEntity extends PathfinderMob {

    public static final int SLOT_PICKAXE = 0;
    public static final int SLOT_STORAGE_START = 1;
    public static final int STORAGE_SLOTS = 9;
    public static final int SLOT_CELL_START = SLOT_STORAGE_START + STORAGE_SLOTS;
    public static final int CELL_SLOTS = 3;
    public static final int TOTAL_SLOTS = 1 + STORAGE_SLOTS + CELL_SLOTS;

    public static final int HIT_ENERGY_COST = 1_000;
    public static final int EXPLOSION_ENERGY_COST = 5_000;

    public static final byte EVENT_PICKUP = 100;
    public static final byte EVENT_ATTACK = 101;
    public static final int TRANSFORM_DURATION = 60;
    public static final int BOOT_DURATION = 45;
    public static final int POWER_OFF_DURATION = 18;

    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID =
            SynchedEntityData.defineId(RobotEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> DATA_MINING =
            SynchedEntityData.defineId(RobotEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_FOLLOW_OWNER =
            SynchedEntityData.defineId(RobotEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_TRANSFORM_TICKS =
            SynchedEntityData.defineId(RobotEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DETRANSFORM_TICKS =
            SynchedEntityData.defineId(RobotEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_BOOT_TICKS =
            SynchedEntityData.defineId(RobotEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_POWER_OFF_TICKS =
            SynchedEntityData.defineId(RobotEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ENERGY =
            SynchedEntityData.defineId(RobotEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MAX_ENERGY =
            SynchedEntityData.defineId(RobotEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_FLYING =
            SynchedEntityData.defineId(RobotEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_AUTO_RECHARGER =
            SynchedEntityData.defineId(RobotEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_RECHARGING =
            SynchedEntityData.defineId(RobotEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_DOCKED =
            SynchedEntityData.defineId(RobotEntity.class, EntityDataSerializers.BOOLEAN);

    public final AnimationState pickupAnimationState = new AnimationState();
    public final AnimationState mineAnimationState = new AnimationState();
    public final AnimationState attackAnimationState = new AnimationState();
    public final AnimationState combatTransformAnimationState = new AnimationState();
    public final AnimationState detransformAnimationState = new AnimationState();
    public final AnimationState summonBootAnimationState = new AnimationState();
    public final AnimationState powerOffAnimationState = new AnimationState();
    public final AnimationState flyAnimationState = new AnimationState();
    public final AnimationState dockAnimationState = new AnimationState();

    private final RobotEnergyManager energyManager = new RobotEnergyManager();
    private final RobotInventoryHelper inventoryHelper = new RobotInventoryHelper();
    private final RobotPatrolState patrolState = new RobotPatrolState();
    private final ItemStackHandler inventory = inventoryHelper.createInventory(this);

    public RobotEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER_UUID, Optional.empty());
        builder.define(DATA_MINING, false);
        builder.define(DATA_FOLLOW_OWNER, true);
        builder.define(DATA_TRANSFORM_TICKS, 0);
        builder.define(DATA_DETRANSFORM_TICKS, 0);
        builder.define(DATA_BOOT_TICKS, 0);
        builder.define(DATA_POWER_OFF_TICKS, 0);
        builder.define(DATA_ENERGY, 0);
        builder.define(DATA_MAX_ENERGY, 0);
        builder.define(DATA_FLYING, false);
        builder.define(DATA_AUTO_RECHARGER, false);
        builder.define(DATA_RECHARGING, false);
        builder.define(DATA_DOCKED, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RobotAutoRechargeGoal(this));
        this.goalSelector.addGoal(2, new RobotJetpackFlyGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15D, true) {
            @Override
            public boolean canUse() {
                return canPerformMelee() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return canPerformMelee() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(3, new RobotFollowOwnerGoal(this, 1.05D, 6.0F, 2.5F));
        this.goalSelector.addGoal(4, new RobotStayPatrolGoal(this));
        this.goalSelector.addGoal(5, new RobotPickupItemGoal(this));
        this.goalSelector.addGoal(6, new RobotMineStoneGoal(this));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F) {
            @Override
            public boolean canUse() {
                return isPowered() && !isTransforming() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return isPowered() && !isTransforming() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this) {
            @Override
            public boolean canUse() {
                return isPowered() && !isTransforming() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return isPowered() && !isTransforming() && super.canContinueToUse();
            }
        });

        this.targetSelector.addGoal(1, new RobotDefendOwnerTargetGoal(this));
        this.targetSelector.addGoal(2, new RobotStayHostileTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                if (!isPowered() || !hasSword() || isTransforming()) {
                    return false;
                }
                LivingEntity attacker = this.mob.getLastHurtByMob();
                if (attacker != null && attacker.getUUID().equals(getOwnerUUID())) {
                    return false;
                }
                return super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return isPowered() && hasSword() && !isTransforming() && super.canContinueToUse();
            }
        });
    }

    private boolean canPerformMelee() {
        if (!isPowered() || !hasSword() || isTransforming() || isFlying()) {
            return false;
        }
        return isFollowOwner() || getTarget() == null || isWithinPatrolArea(getTarget().blockPosition());
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            RobotAnimationController.tickServerTimers(this);
            if (!isFollowOwner() && !patrolState.hasPatrolAnchor()) {
                setPatrolAnchor(this.blockPosition());
            }
        } else {
            RobotAnimationController.tickClientAnimations(this);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        if (this.isInvulnerableTo(source) || this.invulnerableTime > 10) {
            return false;
        }

        int cost = source.is(DamageTypeTags.IS_EXPLOSION) ? EXPLOSION_ENERGY_COST : HIT_ENERGY_COST;
        if (getEnergyStored() > 0) {
            consumeEnergy(cost);
        }

        this.invulnerableTime = 20;
        this.hurtTime = 10;
        this.hurtDuration = 10;
        this.setHealth(this.getMaxHealth());
        this.playHurtSound(source);
        this.level().broadcastDamageEvent(this, source);
        return true;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        if (isFlying() || hasSword()) {
            return false;
        }
        return super.causeFallDamage(fallDistance, multiplier, source);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_PICKUP) {
            this.pickupAnimationState.start(this.tickCount);
        } else if (id == EVENT_ATTACK) {
            this.attackAnimationState.start(this.tickCount);
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (this.level() instanceof ServerLevel serverLevel) {
            this.level().broadcastEntityEvent(this, EVENT_ATTACK);
            boolean hit = super.doHurtTarget(target);
            if (hit && hasSword()) {
                ItemStack sword = getTool();
                sword.hurtAndBreak(1, serverLevel, null, item ->
                        this.inventory.setStackInSlot(SLOT_PICKAXE, ItemStack.EMPTY));
                syncMainHandPickaxe();
            }
            return hit;
        }
        return super.doHurtTarget(target);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (getOwnerUUID() == null) {
            setOwnerUUID(player.getUUID());
        } else if (!isOwner(player)) {
            player.displayClientMessage(Component.translatable("message.enrichment_solution.robot.not_owner"), true);
            return InteractionResult.FAIL;
        }

        ItemStack held = player.getItemInHand(hand);
        if (held.is(ModItems.AUTO_RECHARGER_UPGRADE_TEMPLATE.get()) && !hasAutoRecharger()) {
            setAutoRecharger(true);
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            this.playSound(SoundEvents.ANVIL_USE, 0.7F, 1.2F);
            player.displayClientMessage(Component.translatable("message.enrichment_solution.robot.auto_recharger_installed"), true);
            return InteractionResult.CONSUME;
        }

        if (player.isShiftKeyDown()) {
            return handleShiftInteract(player);
        }

        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInv, p) -> new RobotMenu(containerId, playerInv, this),
                Component.translatable("container.enrichment_solution.robot")
        ), buf -> buf.writeInt(this.getId()));

        return InteractionResult.CONSUME;
    }

    private InteractionResult handleShiftInteract(Player player) {
        if (isRecharging()) {
            setRechargeCancelled(true);
            setRecharging(false);
            this.getNavigation().stop();
            player.displayClientMessage(Component.translatable("message.enrichment_solution.robot.recharge_cancelled"), true);
            return InteractionResult.CONSUME;
        }
        ItemStack robotItem = new ItemStack(ModItems.ROBOT.get());
        CompoundTag tag = new CompoundTag();
        tag.put(RobotItem.TAG_INVENTORY, inventory.serializeNBT(this.registryAccess()));
        tag.putBoolean(RobotItem.TAG_AUTO_RECHARGER, hasAutoRecharger());
        BlockPos chargerPos = energyManager.getChargerPos();
        if (chargerPos != null) {
            tag.putInt(RobotItem.TAG_CHARGER_X, chargerPos.getX());
            tag.putInt(RobotItem.TAG_CHARGER_Y, chargerPos.getY());
            tag.putInt(RobotItem.TAG_CHARGER_Z, chargerPos.getZ());
        }
        robotItem.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        if (!player.getInventory().add(robotItem)) {
            this.spawnAtLocation(robotItem);
        }
        this.discard();
        return InteractionResult.CONSUME;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);
        RobotInventoryHelper.dropAllItems(level, this, inventory);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        UUID owner = getOwnerUUID();
        if (owner != null) {
            compound.putUUID("OwnerUUID", owner);
        }
        compound.putBoolean("FollowOwner", isFollowOwner());
        compound.putBoolean("AutoRecharger", hasAutoRecharger());
        energyManager.save(compound);
        patrolState.save(compound);
        compound.put("Inventory", inventory.serializeNBT(this.registryAccess()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.hasUUID("OwnerUUID")) {
            setOwnerUUID(compound.getUUID("OwnerUUID"));
        }
        if (compound.contains("FollowOwner")) {
            setFollowOwner(compound.getBoolean("FollowOwner"));
        }
        if (compound.contains("AutoRecharger")) {
            setAutoRecharger(compound.getBoolean("AutoRecharger"));
        }
        energyManager.load(compound);
        patrolState.load(compound);
        if (compound.contains("Inventory")) {
            inventoryHelper.deserializeInventory(this, inventory, compound.getCompound("Inventory"));
            if (!this.level().isClientSide) {
                syncMainHandPickaxe();
                syncEnergyData();
            }
        }
    }

    public void syncMainHandPickaxe() {
        this.setItemSlot(EquipmentSlot.MAINHAND, getTool().copy());
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public static boolean isValidTool(ItemStack stack) {
        return RobotInventoryHelper.isValidTool(stack);
    }

    public ItemStack getTool() {
        return RobotInventoryHelper.getTool(inventory);
    }

    public ItemStack getPickaxe() {
        return RobotInventoryHelper.getPickaxe(inventory);
    }

    public boolean hasPickaxe() {
        return !getPickaxe().isEmpty();
    }

    public boolean hasSword() {
        if (this.level().isClientSide) {
            return getMainHandItem().is(ItemTags.SWORDS);
        }
        return inventory.getStackInSlot(SLOT_PICKAXE).is(ItemTags.SWORDS);
    }

    public boolean isFollowOwner() {
        return this.entityData.get(DATA_FOLLOW_OWNER);
    }

    public void setFollowOwner(boolean followOwner) {
        this.entityData.set(DATA_FOLLOW_OWNER, followOwner);
        if (!followOwner && !this.level().isClientSide) {
            setPatrolAnchor(this.blockPosition());
        }
    }

    public boolean isFlying() {
        return this.entityData.get(DATA_FLYING);
    }

    public void setFlying(boolean flying) {
        this.entityData.set(DATA_FLYING, flying);
    }

    public void setPatrolAnchor(BlockPos pos) {
        patrolState.setPatrolAnchor(pos);
    }

    public boolean isWithinPatrolArea(BlockPos pos) {
        return patrolState.isWithinPatrolArea(pos.getX() + 0.5D, pos.getZ() + 0.5D);
    }

    public boolean isWithinPatrolArea(double x, double z) {
        return patrolState.isWithinPatrolArea(x, z);
    }

    public AABB getPatrolAreaBox() {
        return patrolState.getPatrolAreaBox(this.getX(), this.getY(), this.getZ());
    }

    public BlockPos getPatrolAnchorCenterPos() {
        return patrolState.getPatrolAnchorCenterPos(this.getX(), this.getY(), this.getZ());
    }

    public int getTransformTicks() {
        return this.entityData.get(DATA_TRANSFORM_TICKS);
    }

    public void setTransformTicks(int ticks) {
        this.entityData.set(DATA_TRANSFORM_TICKS, ticks);
    }

    public int getDetransformTicks() {
        return this.entityData.get(DATA_DETRANSFORM_TICKS);
    }

    public void setDetransformTicks(int ticks) {
        this.entityData.set(DATA_DETRANSFORM_TICKS, ticks);
    }

    public int getBootTicks() {
        return this.entityData.get(DATA_BOOT_TICKS);
    }

    public void setBootTicks(int ticks) {
        this.entityData.set(DATA_BOOT_TICKS, ticks);
    }

    public boolean isCombatTransforming() {
        return getTransformTicks() > 0;
    }

    public boolean isDetransforming() {
        return getDetransformTicks() > 0;
    }

    public boolean isBooting() {
        return getBootTicks() > 0;
    }

    public int getPowerOffTicks() {
        return this.entityData.get(DATA_POWER_OFF_TICKS);
    }

    public void setPowerOffTicks(int ticks) {
        this.entityData.set(DATA_POWER_OFF_TICKS, ticks);
    }

    public boolean isTransforming() {
        return isCombatTransforming() || isDetransforming() || isBooting();
    }

    public int getEnergyStored() {
        if (this.level().isClientSide) {
            return this.entityData.get(DATA_ENERGY);
        }
        return energyManager.getEnergyStored(inventory);
    }

    public int getMaxEnergyStored() {
        if (this.level().isClientSide) {
            return this.entityData.get(DATA_MAX_ENERGY);
        }
        return energyManager.getMaxEnergyStored(inventory);
    }

    public boolean isPowered() {
        return getEnergyStored() > 0;
    }

    public void syncEnergyData() {
        if (this.level().isClientSide) {
            return;
        }
        int stored = getEnergyStored();
        int max = getMaxEnergyStored();
        this.entityData.set(DATA_ENERGY, stored);
        this.entityData.set(DATA_MAX_ENERGY, max);

        int transition = energyManager.updatePowerTransition(stored, max);
        if (transition < 0) {
            setTransformTicks(0);
            setDetransformTicks(0);
            setBootTicks(0);
            setPowerOffTicks(POWER_OFF_DURATION);
            setMining(false);
            this.getNavigation().stop();
            this.setTarget(null);
        } else if (transition > 0) {
            setPowerOffTicks(0);
            triggerWakeSequence();
        }
    }

    private void triggerWakeSequence() {
        if (hasSword()) {
            inventoryHelper.setWasHoldingSword(true);
            setTransformTicks(TRANSFORM_DURATION);
        } else {
            setBootTicks(BOOT_DURATION);
        }
    }

    public void consumeEnergy(int amount) {
        energyManager.consumeEnergy(this, inventory, amount);
    }

    public int chargeEnergy(int amount) {
        return energyManager.chargeEnergy(this, inventory, amount);
    }

    public boolean hasAutoRecharger() {
        return this.entityData.get(DATA_AUTO_RECHARGER);
    }

    public void setAutoRecharger(boolean autoRecharger) {
        this.entityData.set(DATA_AUTO_RECHARGER, autoRecharger);
    }

    public boolean isRecharging() {
        return this.entityData.get(DATA_RECHARGING);
    }

    public void setRecharging(boolean recharging) {
        this.entityData.set(DATA_RECHARGING, recharging);
    }

    public boolean isDocked() {
        return this.entityData.get(DATA_DOCKED);
    }

    public void setDocked(boolean docked) {
        this.entityData.set(DATA_DOCKED, docked);
    }

    public boolean isRechargeCancelled() {
        return energyManager.isRechargeCancelled();
    }

    public void setRechargeCancelled(boolean cancelled) {
        energyManager.setRechargeCancelled(cancelled);
    }

    public boolean hasChargerPos() {
        return energyManager.getChargerPos() != null;
    }

    @Nullable
    public BlockPos getChargerPos() {
        return energyManager.getChargerPos();
    }

    public void setChargerPos(@Nullable BlockPos pos) {
        energyManager.setChargerPos(pos);
    }

    public void startSummonBoot() {
        syncEnergyData();
        if (!isPowered()) {
            return;
        }
        triggerWakeSequence();
    }

    @Nullable
    public Player getOwnerPlayer() {
        UUID uuid = getOwnerUUID();
        return uuid != null ? this.level().getPlayerByUUID(uuid) : null;
    }

    public boolean hasStorageSpaceFor(ItemStack stack) {
        return RobotInventoryHelper.hasStorageSpaceFor(inventory, stack);
    }

    public boolean hasAnyStorageSlotFree() {
        return RobotInventoryHelper.hasAnyStorageSlotFree(inventory);
    }

    public ItemStack insertIntoStorage(ItemStack stack) {
        return RobotInventoryHelper.insertIntoStorage(inventory, stack);
    }

    public boolean tryPlaceScaffoldBlock(BlockPos pos) {
        return RobotInventoryHelper.tryPlaceScaffoldBlock(this, inventory, pos);
    }

    public void triggerPickupAnimation() {
        this.level().broadcastEntityEvent(this, EVENT_PICKUP);
    }

    public boolean isMining() {
        return this.entityData.get(DATA_MINING);
    }

    public void setMining(boolean mining) {
        this.entityData.set(DATA_MINING, mining);
    }

    @Nullable
    public UUID getOwnerUUID() {
        return this.entityData.get(DATA_OWNER_UUID).orElse(null);
    }

    public void setOwnerUUID(@Nullable UUID uuid) {
        this.entityData.set(DATA_OWNER_UUID, Optional.ofNullable(uuid));
    }

    public boolean isOwner(Player player) {
        UUID owner = getOwnerUUID();
        return owner != null && owner.equals(player.getUUID());
    }
}
