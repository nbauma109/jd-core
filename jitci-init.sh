#!/usr/bin/env bash
curl -vkL https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.8.1%2B1/OpenJDK17U-jdk_x64_linux_hotspot_17.0.8.1_1.tar.gz -o $HOME/OpenJDK17U-jdk_x64_linux_hotspot_17.0.8.1_1.tar.gz
tar xzvf $HOME/OpenJDK17U-jdk_x64_linux_hotspot_17.0.8.1_1.tar.gz -C $HOME
export JAVA_HOME=$HOME/jdk-17.0.8.1+1
export M3_VERSION=3.8.8
#export M3_VERSION=$(curl -Ls -o /dev/null -w %{url_effective} https://github.com/apache/maven/releases/latest | sed 's,https://github.com/apache/maven/releases/tag/maven-,,g')
curl -vkL https://archive.apache.org/dist/maven/maven-3/${M3_VERSION}/binaries/apache-maven-${M3_VERSION}-bin.zip -o $HOME/apache-maven-${M3_VERSION}-bin.zip
unzip -o $HOME/apache-maven-${M3_VERSION}-bin.zip -d $HOME
$HOME/apache-maven-${M3_VERSION}/bin/mvn -B de.qaware.maven:go-offline-maven-plugin:resolve-dependencies --no-transfer-progress
