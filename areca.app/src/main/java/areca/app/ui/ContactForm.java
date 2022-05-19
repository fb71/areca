/*
 * Copyright (C) 2022, the @authors. All rights reserved.
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
package areca.app.ui;

import areca.app.model.Contact;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.ui.form.Form;
import areca.ui.form.UI;
import areca.ui.viewer.SingleValueAdapter;
import areca.ui.viewer.TextViewer;

@RuntimeInfo
public class ContactForm extends Form {

    public static final ClassInfo<ContactForm> TYPE = ContactFormClassInfo.instance();

    Contact                     contact;

    @UI(viewer = TextViewer.class)
    SingleValueAdapter<String>  firstname = new PropertyAdapter<>( () -> contact.firstname );

    @UI(viewer = TextViewer.class)
    SingleValueAdapter<String>  lastname = new PropertyAdapter<>( () -> contact.lastname );


    public ContactForm( Contact contact ) {
        this.contact = contact;
    }

    /* XXX Making reflection code compile */
    protected ContactForm() {
    }

}