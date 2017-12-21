
name := "akka-memory-db"

version := "0.1"

scalaVersion := "2.12.4"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-Ywarn-unused-import", "-language:_")

lazy val akkaVersion = "2.5.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-metrics" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
)

scalacOptions in Test ++= (scalacOptions in Compile).value :+ "-Yrangepos"
fork in Test := true

fork in run := true
connectInput in run := true

cancelable in Global := true

lazy val root = (project in file(".")).enablePlugins(DockerPlugin, JavaAppPackaging)

javaOptions in run ++= Seq("-Xmx5g")