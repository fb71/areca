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

An important part of the Pageflow system is the **context** of a Page. A Page can share variables with other Pages via its context. Context variables are **injected** using annotations.

<pre><code class="language-java">
public class StartPage {

    // inject the system interface for this page
    @Page.Context
    protected PageSite pageSite;

    // create and inject the standard page UI container
    @Page.Part
    protected PageContainer ui;
    
    ...
</code></pre>

## Data modeling

The following is an example of a simple **Entity**:

<pre><code class="language-java">
public class Person extends Entity {

    public static Person TYPE;

    @Nullable
    @Queryable
    @DefaultValue("Ellen")
    public Property&lt;String&gt; firstname;

    @Nullable
    @Queryable
    @DefaultValue("Ripley")
    public Property&lt;String&gt; lastname;

    public ManyAssociation&lt;Contact&gt; friends;
}
</code></pre>


## UI data binding

...

## Asynchronous programming with Promise

...

