#!/bin/bash
# 
# The Nutch command script
#
# Environment Variables
#
#   NUTCH_JAVA_HOME The java implementation to use.  Overrides JAVA_HOME.
#
#   NUTCH_HEAPSIZE  The maximum amount of heap to use, in MB. 
#                   Default is 1000.
#
#   NUTCH_OPTS      Extra Java runtime options.
#

# resolve links - $0 may be a softlink
THIS="$0"
while [ -h "$THIS" ]; do
  ls=`ls -ld "$THIS"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '.*/.*' > /dev/null; then
    THIS="$link"
  else
    THIS=`dirname "$THIS"`/"$link"
  fi
done

# if no args specified, show usage
if [ $# = 0 ]; then
  echo "Usage: nutch COMMAND"
  echo "where COMMAND is one of:"
  echo "  crawl             one-step crawler for intranets"
  echo "  readdb            read / dump crawl db"
  echo "  readlinkdb        read / dump link db"
  echo "  inject            inject new urls into the database"
  echo "  generate          generate new segments to fetch"
  echo "  fetch             fetch a segment's pages"
  echo "  parse             parse a segment's pages"
  echo "  segread           read / dump segment data"
  echo "  updatedb          update crawl db from segments after fetching"
  echo "  invertlinks       create a linkdb from parsed segments"
  echo "  index             run the indexer on parsed segments and linkdb"
  echo "  merge             merge several segment indexes"
  echo "  dedup             remove duplicates from a set of segment indexes"
  echo "  plugin            load a plugin and run one of its classes main()"
  echo "  server            run a search server"
  echo " or"
  echo "  CLASSNAME         run the class named CLASSNAME"
  echo "Most commands print help when invoked w/o parameters."
  exit 1
fi

# get arguments
COMMAND=$1
shift

# some directories
THIS_DIR=`dirname "$THIS"`
NUTCH_HOME=`cd "$THIS_DIR/.." ; pwd`

# some Java parameters
if [ "$NUTCH_JAVA_HOME" != "" ]; then
  #echo "run java in $NUTCH_JAVA_HOME"
  JAVA_HOME=$NUTCH_JAVA_HOME
fi
  
if [ "$JAVA_HOME" = "" ]; then
  echo "Error: JAVA_HOME is not set."
  exit 1
fi

JAVA=$JAVA_HOME/bin/java
JAVA_HEAP_MAX=-Xmx1024m 
NUTCH_OPTS="$NUTCH_OPTS -XX:MaxPermSize=128m"

# check envvars which might override default args
if [ "$NUTCH_HEAPSIZE" != "" ]; then
  #echo "run with heapsize $NUTCH_HEAPSIZE"
  JAVA_HEAP_MAX="-Xmx""$NUTCH_HEAPSIZE""m"
  #echo $JAVA_HEAP_MAX
fi

# CLASSPATH initially contains $NUTCH_CONF_DIR, or defaults to $NUTCH_HOME/conf
CLASSPATH=${NUTCH_CONF_DIR:=$NUTCH_HOME/conf}
CLASSPATH=${CLASSPATH}:$JAVA_HOME/lib/tools.jar

# so that filenames w/ spaces are handled correctly in loops below
IFS=

# for developers, add plugins, job & test code to CLASSPATH
if [ -d "$NUTCH_HOME/build/plugins" ]; then
  CLASSPATH=${CLASSPATH}:$NUTCH_HOME/build
fi
for f in $NUTCH_HOME/build/nutch-*.job; do
  CLASSPATH=${CLASSPATH}:$f;
done
if [ -d "$NUTCH_HOME/build/test/classes" ]; then
  CLASSPATH=${CLASSPATH}:$NUTCH_HOME/build/test/classes
fi

# for releases, add Nutch job to CLASSPATH
for f in $NUTCH_HOME/nutch-*.job; do
  CLASSPATH=${CLASSPATH}:$f;
done

# add plugins to classpath
if [ -d "$NUTCH_HOME/plugins" ]; then
  CLASSPATH=${NUTCH_HOME}:${CLASSPATH}
fi

# add libs to CLASSPATH
for f in $NUTCH_HOME/lib/*.jar; do
  CLASSPATH=${CLASSPATH}:$f;
done

for f in $NUTCH_HOME/lib/admin-ui-ext/*.jar; do
  CLASSPATH=${CLASSPATH}:$f;
done

for f in $NUTCH_HOME/lib/ingrid-ext/*.jar; do
  CLASSPATH=${CLASSPATH}:$f;
done

for f in $NUTCH_HOME/lib/jetty-ext/*.jar; do
  CLASSPATH=${CLASSPATH}:$f;
done

for f in $NUTCH_HOME/*.jar; do
  CLASSPATH=${CLASSPATH}:$f;
done


# restore ordinary behaviour
unset IFS

# figure out which class to run
if [ "$COMMAND" = "crawl" ] ; then
  CLASS=org.apache.nutch.crawl.Crawl
elif [ "$COMMAND" = "inject" ] ; then
  CLASS=org.apache.nutch.crawl.Injector
elif [ "$COMMAND" = "generate" ] ; then
  CLASS=org.apache.nutch.crawl.Generator
elif [ "$COMMAND" = "fetch" ] ; then
  CLASS=org.apache.nutch.fetcher.Fetcher
elif [ "$COMMAND" = "parse" ] ; then
  CLASS=org.apache.nutch.parse.ParseSegment
elif [ "$COMMAND" = "readdb" ] ; then
  CLASS=org.apache.nutch.crawl.CrawlDbReader
elif [ "$COMMAND" = "readlinkdb" ] ; then
  CLASS=org.apache.nutch.crawl.LinkDbReader
elif [ "$COMMAND" = "segread" ] ; then
  CLASS=org.apache.nutch.segment.SegmentReader
elif [ "$COMMAND" = "updatedb" ] ; then
  CLASS=org.apache.nutch.crawl.CrawlDb
elif [ "$COMMAND" = "invertlinks" ] ; then
  CLASS=org.apache.nutch.crawl.LinkDb
elif [ "$COMMAND" = "index" ] ; then
  CLASS=org.apache.nutch.indexer.Indexer
elif [ "$COMMAND" = "dedup" ] ; then
  CLASS=org.apache.nutch.indexer.DeleteDuplicates
elif [ "$COMMAND" = "merge" ] ; then
  CLASS=org.apache.nutch.indexer.IndexMerger
elif [ "$COMMAND" = "plugin" ] ; then
  CLASS=org.apache.nutch.plugin.PluginRepository
elif [ "$COMMAND" = "server" ] ; then
  CLASS='org.apache.nutch.searcher.DistributedSearch$Server'
else
  CLASS=$COMMAND
fi

# cygwin path translation
if expr `uname` : 'CYGWIN*' > /dev/null; then
  CLASSPATH=`cygpath -p -w "$CLASSPATH"`
fi

# run it
exec "$JAVA" $JAVA_HEAP_MAX $NUTCH_OPTS -Dpid=$$ -classpath "$CLASSPATH" $CLASS "$@"
# run it in debugging mode
#exec "$JAVA" $JAVA_HEAP_MAX $NUTCH_OPTS -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=5000,server=y,suspend=n -Dpid=$$ -classpath "$CLASSPATH" $CLASS "$@"

