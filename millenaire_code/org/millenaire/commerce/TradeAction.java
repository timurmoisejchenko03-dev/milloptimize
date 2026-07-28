/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package org.millenaire.commerce;

import javax.annotation.Nullable;

public enum TradeAction {
    BUY_1(0, true, 1),
    BUY_8(1, true, 8),
    BUY_64(2, true, 64),
    SELL_1(3, false, 1),
    SELL_8(4, false, 8),
    SELL_64(5, false, 64);

    public static final int ACTIONS_PER_GOOD;
    private final int actionType;
    private final boolean buyFromShop;
    private final int quantity;

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

    static {
        ACTIONS_PER_GOOD = TradeAction.values().length;
    }

    public record DecodedButton(int goodIndex, TradeAction action) {
    }
}

