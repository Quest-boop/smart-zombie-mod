package com.smartzombie.init;

import com.smartzombie.SmartZombieMod;
import com.smartzombie.entity.SmartZombieEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SmartZombieMod.MOD_ID);

    public static final RegistryObject<EntityType<SmartZombieEntity>> SMART_ZOMBIE =
            ENTITY_TYPES.register("smart_zombie",
                    () -> EntityType.Builder.<SmartZombieEntity>of(SmartZombieEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.95f)
                            .clientTrackingRange(8)
                            .build("smart_zombie"));
}
