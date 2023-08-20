# Welcome to Areca

This is a small demo of the Areca toolkit. It is meant to showcase the **UI** and give a brief overview of the **programming**. You can <a href="#flip">flip</a> the page at any time to switch between general description and some code examples.

## What is Areca?

### TL;DR

Areca is a toolkit build on top of the great [TeaVM](http://teavm.org). It is meant to:

> - build apps for **mobile** and **desktop**
> - with a **simple API**
> - just **Java** (or Kotlin, Groovy, Scala, ...)
> - no HTML, XML, JavaScript

### Motivation

When I started to make apps for mobile devices I first looked into Android and I found that building Android apps with Gradle is unbelievable slow. Really not fun to work with. Next bummer: Android development is tied to IntelliJ, which I don't like. And, most important: it is just one platform, no desktop, no iOS.

I really like to use the Browser as a platform independent environment - but I don't like the idea to program in JavaScript (or TypeScript, React, Angular, whatever). Then I discovered [TeaVM](http://teavm.org). I was really excited about it. It seemed possible to have ...

So I started to learn, experiment and test to figure out what's possible with TeaVM and what is missing to build apps with minimal effort. The result is the Areca toolkit. Below is a brief overview of the main parts of Areca.

## UI components

The UI components are the basic foundation of the entire UI. 

> - **Basic**: Text, Button, Input, ...
> - **Composites**: consists of elements and layout them

The **hierarchy** of composites and components that form an actual application is described in pure Java. Have a look at the <a href="#flip">code example</a> to get an idea how it feels.

The **implementation** is simple yet powerful. It uses asynchronous events to render the components to HTML. Those events are processed inside [requestAnimationFrame callbacks](https://developer.mozilla.org/en-US/docs/Web/API/window/requestAnimationFrame). This prevents the UI from lagging during updates.

## Pageflow

The Pageflow is the standard 'windowing' system. It provides basic UI abstractions for an application. Things like Pages (equivalent to a window), Dialogs, Toolbars, Actions, etc.

Just <a href="#open">open a new Page</a> to see it working.


## Async programming

...

## Data modeling

...

<h2 id="last">Last</h2>

...
<br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/>
...
