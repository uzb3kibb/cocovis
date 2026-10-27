package dev.coco.cocovis.module;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Module {
    public final String name;
    public final String description;
    private boolean enabled;
    private final Consumer<Boolean> onChange;
    /** Если задано — состояние берётся отсюда (для модулей, которыми владеет другой мод). */
    private BooleanSupplier state;
    /** Если задано — в меню появится кнопка «Настроить». */
    public Runnable settings;
    /** Подпись кнопки справа от названия. */
    public String settingsLabel = "Настроить";
    private Supplier<String> descriptionSupplier;
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

    public Module withSettings(String label, Runnable r) {
        this.settingsLabel = label;
        this.settings = r;
        return this;
    }

    /** Описание, которое может меняться (например «Сейчас: Huntsman · Ржавый»). */
    public Module withDescription(Supplier<String> s) {
        this.descriptionSupplier = s;
        return this;
    }

    public String description() {
        return descriptionSupplier != null ? descriptionSupplier.get() : description;
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
