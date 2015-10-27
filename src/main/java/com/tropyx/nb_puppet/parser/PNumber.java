
package com.tropyx.nb_puppet.parser;

public class PNumber extends PElement {
    private final int value;
    private final int endOffset;

    public PNumber(PElement parent, int offset, int endOffset, int value) {
        super(NUMBER, parent, offset);
        this.value = value;
        this.endOffset = endOffset;
    }

    public int getValue() {
        return value;
    }

    @Override
    public int getEndOffset() {
        return endOffset;
    }


}
