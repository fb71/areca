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
import org.teavm.jso.dom.html.HTMLElement;

/**
 * Simple Java interface of the <a href=https://prismjs.com</a> sytax hightlighter.
 *
 * @author Falko Bräutigam
 */
public abstract class Prism
        implements JSObject {

    @JSBody(script = "return window.Prism;")
    public static native Prism instance();

    @JSMethod
    public abstract void highlightAllUnder( HTMLElement container );

}
