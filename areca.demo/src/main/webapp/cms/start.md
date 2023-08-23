# Welcome to Areca

This is a short demo of the Areca toolkit. It is meant to showcase the **UI** and give a brief overview of the **programming**. You can **<a href="#flip">flip</a>** the page at any time to switch between general description and some code examples.

## What is Areca?

### TL;DR

Areca is a toolkit build on top of the great [TeaVM](http://teavm.org).

Areca is meant to:

> - build apps for **mobile** and **desktop**
> - with a **simple API**
> - using just **Java**

### Motivation

When I started to make apps for mobile devices I first looked into Android and I found that building Android apps with Gradle is unbelievable slow. Really not fun to work with. Next bummer: Android development is tied to IntelliJ, which I don't like. And, most important: it is just one platform, no desktop, no iOS.

I really like the idea to use the Browser as a platform independent environment - but I don't like to code JavaScript (or TypeScript, React, Angular, whatever). Then I discovered [TeaVM](http://teavm.org) and I started to learn and experiment to figure out what is possible with TeaVM and what is missing in order to be able to build apps with minimal effort. The result is the Areca toolkit. Below is a brief overview of some important parts of it.

## UI components

The UI components are the basic foundation of the entire UI.

Basically there are two kinds of UI elements:

- **Components**: like <a href="#components">Text, Button, Input</a>, etc.
- **Composites**: do the <a href="#layout">Layout</a>

The **hierarchy** of composites and components of an application is described in pure Java. Have a look at the <a href="#flip">code example</a> to get an idea how it feels. Nothing fancy happens here. Data binding, 'windowing' and other stuff is done in layers above. The UI components are just a way to build any kind of UI, with a simple API and allow for an efficient backend implementation.

The **implementation** is simple yet powerful. It uses [asynchronous event processing](https://developer.mozilla.org/en-US/docs/Web/API/window/requestAnimationFrame) to render the components to the DOM/HTML. This prevents the UI from lagging during updates.

## Pageflow

The Pageflow is the standard 'window' system. It provides basic UI abstractions for an application. Things like **Pages** (equivalent to a window), **Dialogs**, **Toolbars** and **Actions**. It looks and feels like a mobile UI but works also on the desktop. The Pageflow model is best suited for rather simple, list-detail kind of apps.


## Data modeling

The [Model2](https://github.com/Polymap4/polymap4-model) system helps to work with [Domain Models](http://en.wikipedia.org/wiki/Domain_model) and [Domain-driven Design](http://en.wikipedia.org/wiki/Domain-driven_design) (DDD) in Java. It allows to describe the various entities, their attributes, roles, and relationships, plus the constraints and concerns that govern the problem in pure Java syntax. No pre-processors or new language elements, just the standard Java platform. This work was heavily inspired by the early [Qi4j](http://qi4j.org/).

The Model2 code was ported to TeaVM to be able to use it with Areca. Also a backend based on the Browser's [IndexedDB](https://developer.mozilla.org/en-US/docs/Web/API/IndexedDB_API) was made. This allows to store and query Entities in the Browser environment.

## UI data binding

...

## Async programming

Everything that works with events in JavaScript is done via asynchronous APIs. Such APIs work with callback functions. Instead of waiting for a result and blocking the caller thread the client code registeres a callback that is called as soon as the result is available. While this is great to build non-blocking applications, the code is sometimes hard to read and maintain. Especially if a callback does anopther async operation, and another, etc.

The deal with this situation Areca provides a [Promise](https://github.com/fb71/areca/blob/master/areca.common/src/main/java/areca/common/Promise.java) class. A Promise represents the result(s) of an asynchronous computation and allows to chain several operations and handle exceptions.

<br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/>
