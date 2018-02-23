# The setenv.sh is sourced at start of the Catalina control script in catalina.sh
#
# See "Control Script for the CATALINA Server Environment Variable Prerequisites"
# in catalina.sh for explanation
#

# Defines the JMV options
export JAVA_OPTS="-Dlog4j.debug"

# Defines specific Tomcat options
# we define 20 Gigas Bytes for the instance 
# and JMX options unsecure options to connect the jvisualvm console
export CATALINA_OPTS="-Xms256m -Xmx512m \
        -Dcom.sun.management.jmxremote.port=9011 \
        -Dcom.sun.management.jmxremote.authenticate=false \
        -Dcom.sun.management.jmxremote.ssl=false"
      
# Add option to force the loading of webapp provided jar insteand of tomee jars
# For IKATS Ingestion, we need overrided versions of Apache Commons and HSQL DB (for unit testing)
export CATALINA_OPTS="$CATALINA_OPTS -Dopenejb.classloader.forced-load=org.apache.commons,org.hsqldb"
        
# Option to specify JPDA debbug through a specific port 
# Default 8000 port is used by Gunicorn.
export JPDA_ADDRESS=9010
