/*
 * Copyright (C) 2014 mkleint
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.tropyx.nb_puppet.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PElement {
    
    public static final int ROOT = 0;
    public static final int RESOURCE = 1;
    public static final int CLASS = 2;
    public static final int CLASS_REF = 3;
    public static final int CLASS_PARAM = 4;
    public static final int VARIABLE = 5;
    public static final int STRING = 6;
    public static final int RESOURCE_ATTR = 7;
    public static final int ARRAY = 8;
    public static final int HASH = 9;
    public static final int REGEXP = 10;
    public static final int TYPE_REF = 11;
    public static final int BLOB = 12;
    public static final int VARIABLE_DEFINITION = 13;
    public static final int DEFINE = 14;
    public static final int NODE = 15;
    public static final int CASE = 16;
    public static final int CONDITION = 17;
    public static final int FUNCTION = 18;
    public static final int IDENTIFIER = 19;
    public static final int NUMBER = 20;
    public static final int FLOAT = 21;
    public static final int ERROR = 22;
    public static final int RESOURCE_REF = 23;

    private final int kind;    
    private final List<PElement> children = new ArrayList<>();
    private PElement parent;
    private final int offset;

    public PElement(int type, PElement parent, int offset) {
        this.kind = type;
        this.offset = offset;
        setParent(parent);
    }

    public List<PElement> getChildren() {
        return children;
    }

    public PElement getParent() {
        return parent;
    }

    public PElement getParentIgnore(Class<?>... classes) {
        List<Class<?>> f = Arrays.asList(classes);
        PElement par = getParent();
        while (par != null && f.contains(par.getClass())) {
            par = par.getParent();
        }
        return par;
    }

    public PElement getChildAtOffset(int offset) {
        for (PElement child : getChildren()) {
            if (child.getOffset() <= offset && child.getEndOffset() >= offset) {
                return child.getChildAtOffset(offset);
            }
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T extends PElement> List<T> getChildrenOfType(Class<T> clazz, boolean recursive) {
        List<T> toRet = new ArrayList<>();
        for (PElement ch : children) {
            if (clazz.equals(ch.getClass())) {
                toRet.add((T)ch);
            }
            if (recursive) {
                toRet.addAll(ch.getChildrenOfType(clazz, recursive));
            }
        }
        return toRet;
    }

    public final void setParent(PElement parent) {
        if (this.parent != null) {
            this.parent.removeChild(this);
        }
        this.parent = parent;
        if (parent != null) {
            parent.addChild(this);
        }
    }

    public int getOffset() {
        return offset;
    }

    public int getEndOffset() {
        int size = children.size();
        if (size > 0) {
            return children.get(size - 1).getEndOffset();
        }
        return getOffset(); //TODO??
    }
    
    public int getKind() {
        return kind;
    }

    public boolean isKind(int type) {
        return this.kind == type;
    }

    private void addChild(PElement aThis) {
        children.add(aThis);
    }

    private void removeChild(PElement aThis) {
        children.remove(aThis);
    }

    public String toStringRecursive() {
        StringBuilder sb = new StringBuilder(toString());
        if (children.size() > 0) {
            for (PElement ch : children) {
                String s = ch.toStringRecursive();
                s = s.replace("\n", "\n  ");
                sb.append("\n  ").append(s);
            }
        }
        return sb.toString();
    }

    public String toStringToRoot() {
        StringBuilder sb = new StringBuilder();
        sb.insert(0, toString());
        PElement par = this.parent;
        while (par != null) {
            sb.insert(0,par.toString() + " -> ");
            par = par.parent;
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }


}
