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
package areca.app.service.email;

import org.teavm.jso.JSBody;
import org.teavm.jso.core.JSString;
import org.teavm.jso.dom.xml.Document;

/**
 *
 * @author Falko Bräutigam
 */
public abstract class DOMParser {

    @JSBody(script = "new DOMParser();")
    public static native DOMParser create();


    @JSBody(params = "{text, mimeType}", script = "")
    public abstract Document parseFromString( JSString text, JSString  mimeType );

}
