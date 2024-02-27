import sbt.*

object AppDependencies {
  import play.core.PlayVersion

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"       %% "play-frontend-hmrc-play-30"             % "8.5.0",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping-play-30"  % "2.0.0",
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-30"             % "8.4.0",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"                     % "1.7.0",
    "uk.gov.hmrc"       %% "domain-play-30"                         % "9.0.0",
    "org.typelevel"     %% "cats-core"                              % "2.9.0"
  )

  val test = Seq(
    "org.scalatest"           %% "scalatest"               % "3.2.15",
    "org.scalatestplus"       %% "scalacheck-1-15"         % "3.2.11.0",
    "org.scalatestplus"       %% "mockito-4-6"             % "3.2.15.0",
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"  % "8.4.0",
    "org.scalatestplus.play"  %% "scalatestplus-play"      % "5.1.0",
    "org.pegdown"             %  "pegdown"                 % "1.6.0",
    "org.jsoup"               %  "jsoup"                   % "1.15.4",
    "org.playframework"       %% "play-test"               % PlayVersion.current,
    "org.mockito"             %% "mockito-scala"           % "1.17.30",
    "org.scalacheck"          %% "scalacheck"              % "1.17.0",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30" % "1.7.0",
    "com.vladsch.flexmark"    %  "flexmark-all"            % "0.64.6"
  ).map(_ % "test, it")

  def apply(): Seq[ModuleID] = compile ++ test
}
