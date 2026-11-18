package com.enrichmentsolution.mod.registry;

import com.enrichmentsolution.mod.EnrichmentSolutionMod;
import com.enrichmentsolution.mod.common.entity.RobotEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, EnrichmentSolutionMod.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<RobotEntity>> ROBOT =
            ENTITY_TYPES.register("robot", () -> EntityType.Builder.of(RobotEntity::new, MobCategory.MISC)
                    .sized(0.95F, 2.75F)
                    .clientTrackingRange(8)
                    .build("robot"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }

    private ModEntities() {}
}
