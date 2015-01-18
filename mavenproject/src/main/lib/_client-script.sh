#!/bin/sh

if test -z "${JCM_INVOKE_CLASS}"
then
  echo "ERROR: JCM_INVOKE_CLASS environment variable is not set."
  exit 1
fi


# Capture the current working directory so that we can change to it later.
# Then capture the location of this script and the Directory Server instance
# root so that we can use them to create appropriate paths.

WORKING_DIR=`pwd`

cd `dirname "${0}"`
SCRIPT_DIR=`pwd`

cd ..
INSTANCE_ROOT=`pwd`
export INSTANCE_ROOT

cd "${WORKING_DIR}"


# See if JAVA_HOME is set.  If not, then see if there is a java executable in
# the path and try to figure it out.

if test -z "${JAVA_BIN}"
then
  if test -z "${JAVA_HOME}"
  then
    if test -f "${INSTANCE_ROOT}/lib/set-java-home"
    then
      . "${INSTANCE_ROOT}/lib/set-java-home"
      JAVA_BIN="${JAVA_HOME}/bin/java"
      export JAVA_BIN
    else
      JAVA_BIN=`which java 2> /dev/null`
      if test ${?} -eq 0
      then
        export JAVA_BIN
      else
        echo "Please set JAVA_HOME to the root of a Java 5 (or later) installation."
        exit 1
      fi
    fi
  else
    JAVA_BIN="${JAVA_HOME}/bin/java"
    export JAVA_BIN
  fi
fi


# Explicitly set the PATH, LD_LIBRARY_PATH, LD_PRELOAD, and other important
# system environment variables for security and compatibility reasons.

PATH=/bin:/usr/bin
LD_LIBRARY_PATH=
LD_LIBRARY_PATH_32=
LD_LIBRARY_PATH_64=
LD_PRELOAD=
LD_PRELOAD_32=
LD_PRELOAD_64=
export PATH LD_LIBRARY_PATH LD_LIBRARY_PATH_32 LD_LIBRARY_PATH_64 LD_PRELOAD LD_PRELOAD_32 LD_PRELOAD_34


# Configure the appropriate CLASSPATH.

CLASSPATH=${INSTANCE_ROOT}/classes
for JAR in ${INSTANCE_ROOT}/lib/*.jar
do
  CLASSPATH=${CLASSPATH}:${JAR}
done
CLASSPATH=${CLASSPATH}:${INSTANCE_ROOT}/lib
export CLASSPATH

# Launch the appropriate server utility.

"${JAVA_BIN}" ${JAVA_ARGS} "${JCM_INVOKE_CLASS}" "${@}"