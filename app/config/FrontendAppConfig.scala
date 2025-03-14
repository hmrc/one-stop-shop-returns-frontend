/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package config

import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.i18n.Lang
import play.api.mvc.RequestHeader
import uk.gov.hmrc.play.bootstrap.binders.SafeRedirectUrl

import java.net.URI

@Singleton
class FrontendAppConfig @Inject() (configuration: Configuration) {

  val host: String    = configuration.get[String]("host")
  val appName: String = configuration.get[String]("appName")
  val origin: String  = configuration.get[String]("origin")

  private val contactHost = configuration.get[String]("contact-frontend.host")
  private val contactFormServiceIdentifier = "one-stop-shop-returns-frontend"

  def feedbackUrl(implicit request: RequestHeader): String =
    s"$contactHost/contact/beta-feedback?service=$contactFormServiceIdentifier&backUrl=${SafeRedirectUrl(host + request.uri).encodedUrl}"

  val loginUrl: String         = configuration.get[String]("urls.login")
  val loginContinueUrl: String = configuration.get[String]("urls.loginContinue")
  val signOutUrl: String       = configuration.get[String]("urls.signOut")
  val ivUpliftUrl: String      = configuration.get[String]("urls.ivUplift")

  val allowedRedirectUrls: Seq[String] = configuration.get[Seq[String]]("urls.allowedRedirects")

  private val exitSurveyBaseUrl: String = configuration.get[String]("feedback-frontend.host") + configuration.get[String]("feedback-frontend.url")
  lazy val exitSurveyUrl: String        = s"$exitSurveyBaseUrl/${origin.toLowerCase}"

  val languageTranslationEnabled: Boolean =
    configuration.get[Boolean]("features.welsh-translation")

  val ossEnrolment: String       = configuration.get[String]("oss-enrolment")

  val ossEnrolmentEnabled: Boolean =
    configuration.get[Boolean]("features.oss-enrolment")

  def languageMap: Map[String, Lang] = Map(
    "en" -> Lang("en"),
    "cy" -> Lang("cy")
  )

  val timeout: Int   = configuration.get[Int]("timeout-dialog.timeout")
  val countdown: Int = configuration.get[Int]("timeout-dialog.countdown")

  val cacheTtl: Long = configuration.get[Long]("mongodb.timeToLiveInSeconds")
  val cachedVatReturnTtl: Int = configuration.get[Int]("mongodb.cachedTimeToLiveInSeconds")
  val saveForLaterTtl: Int = configuration.get[Int]("mongodb.saveForLaterTTLInDays")

  val auditLogins: Boolean = configuration.get[Boolean]("features.auditLogins")

  val changeYourRegistrationUrl: String = configuration.get[String]("urls.changeYourRegistration")

  val deleteAllFixedEstablishmentUrl: String = configuration.get[String]("urls.deleteAllFixedEstablishment")
  val leaveOneStopShopUrl: String = configuration.get[String]("urls.leaveOneStopShop")

  val cacheRegistrations: Boolean = configuration.get[Boolean]("features.cacheRegistrations")

  val amendRegistrationEnabled: Boolean = configuration.get[Boolean]("features.amendRegistrationEnabled")

  val rejoinThisService: String = configuration.get[String]("urls.rejoinThisService")

  val ivEvidenceStatusUrl: String =
    s"${configuration.get[Service]("microservice.services.identity-verification").baseUrl}/disabled-evidences?origin=$origin"

  private val ivJourneyServiceUrl: String =
    s"${configuration.get[Service]("microservice.services.identity-verification").baseUrl}/journey/"

  def ivJourneyResultUrl(journeyId: String): String = new URI(s"$ivJourneyServiceUrl$journeyId").toString

  val internalAuthToken: String = configuration.get[String]("internal-auth.token")

  val strategicReturnApiEnabled: Boolean = configuration.get[Boolean]("features.strategic-returns.enabled")

}
