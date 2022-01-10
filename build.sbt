import Options._


inThisBuild(Seq(
  organization := "io.github.outwatch",

  crossScalaVersions := Seq("2.12.15", "2.13.6", "3.1.1-RC1"),
  scalaVersion := crossScalaVersions.value.last,

  licenses += ("Apache 2", url("https://www.apache.org/licenses/LICENSE-2.0.txt")),
  homepage := Some(url("https://outwatch.github.io/")),
  scmInfo := Some(ScmInfo(
    url("https://github.com/OutWatch/outwatch"),
    "scm:git:git@github.com:OutWatch/outwatch.git",
    Some("scm:git:git@github.com:OutWatch/outwatch.git"))
  ),

  pomExtra :=
    <developers>
        <developer>
        <id>jk</id>
        <name>Johannes Karoff</name>
        <url>https://github.com/cornerman</url>
        </developer>
        <developer>
        <id>fx</id>
        <name>Felix Dietze</name>
        <url>https://github.com/fdietze</url>
        </developer>
        <developer>
        <id>ltj</id>
        <name>Luka Jacobowitz</name>
        <url>https://github.com/LukaJCB</url>
        </developer>
    </developers>,
))

val jsdomVersion = "13.2.0"
val silencerVersion = "1.7.7"
val colibriVersion = "0.2.2"

val isDotty = Def.setting(
  CrossVersion.partialVersion(scalaVersion.value).exists(_._1 == 3)
)

lazy val commonSettings = Seq(
  useYarn := true,

  libraryDependencies ++= Seq(
    "org.scalatest" %%% "scalatest" % "3.2.10" % Test,
  ),

  scalacOptions ++= CrossVersion.partialVersion(scalaVersion.value).map(v =>
    allOptionsForVersion(s"${v._1}.${v._2}", true)
  ).getOrElse(Nil),
  Compile / console / scalacOptions ~= (_.diff(badConsoleFlags)),
  Test / scalacOptions --= Seq("-Xfatal-warnings"), // allow usage of deprecated calls in tests

  scalacOptions ++= (if (isDotty.value) Seq("-Ykind-projector")
  else
    Seq()),

  libraryDependencies ++= (if (isDotty.value) Nil
  else
    Seq(
      compilerPlugin(("org.typelevel" %% "kind-projector" % "0.13.2").cross(CrossVersion.full))
      ))

)

lazy val outwatchReactive = project
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .in(file("reactive"))
  .settings(commonSettings)
  .settings(
    name := "OutWatch-Reactive",
    normalizedName := "outwatch-reactive",

    libraryDependencies ++= Seq(
      "com.github.cornerman" %%% "colibri" % colibriVersion,
    )
  )

lazy val outwatchUtil = project
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .dependsOn(outwatch)
  .in(file("util"))
  .settings(commonSettings)
  .settings(
    name := "OutWatch-Util",
    normalizedName := "outwatch-util",
  )

lazy val outwatchMonix = project
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .dependsOn(outwatch)
  .in(file("monix"))
  .settings(commonSettings)
  .settings(
    name := "OutWatch-Monix",
    normalizedName := "outwatch-monix",

    libraryDependencies ++= Seq(
      "com.github.cornerman" %%% "colibri-monix" % colibriVersion,
    )
  )

lazy val outwatchRepairDom = project
  .in(file("repairdom"))
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .dependsOn(outwatch)
  .settings(commonSettings)
  .settings(
    name := "OutWatch-RepairDom",
    normalizedName := "outwatch-repairdom",
  )

lazy val outwatchSnabbdom = project
  .in(file("snabbdom"))
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .settings(commonSettings)
  .settings(
    name := "OutWatch-Snabbdom",
    normalizedName := "outwatch-snabbdom",

    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "2.1.0"
    ),

    Compile/npmDependencies ++= Seq(
      "snabbdom" -> "git://github.com/outwatch/snabbdom.git#semver:0.7.5"
    )
  )

lazy val outwatch = project
  .in(file("outwatch"))
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .dependsOn(outwatchSnabbdom, outwatchReactive)
  .settings(commonSettings)
  .settings(
    name := "OutWatch",
    normalizedName := "outwatch",

    libraryDependencies ++= Seq(
      "com.raquo"            %%% "domtypes" % "0.15.1",
      "com.github.cornerman" %%% "colibri-jsdom" % colibriVersion,
    )
  )

lazy val tests = project
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .dependsOn(outwatchMonix, outwatchUtil, outwatchRepairDom)
  .settings(commonSettings)
  .settings(
    publish/skip := true,

    Test/requireJsDomEnv := true,
    installJsdom/version := jsdomVersion,
  )

lazy val bench = project
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .dependsOn(outwatchMonix)
  .settings(
    publish/skip := true,

    resolvers ++=
      ("jitpack" at "https://jitpack.io") ::
      Nil,

    libraryDependencies ++=
      "com.github.fdietze.bench" %%% "bench" % "d411db1" ::
      Nil,

    Compile/scalaJSStage := FullOptStage,
    scalacOptions ++= Seq ("-Xdisable-assertions"),

    useYarn := true,

    Compile/npmDependencies ++= Seq(
      "jsdom" -> jsdomVersion
    ),
  )

lazy val jsdocs = project
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .dependsOn(outwatch)
  .settings(
    webpackBundlingMode := BundlingMode.LibraryOnly(),
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "2.1.0",
      "com.github.cornerman" %%% "colibri-monix" % colibriVersion,
      /* "com.github.cornerman" %%% "colibri-rx" % colibriVersion, */
    ),
    Compile / npmDependencies ++= Seq(
      "js-beautify" -> "1.14.0"
    )
  )

lazy val docs = project
  .in(file("outwatch-docs")) // important: it must not be docs/
  .enablePlugins(MdocPlugin, DocusaurusPlugin)
  .settings(
    test/skip := true,
    publish/skip  := true,
    moduleName := "outwatch-docs",
    mdocJS := Some(jsdocs),
    mdocJSLibraries := (jsdocs / Compile / fullOptJS / webpack).value,
    mdocVariables := Map(
      /* TODO: "SCALAJSVERSION" -> scalaJSVersions.current, */
      "VERSION" -> version.value,
      "REPOURL" -> "https://github.com/OutWatch/outwatch/blob/master",
      "js-mount-node" -> "docPreview"
    ),
    libraryDependencies += "org.scala-js" %% "scalajs-linker" % scalaJSVersion,
    libraryDependencies += {
      scalaBinaryVersion.value match {
        // keep these pinned to mdoc.js Scala versions
        // scala-steward:off
        case "2.12" => "org.scala-js" %% "scalajs-compiler" % scalaJSVersion cross CrossVersion.constant("2.12.15")
        case "2.13" => "org.scala-js" %% "scalajs-compiler" % scalaJSVersion cross CrossVersion.constant("2.13.6")
        case "3"    => "org.scala-js" %% "scalajs-compiler" % scalaJSVersion cross CrossVersion.constant("3.1.1-RC1")
        // scala-steward:on
      }
    }
  )

lazy val root = project
  .in(file("."))
  .settings(
    name := "outwatch-root",
    publish/skip := true,
  )
  .aggregate(outwatch, outwatchMonix, outwatchSnabbdom, outwatchReactive, outwatchUtil, outwatchRepairDom, tests)
