#!/bin/sh
cd `dirname $0`
rm -r target/areca.app-0.0.1-SNAPSHOT/WEB-INF/lib/areca.*
cp ../areca.common/target/areca.common-0.0.1-SNAPSHOT.jar target/areca.app-0.0.1-SNAPSHOT/WEB-INF/lib/

ls -l target/areca.app-0.0.1-SNAPSHOT/WEB-INF/lib
