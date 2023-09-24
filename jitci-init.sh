#!/usr/bin/env bash
export JAVA_HOME=$HOME/tools/jdk17/jdk-17.0.1
export M3_VERSION=$(curl -Ls -o /dev/null -w %{url_effective} https://github.com/apache/maven/releases/latest | sed 's,https://github.com/apache/maven/releases/tag/maven-,,g')
curl -vkL https://archive.apache.org/dist/maven/maven-3/${M3_VERSION}/binaries/apache-maven-${M3_VERSION}-bin.zip -o $HOME/apache-maven-${M3_VERSION}-bin.zip
unzip -o $HOME/apache-maven-${M3_VERSION}-bin.zip -d $HOME
$HOME/apache-maven-${M3_VERSION}/bin/mvn -B de.qaware.maven:go-offline-maven-plugin:resolve-dependencies --no-transfer-progress
