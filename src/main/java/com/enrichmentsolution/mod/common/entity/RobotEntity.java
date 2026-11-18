package com.enrichmentsolution.mod.common.entity;

import com.enrichmentsolution.mod.common.entity.goal.RobotAutoRechargeGoal;
import com.enrichmentsolution.mod.common.entity.goal.RobotDefendOwnerTargetGoal;
import com.enrichmentsolution.mod.common.entity.goal.RobotFollowOwnerGoal;
import com.enrichmentsolution.mod.common.entity.goal.RobotJetpackFlyGoal;
import com.enrichmentsolution.mod.common.entity.goal.RobotMineStoneGoal;
import com.enrichmentsolution.mod.common.entity.goal.RobotPickupItemGoal;
import com.enrichmentsolution.mod.common.entity.goal.RobotStayHostileTargetGoal;
import com.enrichmentsolution.mod.common.entity.goal.RobotStayPatrolGoal;
import com.enrichmentsolution.mod.common.item.EnergyCellBlockItem;
import com.enrichmentsolution.mod.common.item.RobotItem;
import com.enrichmentsolution.mod.common.menu.RobotMenu;
import com.enrichmentsolution.mod.registry.ModDataComponents;
import com.enrichmentsolution.mod.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.Containers;
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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

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

    private boolean wasHoldingSword = false;
    private boolean wasPowered = false;
    private boolean hasPatrolAnchor = false;
    private int patrolAnchorChunkX = 0;
    private int patrolAnchorChunkZ = 0;
    @Nullable
    private BlockPos chargerPos = null;
    private boolean rechargeCancelled = false;

    private final ItemStackHandler inventory = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_PICKAXE) {
                return isValidTool(stack);
            }
            if (slot >= SLOT_CELL_START && slot < TOTAL_SLOTS) {
                return stack.getItem() instanceof EnergyCellBlockItem;
            }
            return true;
        }

        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            if (!RobotEntity.this.level().isClientSide) {
                if (slot == SLOT_PICKAXE) {
                    boolean nowHasSword = inventory.getStackInSlot(SLOT_PICKAXE).is(ItemTags.SWORDS);
                    if (isPowered()) {
                        if (nowHasSword && !wasHoldingSword) {
                            setDetransformTicks(0);
                            setTransformTicks(TRANSFORM_DURATION);
                        } else if (!nowHasSword && wasHoldingSword) {
                            setTransformTicks(0);
                            setDetransformTicks(TRANSFORM_DURATION);
                        }
                    }
                    wasHoldingSword = nowHasSword;
                    RobotEntity.this.syncMainHandPickaxe();
                } else if (slot >= SLOT_CELL_START && slot < TOTAL_SLOTS) {
                    RobotEntity.this.syncEnergyData();
                }
            }
        }
    };

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
                if (!RobotEntity.this.isPowered() || !RobotEntity.this.hasSword()
                        || RobotEntity.this.isTransforming() || RobotEntity.this.isFlying()) {
                    return false;
                }
                if (!RobotEntity.this.isFollowOwner() && RobotEntity.this.getTarget() != null
                        && !RobotEntity.this.isWithinPatrolArea(RobotEntity.this.getTarget().blockPosition())) {
                    return false;
                }
                return super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                if (!RobotEntity.this.isPowered() || !RobotEntity.this.hasSword()
                        || RobotEntity.this.isTransforming() || RobotEntity.this.isFlying()) {
                    return false;
                }
                if (!RobotEntity.this.isFollowOwner() && RobotEntity.this.getTarget() != null
                        && !RobotEntity.this.isWithinPatrolArea(RobotEntity.this.getTarget().blockPosition())) {
                    return false;
                }
                return super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(3, new RobotFollowOwnerGoal(this, 1.05D, 6.0F, 2.5F));
        this.goalSelector.addGoal(4, new RobotStayPatrolGoal(this));
        this.goalSelector.addGoal(5, new RobotPickupItemGoal(this));
        this.goalSelector.addGoal(6, new RobotMineStoneGoal(this));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F) {
            @Override
            public boolean canUse() {
                return RobotEntity.this.isPowered() && !RobotEntity.this.isTransforming() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return RobotEntity.this.isPowered() && !RobotEntity.this.isTransforming() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this) {
            @Override
            public boolean canUse() {
                return RobotEntity.this.isPowered() && !RobotEntity.this.isTransforming() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return RobotEntity.this.isPowered() && !RobotEntity.this.isTransforming() && super.canContinueToUse();
            }
        });

        this.targetSelector.addGoal(1, new RobotDefendOwnerTargetGoal(this));
        this.targetSelector.addGoal(2, new RobotStayHostileTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                if (!RobotEntity.this.isPowered() || !RobotEntity.this.hasSword() || RobotEntity.this.isTransforming()) {
                    return false;
                }
                LivingEntity attacker = this.mob.getLastHurtByMob();
                if (attacker != null && attacker.getUUID().equals(RobotEntity.this.getOwnerUUID())) {
                    return false;
                }
                return super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return RobotEntity.this.isPowered() && RobotEntity.this.hasSword() && !RobotEntity.this.isTransforming() && super.canContinueToUse();
            }
        });
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            int transform = getTransformTicks();
            if (transform > 0) {
                setTransformTicks(transform - 1);
            }
            int detransform = getDetransformTicks();
            if (detransform > 0) {
                setDetransformTicks(detransform - 1);
            }
            int boot = getBootTicks();
            if (boot > 0) {
                setBootTicks(boot - 1);
            }
            int powerOff = getPowerOffTicks();
            if (powerOff > 0) {
                setPowerOffTicks(powerOff - 1);
            }
            if (transform > 0 || detransform > 0 || boot > 0 || !isPowered()) {
                this.getNavigation().stop();
                this.setTarget(null);
                setMining(false);
                if (isFlying()) {
                    setFlying(false);
                    setNoGravity(false);
                }
            }
            if (!isFollowOwner() && !hasPatrolAnchor) {
                setPatrolAnchor(this.blockPosition());
            }
        } else {
            if (isFlying()) {
                flyAnimationState.startIfStopped(this.tickCount);
            } else {
                flyAnimationState.stop();
            }
            if (isDocked()) {
                dockAnimationState.startIfStopped(this.tickCount);
            } else {
                dockAnimationState.stop();
            }
            if (getBootTicks() > 0 && !summonBootAnimationState.isStarted()) {
                powerOffAnimationState.stop();
                summonBootAnimationState.start(this.tickCount);
            } else if (getBootTicks() <= 0 && summonBootAnimationState.isStarted()) {
                summonBootAnimationState.stop();
            }

            if (getTransformTicks() > 0 && !combatTransformAnimationState.isStarted()) {
                powerOffAnimationState.stop();
                detransformAnimationState.stop();
                combatTransformAnimationState.start(this.tickCount);
            } else if (getTransformTicks() <= 0 && combatTransformAnimationState.isStarted()) {
                combatTransformAnimationState.stop();
            }

            if (getDetransformTicks() > 0 && !detransformAnimationState.isStarted()) {
                powerOffAnimationState.stop();
                combatTransformAnimationState.stop();
                detransformAnimationState.start(this.tickCount);
            } else if (getDetransformTicks() <= 0 && detransformAnimationState.isStarted()) {
                detransformAnimationState.stop();
            }

            if (getPowerOffTicks() > 0 && !powerOffAnimationState.isStarted()) {
                summonBootAnimationState.stop();
                combatTransformAnimationState.stop();
                detransformAnimationState.stop();
                powerOffAnimationState.start(this.tickCount);
            } else if (isPowered() && powerOffAnimationState.isStarted()) {
                powerOffAnimationState.stop();
            }

            if (isMining() && isPowered() && !isTransforming()) {
                mineAnimationState.startIfStopped(this.tickCount);
            } else {
                mineAnimationState.stop();
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide) {
            return super.hurt(source, amount);
        }
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        if (this.invulnerableTime > 10) {
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
            this.playSound(net.minecraft.sounds.SoundEvents.ANVIL_USE, 0.7F, 1.2F);
            player.displayClientMessage(Component.translatable("message.enrichment_solution.robot.auto_recharger_installed"), true);
            return InteractionResult.CONSUME;
        }

        if (player.isShiftKeyDown()) {
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

        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInv, p) -> new RobotMenu(containerId, playerInv, this),
                Component.translatable("container.enrichment_solution.robot")
        ), buf -> buf.writeInt(this.getId()));

        return InteractionResult.CONSUME;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, this.getX(), this.getY(), this.getZ(), stack.copy());
                inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
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
        if (chargerPos != null) {
            compound.putInt("ChargerX", chargerPos.getX());
            compound.putInt("ChargerY", chargerPos.getY());
            compound.putInt("ChargerZ", chargerPos.getZ());
        }
        compound.putBoolean("HasPatrolAnchor", hasPatrolAnchor);
        compound.putInt("PatrolAnchorChunkX", patrolAnchorChunkX);
        compound.putInt("PatrolAnchorChunkZ", patrolAnchorChunkZ);
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
        if (compound.contains("ChargerX") && compound.contains("ChargerY") && compound.contains("ChargerZ")) {
            setChargerPos(new BlockPos(compound.getInt("ChargerX"), compound.getInt("ChargerY"), compound.getInt("ChargerZ")));
        }
        if (compound.contains("HasPatrolAnchor")) {
            hasPatrolAnchor = compound.getBoolean("HasPatrolAnchor");
            patrolAnchorChunkX = compound.getInt("PatrolAnchorChunkX");
            patrolAnchorChunkZ = compound.getInt("PatrolAnchorChunkZ");
        }
        if (compound.contains("Inventory")) {
            inventory.deserializeNBT(this.registryAccess(), compound.getCompound("Inventory"));
            if (inventory.getSlots() != TOTAL_SLOTS) {
                ItemStack[] preserved = new ItemStack[Math.min(inventory.getSlots(), TOTAL_SLOTS)];
                for (int i = 0; i < preserved.length; i++) {
                    preserved[i] = inventory.getStackInSlot(i);
                }
                inventory.setSize(TOTAL_SLOTS);
                for (int i = 0; i < preserved.length; i++) {
                    inventory.setStackInSlot(i, preserved[i]);
                }
            }
            wasHoldingSword = inventory.getStackInSlot(SLOT_PICKAXE).is(ItemTags.SWORDS);
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
        return stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.SWORDS);
    }

    public ItemStack getTool() {
        ItemStack stack = inventory.getStackInSlot(SLOT_PICKAXE);
        return isValidTool(stack) ? stack : ItemStack.EMPTY;
    }

    public ItemStack getPickaxe() {
        ItemStack stack = inventory.getStackInSlot(SLOT_PICKAXE);
        return stack.is(ItemTags.PICKAXES) ? stack : ItemStack.EMPTY;
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
        ChunkPos chunkPos = new ChunkPos(pos);
        this.patrolAnchorChunkX = chunkPos.x;
        this.patrolAnchorChunkZ = chunkPos.z;
        this.hasPatrolAnchor = true;
    }

    public boolean isWithinPatrolArea(BlockPos pos) {
        return isWithinPatrolArea(pos.getX() + 0.5D, pos.getZ() + 0.5D);
    }

    public boolean isWithinPatrolArea(double x, double z) {
        if (!hasPatrolAnchor) {
            return true;
        }
        int cx = SectionPos.blockToSectionCoord(x);
        int cz = SectionPos.blockToSectionCoord(z);
        return Math.abs(cx - patrolAnchorChunkX) <= 1 && Math.abs(cz - patrolAnchorChunkZ) <= 1;
    }

    public AABB getPatrolAreaBox() {
        int anchorX = hasPatrolAnchor ? patrolAnchorChunkX : SectionPos.blockToSectionCoord(this.getX());
        int anchorZ = hasPatrolAnchor ? patrolAnchorChunkZ : SectionPos.blockToSectionCoord(this.getZ());
        double minX = (anchorX - 1) * 16.0D;
        double maxX = (anchorX + 2) * 16.0D;
        double minZ = (anchorZ - 1) * 16.0D;
        double maxZ = (anchorZ + 2) * 16.0D;
        return new AABB(minX, this.getY() - 16.0D, minZ, maxX, this.getY() + 24.0D, maxZ);
    }

    public BlockPos getPatrolAnchorCenterPos() {
        int anchorX = hasPatrolAnchor ? patrolAnchorChunkX : SectionPos.blockToSectionCoord(this.getX());
        int anchorZ = hasPatrolAnchor ? patrolAnchorChunkZ : SectionPos.blockToSectionCoord(this.getZ());
        return new BlockPos(anchorX * 16 + 8, (int) Math.round(this.getY()), anchorZ * 16 + 8);
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
        int total = 0;
        for (int i = SLOT_CELL_START; i < TOTAL_SLOTS; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.getItem() instanceof EnergyCellBlockItem) {
                total += stack.getOrDefault(ModDataComponents.STORED_ENERGY.get(), 0);
            }
        }
        return total;
    }

    public int getMaxEnergyStored() {
        if (this.level().isClientSide) {
            return this.entityData.get(DATA_MAX_ENERGY);
        }
        int total = 0;
        for (int i = SLOT_CELL_START; i < TOTAL_SLOTS; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.getItem() instanceof EnergyCellBlockItem cellItem) {
                total += cellItem.getTier().getCapacity();
            }
        }
        return total;
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
        if (max > 0 && stored > max * 0.10D) {
            this.rechargeCancelled = false;
        }

        boolean nowPowered = stored > 0;
        boolean prevPowered = wasPowered;
        wasPowered = nowPowered;

        if (prevPowered && !nowPowered) {
            setTransformTicks(0);
            setDetransformTicks(0);
            setBootTicks(0);
            setPowerOffTicks(POWER_OFF_DURATION);
            setMining(false);
            this.getNavigation().stop();
            this.setTarget(null);
        } else if (!prevPowered && nowPowered) {
            setPowerOffTicks(0);
            triggerWakeSequence();
        }
    }

    private void triggerWakeSequence() {
        if (hasSword()) {
            this.wasHoldingSword = true;
            setTransformTicks(TRANSFORM_DURATION);
        } else {
            setBootTicks(BOOT_DURATION);
        }
    }

    public void consumeEnergy(int amount) {
        if (amount <= 0 || this.level().isClientSide) {
            return;
        }
        int remaining = amount;
        for (int i = SLOT_CELL_START; i < TOTAL_SLOTS && remaining > 0; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!(stack.getItem() instanceof EnergyCellBlockItem)) {
                continue;
            }
            int cellEnergy = stack.getOrDefault(ModDataComponents.STORED_ENERGY.get(), 0);
            if (cellEnergy <= 0) {
                continue;
            }
            int take = Math.min(cellEnergy, remaining);
            int updated = cellEnergy - take;
            if (updated > 0) {
                stack.set(ModDataComponents.STORED_ENERGY.get(), updated);
            } else {
                stack.remove(ModDataComponents.STORED_ENERGY.get());
            }
            remaining -= take;
        }
        syncEnergyData();
    }

    public int chargeEnergy(int amount) {
        if (amount <= 0 || this.level().isClientSide) {
            return 0;
        }
        int remaining = amount;
        int accepted = 0;
        for (int i = SLOT_CELL_START; i < TOTAL_SLOTS && remaining > 0; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!(stack.getItem() instanceof EnergyCellBlockItem cellItem)) {
                continue;
            }
            int current = stack.getOrDefault(ModDataComponents.STORED_ENERGY.get(), 0);
            int cap = cellItem.getTier().getCapacity();
            int space = cap - current;
            if (space <= 0) {
                continue;
            }
            int add = Math.min(space, remaining);
            stack.set(ModDataComponents.STORED_ENERGY.get(), current + add);
            remaining -= add;
            accepted += add;
        }
        if (accepted > 0) {
            syncEnergyData();
        }
        return accepted;
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
        return rechargeCancelled;
    }

    public void setRechargeCancelled(boolean cancelled) {
        this.rechargeCancelled = cancelled;
    }

    public boolean hasChargerPos() {
        return chargerPos != null;
    }

    @Nullable
    public BlockPos getChargerPos() {
        return chargerPos;
    }

    public void setChargerPos(@Nullable BlockPos pos) {
        this.chargerPos = pos;
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
        if (stack.isEmpty()) {
            return false;
        }
        ItemStack remaining = stack.copy();
        for (int i = SLOT_STORAGE_START; i < TOTAL_SLOTS; i++) {
            remaining = inventory.insertItem(i, remaining, true);
            if (remaining.isEmpty()) {
                return true;
            }
        }
        return remaining.getCount() < stack.getCount();
    }

    public boolean hasAnyStorageSlotFree() {
        for (int i = SLOT_STORAGE_START; i < TOTAL_SLOTS; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    public ItemStack insertIntoStorage(ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int i = SLOT_STORAGE_START; i < TOTAL_SLOTS; i++) {
            remaining = inventory.insertItem(i, remaining, false);
            if (remaining.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }
        return remaining;
    }

    public boolean tryPlaceScaffoldBlock(net.minecraft.core.BlockPos pos) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        BlockState existing = serverLevel.getBlockState(pos);
        if (!existing.canBeReplaced()) {
            return false;
        }
        int slot = findScaffoldSlot();
        if (slot < 0) {
            return false;
        }
        ItemStack stack = inventory.getStackInSlot(slot);
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        BlockState placeState = blockItem.getBlock().defaultBlockState();
        if (serverLevel.setBlock(pos, placeState, Block.UPDATE_ALL)) {
            inventory.extractItem(slot, 1, false);
            SoundType soundType = placeState.getSoundType(serverLevel, pos, this);
            serverLevel.playSound(null, pos, soundType.getPlaceSound(), net.minecraft.sounds.SoundSource.BLOCKS,
                    (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);
            return true;
        }
        return false;
    }

    private int findScaffoldSlot() {
        int fallback = -1;
        for (int i = SLOT_STORAGE_START; i < TOTAL_SLOTS; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
                continue;
            }
            Block block = blockItem.getBlock();
            if (block instanceof BaseEntityBlock || block instanceof FallingBlock) {
                continue;
            }
            BlockState state = block.defaultBlockState();
            if (!state.isCollisionShapeFullBlock(this.level(), this.blockPosition())) {
                continue;
            }
            if (block == Blocks.COBBLESTONE || block == Blocks.COBBLED_DEEPSLATE
                    || block == Blocks.DIRT || block == Blocks.STONE) {
                return i;
            }
            if (fallback < 0) {
                fallback = i;
            }
        }
        return fallback;
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
