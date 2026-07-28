/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package org.millenaire.commerce;

import javax.annotation.Nullable;

public final class TradeAction
extends Enum<TradeAction> {
    public static final /* enum */ TradeAction BUY_1 = new TradeAction(0, true, 1);
    public static final /* enum */ TradeAction BUY_8 = new TradeAction(1, true, 8);
    public static final /* enum */ TradeAction BUY_64 = new TradeAction(2, true, 64);
    public static final /* enum */ TradeAction SELL_1 = new TradeAction(3, false, 1);
    public static final /* enum */ TradeAction SELL_8 = new TradeAction(4, false, 8);
    public static final /* enum */ TradeAction SELL_64 = new TradeAction(5, false, 64);
    public static final int ACTIONS_PER_GOOD;
    private final int actionType;
    private final boolean buyFromShop;
    private final int quantity;
    private static final /* synthetic */ TradeAction[] $VALUES;

    public static TradeAction[] values() {
        return (TradeAction[])$VALUES.clone();
    }

    public static TradeAction valueOf(String name) {
        return Enum.valueOf(TradeAction.class, name);
    }

    private TradeAction(int actionType, boolean buyFromShop, int quantity) {
        this.actionType = actionType;
        this.buyFromShop = buyFromShop;
        this.quantity = quantity;
    }

    public int actionType() {
        return this.actionType;
    }

    public boolean isBuyFromShop() {
        return this.buyFromShop;
    }

    public int quantity() {
        return this.quantity;
    }

    public int toButtonId(int goodIndex) {
        return goodIndex * ACTIONS_PER_GOOD + this.actionType;
    }

    @Nullable
    public static TradeAction fromDirectionAndQuantity(boolean buyFromShop, int quantity) {
        for (TradeAction action : TradeAction.values()) {
            if (action.buyFromShop != buyFromShop || action.quantity != quantity) continue;
            return action;
        }
        return null;
    }

    @Nullable
    public static DecodedButton decodeButtonId(int buttonId) {
        if (buttonId < 0) {
            return null;
        }
        int goodIndex = buttonId / ACTIONS_PER_GOOD;
        int actionType = buttonId % ACTIONS_PER_GOOD;
        for (TradeAction action : TradeAction.values()) {
            if (action.actionType != actionType) continue;
            return new DecodedButton(goodIndex, action);
        }
        return null;
    }

    private static /* synthetic */ TradeAction[] $values() {
        return new TradeAction[]{BUY_1, BUY_8, BUY_64, SELL_1, SELL_8, SELL_64};
    }

    static {
        $VALUES = TradeAction.$values();
        ACTIONS_PER_GOOD = TradeAction.values().length;
    }

    public record DecodedButton(int goodIndex, TradeAction action) {
    }
}

