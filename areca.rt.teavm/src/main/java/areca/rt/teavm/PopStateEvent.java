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
package areca.rt.teavm;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.events.Event;

/**
 * Browser history event.
 * <p>
 * XXX remove when TeaVM provides one.
 *
 * @author Falko Bräutigam
 */
public abstract class PopStateEvent
        implements Event {

    @JSProperty
    public abstract JSObject getState();


    /**
     * Default state
     */
    public static abstract class BrowserHistoryState
            implements JSObject {

        @JSBody(params = "newState", script = "return {state: newState};")
        public static native BrowserHistoryState create( String newState );

        @JSProperty
        public abstract String getState();
    }

}
