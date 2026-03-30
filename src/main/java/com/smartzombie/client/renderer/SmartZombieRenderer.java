package com.smartzombie.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.smartzombie.SmartZombieMod;
import com.smartzombie.entity.SmartZombieEntity;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer for Smart Zombie.
 * Uses the vanilla zombie model but with a custom texture
 * that makes it look like a half-human, half-zombie hybrid.
 * Falls back gracefully to zombie texture if custom one is missing.
 */
public class SmartZombieRenderer extends HumanoidMobRenderer<SmartZombieEntity, ZombieModel<SmartZombieEntity>> {

    // Primary custom texture - replace with your actual texture file
    private static final ResourceLocation SMART_ZOMBIE_TEXTURE =
            new ResourceLocation(SmartZombieMod.MOD_ID, "textures/entity/smart_zombie.png");

    // Fallback to vanilla zombie texture
    private static final ResourceLocation FALLBACK_TEXTURE =
            new ResourceLocation("minecraft", "textures/entity/zombie/zombie.png");

    public SmartZombieRenderer(EntityRendererProvider.Context context) {
        super(context,
              new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE)),
              0.5f);

        // Add armor and held item layers so equipment renders correctly
        this.addLayer(new HumanoidArmorLayer<>(this,
                new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
                new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)),
                context.getModelSet()));

        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(SmartZombieEntity entity) {
        // Try to use the custom texture; Minecraft will gracefully fall back
        // if it doesn't exist (shows magenta/black missing texture indicator).
        // See src/main/resources/assets/smartzombie/textures/entity/ for where to place your PNG.
        return SMART_ZOMBIE_TEXTURE;
    }

    @Override
    protected void scale(SmartZombieEntity entity, PoseStack poseStack, float partialTick) {
        // Slightly taller than a normal zombie to feel more "human"
        poseStack.scale(1.0f, 1.02f, 1.0f);
        super.scale(entity, poseStack, partialTick);
    }
}
