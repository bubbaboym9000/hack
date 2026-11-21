package com.nightfall.core;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/** Players on this list are never targeted by KillAura, TriggerBot, AimAssist and friends. */
public final class FriendManager {
    private static final Set<String> FRIENDS = new TreeSet<>();

    private FriendManager() {
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public static boolean isFriend(String name) {
        return FRIENDS.contains(key(name));
    }

    public static void add(String name) {
        FRIENDS.add(key(name));
    }

    public static boolean remove(String name) {
        return FRIENDS.remove(key(name));
    }

    public static Set<String> all() {
        return Collections.unmodifiableSet(FRIENDS);
    }

    static void replaceAll(Collection<String> names) {
        FRIENDS.clear();
        for (String n : names) {
            FRIENDS.add(key(n));
        }
    }
}
