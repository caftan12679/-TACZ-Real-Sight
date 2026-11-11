package com.taczaddon.sightfov.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
   /**
    * 开镜阻尼参数。
    *
    * <p>改写 TaCZ 的 SecondOrderDynamics 构造参数，让开镜过渡更沉、更有重量感。</p>
    */

@Mixin(
   targets = {"com.tacz.guns.client.event.FirstPersonRenderGunEvent"},
   remap = false
)
public abstract class AimDynamicsMixin {
   @ModifyArg(
      method = {"<clinit>"},
      at = @At(
   value = "INVOKE",
   target = "Lcom/tacz/guns/util/math/SecondOrderDynamics;<init>(FFFF)V",
   ordinal = 0
),
      index = 0,
      remap = false
   )
   private static float taczsightfov$aimFrequency(float var0) {
      return 0.7F;
   }

   @ModifyArg(
      method = {"<clinit>"},
      at = @At(
   value = "INVOKE",
   target = "Lcom/tacz/guns/util/math/SecondOrderDynamics;<init>(FFFF)V",
   ordinal = 0
),
      index = 1,
      remap = false
   )
   private static float taczsightfov$aimDamping(float var0) {
      return 1.5F;
   }

   @ModifyArg(
      method = {"<clinit>"},
      at = @At(
   value = "INVOKE",
   target = "Lcom/tacz/guns/util/math/SecondOrderDynamics;<init>(FFFF)V",
   ordinal = 0
),
      index = 2,
      remap = false
   )
   private static float taczsightfov$aimResponse(float var0) {
      return 0.25F;
   }
}
