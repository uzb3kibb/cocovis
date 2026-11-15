package dev.coco.cocovis.module;

import net.minecraft.world.item.Item;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class Module {
    public final String name;
    public final Item icon;
    private final String description;
    private final BooleanSupplier state;
    private final Consumer<Boolean> setter;
    private Supplier<String> descriptionSupplier;
    public Runnable settings;
    public String settingsLabel = "Настроить";

    public Module(String name, String description, Item icon, BooleanSupplier state, Consumer<Boolean> setter) {
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.state = state;
        this.setter = setter;
    }

    public Module withSettings(String label, Runnable action) {
        this.settingsLabel = label;
        this.settings = action;
        return this;
    }

    public Module withDescription(Supplier<String> supplier) {
        this.descriptionSupplier = supplier;
        return this;
    }

    public String description() {
        return descriptionSupplier != null ? descriptionSupplier.get() : description;
    }

    public boolean isEnabled() {
        return state.getAsBoolean();
    }

    public void toggle() {
        setter.accept(!isEnabled());
    }
}
