/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package org.millenaire.village;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import org.millenaire.village.TravelBookScreenState;

public final class TravelBookNavigationState {
    private static final int MAX_HISTORY = 50;
    private static final Map<UUID, PlayerNavState> STATES = new ConcurrentHashMap<UUID, PlayerNavState>();

    private TravelBookNavigationState() {
    }

    public static void navigate(UUID player, TravelBookScreenState state, String culture, String category, String item) {
        PlayerNavState nav = TravelBookNavigationState.getOrCreate(player);
        if (nav.currentState != null) {
            nav.history.push(new NavEntry(nav.currentState, nav.currentCulture, nav.currentCategory, nav.currentItem));
            if (nav.history.size() > 50) {
                nav.history.removeLast();
            }
        }
        nav.currentState = state;
        nav.currentCulture = culture;
        nav.currentCategory = category;
        nav.currentItem = item;
        if (!item.isEmpty() && !nav.currentCategoryItems.contains(item)) {
            nav.currentCategoryItems = List.of();
        }
        TravelBookNavigationState.updateItemIndex(nav);
    }

    @Nullable
    public static NavEntry goBack(UUID player) {
        PlayerNavState nav = STATES.get(player);
        if (nav == null || nav.history.isEmpty()) {
            return null;
        }
        NavEntry prev = nav.history.pop();
        nav.currentState = prev.state();
        nav.currentCulture = prev.culture();
        nav.currentCategory = prev.category();
        nav.currentItem = prev.item();
        TravelBookNavigationState.updateItemIndex(nav);
        return prev;
    }

    @Nullable
    public static String getNextItem(UUID player) {
        PlayerNavState nav = STATES.get(player);
        if (nav == null || nav.currentCategoryItems.isEmpty()) {
            return null;
        }
        int nextIdx = nav.currentItemIndex + 1;
        if (nextIdx >= nav.currentCategoryItems.size()) {
            return null;
        }
        return nav.currentCategoryItems.get(nextIdx);
    }

    @Nullable
    public static String getPrevItem(UUID player) {
        PlayerNavState nav = STATES.get(player);
        if (nav == null || nav.currentCategoryItems.isEmpty()) {
            return null;
        }
        int prevIdx = nav.currentItemIndex - 1;
        if (prevIdx < 0) {
            return null;
        }
        return nav.currentCategoryItems.get(prevIdx);
    }

    public static boolean hasNext(UUID player) {
        return TravelBookNavigationState.getNextItem(player) != null;
    }

    public static boolean hasPrev(UUID player) {
        return TravelBookNavigationState.getPrevItem(player) != null;
    }

    public static boolean hasBack(UUID player) {
        PlayerNavState nav = STATES.get(player);
        return nav != null && !nav.history.isEmpty();
    }

    public static void setCurrentCategoryItems(UUID player, List<String> items) {
        PlayerNavState nav = TravelBookNavigationState.getOrCreate(player);
        nav.currentCategoryItems = List.copyOf(items);
        TravelBookNavigationState.updateItemIndex(nav);
    }

    public static TravelBookScreenState getCurrentState(UUID player) {
        PlayerNavState nav = STATES.get(player);
        return nav != null ? nav.currentState : TravelBookScreenState.HOME;
    }

    public static String getCurrentCulture(UUID player) {
        PlayerNavState nav = STATES.get(player);
        return nav != null ? nav.currentCulture : "";
    }

    public static String getCurrentCategory(UUID player) {
        PlayerNavState nav = STATES.get(player);
        return nav != null ? nav.currentCategory : "";
    }

    public static String getCurrentItem(UUID player) {
        PlayerNavState nav = STATES.get(player);
        return nav != null ? nav.currentItem : "";
    }

    public static void clear(UUID player) {
        STATES.remove(player);
    }

    public static void clearAll() {
        STATES.clear();
    }

    private static PlayerNavState getOrCreate(UUID player) {
        return STATES.computeIfAbsent(player, k -> new PlayerNavState());
    }

    private static void updateItemIndex(PlayerNavState nav) {
        if (nav.currentCategoryItems.isEmpty() || nav.currentItem.isEmpty()) {
            nav.currentItemIndex = -1;
            return;
        }
        nav.currentItemIndex = nav.currentCategoryItems.indexOf(nav.currentItem);
    }

    private static class PlayerNavState {
        final Deque<NavEntry> history = new ArrayDeque<NavEntry>();
        TravelBookScreenState currentState = null;
        String currentCulture = "";
        String currentCategory = "";
        String currentItem = "";
        List<String> currentCategoryItems = List.of();
        int currentItemIndex = -1;

        private PlayerNavState() {
        }
    }

    record NavEntry(TravelBookScreenState state, String culture, String category, String item) {
    }
}

