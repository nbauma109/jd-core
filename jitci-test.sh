#!/usr/bin/env bash
export M3_VERSION=$(curl -Ls -o /dev/null -w %{url_effective} https://github.com/apache/maven/releases/latest | sed 's,https://github.com/apache/maven/releases/tag/maven-,,g')
export JAVA_HOME=$HOME/jdk-17.0.8.1+1
export PATH=$HOME/apache-maven-${M3_VERSION}/bin:$PATH
mvn -B test --no-transfer-progress
