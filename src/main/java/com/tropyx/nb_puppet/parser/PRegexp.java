
package com.tropyx.nb_puppet.parser;

public class PRegexp extends PElement {
    private final String value;

    public PRegexp(PElement parent, int offset, String value) {
        super(REGEXP, parent, offset);
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
