# Docker image for Performance Client

This folder contains "dockerfile" to build docker image with our distribution.

## To build

From root execute command:

```
$ docker build --rm --tag takari/nexus-perf dockerfile/
```

## To run

From root execute command:

```
docker run -d --name nexus-perf1 takari/nexus-perf:latest
```

## OSX loops

If you are on OSX, you have some extra loops to jump due to docker setup (it actually
runs on a VM, not on your host).

TBD
