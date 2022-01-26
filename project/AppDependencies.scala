import sbt._

object AppDependencies {
  import play.core.PlayVersion

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"       %% "play-frontend-hmrc"             % "2.0.0-play-28",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping"  % "1.10.0-play-28",
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28"     % "5.20.0",
    "uk.gov.hmrc"       %% "play-language"                  % "5.1.0-play-28",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"             % "0.59.0",
    "uk.gov.hmrc"       %% "domain"                         % "7.0.0-play-28",
    "org.typelevel"     %% "cats-core"                      % "2.7.0"
  )

  val test = Seq(
    "org.scalatest"           %% "scalatest"               % "3.2.10",
    "org.scalatestplus"       %% "scalacheck-1-15"         % "3.2.11.0",
    "org.scalatestplus"       %% "mockito-3-4"             % "3.2.10.0",
    "org.scalatestplus.play"  %% "scalatestplus-play"      % "5.1.0",
    "org.pegdown"             %  "pegdown"                 % "1.6.0",
    "org.jsoup"               %  "jsoup"                   % "1.14.3",
    "com.typesafe.play"       %% "play-test"               % PlayVersion.current,
    "org.mockito"             %% "mockito-scala"           % "1.17.0",
    "org.scalacheck"          %% "scalacheck"              % "1.15.4",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28" % "0.59.0",
    "com.vladsch.flexmark"    %  "flexmark-all"            % "0.62.2",
    "com.github.tomakehurst"  %  "wiremock-standalone"     % "2.27.2"
  ).map(_ % "test, it")

  def apply(): Seq[ModuleID] = compile ++ test
}
