#!/bin/bash
#
# Copyright (c) 2007-2013 Sonatype, Inc. All rights reserved.
#
# This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
# which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
#


scenario=${1:-src/standard-data}

timestamp=$(date '+%Y%m%d-%H%M%S')

mkdir $scenario/logs

java -cp target/*-jar-with-dependencies.jar \
   com.sonatype.nexus.perftest.PerformanceTestRunner \
   $scenario 2>&1 | tee $scenario/logs/run-$timestamp.log
