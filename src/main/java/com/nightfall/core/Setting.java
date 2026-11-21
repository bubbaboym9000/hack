package com.nightfall.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Locale;

/**
 * A single configurable value on a module. Three flavours: Bool, Num and Mode.
 */
public abstract class Setting<T> {
    public final String name;
    protected T value;
    protected final T defaultValue;

    protected Setting(String name, T defaultValue) {
        this.name = name;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
    }

    public T get() {
        return value;
    }

    public void set(T newValue) {
        this.value = newValue;
    }

    public void reset() {
        this.value = defaultValue;
    }

    /** Text shown next to the setting name in the GUI. */
    public abstract String display();

    public abstract JsonElement toJson();

    public abstract void fromJson(JsonElement element);

    /** Parses a value typed into chat. Returns false if the text is not valid. */
    public abstract boolean parse(String text);

    /** GUI click: button 0 = left, 1 = right. */
    public abstract void click(int button);

    // ------------------------------------------------------------------

    public static final class Bool extends Setting<Boolean> {
        public Bool(String name, boolean defaultValue) {
            super(name, defaultValue);
        }

        public boolean on() {
            return value;
        }

        @Override
        public String display() {
            return value ? "On" : "Off";
        }

        @Override
        public JsonElement toJson() {
            return new JsonPrimitive(value);
        }

        @Override
        public void fromJson(JsonElement element) {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()) {
                value = element.getAsBoolean();
            }
        }

        @Override
        public boolean parse(String text) {
            String t = text.toLowerCase(Locale.ROOT);
            if (t.equals("true") || t.equals("on") || t.equals("1")) {
                value = true;
                return true;
            }
            if (t.equals("false") || t.equals("off") || t.equals("0")) {
                value = false;
                return true;
            }
            return false;
        }

        @Override
        public void click(int button) {
            value = !value;
        }
    }

    // ------------------------------------------------------------------

    public static final class Num extends Setting<Double> {
        public final double min;
        public final double max;
        public final double step;

        public Num(String name, double defaultValue, double min, double max, double step) {
            super(name, defaultValue);
            this.min = min;
            this.max = max;
            this.step = step;
        }

        public double d() {
            return value;
        }

        public float f() {
            return value.floatValue();
        }

        public int i() {
            return (int) Math.round(value);
        }

        @Override
        public void set(Double newValue) {
            this.value = clamp(newValue);
        }

        private double clamp(double v) {
            double c = Math.max(min, Math.min(max, v));
            if (step > 0) {
                c = Math.round(c / step) * step;
            }
            c = Math.round(c * 10000.0) / 10000.0;
            return Math.max(min, Math.min(max, c));
        }

        @Override
        public String display() {
            return step >= 1 ? Integer.toString(i()) : String.format(Locale.ROOT, "%.2f", value);
        }

        @Override
        public JsonElement toJson() {
            return new JsonPrimitive(value);
        }

        @Override
        public void fromJson(JsonElement element) {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                set(element.getAsDouble());
            }
        }

        @Override
        public boolean parse(String text) {
            try {
                set(Double.parseDouble(text));
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        @Override
        public void click(int button) {
            if (button == 0) {
                set(value >= max ? min : value + step);
            } else {
                set(value <= min ? max : value - step);
            }
        }
    }

    // ------------------------------------------------------------------

    public static final class Mode extends Setting<String> {
        public final String[] modes;

        public Mode(String name, String defaultValue, String... modes) {
            super(name, defaultValue);
            this.modes = modes;
        }

        public boolean is(String mode) {
            return value.equalsIgnoreCase(mode);
        }

        private int index() {
            for (int i = 0; i < modes.length; i++) {
                if (modes[i].equalsIgnoreCase(value)) {
                    return i;
                }
            }
            return 0;
        }

        @Override
        public String display() {
            return value;
        }

        @Override
        public JsonElement toJson() {
            return new JsonPrimitive(value);
        }

        @Override
        public void fromJson(JsonElement element) {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                parse(element.getAsString());
            }
        }

        @Override
        public boolean parse(String text) {
            for (String m : modes) {
                if (m.equalsIgnoreCase(text)) {
                    value = m;
                    return true;
                }
            }
            return false;
        }

        @Override
        public void click(int button) {
            int next = (index() + (button == 0 ? 1 : -1) + modes.length) % modes.length;
            value = modes[next];
        }
    }
}
