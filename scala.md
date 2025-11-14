## new a project with scala and sbt
```bash
sbt new scala/scala-seed.g8
cd <project-name>
sbt run
```
## add dependencies
In `build.sbt`, add:
```scala
libraryDependencies += "org.typelevel" %% "cats-core" % "2.9.0"
```
Then run:
```bash
sbt update
```
## a gRPC service with zio
In `build.sbt`, add:
```scala
libraryDependencies += "dev.zio" %% "zio-grpc-core" % "0.6.0"
```
Create a proto file `src/main/protobuf/helloworld.proto`:
```proto
syntax = "proto3";
package helloworld;
service Greeter {
  rpc SayHello (HelloRequest) returns (HelloReply);
}
message HelloRequest {
  string name = 1;
}
message HelloReply {
  string message = 1;
}
```
Generate Scala code using sbt plugin `sbt-protoc`.
