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
package areca.rt.server.servlet;

import java.util.List;

/**
 *
 * @author Falko Bräutigam
 */
class JsonClient2ServerMessage {

    public boolean startSession;

    public List<JsonClickEvent> events;

    /**
     *
     */
    public static class JsonClickEvent {
        public String eventType;
        public String position;
        public Integer componentId;
        public String content;
    }

}