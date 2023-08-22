# APIs and Programming

## A minimal app

A minimal application consists of just an app class with the main entry point and a main [Page](https://github.com/fb71/areca/blob/master/areca.ui/src/main/java/areca/ui/pageflow/Page.java).

The app class of this demo basically looks like this:

<pre><code class="language-java">
public class DemoApp extends TeaApp {

    public static void main( String[] args ) throws Exception {
        catchAll( () -> {
            new DemoApp().createUI( rootWindow -> {
                Pageflow.start( rootWindow )
                        .create( new StartPage() )
                        .open();
            });
        });
    }
}
</code></pre>

A simple main page could look like this:

<pre><code class="language-java">
@RuntimeInfo
public class StartPage {

    // inject the system interface for this page
    @Page.Context
    protected PageSite pageSite;

    // create and inject the standard page UI container
    @Page.Part
    protected PageContainer ui;

    @Page.CreateUI
    public UIComponent create( UIComposite parent ) {
        ui.body.add( new UIComposite() {{
            add( new Text() {{
                content.set( "Text..." );
            }});
            add( new Button() {{
                label.set( "CLOSE" );
                events.on( ev -> {
                    pageflow.close();
                });
            }});
        }});
        return ui;
    }
}
</code></pre>

That's it. This is a complete application.

The demo uses [Marked](https://github.com/markedjs/marked) for parsing markdown and [Prism](https://prismjs.com) for syntax highlighting. The entire code of the demo is [on GitHub](https://github.com/fb71/areca/tree/master/areca.demo/src/main/java/areca/demo).

## UI components

Typical code to create UI components looks like this:

<pre><code class="language-java">
    ui.body.add( new UIComposite() {{
        layout.set( RowLayout.filled() );
        
        add( new Text() {{
            content.set( "This the text..." );
        }});

        add( new Button() {{
            label.set( "CLICK ME" );
            events.on( SELECT, ev -> {
                ...
            });
        }});
    }});
</code></pre>

Note how **object initializers** are used to create a hierarchy of depending objects. Is is minimal code and the structure of the code reflects the hierarchy of the UI components.

## Pageflow

...

<pre><code class="language-java">
    @Page.CreateUI
    public UIComponent create( UIComposite parent ) {
        ui.init( parent ).title.set( "Areca Demo" );
        ui.body.layout.set( SwitcherLayout.defaults() );
        ui.body.add( new UIComposite() {{
            add( createText( cms.file( "start" ) ) );
        }});
        return ui;
    }
</code></pre>

<pre><code class="language-java">
    @Page.CreateUI
    public UIComponent create( UIComposite parent ) {
        ui.init( parent ).title.set( "Areca Demo" );
        ui.body.layout.set( SwitcherLayout.defaults() );
        ui.body.add( new UIComposite() {{
            add( createText( cms.file( "start" ) ) );
        }});
        return ui;
    }
</code></pre>

## Asynchronous programming with Promise

...

<pre><code class="language-java">
    @Page.CreateUI
    public UIComponent create( UIComposite parent ) {
        ui.init( parent ).title.set( "Areca Demo" );
        ui.body.layout.set( SwitcherLayout.defaults() );
        ui.body.add( new UIComposite() {{
            add( createText( cms.file( "start" ) ) );
        }});
        return ui;
    }
</code></pre>

## Five

...