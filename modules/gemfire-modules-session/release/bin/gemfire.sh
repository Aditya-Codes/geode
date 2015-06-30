#!/bin/bash
# Set GEMFIRE to the product toplevel directory
GEMFIRE=`dirname $0`
OLDPWD=$PWD
cd $GEMFIRE
GEMFIRE=`dirname $PWD`
cd $OLDPWD

if [ "x$WINDIR" != "x" ]; then
  echo "ERROR: The variable WINDIR is set indicating this script is running in a Windows OS, please use the .bat file version instead."
  exit 1
fi

if [ ! -f $GEMFIRE/lib/gemfire.jar ]; then
  echo "ERROR: Could not determine GEMFIRE location."
  exit 1
fi

GEMFIRE_JARS=$GEMFIRE/lib/gemfire.jar:$GEMFIRE/lib/antlr.jar

if [ "x$CLASSPATH" != "x" ]; then
  GEMFIRE_JARS=$GEMFIRE_JARS:$CLASSPATH
fi

# Command line args that start with -J will be passed to the java vm in JARGS.
# See java --help for a listing of valid vm args.
# Example: -J-Xmx1g sets the max heap size to 1 gigabyte.

JARGS=
GEMFIRE_ARGS=
for i in "$@"
do
  if [ "-J" == "${i:0:2}" ]
  then
    JARGS="${JARGS} \"${i#-J}\""
  else
    GEMFIRE_ARGS="${GEMFIRE_ARGS} \"${i}\""
  fi
done

eval ${GF_JAVA:-java} ${JAVA_ARGS} ${JARGS} -classpath ${GEMFIRE_JARS} com.gemstone.gemfire.internal.SystemAdmin ${GEMFIRE_ARGS}
