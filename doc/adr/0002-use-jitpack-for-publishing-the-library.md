# 2. Use Jitpack for publishing the library

Date: 2019-07-21

## Status

Accepted

## Context

We need to find a convenient way to publish this as an artifact.
I explored configuring Bintray, Mavencentral, and other more "traditional" repositories, but that 
turned out to be a bit of a pain.

## Decision

Publish using Jitpack. Jitpack can be used to build based on git tagged versions with near zero config.

## Consequences

Publishing with Jitpack is easy and required only authorizing it with Github. Maybe people find 
mavencentral or jfrog or bintray to be a bit more trustable/discoverable but jitpack is good for now!
