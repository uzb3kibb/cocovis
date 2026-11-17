package dev.coco.cocovis.module;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class Module {
    public final String name;
    private final String description;
    private final BooleanSupplier state;
    private final Consumer<Boolean> setter;
    private Supplier<String> descriptionSupplier;
    /** Options shown in the drawer under the module row. */
    public final List<Setting> options = new ArrayList<>();

    public Module(String name, String description, BooleanSupplier state, Consumer<Boolean> setter) {
        this.name = name;
        this.description = description;
        this.state = state;
        this.setter = setter;
    }

    public Module with(Setting option) {
        options.add(option);
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
