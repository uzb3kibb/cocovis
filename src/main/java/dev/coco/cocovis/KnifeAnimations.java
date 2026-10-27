package dev.coco.cocovis;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Свои анимации ножа от первого лица: покачивание в руке, выхват, взмах и осмотр.
 * Формулы совпадают с теми, по которым делался предпросмотр. Работает только для правой основной руки.
 */
public final class KnifeAnimations {
    /** Направление вдоль клинка в системе камеры при обычном хвате. */
    private static final Vector3f BLADE_AXIS = new Vector3f(-0.93f, 0.33f, -0.16f).normalize();

    private KnifeAnimations() {}

    private static boolean animate(HumanoidArm arm) {
        if (arm != HumanoidArm.RIGHT) return false;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || !Knives.isActive() || !DiamondConfig.get().knifeAnimations) return false;
        if (p.getMainArm() != HumanoidArm.RIGHT) return false;
        return Knives.isCustomSword(p.getMainHandItem());
    }

    /** Хват: вызывается после того, как игра поставила предмет в точку руки (applyItemArmTransform). */
    public static void hold(PoseStack ps, HumanoidArm arm, float equip) {
        if (!animate(arm)) return;

        // лёгкое «дыхание» ножа в руке
        double t = System.nanoTime() / 1.0e9;
        ps.translate(0f, (float) (Math.sin(t * 1.7) * 0.0035), 0f);
        rotZ(ps, (float) (Math.sin(t * 1.1) * 0.8));

        // выхват: нож подбрасывается снизу
        if (equip > 0f) {
            ps.translate(0f, 0.04f * equip, 0f);
            rotX(ps, -48f * equip);
            rotZ(ps, 26f * equip);
            rotY(ps, -14f * equip);
        }

        float ip = Knives.inspectProgress();
        if (ip >= 0f) inspect(ps, ip);
    }

    /** Осмотр: нож поднимается лезвием вверх, прокручивается вокруг своей оси и возвращается. */
    private static void inspect(PoseStack ps, float t) {
        float up = smooth(t / 0.16f) * (1f - smooth((t - 0.84f) / 0.16f));
        float phase = smooth((t - 0.16f) / 0.60f);
        float spin = 360f * phase;
        float wob = (float) Math.sin(phase * Math.PI);
        ps.translate(-0.30f * up, 0.17f * up, 0.10f * up);
        rotZ(ps, -72f * up);
        rotX(ps, -14f * wob);
        ps.mulPose(new Quaternionf().rotationAxis((float) Math.toRadians(spin), BLADE_AXIS.x, BLADE_AXIS.y, BLADE_AXIS.z));
    }

    /** Взмах: замах вправо, рубящий удар влево, возврат. Возвращает true, если стандартную анимацию нужно отменить. */
    public static boolean attack(PoseStack ps, HumanoidArm arm, float s) {
        if (!animate(arm) || s <= 0f) return false;
        Knives.cancelInspect();
        float wind = smooth(s / 0.28f);
        float strike = smooth((s - 0.28f) / 0.30f);
        float back = smooth((s - 0.58f) / 0.42f);
        float yaw = -34f * wind + 98f * strike - 64f * back;
        float peak = (float) Math.sin(Math.PI * clamp((s - 0.12f) / 0.62f));
        ps.translate(-0.12f * peak, 0.02f * peak, -0.10f * peak);
        rotY(ps, yaw);
        rotZ(ps, -28f * peak);
        rotX(ps, 10f * peak);
        return true;
    }

    private static float clamp(float x) {
        return Math.max(0f, Math.min(1f, x));
    }

    private static float smooth(float x) {
        x = clamp(x);
        return x * x * (3f - 2f * x);
    }

    private static void rotX(PoseStack ps, float deg) {
        ps.mulPose(new Quaternionf().rotationX((float) Math.toRadians(deg)));
    }

    private static void rotY(PoseStack ps, float deg) {
        ps.mulPose(new Quaternionf().rotationY((float) Math.toRadians(deg)));
    }

    private static void rotZ(PoseStack ps, float deg) {
        ps.mulPose(new Quaternionf().rotationZ((float) Math.toRadians(deg)));
    }
}
