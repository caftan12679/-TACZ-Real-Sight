package com.taczaddon.sightfov;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.nbt.AttachmentItemDataAccessor;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.resource.index.CommonGunIndex;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
   /**
    * 瞄具分类工具。
    *
    * <p>根据瞄具的 JSON 索引判断它属于哪一类（手枪 / 有瞄具 / 光学镜），
 供 FOV 与模型位移逻辑决定该怎么处理。</p>
    */

public final class OpticClassifier {
   private static final float MAX_SIGHT_ZOOM = 2.0F;

   private OpticClassifier() {
   }

   public static boolean isPistol(IGun var0, ItemStack var1) {
      ResourceLocation var2 = var0.getGunId(var1);
      return var2 == null ? false : TimelessAPI.getCommonGunIndex(var2).map(CommonGunIndex::getType).map((var0x) -> "pistol".equalsIgnoreCase(var0x)).orElse(false);
   }

   public static boolean hasOptic(IGun var0, ItemStack var1) {
      return index(var0, var1) != null;
   }

   public static boolean isSightOptic(IGun var0, ItemStack var1) {
      ClientAttachmentIndex var2 = index(var0, var1);
      if (var2 == null) {
         return false;
      } else if (var2.isSight()) {
         return true;
      } else {
         float var3 = currentZoom(var0, var1, var2);
         return Float.isFinite(var3) && var3 <= 2.0F;
      }
   }

   /**
    * 判断当前瞄具是不是光学镜。
    *
    * <p>只看瞄具本身，不看当前拧在哪一档 —— 原先读的是当前档位倍率
    * (currentZoom &gt; 2.0)，于是可变倍率瞄具转到低倍档时会被误判成非放大镜：
    * 世界 FOV 按 1x 处理、枪模退回腰射位置，而实际成像仍是高倍视野。
    * 这就是 LPVO 这类光学镜拧回 1x 后模型 FOV 还停在高倍样子的原因。</p>
    *
    * <p>isScope() 是瞄具的固有属性，与档位无关，用它判断才稳定。</p>
    */
   public static boolean isMagnifyingScope(IGun var0, ItemStack var1) {
      ClientAttachmentIndex var2 = index(var0, var1);
      return var2 != null && var2.isScope();
   }

   private static ClientAttachmentIndex index(IGun var0, ItemStack var1) {
      ResourceLocation var2 = var0.getAttachmentId(var1, AttachmentType.SCOPE);
      if (DefaultAssets.isEmptyAttachmentId(var2)) {
         var2 = var0.getBuiltInAttachmentId(var1, AttachmentType.SCOPE);
      }

      return DefaultAssets.isEmptyAttachmentId(var2) ? null : TimelessAPI.getClientAttachmentIndex(var2).orElse(null);
   }

   private static float currentZoom(IGun var0, ItemStack var1, ClientAttachmentIndex var2) {
      float[] var3 = var2.getZoom();
      if (var3 != null && var3.length != 0) {
         CompoundTag var4 = var0.getAttachmentTag(var1, AttachmentType.SCOPE);
         int var5 = var4 == null ? 0 : AttachmentItemDataAccessor.getZoomNumberFromTag(var4);
         return var3[Math.floorMod(var5, var3.length)];
      } else {
         return Float.NaN;
      }
   }
}
