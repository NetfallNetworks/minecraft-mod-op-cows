package com.github.netfallnetworks.mooofdoom.registry;

import com.github.netfallnetworks.mooofdoom.MooOfDoom;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, MooOfDoom.MODID);

    // Milk Projectile entity will be registered in a later task when the entity class is ready
}
