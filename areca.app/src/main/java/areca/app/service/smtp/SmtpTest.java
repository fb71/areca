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

import java.io.IOException;
import areca.app.model.Address;
import areca.app.service.TransportService.TransportMessage;
import areca.app.service.carddav.CarddavTest;
import areca.app.service.imap.AppendCommand;
import areca.app.service.imap.ImapTest;
import areca.app.service.mail.EmailAddress;
import areca.app.service.smtp.SmtpRequest.AuthPlainCommand;
import areca.app.service.smtp.SmtpRequest.DataCommand;
import areca.app.service.smtp.SmtpRequest.DataContentCommand;
import areca.app.service.smtp.SmtpRequest.HeloCommand;
import areca.app.service.smtp.SmtpRequest.MailFromCommand;
import areca.app.service.smtp.SmtpRequest.QuitCommand;
import areca.app.service.smtp.SmtpRequest.RcptToCommand;
import areca.common.Assert;
import areca.common.Promise;
import areca.common.base.Opt;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.common.testrunner.Skip;
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
    @Skip
    public Promise<?> doIt() {
        var request = new SmtpRequest( self -> {
            self.host = "mail.polymap.de";
            self.port = 465;
            self.loginCommand = new HeloCommand( "zuhause" );
            self.commands.add( new AuthPlainCommand( CarddavTest.ARECA_USERNAME, CarddavTest.ARECA_PWD ) );
            self.commands.add( new MailFromCommand( CarddavTest.ARECA_USERNAME ) );
            self.commands.add( new RcptToCommand( "falko@polymap.de" ) );
            self.commands.add( new DataCommand() );
            self.commands.add( new DataContentCommand( "Schäfchen...", "...möchte auch dir etwas senden!" ) );
            self.commands.add( new QuitCommand() );
        });
        return request.submit()
                .onSuccess( command -> {
                    LOG.info( "Response: %s", command );
                })
                .onError( e -> {
                    LOG.info( "Error: %s", e );
                });
    }


    @Test
    public Promise<?> appendTest() {
        var msg = new TransportMessage() {{
            text = "Hällo! :)";
            threadSubject = Opt.of( "SmtpTest - appendTest()" );
            receipient = new EmailAddress( "falko@polymap.de" );
        }};
        var request = ImapTest.newRequest();
        request.commands.addAll( new AppendCommand( "Sent", msg, "areca@polymap.de" ).commands );
        return request.submit().onSuccess( command -> {
        });
    }


//    @Test
//    public void multipartTest() throws MimeException, IOException {
//
//
//
////        ContentHandler contentHandler = new CustomContentHandler();
////
////        MimeConfig mime4jParserConfig = MimeConfig.DEFAULT;
////        BodyDescriptorBuilder bodyDescriptorBuilder = new DefaultBodyDescriptorBuilder();
////        MimeStreamParser mime4jParser = new MimeStreamParser(mime4jParserConfig,DecodeMonitor.SILENT,bodyDescriptorBuilder);
////        mime4jParser.setContentDecoding(true);
////        mime4jParser.setContentHandler(contentHandler);
////
////        InputStream mailIn = null; //"Provide email mime stream here';
////        mime4jParser.parse(mailIn);
//    }

    @Test
    public void addressTest() {
         var address = new EmailAddress( "schaefchen@polymap.de" );
         LOG.info( "Encoded: %s", address.encoded() );
         var result = Address.parseEncoded( address.encoded() );
         LOG.info( "result: %s", result );
         Assert.isEqual( address.encoded(), result.encoded() );
    }
}
