# Welcome to Areca

This is a short demo of the Areca toolkit. It is meant to showcase the **UI** and give a brief overview of the **programming**. You can <a href="#flip">flip</a> the page at any time to switch between general description and some code examples.

## What is Areca?

### TL;DR

Areca is a toolkit build on top of the great [TeaVM](http://teavm.org).

Areca is meant to:

> - build apps for **mobile** and **desktop**
> - with a **simple API**
> - just **Java**
> - no HTML, XML, JavaScript

### Motivation

When I started to make apps for mobile devices I first looked into Android and I found that building Android apps with Gradle is unbelievable slow. Really not fun to work with. Next bummer: Android development is tied to IntelliJ, which I don't like. And, most important: it is just one platform, no desktop, no iOS.

I really like the idea to use the Browser as a platform independent environment - but I don't like code JavaScript (or TypeScript, React, Angular, whatever). Then I discovered [TeaVM](http://teavm.org) and I immediatelly started to learn and experiment to figure out what's possible with TeaVM and what is missing in order to be able to build apps with minimal effort. The result is the Areca toolkit. Below is a brief overview of the main parts of Areca.

## UI components

The UI components are the basic foundation of the entire UI.

Basically there are two kinds of UI elements:

- **Components**: like <a href="#components">Text, Button, Input</a>, and 
- **Composites**: do the <a href="#layout">Layout</a>

The **hierarchy** of composites and components of an application is described in pure Java. Have a look at the <a href="#flip">code example</a> to get an idea how it feels.

The **implementation** is simple yet powerful. It uses asynchronous events to render the components to HTML. Those events are processed inside [requestAnimationFrame callbacks](https://developer.mozilla.org/en-US/docs/Web/API/window/requestAnimationFrame). This prevents the UI from lagging during updates.

## Pageflow

The Pageflow is the standard 'window' system. It provides basic UI abstractions for an application. Things like **Pages** (equivalent to a window), **Dialogs**, **Toolbars**, **Actions**, etc.

Just <a href="#open">open a new Page</a> to see it working.


## Async programming

Everything that works with events in JavaScript is done via asynchronous APIs. Such APIs work with callback functions. Instead of waiting for a result and blocking the caller thread the client code registeres a callback that is called as soon as the result is available. While this is great to build non-blocking applications, the code is sometimes hard to read and maintain. Especially if a callback does anopther async operation, and another, etc.

The deal with this situation Areca provides a [Promise](https://github.com/fb71/areca/blob/master/areca.common/src/main/java/areca/common/Promise.java) class. A Promise represents the result(s) of an asynchronous computation and allows to chain several operations.

## Data modeling

...

## UI data binding

...

<br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/>
