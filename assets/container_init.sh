#! /bin/sh -

set -e

sh inject_configuration.sh /usr/local/tomee/webapps/ikats-ingestion.war

# Launch TomEE with JPDA debugging activated if REMOTE_DEBUG is set
exec catalina.sh ${REMOTE_DEBUG:+jpda} run