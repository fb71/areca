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
package areca.app.service.smtp;

import areca.app.service.carddav.CardDavTest;
import areca.app.service.smtp.SmtpRequest.AuthPlainCommand;
import areca.app.service.smtp.SmtpRequest.DataCommand;
import areca.app.service.smtp.SmtpRequest.HeloCommand;
import areca.app.service.smtp.SmtpRequest.DataContentCommand;
import areca.app.service.smtp.SmtpRequest.MailFromCommand;
import areca.app.service.smtp.SmtpRequest.QuitCommand;
import areca.app.service.smtp.SmtpRequest.RcptToCommand;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.common.testrunner.Test;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class SmtpTest {

    private static final Log LOG = LogFactory.getLog( SmtpTest.class );

    public static final ClassInfo<SmtpTest> info = SmtpTestClassInfo.instance();


    @Test
    public void doIt() {
        var request = new SmtpRequest( self -> {
            self.host = "mail.polymap.de";
            self.port = 465;
            self.loginCommand = new HeloCommand( "zuhause" );
            self.commands.add( new AuthPlainCommand( CardDavTest.ARECA_USERNAME, CardDavTest.ARECA_PWD ) );
            self.commands.add( new MailFromCommand( CardDavTest.ARECA_USERNAME ) );
            self.commands.add( new RcptToCommand( "falko@polymap.de" ) );
            self.commands.add( new DataCommand() );
            self.commands.add( new DataContentCommand( "Schäfchen...", "...möchte auch dir etwas senden!" ) );
            self.commands.add( new QuitCommand() );
        });
        request.submit()
                .onSuccess( command -> {
                    LOG.info( "Response: %s", command );
                });
    }

}
