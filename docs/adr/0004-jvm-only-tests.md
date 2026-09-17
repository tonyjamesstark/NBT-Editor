# Automated tests cover pure-JVM logic only; client behaviour is verified by hand

## Status

accepted

## Context

Almost everything in this mod runs inside a Minecraft client. Registries, `Minecraft.getInstance()`
and the run directory do not exist in a bare JVM, so a plain JUnit test can only reach code that
does not touch them. Covering the rest would mean standing up a Fabric launch inside the test
task, which is a real harness to build and maintain against a game that changes shape every
release.

## Decision

`./gradlew build` runs JUnit 5 over the pure-JVM slice only: text formatting and wrapping, search,
tooltip placement, integer field parsing, the partitioned read-write lock, and client-chest page
read/write against a temp directory. Everything else is verified by running the mod in a dev
client.

New pure logic is expected to arrive with tests, and those tests are mutation-verified: break one
line, confirm the expected tests fail, restore.

## Consequences

CI proves the thing compiles and remaps, plus that slice. It does not prove the mod works. A
change to a screen, a container adapter, or anything that reads the registry is unverified until
somebody launches the client, and "the build is green" is not evidence about those paths.

This is also why extracting pure logic out of client-coupled classes has value beyond tidiness:
it is the only way to move code across the line into what can be tested at all.
