
package com.tropyx.nb_puppet.parser;

public class PFloat extends PElement {
    private final double value;
    private final int endOffset;

    public PFloat(PElement parent, int offset, int endOffset, double value) {
        super(FLOAT, parent, offset);
        this.value = value;
        this.endOffset = endOffset;
    }

    public double getValue() {
        return value;
    }

    @Override
    public int getEndOffset() {
        return endOffset;
    }


}
