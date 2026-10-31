package dev.coco.cocovis;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.HumanoidArm;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Свои анимации ножа от первого лица: покачивание в руке, выхват, удар (два чередующихся взмаха) и осмотр.
 * Формулы совпадают с теми, по которым делался предпросмотр. Работает только для правой основной руки.
 */
public final class KnifeAnimations {
    /** Направление вдоль клинка в системе камеры при обычном хвате. */
    private static final Vector3f BLADE_AXIS = new Vector3f(-0.957f, 0.271f, -0.100f).normalize();
    /** Центр ножа относительно руки (из display-перевода модели): вокруг него идёт прокрутка при осмотре. */
    private static final Vector3f KNIFE_CENTER = new Vector3f(-2.9f / 16f, 3.75f / 16f, -0.72f / 16f);
    /** Сколько после удара игра «проседает» предмет, пока перезаряжается удар. */
    private static final long DIP_MS = 800;
    private static final long SWING_MS = 340;

    private static boolean logged = false;
    private static boolean swingLogged = false;
    private static long swingStart = 0;
    private static long lastSwingMs = 0;
    private static boolean armed = true;
    private static boolean backhand = false;

    private KnifeAnimations() {}

    private static boolean animate(HumanoidArm arm) {
        if (arm != HumanoidArm.RIGHT) return false;
        return ownsSwing();
    }

    /** Нож с анимациями в правой основной руке. Миксин взмаха по этому условию отменяет стандартный взмах. */
    public static boolean ownsSwing() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || !Knives.isActive() || !DiamondConfig.get().knifeAnimations) return false;
        if (p.getMainArm() != HumanoidArm.RIGHT) return false;
        return Knives.isCustomSword(p.getMainHandItem());
    }

    /** Вызывается из миксина при отмене стандартного взмаха — для проверки по журналу. */
    public static void noteSwingHook() {
        if (!swingLogged) {
            swingLogged = true;
            System.out.println("[cocovis] хук взмаха ножа работает");
        }
    }

    /** Хват: вызывается после того, как игра поставила предмет в точку руки (applyItemArmTransform). */
    public static void hold(PoseStack ps, HumanoidArm arm, float equip) {
        if (!animate(arm)) return;
        if (!logged) {
            logged = true;
            System.out.println("[cocovis] хук анимаций ножа работает");
        }

        float swing = swingProgress();   // заодно запоминает время начала удара

        // лёгкое «дыхание» ножа в руке
        double t = System.nanoTime() / 1.0e9;
        ps.translate(0f, (float) (Math.sin(t * 1.7) * 0.0035), 0f);
        rotZ(ps, (float) (Math.sin(t * 1.1) * 0.8));

        // После удара игра на время перезарядки опускает предмет вниз, и нож уходил за край экрана.
        // В CS2 такого нет: сразу после удара возвращаем предмет на место (игра сдвигает его на -0.6 * equip по высоте).
        if (System.currentTimeMillis() - lastSwingMs < DIP_MS) {
            if (equip > 0f) ps.translate(0f, 0.6f * equip, 0f);
            equip = 0f;
        }

        // выхват: нож подбрасывается снизу
        if (equip > 0f) {
            ps.translate(0f, 0.04f * equip, 0f);
            rotX(ps, -48f * equip);
            rotZ(ps, 26f * equip);
            rotY(ps, -14f * equip);
        }

        if (swing > 0f) {
            Knives.cancelInspect();
            strike(ps, swing);
            return;
        }
        float ip = Knives.inspectProgress();
        if (ip >= 0f) inspect(ps, ip);
    }

    /**
     * Свой отсчёт взмаха: игра сообщает только о начале удара, дальше идём по реальному времени.
     * Так анимация плавная при любом FPS и не зависит от скорости стандартного взмаха.
     */
    private static float swingProgress() {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return 0f;
        long now = System.currentTimeMillis();
        float vanilla = vanillaSwing(p);
        if (vanilla <= 0f && swingStart == 0) armed = true;
        if (vanilla > 0f && armed && swingStart == 0) {
            swingStart = now;
            lastSwingMs = now;
            armed = false;
            backhand = !backhand;      // удары чередуются: прямой, обратный, прямой…
        }
        if (swingStart == 0) return 0f;
        float s = (now - swingStart) / (float) SWING_MS;
        if (s >= 1f) {
            swingStart = 0;
            return 0f;
        }
        return s;
    }

    /**
     * Удар: замах лезвием вверх-вправо, рубящая дуга через экран влево, возврат.
     * Обратный удар идёт по тому же пути в обратную сторону.
     */
    private static void strike(PoseStack ps, float s) {
        if (backhand) s = 1f - s;
        float wind = smooth(s / 0.22f);
        float strike = smooth((s - 0.22f) / 0.26f);
        float rec = smooth((s - 0.48f) / 0.52f);
        ps.translate(k(wind, strike, rec, 0.08f, -0.30f, 0.22f),
                k(wind, strike, rec, 0.09f, -0.04f, -0.05f),
                k(wind, strike, rec, 0.03f, -0.12f, 0.09f));
        rotY(ps, k(wind, strike, rec, 6f, 14f, -20f));
        rotZ(ps, k(wind, strike, rec, -46f, 64f, -18f));
        rotX(ps, k(wind, strike, rec, -4f, -2f, 6f));
    }

    private static float k(float wind, float strike, float rec, float a, float b, float c) {
        return a * wind + b * strike + c * rec;
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
        ps.translate(-0.296f * up, 0.325f * up, -0.238f * up);
        rotZ(ps, -67.2f * up + lean);
        rotX(ps, -11.7f * up);
        // прокрутка вокруг самого ножа (через его центр), а не вокруг руки
        ps.translate(KNIFE_CENTER.x, KNIFE_CENTER.y, KNIFE_CENTER.z);
        mul(ps, new Quaternionf().rotationAxis((float) Math.toRadians(spin), BLADE_AXIS.x, BLADE_AXIS.y, BLADE_AXIS.z));
        ps.translate(-KNIFE_CENTER.x, -KNIFE_CENTER.y, -KNIFE_CENTER.z);
    }

    private static float bump(float s, float a, float b) {
        return smooth((s - a) / 0.35f) * (1f - smooth((s - b) / 0.35f));
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
