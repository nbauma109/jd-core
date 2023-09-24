#!/usr/bin/env bash
export M3_VERSION=$(curl -Ls -o /dev/null -w %{url_effective} https://github.com/apache/maven/releases/latest | sed 's,https://github.com/apache/maven/releases/tag/maven-,,g')
export JAVA_HOME=$HOME/jdk-17.0.8.1+1
$HOME/apache-maven-${M3_VERSION}/bin/mvn -B deploy -DskipTests -Dfindbugs.skip=true -Dpmd.skip=true -Dcheckstyle.skip=true -Dsigning.disabled=true -DaltDeploymentRepository=jitci::default::file:///home/jitpack/deploy --no-transfer-progress