# Welcome to Areca

This is a small demo of the Areca toolkit. It is meant to showcase the **UI** and give a brief overview of the **programming**.

## What is Areca?

Areca is a toolkit build on top of the great [TeaVM](http://teavm.org).

- For mobile and desktop
- Easy API
- Just **100% Java** (or Kotlin, Groovy, Scala, ...)
- No HTML, XML, JavaScript
- ...

## Code Example

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