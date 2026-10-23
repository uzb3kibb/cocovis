package dev.coco.cocovis.module;

import java.util.function.Consumer;

public class Module {
    public final String name;
    public final String description;
    private boolean enabled;
    private final Consumer<Boolean> onChange;

    public Module(String name, String description, boolean enabled) {
        this(name, description, enabled, null);
    }

    public Module(String name, String description, boolean enabled, Consumer<Boolean> onChange) {
        this.name = name;
        this.description = description;
        this.enabled = enabled;
        this.onChange = onChange;
    }

    public boolean isEnabled() { return enabled; }

    public void toggle() {
        enabled = !enabled;
        if (onChange != null) onChange.accept(enabled);
    }
}
