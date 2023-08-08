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

import static areca.ui.Orientation.VERTICAL;

import java.util.regex.Pattern;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import org.apache.commons.lang3.StringUtils;

import areca.app.ArecaApp;
import areca.app.model.Message;
import areca.app.model.Message.ContentType;
import areca.common.base.Sequence;
import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Action;
import areca.ui.Size;
import areca.ui.component2.ScrollableComposite;
import areca.ui.component2.Text;
import areca.ui.component2.UIComponent;
import areca.ui.component2.UIComposite;
import areca.ui.layout.FillLayout;
import areca.ui.layout.RowLayout;
import areca.ui.pageflow.Page;
import areca.ui.pageflow.PageContainer;


/**
 *
 * @author Falko Bräutigam
 */
public class MessagePage
        extends Page {

    private static final Log LOG = LogFactory.getLog( MessagePage.class );

    private PageContainer ui;

    private Message msg;


    public MessagePage( Message msg ) {
        this.msg = msg;
    }


    @Override
    protected UIComponent onCreateUI( UIComposite parent ) {
        ui = new PageContainer( this, parent );
        ui.title.set( StringUtils.abbreviate( "Message...", 25 ) );

        ui.body.layout.set( new RowLayout().orientation.set( VERTICAL ).fillWidth.set( true ).fillHeight.set( true )
                .spacing.set( 15 ).margins.set( Size.of( 10, 10 ) ) );

        ui.body.add( new ScrollableComposite() {{
            layout.set( new FillLayout() );
            add( new Text() {{
                format.set( Format.HTML );
                content.set( format( msg, Integer.MAX_VALUE ) );
            }});
        }});

        pageSite.actions.add( new Action() {{
            icon.set( "delete" );
            description.set( "Delete this message" );
            handler.set( ev -> {
                ArecaApp.current().modelUpdates.schedule( uow -> {
                    return uow.entity( msg )
                            .then( loaded -> loaded.delete() )
                            .then( loaded -> uow.submit().onSuccess( __ -> LOG.info( "Submitted." ) ) )
                            .onSuccess( __ -> pageSite.close() );
                });
            });
        }});
        return ui;
    }


    public static final Pattern URL = Pattern.compile( "https?://([^?\\s]+)(\\?[^\\s]+)?", Pattern.CASE_INSENSITIVE );
    public static final Pattern SPACES = Pattern.compile( "^[\\s]+" );

    /**
     * Formats the content of the given {@link Message} as HTML.
     *
     * @return HTML string.
     */
    public static String format( Message msg, int maxLines ) {
        if (msg.contentType.get() == ContentType.HTML ) {
            return msg.content.get();  // XXX sanitize!
        }
        else {
            try {
                // XXX format quotes

                var html = new StringBuilder( 4096 );
                var reader = new BufferedReader( new StringReader( msg.content.get() ) );
                var c = 0;
                for (var line = reader.readLine(); line != null && c++ < maxLines; line = reader.readLine()) {
                    //LOG.info( "> %s", line );
                    // URL
                    var url = URL.matcher( line );
                    if (url.find()) {
                        line = line.replace( url.group( 0 ),
                                String.format( "<a href=\"%s\" target=\"_blank\" rel=\"noopener\">%s</a>", url.group( 0 ), url.group( 1 ) ) );
                    }
                    // spaces
                    var spaces = SPACES.matcher( line );
                    if (spaces.find()) {
                        var s = Sequence.ofInts( 1, spaces.group().length()*2 ).reduce( "", (r,i) -> r + "&nbsp;" );
                        line = s + line.substring( spaces.group().length() );
                    }

                    // line break
                    html.append( line ).append( "<br/>" );
                }
                return html.toString();
            }
            catch (IOException e) {
                throw new RuntimeException( "Should/Can never happen: " + e, e );
            }
        }
    }

}
