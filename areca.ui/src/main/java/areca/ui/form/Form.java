/*
 * Copyright (C) 2020, the @authors. All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3.0 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package areca.ui.form;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Falko Bräutigam
 */
public class Form {

    protected List<FieldBuilder>    fields = new ArrayList<>();


    public FieldBuilder newField() {
        return new FieldBuilder() {{ fields.add( this ); }};
    }

    public void submit() {
        fields.forEach( f -> f._viewer().store() );
    }

    public void revert() {
        fields.forEach( f -> f._viewer().load() );
    }

}
