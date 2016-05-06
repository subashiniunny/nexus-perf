<!--

    Copyright (c) 2007-2013 Sonatype, Inc. All rights reserved.

    This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
    which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.

-->
## Nexus Performance Testing Library

A Sonatype Nexus quick & dirty performance regression and stress test library.

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.takari.nexus/nexus-perf/badge.svg?subject=io.takari.nexus:nexus-perf)](https://maven-badges.herokuapp.com/maven-central/io.takari.nexus/nexus-perf)

### Running

Grab a tarball, grab the uber jar and let's run it!

```
  java -jar nexus-perf-1.0.0-jar-with-dependencies.jar DIRECTORY
```

Where DIRECTORY is a directory you got by untarring the data tarball.
This build produces one, but you can produce many more if want.

### Running from git repository

To run test scenario and record performance metrics in db
(obviously, use actual baseline version).

    ./runtest.sh sample-scenario 2.4.0-09

To run test scenario, record performance metrics and the db
and compare performance to an earlier scenario run

    ./runtest.sh sample-scenario 2.5.0-03 2.4.0-09

To run test scenario, compare performance to an earlier scenario run,
do not record metrics in the db. Useful to test scenario itself

    ./runtest.sh sample-scenario - 2.4.0-09

Environment variables:
* NEXUS_URL - the URL where Nexus runs, if not set defaults to `http://localhost:8081/nexus`
* NEXUS_USERNAME - the username to use, if not set defaults to `admin`
* NEXUS_PASSWORD - the password to use, if not set defaults to `admin123`

### How it works

Using the details in the scenario xml file, the program spins up request threads for nexus. During the scenario run,
metrics are captured. At scenario end, metrics are stored in a local h2 database. If asked, the program will compare
these new metrics with a previous run, and fail if the metrics are outside a threshold.

You can only tell this library:

- where you nexus lives
- what URLs to access
- authentication to use
- number of simulated clients
- rate of requests

### Creating Scenarios

Scenarios are defined using xml files in the scenarios directory. Use existing scenarios as an example or review
the code.

### Adding Scenario Data

CSV and standard NCSA log files ( tar/gzipped ) can be parsed to simulate actual requests.

### Configuring your Nexus Under Test

Setting up Nexus is up to you! This library does not aim to help you with that.

### Building

mvn clean install

This creates an uber jar in target which contains all the needed dependencies. Then packages up the uber-jar and
the data/scenarios into tarballs.

