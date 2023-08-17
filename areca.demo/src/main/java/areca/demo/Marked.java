/*
 * Copyright (C) 2023, the @authors. All rights reserved.
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
package areca.demo;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;

/**
 * The <a href=https://github.com/markedjs/marked>Marked</a> markdown parser.
 *
 * @author Falko Bräutigam
 */
public abstract class Marked
        implements JSObject {

    @JSBody(script = "return window.marked;")
    public static native Marked instance();

    @JSMethod
    public abstract String parse( String markdown );

}
