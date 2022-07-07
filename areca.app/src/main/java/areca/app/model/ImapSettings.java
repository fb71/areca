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
package areca.app.model;

import org.polymap.model2.Property;

import areca.app.service.mail.RequestParams;
import areca.common.reflect.RuntimeInfo;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class ImapSettings extends Common {

    public static final ImapSettingsClassInfo info = ImapSettingsClassInfo.instance();

    public static ImapSettings      TYPE;

    public Property<String>         host;

    public Property<Integer>        port;

    public Property<String>         username;

    public Property<String>         pwd;

    public Property<Integer>        monthsToSync;


    public RequestParams toRequestParams() {
        return new RequestParams() {{
            this.host.value = ImapSettings.this.host.get();
            this.port.value = ImapSettings.this.port.get().toString();
            this.username.value = ImapSettings.this.username.get();
            this.password.value = ImapSettings.this.pwd.get();
        }};
    }

}
