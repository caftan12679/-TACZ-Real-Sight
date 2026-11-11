package com.taczaddon.sightfov.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
   /**
    * 开镜进度曲线。
    *
    * <p>放慢开镜速度并重映射进度曲线，前段快后段缓，手感更接近真实抬枪。</p>
    */

@Mixin(
   targets = {"com.tacz.guns.client.gameplay.LocalPlayerAim"},
   remap = false
)
public abstract class LocalPlayerAimMixin {
   private static final float AIM_SPEED_MULTIPLIER = 2.5F;
   private static final float FAST_PHASE_END = 0.5F;
   private static final float FAST_PHASE_PROGRESS = 0.8F;
   private static final float TRANSITION_START = 0.4F;
   private static final float TRANSITION_END = 0.6F;

   @ModifyVariable(
      method = {"aimProgressCalculate"},
      at = @At("HEAD"),
      argsOnly = true,
      index = 1,
      remap = false
   )
   private float taczsightfov$slowAimProgress(float var1) {
      return var1 / 2.5F;
   }

   @Inject(
      method = {"getClientAimingProgress"},
      at = {@At("RETURN")},
      cancellable = true,
      remap = false
   )
   private void taczsightfov$curveAimProgress(float var1, CallbackInfoReturnable var2) {
      float var3 = Math.max(0.0F, Math.min(1.0F, var2.getReturnValueF()));
      float var4 = 0.8F * (var3 / 0.5F);
      float var5 = Math.max(0.0F, Math.min(1.0F, (var3 - 0.5F) / 0.5F));
      float var6 = var5 * var5 * (3.0F - 2.0F * var5);
      float var7 = 0.8F + 0.19999999F * var6;
      float var8 = Math.max(0.0F, Math.min(1.0F, (var3 - 0.4F) / 0.20000002F));
      float var9 = var8 * var8 * (3.0F - 2.0F * var8);
      float var10 = var4 * (1.0F - var9) + var7 * var9;
      var2.setReturnValue(Math.max(0.0F, Math.min(1.0F, var10)));
   }
}
