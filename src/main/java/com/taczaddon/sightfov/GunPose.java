package com.taczaddon.sightfov;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.bedrock.BedrockCube;
import com.tacz.guns.client.model.bedrock.BedrockCubeBox;
import com.tacz.guns.client.model.bedrock.BedrockCubePerFace;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.joml.Matrix4f;

/**
 * 枪械模型上的锚点计算。
 *
 * <p>两类信息：</p>
 * <ul>
 *   <li>{@link #findStockPivot} —— 枪托支点，后坐力和摆动要绕它转才自然；</li>
 *   <li>{@link #rearCameraZ} —— 模型在视线方向最靠后的位置，用来判断枪会不会怼进镜头。</li>
 * </ul>
 */
public final class GunPose {
   /** 枪托节点的候选名，按顺序尝试，用第一个找到的 */
   private static final String[] STOCK_NODES = new String[]{"stock_pos", "stock", "stock_default", "grip_pos"};

   /** rearPoint 的缓存：模型是常驻对象，没必要每帧重算 */
   private static BedrockGunModel cachedModel;
   private static float[] cachedRearPoint;

   private GunPose() {
   }

   /**
    * 在模型里找「枪托支点」的世界坐标。
    *
    * <p>按 {@link #STOCK_NODES} 逐个试，取第一个存在的节点；顺着父链收集到根的路径，
    * 在独立 PoseStack 上依次施加变换，最后从矩阵里取出平移分量。</p>
    *
    * @return {@code {x, y, z}}；找不到节点或算出非有限值时返回 {@code null}
    */
   public static float[] findStockPivot(PoseStack var0, BedrockGunModel var1) {
      String[] var2 = STOCK_NODES;
      int var3 = var2.length;

      for (int var4 = 0; var4 < var3; var4++) {
         String var5 = var2[var4];
         BedrockPart var6 = var1.getNode(var5);
         if (var6 == null) {
            continue;
         }

         // 从当前节点往上收集到根，插到头部就得到「根 → 当前」的施加顺序
         List<BedrockPart> var7 = new ArrayList<>();
         BedrockPart var8 = var6;

         while (var8 != null) {
            var7.add(0, var8);
            var8 = var8.getParent();
         }

         // 独立 PoseStack，以当前渲染矩阵为起点，只叠加这条链上的变换
         PoseStack var9 = new PoseStack();
         var9.last().pose().set(var0.last().pose());

         for (BedrockPart var11 : var7) {
            var11.translateAndRotateAndScale(var9);
         }

         float var10 = var9.last().pose().m30();
         float var11x = var9.last().pose().m31();
         float var12 = var9.last().pose().m32();

         if (Float.isFinite(var10) && Float.isFinite(var11x) && Float.isFinite(var12)) {
            return new float[]{var10, var11x, var12};
         }
      }

      return null;
   }

   /**
    * 模型最靠后的一点在相机空间里的 Z 值。
    *
    * <p>取包围盒八个角分别乘当前矩阵，投影后取最小 Z。结果小于 0 说明枪模已经怼到镜头后面。</p>
    *
    * @return 相机空间 Z 值；拿不到根节点时返回 {@link Float#NaN}
    */
   public static float rearCameraZ(PoseStack var0, BedrockGunModel var1) {
      float[] var2 = rearPoint(var1);
      if (var2 == null) {
         return Float.NaN;
      }

      Matrix4f var3 = var0.last().pose();
      // 模型单位是 1/16 方块（Bedrock 像素），换算成方块要乘 0.0625
      float var4 = 0.0625F;

      float var5 = var3.m02() * var2[0] * var4
                 + var3.m12() * var2[1] * var4
                 + var3.m22() * var2[2] * var4
                 + var3.m32();
      return var5;
   }

   /** 名字看着像手臂/手部骨骼就跳过，这些部件不该进枪模包围盒 */
   private static boolean isArmPart(String var0) {
      if (var0 == null) {
         return false;
      }

      String var1 = var0.toLowerCase(Locale.ROOT);
      if (var1.equals("hand")
          || var1.equals("arm")
          || var1.startsWith("hand_")
          || var1.endsWith("_hand")
          || var1.endsWith("_arm")
          || var1.contains("thirdperson_hand")) {
         return true;
      }

      if ((var1.startsWith("left") || var1.startsWith("right"))
          && (var1.contains("hand") || var1.contains("arm"))) {
         return true;
      }

      return false;
   }

   /**
    * 算出并缓存模型最靠后的一点。
    *
    * <p>初值取 {@code {0, 0, -Float.MAX_VALUE}}，遍历时不断用更小的 Z 覆盖。</p>
    */
   private static float[] rearPoint(BedrockGunModel var0) {
      if (var0 == cachedModel && cachedRearPoint != null) {
         return cachedRearPoint;
      }

      BedrockPart var1 = var0.getRootNode();
      if (var1 == null) {
         cachedModel = var0;
         cachedRearPoint = null;
         return null;
      }

      float[] var2 = new float[]{0.0F, 0.0F, -Float.MAX_VALUE};
      PoseStack var3 = new PoseStack();
      collectRear(var1, var3, var2);

      cachedModel = var0;
      cachedRearPoint = var2;
      return var2;
   }

   /**
    * 递归遍历骨骼树，把最靠后的顶点累进 {@code var2}。
    *
    * <p>对每个 cube 取包围盒八个角，乘当前矩阵投影到相机空间；
    * 只有 Z 更大时才更新记录。</p>
    */
   private static void collectRear(BedrockPart var0, PoseStack var1, float[] var2) {
      if (isArmPart(var0.name)) {
         return;
      }

      var1.pushPose();
      try {
         var0.translateAndRotateAndScale(var1);
         Matrix4f var3 = var1.last().pose();

         for (BedrockCube var5 : var0.cubes) {
            float var6;
            float var7;
            float var8;
            float var9;
            float var10;
            float var11;

            if (var5 instanceof BedrockCubeBox) {
               BedrockCubeBox var13 = (BedrockCubeBox) var5;
               var6 = var13.minX;
               var7 = var13.minY;
               var8 = var13.minZ;
               var9 = var13.maxX;
               var10 = var13.maxY;
               var11 = var13.maxZ;
            } else if (var5 instanceof BedrockCubePerFace) {
               BedrockCubePerFace var12 = (BedrockCubePerFace) var5;
               var6 = var12.minX;
               var7 = var12.minY;
               var8 = var12.minZ;
               var9 = var12.maxX;
               var10 = var12.maxY;
               var11 = var12.maxZ;
            } else {
               continue;
            }

            // 八个角：用位运算从 min/max 里挑
            for (int var13x = 0; var13x < 8; var13x++) {
               float var14 = (var13x & 1) == 0 ? var6 : var9;
               float var15 = (var13x & 2) == 0 ? var7 : var10;
               float var16 = (var13x & 4) == 0 ? var8 : var11;

               float var17 = var3.m02() * var14
                           + var3.m12() * var15
                           + var3.m22() * var16
                           + var3.m32();

               if (var17 > var2[2]) {
                  var2[0] = var3.m00() * var14
                          + var3.m01() * var15
                          + var3.m02() * var16
                          + var3.m30();
                  var2[1] = var3.m10() * var14
                          + var3.m11() * var15
                          + var3.m12() * var16
                          + var3.m31();
                  var2[2] = var17;
               }
            }
         }

         for (BedrockPart var5x : var0.children) {
            collectRear(var5x, var1, var2);
         }
      } finally {
         var1.popPose();
      }
   }
}
