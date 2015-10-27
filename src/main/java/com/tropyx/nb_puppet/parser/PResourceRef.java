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

import java.util.List;

public class PResourceRef extends PElement {
    private PIdentifier name;
    
    public PResourceRef(PElement parent, int offset) {
        super(RESOURCE_REF, parent, offset);
    }

    public String getName() {
        return name.getName();
    }

    void setName(PIdentifier name) {
        this.name = name;
    }

    public String getType() {
        PElement par = getParentIgnore(PBlob.class);
        if (par != null && par instanceof PTypeReference) {
            String type = ((PTypeReference)par).getType();
            if ("Resource".equals(type)) {
                List<PTypeReference> ty = ((PTypeReference)par).getChildrenOfType(PTypeReference.class, false);
                if (!ty.isEmpty()) {
                    return ty.get(0).getType();
                } else {
                    //wrong def?
                }
            } else {
                return type;
            }

        } else if (par != null && par instanceof PResource) {
            //TODO actual definition of resource
        }
        return null; //TODO or throw exception?
    }

    @Override
    public String toString() {
        return super.toString() + "[" + getName() + "]";
    }

}
