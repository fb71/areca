/*
 * Copyright (C) 2021, the @authors. All rights reserved.
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
package areca.ui.pageflow;

import areca.common.log.LogFactory;
import areca.common.log.LogFactory.Log;
import areca.ui.Position;
import areca.ui.Size;
import areca.ui.component2.Button;
import areca.ui.component2.Property;
import areca.ui.component2.Property.ReadWrite;
import areca.ui.component2.Text;
import areca.ui.component2.UIComposite;
import areca.ui.layout.LayoutManager;

/**
 * Provides the standard common user interface elements for a {@link Page}.
 *
 * @author Falko Bräutigam
 */
public class PageContainer
        extends UIComposite {

    static final Log LOG = LogFactory.getLog( PageContainer.class );

    private static final String CSS_HEADER = "PageHeader";
    private static final String CSS_HEADER_ITEM = "PageHeaderItem";
    private static final String CSS_TITLE = "PageTitle";

    public ReadWrite<?,String>  title = Property.rw( this, "title" );

    public UIComposite          body;

    protected UIComposite       headerComposite;

    protected Text              titleText;

    protected UIComposite       toolbar;

    protected Button            closeBtn;


    public PageContainer( UIComposite parent ) {
        parent.components.add( this );
        layout.set( new PageContainerLayout() );

        // header
        headerComposite = add( new UIComposite() {{
            cssClasses.add( CSS_HEADER );

            closeBtn = add( new Button() {{
                cssClasses.add( CSS_HEADER_ITEM );
                icon.set( "arrow_back" );
            }});
            titleText = add( new Text() {{
                cssClasses.add( CSS_TITLE );
                title.onChange( (newValue, __) -> content.set( newValue ) );
            }});
        }});

//        toolbar = add( new UIComposite() {{
//            layout.set( new FillLayout() );
//            components.add( new Button() {{
//                label.set( "Toolbar" );
//                bordered.set( false );
//                events.on( SELECT, ev -> bordered.set( !bordered.value() ) );
//            }});
//            bordered.set( true );
//        }});

        body = add( new UIComposite() );
    }


    /**
     *
     */
    class PageContainerLayout
            extends LayoutManager {

        public static final int HEADER_HEIGHT = 60;
        public static final int TOOLBAR_HEIGHT = 45;

        @Override
        public void layout( UIComposite composite ) {
            var clientSize = PageContainer.this.clientSize.opt().orElse( Size.of( 50, 50 ) );

            var top = 0;
            headerComposite.position.set( Position.of( 0, top ) );
            headerComposite.size.set( Size.of( clientSize.width(), HEADER_HEIGHT ) );
            top += HEADER_HEIGHT;

            var closeSize = HEADER_HEIGHT - 10;
            var closeMargin = (HEADER_HEIGHT - closeSize) / 2;
            closeBtn.position.set( Position.of( closeMargin, closeMargin ) );
            closeBtn.size.set( Size.of( closeSize, closeSize ) );

            var titleMargin = (HEADER_HEIGHT - 18) / 2;
            titleText.position.set( Position.of( closeMargin + closeSize + titleMargin, titleMargin-1 ) );

//            toolbar.position.set( Position.of( 0, HEADER_HEIGHT ) );
//            toolbar.size.set( Size.of( clientSize.width(), TOOLBAR_HEIGHT ) );

            body.position.set( Position.of( 0, top ) );
            body.size.set( Size.of( clientSize.width(), clientSize.height() - top ) );
        }
    }

}
