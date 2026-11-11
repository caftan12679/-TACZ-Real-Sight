package com.taczaddon.sightfov.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.resource.pojo.display.gun.AdsPath;
import com.taczaddon.sightfov.OpticClassifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
   /**
    * 枪模位置微调。
    *
    * <p>腰射时把枪压低一点，开镜时向玩家拉近，
 让非放大瞄具的持枪视角更接近贴腮瞄准。</p>
    */

@Mixin(
   targets = {"com.tacz.guns.client.event.FirstPersonRenderGunEvent"},
   remap = false
)
public abstract class FirstPersonRenderGunEventMixin {
   private static final float HIP_AWAY = 0.12F;
   private static final float ADS_TOWARD = 0.42F;
   private static final float HIP_DROP_Y = 0.14F;

   @Inject(
      method = {"applyFirstPersonPositioningTransform"},
      at = {@At("RETURN")},
      remap = false
   )
   private static void taczsightfov$pullOpticTowardPlayer(PoseStack var0, BedrockGunModel var1, ItemStack var2, float var3, float var4, AdsPath varAdsPath, CallbackInfo varCi) {
      IGun var6 = IGun.getIGunOrNull(var2);
      if (var6 != null) {
         float var7 = Math.max(0.0F, Math.min(1.0F, var3));
         Matrix4f var8 = var0.last().pose();
         float var9 = 0.14F * (1.0F - var7);
         if (var9 > 5.0E-4F) {
            var8.m31(var8.m31() - var9);
         }

         if (OpticClassifier.isPistol(var6, var2)) {
            String var12 = var6.getGunId(var2) == null ? "" : var6.getGunId(var2).getPath();
            float var11;
            if (var12.contains("taurus943")) {
               var11 = -0.24F;
            } else if (var12.contains("rhino357")) {
               var11 = -0.12F;
            } else if (var12.contains("taurus500")) {
               var11 = 0.32F;
            } else {
               var11 = 0.0F;
            }

            if (var11 != 0.0F) {
               var8.m32(var8.m32() + var11 * var7);
            }

         } else if (OpticClassifier.hasOptic(var6, var2)) {
            if (!OpticClassifier.isMagnifyingScope(var6, var2)) {
               float var10 = -0.12F + 0.53999996F * var7;
               var8.m32(var8.m32() + var10);
            }
         }
      }
   }
}
