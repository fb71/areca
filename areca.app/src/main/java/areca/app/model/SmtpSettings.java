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

import org.polymap.model2.Entity;
import org.polymap.model2.Property;

import areca.app.service.mail.RequestParams;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class SmtpSettings
        extends Entity {

    public static final ClassInfo<SmtpSettings> info = SmtpSettingsClassInfo.instance();

    public static SmtpSettings      TYPE;

    public Property<String>         host;

    public Property<Integer>        port;

    public Property<String>         username;

    public Property<String>         pwd;

    public Property<String>         from;


    public RequestParams toRequestParams() {
        return new RequestParams() {{
            this.host.value = SmtpSettings.this.host.get();
            //this.port.value = settings.port.get();
            this.username.value = SmtpSettings.this.username.get();
            this.password.value = SmtpSettings.this.pwd.get();
        }};
    }

}
