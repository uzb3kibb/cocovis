package dev.coco.cocovis.module;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** One option of a module: a toggle, a value cycle or an action button. */
public final class Setting {
    public enum Kind { TOGGLE, CYCLE, ACTION }

    public final Kind kind;
    public final String name;
    private BooleanSupplier getter;
    private Consumer<Boolean> setter;
    private List<String> choices;
    private IntSupplier index;
    private IntConsumer indexSetter;
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

    public static Setting action(String name, Runnable action) {
        Setting s = new Setting(Kind.ACTION, name);
        s.action = action;
        return s;
    }

    public boolean isOn() {
        return getter != null && getter.getAsBoolean();
    }

    public String valueText() {
        if (choices == null || choices.isEmpty()) {
            return "";
        }
        int i = Math.max(0, Math.min(choices.size() - 1, index.getAsInt()));
        return choices.get(i);
    }

    public void click() {
        switch (kind) {
            case TOGGLE -> setter.accept(!isOn());
            case CYCLE -> indexSetter.accept((index.getAsInt() + 1) % choices.size());
            case ACTION -> action.run();
        }
    }
}
