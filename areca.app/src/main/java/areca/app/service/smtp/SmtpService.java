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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import areca.app.ArecaApp;
import areca.app.model.Address;
import areca.app.model.SmtpSettings;
import areca.app.service.Service;
import areca.app.service.TransportService;
import areca.app.service.imap.EmailAddress;
import areca.app.service.smtp.SmtpRequest.AuthPlainCommand;
import areca.app.service.smtp.SmtpRequest.DataCommand;
import areca.app.service.smtp.SmtpRequest.DataContentCommand;
import areca.app.service.smtp.SmtpRequest.HeloCommand;
import areca.app.service.smtp.SmtpRequest.MailFromCommand;
import areca.app.service.smtp.SmtpRequest.QuitCommand;
import areca.app.service.smtp.SmtpRequest.RcptToCommand;
import areca.common.Promise;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;

/**
 *
 * @author Falko Bräutigam
 */
public class SmtpService
        extends Service
        implements TransportService {

    private static final Log LOG = LogFactory.getLog( SmtpService.class );

    @Override
    public String label() {
        return "EMail";
    }

    @Override
    public Promise<List<Transport>> newTransport( Address receipient, TransportContext ctx ) {
        var email = EmailAddress.check( receipient );
        if (email.isPresent()) {
            return ArecaApp.current().settings()
                    .then( uow -> uow.query( SmtpSettings.class ).executeCollect() )
                    .map( rs -> Sequence.of( rs )
                            .map( settings -> (Transport)new SmtpTransport( settings, email.get(), ctx ) )
                            .toList() );
        }
        else {
            return Promise.completed( Collections.emptyList() );
        }
    }

    /**
     *
     */
    protected class SmtpTransport
            extends Transport {

        protected SmtpSettings      settings;

        protected EmailAddress      receipient;

        protected TransportContext  ctx;

        public SmtpTransport( SmtpSettings settings, EmailAddress receipient, TransportContext ctx ) {
            this.settings = settings;
            this.receipient = receipient;
            this.ctx = ctx;
        }


        @Override
        public Promise<Sent> send( TransportMessage msg ) {
            var request = new SmtpRequest( self -> {
                self.host = settings.host.get();
                self.port = settings.port.get();
                LOG.info( "Hostname: ", ArecaApp.hostname() );
                self.loginCommand = new HeloCommand( ArecaApp.hostname() );
                self.commands.add( new AuthPlainCommand( settings.username.get(), settings.pwd.get() ) );
                self.commands.add( new MailFromCommand( settings.from.get() ) );
                self.commands.add( new RcptToCommand( receipient.content ) );
                self.commands.add( new DataCommand() );
                self.commands.add( new DataContentCommand( msg.threadSubject.orElse( "" ), msg.text ) );
                self.commands.add( new QuitCommand() );
            });
            return request.submit()
                    .onSuccess( command -> {
                        LOG.info( "Response: %s", command );
                    })
                    .onError( e -> {
                        LOG.info( "Error: %s", e );
                    })
                    .reduce( new ArrayList<>(), (r,c) -> r.add( c ) )
                    .map( l -> new Sent() {{
                        message = msg;
                        from = settings.from.get();
                    }});
        }
    }
}
