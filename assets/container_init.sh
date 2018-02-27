#! /bin/sh -

set -e

sh inject_configuration.sh /usr/local/tomee/webapps/ikats-ingestion.war

exec catalina.sh run