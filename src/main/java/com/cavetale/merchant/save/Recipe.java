package com.cavetale.merchant.save;

import lombok.Data;

/**
 * JSONable.
 */
@Data
public final class Recipe {
    protected long id = -1;
    protected String merchant;
    protected String inA;
    protected String inB;
    protected String out;
    protected int maxUses = -1;
}
