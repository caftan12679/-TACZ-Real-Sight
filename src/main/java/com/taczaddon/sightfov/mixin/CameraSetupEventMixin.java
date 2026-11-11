package com.taczaddon.sightfov.mixin;

import com.tacz.guns.api.item.IGun;
import com.taczaddon.sightfov.OpticClassifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
   /**
    * 拦截倍率取值。
    *
    * <p>手枪和 1 倍瞄具直接返回 1.0，不参与世界 FOV 缩放。</p>
    */

@Mixin(
   targets = {"com.tacz.guns.client.event.CameraSetupEvent"},
   remap = false
)
public abstract class CameraSetupEventMixin {
   @Redirect(
      method = {"applyScopeMagnification"},
      at = @At(
   value = "INVOKE",
   target = "Lcom/tacz/guns/api/item/IGun;getAimingZoom(Lnet/minecraft/world/item/ItemStack;)F"
),
      remap = false
   )
   private static float taczsightfov$resolveAimingZoom(IGun var0, ItemStack var1) {
      if (OpticClassifier.isPistol(var0, var1)) {
         return 1.0F;
      } else {
         return OpticClassifier.isMagnifyingScope(var0, var1) ? var0.getAimingZoom(var1) : 1.0F;
      }
   }
}
