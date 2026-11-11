package com.taczaddon.sightfov;

import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.client.other.KeepingItemRenderer;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.event.CameraSetupEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent.ComputeFov;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
   /**
    * 第一人称视野处理器。
    *
    * <p>非光学瞄具开镜时把世界 FOV 锁在腰射值，松开后平滑回弹。
 这样红点、全息、机瞄开镜时画面不会莫名收窄。</p>
    */

@EventBusSubscriber(
   modid = "tacz_sight_fov",
   value = {Dist.CLIENT},
   bus = Bus.FORGE
)
public final class FirstPersonFovHandler {
   private static final float RELEASE_HOLD_SECONDS = 0.25F;
   private static final float RELEASE_DECAY_SECONDS = 0.18F;
   private static ResourceLocation hipGunId;
   private static float hipFov = Float.NaN;
   private static float correction;
   private static long releaseStartNanos;

   private FirstPersonFovHandler() {
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onComputeFov(ComputeFov var0) {
      if (!var0.usedConfiguredFov()) {
         Entity var1 = var0.getCamera().getEntity();
         if (var1 instanceof LocalPlayer) {
            LocalPlayer var2 = (LocalPlayer)var1;
            ItemStack var3 = KeepingItemRenderer.getRenderer().getCurrentItem();
            IGun var4 = IGun.getIGunOrNull(var3);
            if (var4 != null) {
               float var5 = IClientPlayerGunOperator.fromLocalPlayer(var2).getClientAimingProgress(1.0F);
               if (OpticClassifier.isPistol(var4, var3)) {
                  if (var5 > 0.0F) {
                     float var13 = Minecraft.getInstance().options.getBackgroundOpacity(70.0F);
                     CameraSetupEvent.ITEM_MODEL_FOV_DYNAMICS.update(var13);
                     var0.setFOV((double)var13);
                  }

               } else if (OpticClassifier.isMagnifyingScope(var4, var3)) {
                  correction = 0.0F;
                  releaseStartNanos = 0L;
               } else {
                  ResourceLocation var6 = var4.getGunId(var3);
                  float var7 = (float)var0.getFOV();
                  boolean var8 = var5 > 0.0F;
                  if (var8) {
                     correction = 1.0F;
                     releaseStartNanos = 0L;
                  } else {
                     if (!(correction > 0.0F)) {
                        hipGunId = var6;
                        hipFov = var7;
                        return;
                     }

                     long var9 = System.nanoTime();
                     if (releaseStartNanos == 0L) {
                        releaseStartNanos = var9;
                     }

                     float var11 = (float)(var9 - releaseStartNanos) / 1.0E9F;
                     if (var11 > 0.25F) {
                        float var12 = Math.min(1.0F, (var11 - 0.25F) / 0.18F);
                        correction = 1.0F - var12 * var12 * (3.0F - 2.0F * var12);
                        if (var12 >= 1.0F) {
                           correction = 0.0F;
                           releaseStartNanos = 0L;
                           hipGunId = var6;
                           hipFov = var7;
                           return;
                        }
                     } else {
                        correction = 1.0F;
                     }
                  }

                  if (hipGunId != null && hipGunId.equals(var6) && Float.isFinite(hipFov) && !(hipFov <= 0.0F)) {
                     float var14 = var7 + (hipFov - var7) * correction;
                     CameraSetupEvent.ITEM_MODEL_FOV_DYNAMICS.update(var14);
                     var0.setFOV((double)var14);
                  }
               }
            }
         }
      }
   }
}
