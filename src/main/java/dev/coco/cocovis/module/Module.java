package dev.coco.cocovis.module;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class Module {
    public final String name;
    public final String description;
    private boolean enabled;
    private final Consumer<Boolean> onChange;
    /** Если задано — состояние берётся отсюда (для модулей, которыми владеет другой мод). */
    private BooleanSupplier state;
    /** Если задано — в меню появится кнопка «Настроить». */
    public Runnable settings;
    public Item icon = Items.PAPER;

    public Module(String name, String description, boolean enabled) {
        this(name, description, enabled, null);
    }

    public Module(String name, String description, boolean enabled, Consumer<Boolean> onChange) {
        this.name = name;
        this.description = description;
        this.enabled = enabled;
        this.onChange = onChange;
    }

    public Module withSettings(Runnable r) {
        this.settings = r;
        return this;
    }

    public Module withIcon(Item item) {
        this.icon = item;
        return this;
    }

    public Module withState(BooleanSupplier s) {
        this.state = s;
        return this;
    }

    public boolean isEnabled() {
        return state != null ? state.getAsBoolean() : enabled;
    }

    public void toggle() {
        boolean next = !isEnabled();
        if (state == null) enabled = next;
        if (onChange != null) onChange.accept(next);
    }
}
