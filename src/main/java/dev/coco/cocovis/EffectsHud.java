package dev.coco.cocovis;

import dev.coco.cocovis.gui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Active potion effects: card with progress bars (ref. 2) or compact pills (ref. 1). */
public final class EffectsHud {
    public static int boundsX, boundsY, boundsW, boundsH;

    private static final int TEXT = 0xFFE9EAEF;
    private static final int MUTED = 0xFF8A8D98;
    private static final int TIME = 0xFF8FA6FF;
    private static final int RED = 0xFFFF5F5F;

    private static final Map<String, Integer> MAX = new HashMap<>();
    private static final Map<String, Identifier> SPRITES = new HashMap<>();
    private static Method blit;
    private static Object pipeline;
    private static boolean blitFailed;

    public record Entry(String id, String name, int amp, int ticks, boolean infinite, int color) {}

    private EffectsHud() {}

    public static void render(GuiGraphicsExtractor g, Minecraft mc) {
        CocoVisConfig cfg = CocoVisConfig.get();
        if (!cfg.showEffects || mc.player == null) {
            boundsW = 0;
            boundsH = 0;
            return;
        }
        draw(g, mc, false);
    }

    /** Effects of a player (also used for the nametag dots). */
    public static List<Entry> of(net.minecraft.world.entity.LivingEntity entity) {
        List<Entry> list = new ArrayList<>();
        for (MobEffectInstance inst : entity.getActiveEffects()) {
            MobEffect eff = inst.getEffect().value();
            String id = String.valueOf(BuiltInRegistries.MOB_EFFECT.getKey(eff));
            list.add(new Entry(id, eff.getDisplayName().getString(), inst.getAmplifier(),
                    inst.getDuration(), inst.getDuration() < 0, eff.getColor()));
        }
        list.sort(Comparator.comparing(Entry::id));
        return list;
    }

    private static List<Entry> collect(Minecraft mc, boolean preview) {
        if (preview) {
            List<Entry> sample = new ArrayList<>();
            sample.add(new Entry("minecraft:strength", "Сила", 2, 880, false, 0x932423));
            sample.add(new Entry("minecraft:fire_resistance", "Огнестойкость", 0, 6800, false, 0xE49A3A));
            sample.add(new Entry("minecraft:regeneration", "Регенерация", 1, 160, false, 0xCD5CAB));
            return sample;
        }
        List<Entry> list = of(mc.player);
        Set<String> seen = new HashSet<>();
        for (Entry e : list) {
            seen.add(e.id());
            MAX.merge(e.id(), e.ticks(), Math::max);
        }
        MAX.keySet().retainAll(seen);
        return list;
    }

    private static float ratio(Entry e, boolean preview) {
        if (e.infinite()) {
            return 1f;
        }
        int max = preview ? e.ticks() * 3 / 2 : MAX.getOrDefault(e.id(), e.ticks());
        return Math.max(0f, Math.min(1f, e.ticks() / (float) Math.max(1, max)));
    }

    private static String time(Entry e, boolean pad) {
        if (e.infinite()) {
            return "∞";
        }
        int total = Math.max(0, e.ticks() / 20);
        int m = total / 60;
        int s = total % 60;
        return pad ? String.format(Locale.ROOT, "%02d:%02d", m, s) : String.format(Locale.ROOT, "%d:%02d", m, s);
    }

    private static String fit(Font font, String s, int maxW) {
        if (maxW <= 0) {
            return "";
        }
        if (Ui.width(font, s) <= maxW) {
            return s;
        }
        while (!s.isEmpty() && Ui.width(font, s + "…") > maxW) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "…";
    }

    public static void draw(GuiGraphicsExtractor g, Minecraft mc, boolean preview) {
        CocoVisConfig cfg = CocoVisConfig.get();
        List<Entry> list = collect(mc, preview);
        if (list.isEmpty()) {
            boundsW = 0;
            boundsH = 0;
            return;
        }
        Font font = mc.font;
        float s = cfg.effScale <= 0 ? 1.0f : cfg.effScale;
        g.pose().pushMatrix();
        g.pose().translate((float) cfg.effX, (float) cfg.effY);
        g.pose().scale(s, s);
        int[] size = cfg.effectsPills ? drawPills(g, font, list, preview) : drawCard(g, font, list, preview);
        g.pose().popMatrix();
        boundsX = cfg.effX;
        boundsY = cfg.effY;
        boundsW = Math.round(size[0] * s);
        boundsH = Math.round(size[1] * s);
    }

    private static int[] drawCard(GuiGraphicsExtractor g, Font font, List<Entry> list, boolean preview) {
        int w = 150;
        int headH = 22;
        int rowH = 28;
        int h = headH + list.size() * rowH + 4;
        Ui.card(g, 0, 0, w, h);
        drawFlask(g, 8, 6, TIME);
        Ui.text(g, font, "Эффекты", 22, 7, TEXT, false);
        String count = String.valueOf(list.size());
        Ui.text(g, font, count, w - 8 - Ui.width(font, count), 7, MUTED, false);
        for (int i = 0; i < list.size(); i++) {
            Entry e = list.get(i);
            int y = headH + i * rowH;
            drawIcon(g, e.id(), 8, y + 1, 16, e.color());
            String time = time(e, true);
            int timeW = Ui.width(font, time);
            Ui.text(g, font, fit(font, e.name() + " " + (e.amp() + 1), w - 30 - timeW - 14), 30, y + 4, TEXT, false);
            Ui.text(g, font, time, w - 8 - timeW, y + 4, TIME, false);
            int bx = 8;
            int bw = w - 16;
            int by = y + 19;
            Ui.roundFill(g, bx, by, bw, 3, 1, 0xFF23242B);
            int fw = Math.max(2, Math.round(bw * ratio(e, preview)));
            Ui.hBar(g, bx, by, fw, 3, 0xFF8B5CF6, 0xFF5B8CFF);
        }
        return new int[] {w, h};
    }

    private static int[] drawPills(GuiGraphicsExtractor g, Font font, List<Entry> list, boolean preview) {
        int ph = 14;
        int gap = 2;
        int maxW = 0;
        String head = "Эффекты";
        int hw = 6 + 9 + 4 + Ui.width(font, head) + 7;
        Ui.roundFill(g, 0, 0, hw, ph, 4, 0xCC1B1C21);
        drawFlask(g, 6, 2, TIME);
        Ui.text(g, font, head, 19, 3, TEXT, false);
        maxW = hw;
        int y = ph + gap;
        for (Entry e : list) {
            String text = e.name() + (e.amp() > 0 ? " " + (e.amp() + 1) : "");
            String time = time(e, false);
            int w = 4 + 10 + 4 + Ui.width(font, text) + 4 + Ui.width(font, time) + 6;
            Ui.roundFill(g, 0, y, w, ph, 4, 0xCC1B1C21);
            drawIcon(g, e.id(), 4, y + 2, 10, e.color());
            Ui.text(g, font, text, 18, y + 3, TEXT, false);
            boolean low = !e.infinite() && e.ticks() <= 100;
            Ui.text(g, font, time, 18 + Ui.width(font, text) + 4, y + 3, low ? RED : TEXT, false);
            maxW = Math.max(maxW, w);
            y += ph + gap;
        }
        return new int[] {maxW, y - gap};
    }

    private static void drawFlask(GuiGraphicsExtractor g, int x, int y, int c) {
        g.fill(x + 3, y, x + 6, y + 3, c);
        Ui.roundFill(g, x, y + 3, 9, 7, 3, c);
        g.fill(x + 2, y + 6, x + 7, y + 8, 0xFF101115);
    }

    /** Draws the vanilla effect sprite (via reflection, so a missing method never crashes). */
    private static void drawIcon(GuiGraphicsExtractor g, String id, int x, int y, int size, int color) {
        if (!blitFailed) {
            try {
                if (blit == null) {
                    for (Method m : GuiGraphicsExtractor.class.getMethods()) {
                        if (m.getName().equals("blitSprite") && m.getParameterCount() == 6
                                && m.getParameterTypes()[1] == Identifier.class
                                && m.getParameterTypes()[2] == int.class) {
                            blit = m;
                            break;
                        }
                    }
                    pipeline = Class.forName("net.minecraft.client.renderer.RenderPipelines")
                            .getField("GUI_TEXTURED").get(null);
                    if (blit == null) {
                        blitFailed = true;
                    }
                }
                if (blit != null) {
                    Identifier sprite = SPRITES.computeIfAbsent(id, k -> {
                        int colon = k.indexOf(':');
                        String ns = colon < 0 ? "minecraft" : k.substring(0, colon);
                        String path = colon < 0 ? k : k.substring(colon + 1);
                        return Identifier.fromNamespaceAndPath(ns, "mob_effect/" + path);
                    });
                    blit.invoke(g, pipeline, sprite, x, y, size, size);
                    return;
                }
            } catch (Throwable t) {
                blitFailed = true;
            }
        }
        Ui.roundFill(g, x, y, size, size, 3, 0xFF000000 | color);
    }
}
