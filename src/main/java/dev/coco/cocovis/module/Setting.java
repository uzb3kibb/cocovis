package dev.coco.cocovis.module;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** One option of a module: a toggle, a value cycle, a slider or an action button. */
public final class Setting {
    public enum Kind { TOGGLE, CYCLE, SLIDER, ACTION }

    public final Kind kind;
    public final String name;
    private BooleanSupplier getter;
    private Consumer<Boolean> setter;
    private List<String> choices;
    private IntSupplier index;
    private IntConsumer indexSetter;
    private int min;
    private int max;
    private int step = 1;
    private Runnable action;

    private Setting(Kind kind, String name) {
        this.kind = kind;
        this.name = name;
    }

    public static Setting toggle(String name, BooleanSupplier getter, Consumer<Boolean> setter) {
        Setting s = new Setting(Kind.TOGGLE, name);
        s.getter = getter;
        s.setter = setter;
        return s;
    }

    public static Setting cycle(String name, List<String> choices, IntSupplier index, IntConsumer setter) {
        Setting s = new Setting(Kind.CYCLE, name);
        s.choices = choices;
        s.index = index;
        s.indexSetter = setter;
        return s;
    }

    public static Setting slider(String name, int min, int max, int step, IntSupplier value, IntConsumer setter) {
        Setting s = new Setting(Kind.SLIDER, name);
        s.min = min;
        s.max = max;
        s.step = Math.max(1, step);
        s.index = value;
        s.indexSetter = setter;
        return s;
    }

    public static Setting action(String name, Runnable action) {
        Setting s = new Setting(Kind.ACTION, name);
        s.action = action;
        return s;
    }

    /** Height of this option row in the drawer (sliders are taller). */
    public int height() {
        return kind == Kind.SLIDER ? 36 : 24;
    }

    public boolean isOn() {
        return getter != null && getter.getAsBoolean();
    }

    public String valueText() {
        if (kind == Kind.SLIDER) {
            return String.valueOf(index.getAsInt());
        }
        if (choices == null || choices.isEmpty()) {
            return "";
        }
        int i = Math.max(0, Math.min(choices.size() - 1, index.getAsInt()));
        return choices.get(i);
    }

    public float fraction() {
        if (kind != Kind.SLIDER || max <= min) {
            return 0f;
        }
        return Math.max(0f, Math.min(1f, (index.getAsInt() - min) / (float) (max - min)));
    }

    /** Sets a slider from a 0..1 position along its track. */
    public void setFraction(float f) {
        if (kind != Kind.SLIDER) {
            return;
        }
        float clamped = Math.max(0f, Math.min(1f, f));
        int raw = Math.round(min + clamped * (max - min));
        int snapped = Math.round(raw / (float) step) * step;
        indexSetter.accept(Math.max(min, Math.min(max, snapped)));
    }

    public void click() {
        switch (kind) {
            case TOGGLE -> setter.accept(!isOn());
            case CYCLE -> indexSetter.accept((index.getAsInt() + 1) % choices.size());
            case ACTION -> action.run();
            case SLIDER -> { }
        }
    }
}
