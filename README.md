## App

Areca is an [application](areca/tree/master/areca.app) that helps people to collaborate on projects. It provides means to organize and share messages and documents. Users can use the messenger backend of their choice, including Email, Signal, Threema, etc. Areca runs on the client device only - no server is needed. All data is replicated between the devices automatically.

## Framework

Areca is also a software **framework** that allows to build applications for mobile and browser using Java or other JVM languages. It is based on [TeaVM](http://teavm.org/). The framework provides

  * (Data modeling framework)[Polymap4/polymap4-model/tree/areca]
  * Component based (UI)[]
  * ...

## Build

#### Prerequisites

  * Java (_version >= 13_)
  * Maven
  * GIT

#### Build platform independant part

  * `git clone https://github.com/fb71/areca.git`
  * `git clone -b areca https://github.com/Polymap4/polymap4-model.git`
  * `cd areca`
  * `mvn package`
