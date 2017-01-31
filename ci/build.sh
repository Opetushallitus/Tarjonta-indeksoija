#!/bin/sh

export JAVA_HOME="${bamboo_capability_system_jdk_JDK_1_8}"
export PATH=$JAVA_HOME/bin:$PATH

test() {
  ./lein clean
  ./lein compile
  ./lein ci-test
}

uberjar() {
  ./lein clean
  mkdir ./resources/public
  echo "artifactId=tarjonta-indeksoija-service" > ./resources/public/buildversion.txt
  echo "version=0.1.0-SNAPSHOT" >> ./resources/public/buildversion.txt
  echo "buildNumber=$bamboo_buildNumber" >> ./resources/public/buildversion.txt
  echo "vcsRevision=$(git rev-parse HEAD)" >> ./resources/public/buildversion.txt
  echo "buildTime=$bamboo_buildTimeStamp" >> ./resources/public/buildversion.txt
  ./lein create-uberjar
}

command="$1"

case "$command" in
    "test" )
        test
        ;;
    "uberjar" )
        uberjar
        ;;
    *)
        echo "Unknown command $command"
        ;;
esac