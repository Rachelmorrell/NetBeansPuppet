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
import java.util.List;

public class PResource extends PElement {
    private final List<PElement> titles = new ArrayList<>();
    private final PTypeReference resourceType;
    private final List<PResourceAttribute> atributes = new ArrayList<>();
    
    public PResource(PElement parent, int offset, PTypeReference resourceType) {
        super(RESOURCE, parent, offset);
        this.resourceType = resourceType;
    }

    public PTypeReference getResourceType() {
        return resourceType;
    }

    void addTitle(PElement title) {
        titles.add(title);
    }

    public List<PElement> getTitles() {
        return titles;
    }

    public List<PResourceAttribute> getAtributes() {
        return atributes;
    }

    public void addAttribute(PResourceAttribute attribute) {
        this.atributes.add(attribute);
    }

    @Override
    public String toString() {
        return super.toString() + "[" + resourceType + ']';
    }
    
}
