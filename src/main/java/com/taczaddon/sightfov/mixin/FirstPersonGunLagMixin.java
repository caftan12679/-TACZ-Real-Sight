package com.taczaddon.sightfov.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.resource.pojo.display.gun.AdsPath;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
   /**
    * 枪械惯性 / 拖影 / 呼吸晃动。
    *
    * <p>模拟手持武器转动时的滞后感，附带随机抖动和呼吸起伏。
 全部是纯视觉，不影响弹道。</p>
    */

@Mixin(
   targets = {"com.tacz.guns.client.event.FirstPersonRenderGunEvent"},
   remap = false
)
public abstract class FirstPersonGunLagMixin {
   private static final float YAW_TAU_1_SECONDS = 0.055F;
   private static final float YAW_TAU_2_SECONDS = 0.115F;
   private static final float PITCH_TAU_1_SECONDS = 0.06F;
   private static final float PITCH_TAU_2_SECONDS = 0.12F;
   private static final float TURN_SPEED_TAU_SECONDS = 0.135F;
   private static final float APPLIED_LEAD_TAU_SECONDS = 0.04F;
   private static final float TURN_DEADZONE_DEG_PER_SEC = 30.0F;
   private static final float TURN_FULL_DEG_PER_SEC = 380.0F;
   private static final float MAX_YAW_LAG_DEGREES = 18.0F;
   private static final float MAX_PITCH_DOWN_DEGREES = 4.0F;
   private static final float MAX_PITCH_UP_DEGREES = 3.0F;
   private static final float ARM_PIVOT_X = 0.0F;
   private static final float ARM_PIVOT_Y = -0.3F;
   private static final float ARM_PIVOT_Z = 0.1F;
   private static final float AIM_LAG_SCALE = 0.75F;
   private static final float ADS_TIME_SCALE = 0.19F;
   private static final float MAX_DELTA_SECONDS = 0.1F;
   private static final float ADS_MOUSE_LEAD_SECONDS = 0.12F;
   private static final float ADS_MOUSE_MAX_LEAD_DEGREES = 15.0F;
   private static final float ADS_MOUSE_RATE_TAU_SECONDS = 0.08F;
   private static final float SHAKE_IDLE_YAW_DEGREES = 0.18F;
   private static final float SHAKE_IDLE_PITCH_DEGREES = 0.12F;
   private static final float SHAKE_IDLE_ROLL_DEGREES = 0.08F;
   private static final float SHAKE_FIRE_YAW_DEGREES = 0.35F;
   private static final float SHAKE_FIRE_PITCH_DEGREES = 0.22F;
   private static final float SHAKE_FIRE_ROLL_DEGREES = 0.12F;
   private static final float SHAKE_IDLE_MIN_INTERVAL_SECONDS = 3.0F;
   private static final float SHAKE_IDLE_MAX_INTERVAL_SECONDS = 6.0F;
   private static final float SHAKE_FIRE_MIN_INTERVAL_SECONDS = 1.2F;
   private static final float SHAKE_FIRE_MAX_INTERVAL_SECONDS = 2.4F;
   private static final float SHAKE_IDLE_MIN_DURATION_SECONDS = 0.8F;
   private static final float SHAKE_IDLE_MAX_DURATION_SECONDS = 1.4F;
   private static final float SHAKE_FIRE_MIN_DURATION_SECONDS = 0.6F;
   private static final float SHAKE_FIRE_MAX_DURATION_SECONDS = 1.0F;
   private static final float BREATH_YAW_DEGREES = 0.08F;
   private static final float BREATH_PITCH_DEGREES = 0.05F;
   private static final float BREATH_ROLL_DEGREES = 0.03F;
   private static final Random RANDOM = new Random();
   private static boolean taczsightfov$initialized;
   private static float taczsightfov$yawStage1;
   private static float taczsightfov$yawStage2;
   private static float taczsightfov$pitchStage1;
   private static float taczsightfov$pitchStage2;
   private static float taczsightfov$lastYaw;
   private static float taczsightfov$lastPitch;
   private static float taczsightfov$smoothedTurnSpeed;
   private static float taczsightfov$smoothedYawRate;
   private static float taczsightfov$smoothedPitchRate;
   private static float taczsightfov$appliedYawLead;
   private static float taczsightfov$appliedPitchLead;
   private static long taczsightfov$lastUpdateNanos;
   private static long taczsightfov$nextShakeNanos;
   private static long taczsightfov$shakeStartNanos;
   private static long taczsightfov$shakeEndNanos;
   private static float taczsightfov$shakeYawAmplitude;
   private static float taczsightfov$shakePitchAmplitude;
   private static float taczsightfov$shakeRollAmplitude;

   @Inject(
      method = {"applyFirstPersonPositioningTransform"},
      at = {@At("RETURN")},
      remap = false
   )
   private static void taczsightfov$leadWeaponOnLook(PoseStack var0, BedrockGunModel var1, ItemStack var2, float var3, float var4, AdsPath varAdsPath, CallbackInfo varCi) {
      LocalPlayer var6 = Minecraft.getInstance().player;
      if (var6 != null) {
         float var7 = var6.getYRot();
         float var8 = var6.getXRot();
         long var9 = System.nanoTime();
         if (!taczsightfov$initialized) {
            taczsightfov$yawStage1 = var7;
            taczsightfov$yawStage2 = var7;
            taczsightfov$pitchStage1 = var8;
            taczsightfov$pitchStage2 = var8;
            taczsightfov$lastYaw = var7;
            taczsightfov$lastPitch = var8;
            taczsightfov$lastUpdateNanos = var9;
            taczsightfov$nextShakeNanos = var9 + secondsToNanos(randomBetween(3.0F, 6.0F));
            taczsightfov$initialized = true;
         } else {
            float var11 = Math.min(0.1F, (float)(var9 - taczsightfov$lastUpdateNanos) / 1.0E9F);
            taczsightfov$lastUpdateNanos = var9;
            if (!(var11 <= 0.0F)) {
               float var12 = taczsightfov$wrapDegrees(var7 - taczsightfov$lastYaw);
               float var13 = var8 - taczsightfov$lastPitch;
               taczsightfov$lastYaw = var7;
               taczsightfov$lastPitch = var8;
               float var14 = Math.max(0.0F, Math.min(1.0F, var3));
               float var15 = 1.0F - 0.81F * var14;
               float var16 = var12 / var11;
               float var17 = var13 / var11;
               float var18 = 1.0F - (float)Math.exp((double)(-var11 / 0.08F));
               taczsightfov$smoothedYawRate += (var16 - taczsightfov$smoothedYawRate) * var18;
               taczsightfov$smoothedPitchRate += (var17 - taczsightfov$smoothedPitchRate) * var18;
               float var19 = Math.max(Math.abs(var16), Math.abs(var17));
               float var20 = 1.0F - (float)Math.exp((double)(-var11 / (0.135F * var15)));
               taczsightfov$smoothedTurnSpeed += (var19 - taczsightfov$smoothedTurnSpeed) * var20;
               float var21 = taczsightfov$clamp((taczsightfov$smoothedTurnSpeed - 30.0F) / 350.0F, 0.0F, 1.0F);
               float var22 = var21 * var21 * (3.0F - 2.0F * var21);
               float var23 = 1.0F - (float)Math.exp((double)(-var11 / (0.055F * var15)));
               float var24 = 1.0F - (float)Math.exp((double)(-var11 / (0.115F * var15)));
               float var25 = 1.0F - (float)Math.exp((double)(-var11 / (0.06F * var15)));
               float var26 = 1.0F - (float)Math.exp((double)(-var11 / (0.12F * var15)));
               taczsightfov$yawStage1 += taczsightfov$wrapDegrees(var7 - taczsightfov$yawStage1) * var23;
               taczsightfov$yawStage2 += taczsightfov$wrapDegrees(taczsightfov$yawStage1 - taczsightfov$yawStage2) * var24;
               taczsightfov$pitchStage1 += (var8 - taczsightfov$pitchStage1) * var25;
               taczsightfov$pitchStage2 += (taczsightfov$pitchStage1 - taczsightfov$pitchStage2) * var26;
               float var27 = taczsightfov$clamp(taczsightfov$wrapDegrees(var7 - taczsightfov$yawStage2), -18.0F, 18.0F) * var22;
               float var28 = taczsightfov$clamp(var8 - taczsightfov$pitchStage2, -3.0F, 4.0F) * var22;
               float var29 = taczsightfov$clamp(taczsightfov$smoothedYawRate * 0.12F, -15.0F, 15.0F) * var14;
               float var30 = taczsightfov$clamp(taczsightfov$smoothedPitchRate * 0.12F, -3.0F, 4.0F) * var14;
               float var31 = taczsightfov$clamp(var27 + var29, -18.0F, 18.0F);
               float var32 = taczsightfov$clamp(var28 + var30, -3.0F, 4.0F);
               float var33 = Math.max(0.025F, 0.04F * var15);
               float var34 = 1.0F - (float)Math.exp((double)(-var11 / var33));
               taczsightfov$appliedYawLead += (var31 - taczsightfov$appliedYawLead) * var34;
               taczsightfov$appliedPitchLead += (var32 - taczsightfov$appliedPitchLead) * var34;
               float var35 = 1.0F - 0.25F * var14;
               float var36 = taczsightfov$appliedYawLead * var35;
               float var37 = taczsightfov$appliedPitchLead * var35;
               Matrix4f var38 = (new Matrix4f(var0.last().pose())).invert();
               Vector3f var39 = var38.transformPosition(new Vector3f(0.0F, -0.3F, 0.1F));
               var0.translate(var39.x, var39.y, var39.z);
               if (Math.abs(var36) > 0.01F) {
                  var0.mulPose((new Quaternionf()).rotationY((float)Math.toRadians((double)var36)));
               }

               if (Math.abs(var37) > 0.01F) {
                  var0.mulPose((new Quaternionf()).rotationX((float)Math.toRadians((double)var37)));
               }

               double var40 = (double)var9 * 1.0E-9D;
               float var43 = 0.08F * var14 * (float)Math.sin(var40 * 0.9D + 0.4D);
               float var44 = 0.05F * var14 * (float)Math.sin(var40 * 0.7D + 1.3D);
               float var45 = 0.03F * var14 * (float)Math.sin(var40 * 0.55D + 2.2D);
               if (Math.abs(var43) > 0.001F) {
                  var0.mulPose((new Quaternionf()).rotationY((float)Math.toRadians((double)var43)));
               }

               if (Math.abs(var44) > 0.001F) {
                  var0.mulPose((new Quaternionf()).rotationX((float)Math.toRadians((double)var44)));
               }

               if (Math.abs(var45) > 0.001F) {
                  var0.mulPose((new Quaternionf()).rotationZ((float)Math.toRadians((double)var45)));
               }

               applyRandomShake(var0, var9, var14);
               var0.translate(-var39.x, -var39.y, -var39.z);
            }
         }
      }
   }

   private static void applyRandomShake(PoseStack var0, long var1, float var3) {
      boolean var4 = Minecraft.getInstance().options.keyAttack.isDown();
      if (var1 >= taczsightfov$nextShakeNanos) {
         boolean var5 = var3 > 0.25F || var4;
         if (var5) {
            startShake(var1, var4);
            taczsightfov$nextShakeNanos = var1 + secondsToNanos(var4 ? randomBetween(1.2F, 2.4F) : randomBetween(3.0F, 6.0F));
         } else {
            taczsightfov$nextShakeNanos = var1 + secondsToNanos(randomBetween(3.0F, 6.0F));
         }
      }

      if (var1 < taczsightfov$shakeEndNanos) {
         float var12 = Math.max(1.0F, (float)(taczsightfov$shakeEndNanos - taczsightfov$shakeStartNanos));
         float var6 = (float)(var1 - taczsightfov$shakeStartNanos) / var12;
         float var7 = (float)(Math.sin(Math.PI * (double)Math.max(0.0F, Math.min(1.0F, var6))) * Math.sin(Math.PI * (double)Math.max(0.0F, Math.min(1.0F, var6))));
         float var8 = taczsightfov$shakeYawAmplitude * var7 * (float)Math.sin((double)var6 * Math.PI * 4.0D);
         float var9 = taczsightfov$shakePitchAmplitude * var7 * (float)Math.sin((double)var6 * Math.PI * 4.0D + 1.2D);
         float var10 = taczsightfov$shakeRollAmplitude * var7 * (float)Math.sin((double)var6 * Math.PI * 4.0D + 2.4D);
         float var11 = 0.25F + 0.75F * var3;
         if (Math.abs(var8) > 0.001F) {
            var0.mulPose((new Quaternionf()).rotationY((float)Math.toRadians((double)(var8 * var11))));
         }

         if (Math.abs(var9) > 0.001F) {
            var0.mulPose((new Quaternionf()).rotationX((float)Math.toRadians((double)(var9 * var11))));
         }

         if (Math.abs(var10) > 0.001F) {
            var0.mulPose((new Quaternionf()).rotationZ((float)Math.toRadians((double)(var10 * var11))));
         }

      }
   }

   private static void startShake(long var0, boolean var2) {
      float var3 = var2 ? randomBetween(0.6F, 1.0F) : randomBetween(0.8F, 1.4F);
      taczsightfov$shakeStartNanos = var0;
      taczsightfov$shakeEndNanos = var0 + secondsToNanos(var3);
      float var4 = var2 ? 0.35F : 0.18F;
      float var5 = var2 ? 0.22F : 0.12F;
      float var6 = var2 ? 0.12F : 0.08F;
      taczsightfov$shakeYawAmplitude = randomSigned(var4 * randomBetween(0.7F, 1.3F));
      taczsightfov$shakePitchAmplitude = randomSigned(var5 * randomBetween(0.7F, 1.3F));
      taczsightfov$shakeRollAmplitude = randomSigned(var6 * randomBetween(0.7F, 1.3F));
   }

   private static float randomBetween(float var0, float var1) {
      return var0 + RANDOM.nextFloat() * (var1 - var0);
   }

   private static float randomSigned(float var0) {
      return RANDOM.nextBoolean() ? var0 : -var0;
   }

   private static long secondsToNanos(float var0) {
      return (long)(var0 * 1.0E9F);
   }

   private static float taczsightfov$wrapDegrees(float var0) {
      float var1 = var0 % 360.0F;
      if (var1 >= 180.0F) {
         var1 -= 360.0F;
      }

      if (var1 < -180.0F) {
         var1 += 360.0F;
      }

      return var1;
   }

   private static float taczsightfov$clamp(float var0, float var1, float var2) {
      return Math.max(var1, Math.min(var2, var0));
   }
}
