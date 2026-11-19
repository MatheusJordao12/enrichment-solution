package com.enrichmentsolution.mod.common.entity;

import com.enrichmentsolution.mod.common.entity.goal.RobotAutoRechargeGoal;
import com.enrichmentsolution.mod.common.entity.goal.RobotDefendOwnerTargetGoal;
import com.enrichmentsolution.mod.common.entity.goal.RobotDepositChestGoal;
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
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
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
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Companion Robot entity with modular energy, inventory/scaffolding, patrol, and animation helpers.
 */
public class RobotEntity extends PathfinderMob {

    public static final int SLOT_PICKAXE = 0;
    public static final int SLOT_CELL_START = 1;
    public static final int CELL_SLOTS = 3;
    public static final int SLOT_STORAGE_START = SLOT_CELL_START + CELL_SLOTS;
    public static final int STORAGE_SLOTS = 9;
    public static final int MAX_STORAGE_SLOTS = 81;
    public static final int TOTAL_SLOTS = 1 + CELL_SLOTS + MAX_STORAGE_SLOTS;

    public static final int HIT_ENERGY_COST = 1_000;
    public static final int EXPLOSION_ENERGY_COST = 5_000;
    public static final int MAX_STORED_XP = 5_000;
    public static final int TOOL_RECHARGE_PER_TICK = 1_000;
    public static final int MINING_ENERGY_COST = 10;
    public static final int MINING_BLOCKS_PER_ENERGY_COST = 9;

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
    private static final EntityDataAccessor<Integer> DATA_SLOT_UPGRADE_TIER =
            SynchedEntityData.defineId(RobotEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MINING_TARGET_Y =
            SynchedEntityData.defineId(RobotEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_DIG_CHUNKS =
            SynchedEntityData.defineId(RobotEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_CHUNK_RADIUS =
            SynchedEntityData.defineId(RobotEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<BlockPos>> DATA_LINKED_CHEST_POS =
            SynchedEntityData.defineId(RobotEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Integer> DATA_STORED_XP =
            SynchedEntityData.defineId(RobotEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MINING_UPGRADE_TIER =
            SynchedEntityData.defineId(RobotEntity.class, EntityDataSerializers.INT);

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
    private boolean hasDigAnchor = false;
    private int digAnchorChunkX = 0;
    private int digAnchorChunkZ = 0;
    private int blocksBrokenCounter = 0;

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
        builder.define(DATA_SLOT_UPGRADE_TIER, 0);
        builder.define(DATA_MINING_TARGET_Y, 0);
        builder.define(DATA_DIG_CHUNKS, false);
        builder.define(DATA_CHUNK_RADIUS, 1);
        builder.define(DATA_LINKED_CHEST_POS, Optional.empty());
        builder.define(DATA_STORED_XP, 0);
        builder.define(DATA_MINING_UPGRADE_TIER, 0);
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
        this.goalSelector.addGoal(3, new RobotDepositChestGoal(this));
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
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }

        // 1. Auto-charge FE tool in slot 0 from Robot's battery
        if (hasAutoRecharger() && getEnergyStored() > 0) {
            ItemStack tool = inventory.getStackInSlot(SLOT_PICKAXE);
            if (!tool.isEmpty()) {
                IEnergyStorage toolEnergy = tool.getCapability(Capabilities.EnergyStorage.ITEM);
                if (toolEnergy != null && toolEnergy.canReceive()) {
                    int maxTransfer = Math.min(TOOL_RECHARGE_PER_TICK, getEnergyStored());
                    int accepted = toolEnergy.receiveEnergy(maxTransfer, false);
                    if (accepted > 0) {
                        consumeEnergy(accepted);
                        syncMainHandPickaxe();
                    }
                }
            }
        }

        // 2. Absorb nearby ExperienceOrb entities within 2.5 blocks
        if (getStoredXp() < MAX_STORED_XP) {
            List<ExperienceOrb> orbs = this.level().getEntitiesOfClass(
                    ExperienceOrb.class,
                    this.getBoundingBox().inflate(2.5D));
            for (ExperienceOrb orb : orbs) {
                if (orb.isAlive() && getStoredXp() < MAX_STORED_XP) {
                    addXp(orb.getValue());
                    orb.discard();
                }
            }
        }

        // 3. Mending repair for damaged slot 0 tool using stored XP (2 durability per 1 XP)
        ItemStack tool = inventory.getStackInSlot(SLOT_PICKAXE);
        if (!tool.isEmpty() && tool.isDamaged() && getStoredXp() > 0) {
            Holder<Enchantment> mending = this.registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.MENDING);
            if (EnchantmentHelper.getItemEnchantmentLevel(mending, tool) > 0) {
                int xpNeeded = (tool.getDamageValue() + 1) / 2;
                int xpConsumed = consumeXp(Math.min(getStoredXp(), xpNeeded));
                if (xpConsumed > 0) {
                    int repairAmount = Math.min(tool.getDamageValue(), xpConsumed * 2);
                    tool.setDamageValue(tool.getDamageValue() - repairAmount);
                    syncMainHandPickaxe();
                }
            }
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
        if (held.is(ModItems.SLOT_UPGRADE_TIER_1.get()) && getSlotUpgradeTier() < 1) {
            return applySlotUpgrade(player, held, 1);
        }
        if (held.is(ModItems.SLOT_UPGRADE_TIER_2.get()) && getSlotUpgradeTier() < 2) {
            return applySlotUpgrade(player, held, 2);
        }
        if (held.is(ModItems.SLOT_UPGRADE_TIER_3.get()) && getSlotUpgradeTier() < 3) {
            return applySlotUpgrade(player, held, 3);
        }
        if (held.is(ModItems.MINING_UPGRADE_TIER_1.get()) && getMiningUpgradeTier() < 1) {
            return applyMiningUpgrade(player, held, 1);
        }
        if (held.is(ModItems.MINING_UPGRADE_TIER_2.get()) && getMiningUpgradeTier() < 2) {
            return applyMiningUpgrade(player, held, 2);
        }
        if (held.is(ModItems.MINING_UPGRADE_TIER_3.get()) && getMiningUpgradeTier() < 3) {
            return applyMiningUpgrade(player, held, 3);
        }
        if (held.is(ModItems.MINING_UPGRADE_TIER_4.get()) && getMiningUpgradeTier() < 4) {
            return applyMiningUpgrade(player, held, 4);
        }
        if (held.is(ModItems.MINING_UPGRADE_TIER_5.get()) && getMiningUpgradeTier() < 5) {
            return applyMiningUpgrade(player, held, 5);
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

    private InteractionResult applySlotUpgrade(Player player, ItemStack held, int tier) {
        setSlotUpgradeTier(tier);
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        this.playSound(SoundEvents.ANVIL_USE, 0.7F, 1.2F);
        return InteractionResult.CONSUME;
    }

    private InteractionResult applyMiningUpgrade(Player player, ItemStack held, int tier) {
        setMiningUpgradeTier(tier);
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        this.playSound(SoundEvents.ANVIL_USE, 0.7F, 1.2F);
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
        tag.putInt(RobotItem.TAG_SLOT_UPGRADE_TIER, getSlotUpgradeTier());
        tag.putInt(RobotItem.TAG_MINING_UPGRADE_TIER, getMiningUpgradeTier());
        tag.putInt(RobotItem.TAG_STORED_XP, getStoredXp());
        tag.putInt(RobotItem.TAG_MINING_TARGET_Y, getMiningTargetY());
        tag.putBoolean(RobotItem.TAG_DIG_CHUNKS, isDigChunks());
        tag.putInt(RobotItem.TAG_CHUNK_RADIUS, getChunkRadius());
        BlockPos chargerPos = energyManager.getChargerPos();
        if (chargerPos != null) {
            tag.putInt(RobotItem.TAG_CHARGER_X, chargerPos.getX());
            tag.putInt(RobotItem.TAG_CHARGER_Y, chargerPos.getY());
            tag.putInt(RobotItem.TAG_CHARGER_Z, chargerPos.getZ());
        }
        BlockPos linkedChestPos = getLinkedChestPos();
        if (linkedChestPos != null) {
            tag.putInt(RobotItem.TAG_LINKED_CHEST_X, linkedChestPos.getX());
            tag.putInt(RobotItem.TAG_LINKED_CHEST_Y, linkedChestPos.getY());
            tag.putInt(RobotItem.TAG_LINKED_CHEST_Z, linkedChestPos.getZ());
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
        compound.putInt("SlotUpgradeTier", getSlotUpgradeTier());
        compound.putInt("MiningUpgradeTier", getMiningUpgradeTier());
        compound.putInt("StoredXp", getStoredXp());
        compound.putInt("BlocksBrokenCounter", blocksBrokenCounter);
        compound.putInt("MiningTargetY", getMiningTargetY());
        compound.putBoolean("DigChunks", isDigChunks());
        compound.putInt("ChunkRadius", getChunkRadius());
        BlockPos linkedChest = getLinkedChestPos();
        if (linkedChest != null) {
            compound.putInt("LinkedChestX", linkedChest.getX());
            compound.putInt("LinkedChestY", linkedChest.getY());
            compound.putInt("LinkedChestZ", linkedChest.getZ());
        }
        if (hasDigAnchor) {
            compound.putBoolean("HasDigAnchor", true);
            compound.putInt("DigAnchorChunkX", digAnchorChunkX);
            compound.putInt("DigAnchorChunkZ", digAnchorChunkZ);
        }
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
        if (compound.contains("SlotUpgradeTier")) {
            setSlotUpgradeTier(compound.getInt("SlotUpgradeTier"));
        }
        if (compound.contains("MiningUpgradeTier")) {
            setMiningUpgradeTier(compound.getInt("MiningUpgradeTier"));
        }
        if (compound.contains("StoredXp")) {
            setStoredXp(compound.getInt("StoredXp"));
        }
        if (compound.contains("BlocksBrokenCounter")) {
            this.blocksBrokenCounter = Math.max(0, compound.getInt("BlocksBrokenCounter"));
        }
        if (compound.contains("MiningTargetY")) {
            setMiningTargetY(compound.getInt("MiningTargetY"));
        }
        if (compound.contains("DigChunks")) {
            setDigChunks(compound.getBoolean("DigChunks"));
        }
        if (compound.contains("ChunkRadius")) {
            setChunkRadius(compound.getInt("ChunkRadius"));
        }
        if (compound.contains("LinkedChestX") && compound.contains("LinkedChestY") && compound.contains("LinkedChestZ")) {
            setLinkedChestPos(new BlockPos(
                    compound.getInt("LinkedChestX"),
                    compound.getInt("LinkedChestY"),
                    compound.getInt("LinkedChestZ")));
        }
        if (compound.getBoolean("HasDigAnchor")) {
            this.hasDigAnchor = true;
            this.digAnchorChunkX = compound.getInt("DigAnchorChunkX");
            this.digAnchorChunkZ = compound.getInt("DigAnchorChunkZ");
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
            ItemStack mainHand = getMainHandItem();
            return mainHand.is(ItemTags.SWORDS) || mainHand.getItem() instanceof SwordItem;
        }
        ItemStack slotTool = inventory.getStackInSlot(SLOT_PICKAXE);
        return slotTool.is(ItemTags.SWORDS) || slotTool.getItem() instanceof SwordItem;
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
        return RobotInventoryHelper.hasStorageSpaceFor(this, inventory, stack);
    }

    public boolean hasAnyStorageSlotFree() {
        return RobotInventoryHelper.hasAnyStorageSlotFree(this, inventory);
    }

    public boolean hasEmptyStorageSlot() {
        return RobotInventoryHelper.hasEmptyStorageSlot(this, inventory);
    }

    public ItemStack insertIntoStorage(ItemStack stack) {
        return RobotInventoryHelper.insertIntoStorage(this, inventory, stack);
    }

    public int getSlotUpgradeTier() {
        return this.entityData.get(DATA_SLOT_UPGRADE_TIER);
    }

    public void setSlotUpgradeTier(int tier) {
        this.entityData.set(DATA_SLOT_UPGRADE_TIER, Math.max(0, Math.min(3, tier)));
    }

    public int getMiningUpgradeTier() {
        return this.entityData.get(DATA_MINING_UPGRADE_TIER);
    }

    public void setMiningUpgradeTier(int tier) {
        this.entityData.set(DATA_MINING_UPGRADE_TIER, Math.max(0, Math.min(5, tier)));
    }

    public int getStoredXp() {
        return this.entityData.get(DATA_STORED_XP);
    }

    public int getMaxStoredXp() {
        return MAX_STORED_XP;
    }

    public void setStoredXp(int xp) {
        this.entityData.set(DATA_STORED_XP, Mth.clamp(xp, 0, MAX_STORED_XP));
    }

    public int addXp(int amount) {
        if (amount <= 0) {
            return 0;
        }
        int current = getStoredXp();
        int space = MAX_STORED_XP - current;
        int added = Math.min(amount, space);
        if (added > 0) {
            setStoredXp(current + added);
        }
        return added;
    }

    public int consumeXp(int amount) {
        if (amount <= 0) {
            return 0;
        }
        int current = getStoredXp();
        int consumed = Math.min(amount, current);
        if (consumed > 0) {
            setStoredXp(current - consumed);
        }
        return consumed;
    }

    public boolean hasMiningEnergy() {
        return getEnergyStored() >= MINING_ENERGY_COST;
    }

    public void onBlockBrokenByRobot() {
        blocksBrokenCounter++;
        if (blocksBrokenCounter >= MINING_BLOCKS_PER_ENERGY_COST) {
            blocksBrokenCounter -= MINING_BLOCKS_PER_ENERGY_COST;
            consumeEnergy(MINING_ENERGY_COST);
        }
    }

    public int getStorageCapacity() {
        return switch (getSlotUpgradeTier()) {
            case 1 -> 27;
            case 2 -> 54;
            case 3 -> 81;
            default -> 9;
        };
    }

    public int getMiningTargetY() {
        return this.entityData.get(DATA_MINING_TARGET_Y);
    }

    public void setMiningTargetY(int y) {
        int clamped = Math.max(-64, Math.min(320, y));
        if (clamped != getMiningTargetY()) {
            this.hasDigAnchor = false;
        }
        this.entityData.set(DATA_MINING_TARGET_Y, clamped);
    }

    public boolean isDigChunks() {
        return this.entityData.get(DATA_DIG_CHUNKS);
    }

    public void setDigChunks(boolean digChunks) {
        if (!digChunks) {
            this.hasDigAnchor = false;
        }
        this.entityData.set(DATA_DIG_CHUNKS, digChunks);
    }

    public int getChunkRadius() {
        return this.entityData.get(DATA_CHUNK_RADIUS);
    }

    public void setChunkRadius(int r) {
        this.entityData.set(DATA_CHUNK_RADIUS, Math.max(0, Math.min(4, r)));
    }

    @Nullable
    public BlockPos getLinkedChestPos() {
        return this.entityData.get(DATA_LINKED_CHEST_POS).orElse(null);
    }

    public void setLinkedChestPos(@Nullable BlockPos pos) {
        this.entityData.set(DATA_LINKED_CHEST_POS, Optional.ofNullable(pos));
    }

    public boolean hasLinkedChest() {
        return getLinkedChestPos() != null;
    }

    public boolean hasDigAnchor() {
        return hasDigAnchor;
    }

    public int getDigAnchorChunkX() {
        return digAnchorChunkX;
    }

    public int getDigAnchorChunkZ() {
        return digAnchorChunkZ;
    }

    public void setDigAnchor(int chunkX, int chunkZ) {
        this.hasDigAnchor = true;
        this.digAnchorChunkX = chunkX;
        this.digAnchorChunkZ = chunkZ;
    }

    public void clearDigAnchor() {
        this.hasDigAnchor = false;
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
