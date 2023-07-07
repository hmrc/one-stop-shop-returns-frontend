import sbt._

object AppDependencies {
  import play.core.PlayVersion

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"       %% "play-frontend-hmrc"             % "7.1.0-play-28",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping"  % "1.12.0-play-28",
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28"     % "7.15.0",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"             % "1.3.0",
    "uk.gov.hmrc"       %% "domain"                         % "8.1.0-play-28",
    "org.typelevel"     %% "cats-core"                      % "2.7.0"
  )

  val test = Seq(
    "org.scalatest"           %% "scalatest"               % "3.2.12",
    "org.scalatestplus"       %% "scalacheck-1-15"         % "3.2.11.0",
    "org.scalatestplus"       %% "mockito-3-4"             % "3.2.10.0",
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"  % "7.15.0",
    "org.scalatestplus.play"  %% "scalatestplus-play"      % "5.1.0",
    "org.pegdown"             %  "pegdown"                 % "1.6.0",
    "org.jsoup"               %  "jsoup"                   % "1.14.3",
    "com.typesafe.play"       %% "play-test"               % PlayVersion.current,
    "org.mockito"             %% "mockito-scala"           % "1.17.7",
    "org.scalacheck"          %% "scalacheck"              % "1.16.0",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28" % "1.3.0",
    "com.vladsch.flexmark"    %  "flexmark-all"            % "0.62.2",
    "com.github.tomakehurst"  %  "wiremock-standalone"     % "2.27.2"
  ).map(_ % "test, it")

  def apply(): Seq[ModuleID] = compile ++ test
}
