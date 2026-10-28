package dev.coco.cocovis;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Свои анимации ножа от первого лица: покачивание в руке, выхват, взмах и осмотр.
 * Формулы совпадают с теми, по которым делался предпросмотр. Работает только для правой основной руки.
 */
public final class KnifeAnimations {
    /** Направление вдоль клинка в системе камеры при обычном хвате. */
    private static final Vector3f BLADE_AXIS = new Vector3f(-0.93f, 0.33f, -0.16f).normalize();

    private static boolean logged = false;

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
        if (!logged) {
            logged = true;
            System.out.println("[cocovis] хук анимаций ножа работает");
        }

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

    /**
     * Осмотр по кадрам CS2 (5,1 с): быстрый подброс лезвием вверх, показ лицом,
     * две полные прокрутки вокруг клинка с наклоном влево и возврат в обычный хват.
     */
    private static void inspect(PoseStack ps, float t) {
        float s = t * 5.1f;
        float up = smooth(s / 0.30f) * (1f - smooth((s - 4.80f) / 0.30f));
        float flick = 90f * (float) Math.sin(Math.PI * smooth(s / 0.5f));
        float flick2 = 90f * (float) Math.sin(Math.PI * smooth((s - 4.6f) / 0.5f));
        float spin = flick + 360f * smooth((s - 1.5f) / 0.4f) + 360f * smooth((s - 3.2f) / 0.4f) + flick2;
        float lean = 4f * up + 10f * bump(s, 1.9f, 3.2f) + 6f * bump(s, 3.7f, 4.6f);
        ps.translate(-0.30f * up, 0.17f * up, 0.10f * up);
        rotZ(ps, -72f * up + lean);
        rotX(ps, -10f * up);
        mul(ps, new Quaternionf().rotationAxis((float) Math.toRadians(spin), BLADE_AXIS.x, BLADE_AXIS.y, BLADE_AXIS.z));
    }

    private static float bump(float s, float a, float b) {
        return smooth((s - a) / 0.35f) * (1f - smooth((s - b) / 0.35f));
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

    /** В 26.3 у PoseStack нет mulPose(Quaternionf): умножаем на матрицу поворота. */
    private static void mul(PoseStack ps, Quaternionf q) {
        ps.mulPose(new Matrix4f().rotation(q));
    }

    private static void rotX(PoseStack ps, float deg) {
        mul(ps, new Quaternionf().rotationX((float) Math.toRadians(deg)));
    }

    private static void rotY(PoseStack ps, float deg) {
        mul(ps, new Quaternionf().rotationY((float) Math.toRadians(deg)));
    }

    private static void rotZ(PoseStack ps, float deg) {
        mul(ps, new Quaternionf().rotationZ((float) Math.toRadians(deg)));
    }
}
