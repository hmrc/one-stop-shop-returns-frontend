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

package controllers.actions

import base.SpecBase
import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.RegistrationConnector
import controllers.actions.TestAuthRetrievals._
import controllers.routes
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{Action, AnyContent, DefaultActionBuilder, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{AuditService, UrlBuilderService}
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Retrieval, ~}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AuthActionSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private type RetrievalsType = Option[Credentials] ~ Enrolments ~ Option[AffinityGroup] ~ Option[String] ~ ConfidenceLevel
  private val vatEnrolmentWithNoOssEnrolment = Enrolments(Set(Enrolment("HMRC-MTD-VAT", Seq(EnrolmentIdentifier("VRN", "123456789")), "Activated")))
  private val vatEnrolmentWithOss = Enrolments(Set(
    Enrolment("HMRC-MTD-VAT", Seq(EnrolmentIdentifier("VRN", "123456789")), "Activated"),
    Enrolment("HMRC-OSS-ORG")
  ))
  private val configNoEnrolment = "features.oss-enrolment" -> false
  private val groupId = UUID.randomUUID().toString

  class Harness(authAction: IdentifierAction, defaultAction: DefaultActionBuilder) extends  {
    def onPageLoad(): Action[AnyContent] = (defaultAction andThen authAction) { _ => Results.Ok }
  }

  private val mockAuthConnector: AuthConnector = mock[AuthConnector]
  private val mockAuditService: AuditService = mock[AuditService]
  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(mockAuthConnector)
    Mockito.reset(mockAuditService)
    Mockito.reset(mockRegistrationConnector)
  }

  "Auth Action" - {

    "when the user is logged in as an Organisation Admin with a VAT enrolment and strong credentials" - {

      "must succeed" in {

        val application = applicationBuilder(None).configure(configNoEnrolment).build()
        when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(testCredentials) ~ vatEnrolmentWithNoOssEnrolment ~ Some(Organisation) ~ Some(groupId) ~ ConfidenceLevel.L50))

        running(application) {
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]
          val appConfig     = application.injector.instanceOf[FrontendAppConfig]
          val urlBuilder = application.injector.instanceOf[UrlBuilderService]

          val authAction = new IdentifierAction(mockAuthConnector, mockAuditService, mockRegistrationConnector, appConfig, urlBuilder)
          val controller = new Harness(authAction, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe OK
        }
      }

      "when ossEnrolments toggle is on" - {

        val config = "features.oss-enrolment" -> true

        "when user has ossEnrolments must succeed" in {

          val application = applicationBuilder(None)
            .configure(config).build()

          when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(testCredentials) ~ vatEnrolmentWithOss ~ Some(Organisation) ~ Some(groupId) ~ ConfidenceLevel.L50))

          running(application) {
            val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]
            val appConfig     = application.injector.instanceOf[FrontendAppConfig]
            val urlBuilder = application.injector.instanceOf[UrlBuilderService]

            val authAction = new IdentifierAction(mockAuthConnector, mockAuditService, mockRegistrationConnector, appConfig, urlBuilder)
            val controller = new Harness(authAction, actionBuilder)
            val result = controller.onPageLoad()(FakeRequest())

            status(result) mustBe OK
          }
        }

        "when user does not have ossEnrolment" - {

          "and has an active registration, then give enrolment and allow login" in {
            val application = applicationBuilder(None)
              .configure(config).build()

            when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
              .thenReturn(Future.successful(Some(testCredentials) ~ vatEnrolmentWithNoOssEnrolment ~ Some(Organisation) ~ Some(groupId) ~ ConfidenceLevel.L50))
            when(mockRegistrationConnector.get()(any())) thenReturn Future.successful(Some(registration))
            when(mockRegistrationConnector.enrolUser()(any())) thenReturn Future.successful(HttpResponse(NO_CONTENT, ""))

            running(application) {
              val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]
              val appConfig = application.injector.instanceOf[FrontendAppConfig]
              val urlBuilder = application.injector.instanceOf[UrlBuilderService]

              val authAction = new IdentifierAction(mockAuthConnector, mockAuditService, mockRegistrationConnector, appConfig, urlBuilder)
              val controller = new Harness(authAction, actionBuilder)
              val result = controller.onPageLoad()(FakeRequest())

              status(result) mustBe OK
            }
          }

          "and no active registration must be redirected to the Not Registered page" in {

            val application = applicationBuilder(None)
              .configure(config).build()

            when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
              .thenReturn(Future.successful(Some(testCredentials) ~ vatEnrolmentWithNoOssEnrolment ~ Some(Organisation) ~ Some(groupId) ~ ConfidenceLevel.L50))
            when(mockRegistrationConnector.get()(any())) thenReturn Future.successful(None)

            running(application) {
              val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]
              val appConfig = application.injector.instanceOf[FrontendAppConfig]
              val urlBuilder = application.injector.instanceOf[UrlBuilderService]

              val authAction = new IdentifierAction(mockAuthConnector, mockAuditService, mockRegistrationConnector, appConfig, urlBuilder)
              val controller = new Harness(authAction, actionBuilder)
              val result = controller.onPageLoad()(FakeRequest())

              status(result) mustBe SEE_OTHER
              redirectLocation(result).value mustEqual routes.NotRegisteredController.onPageLoad().url
            }
          }
        }
      }
    }

    "when the user is logged in as an Individual with a VAT enrolment, strong credentials and confidence level 200" - {

      "must succeed" in {

        val application = applicationBuilder(None).configure(configNoEnrolment).build()

        running(application) {
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]
          val urlBuilder = application.injector.instanceOf[UrlBuilderService]

          when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(testCredentials) ~ vatEnrolmentWithNoOssEnrolment ~ Some(Individual) ~ Some(groupId) ~ConfidenceLevel.L200))

          val action = new IdentifierAction(mockAuthConnector, mockAuditService, mockRegistrationConnector, appConfig, urlBuilder)
          val controller = new Harness(action, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustEqual OK
        }
      }
    }

    "when the user has logged in as an Organisation Admin with strong credentials but no vat enrolment" - {

      "must be redirected to the Not Registered page" in {

        val application = applicationBuilder(None).configure(configNoEnrolment).build()

        running(application) {
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]
          val urlBuilder = application.injector.instanceOf[UrlBuilderService]

          when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(testCredentials) ~ Enrolments(Set.empty) ~ Some(Organisation) ~ Some(groupId) ~ ConfidenceLevel.L50))

          val action = new IdentifierAction(mockAuthConnector, mockAuditService, mockRegistrationConnector, appConfig, urlBuilder)
          val controller = new Harness(action, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.NotRegisteredController.onPageLoad().url
        }
      }
    }

    "when the user has logged in as an Individual with a VAT enrolment and strong credentials, but confidence level less then 200" - {

      "must be redirected to the Not Registered page" in {

        val application = applicationBuilder(None).configure(configNoEnrolment).build()

        running(application) {
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]
          val urlBuilder = application.injector.instanceOf[UrlBuilderService]

          when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(testCredentials) ~ vatEnrolmentWithNoOssEnrolment ~ Some(Individual) ~ Some(groupId) ~ ConfidenceLevel.L50))

          val action = new IdentifierAction(mockAuthConnector, mockAuditService, mockRegistrationConnector, appConfig, urlBuilder)
          val controller = new Harness(action, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.NotRegisteredController.onPageLoad().url
        }
      }
    }

    "when the user has logged in as an Individual without a VAT enrolment" - {

      "must be redirected to the Not Registered page" in {

        val application = applicationBuilder(None).configure(configNoEnrolment).build()

        running(application) {
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]
          val urlBuilder = application.injector.instanceOf[UrlBuilderService]

          when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(testCredentials) ~ Enrolments(Set.empty) ~ Some(Individual) ~ Some(groupId) ~ ConfidenceLevel.L200))

          val action = new IdentifierAction(mockAuthConnector, mockAuditService, mockRegistrationConnector, appConfig, urlBuilder)
          val controller = new Harness(action, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustBe routes.NotRegisteredController.onPageLoad().url
        }
      }
    }

    "when the user hasn't logged in" - {

      "must redirect the user to log in " in {

        val application = applicationBuilder(None).configure(configNoEnrolment).build()

        running(application) {
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]
          val appConfig     = application.injector.instanceOf[FrontendAppConfig]
          val urlBuilder    = application.injector.instanceOf[UrlBuilderService]

          val authAction = new IdentifierAction(new FakeFailingAuthConnector(new MissingBearerToken),
            mockAuditService,
            mockRegistrationConnector,
            appConfig,
            urlBuilder
          )
          val controller = new Harness(authAction, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest("", "/endpoint"))

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must startWith(appConfig.loginUrl)
        }
      }
    }

    "the user's session has expired" - {

      "must redirect the user to log in " in {

        val application = applicationBuilder(userAnswers = None).configure(configNoEnrolment).build()

        running(application) {
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]
          val appConfig     = application.injector.instanceOf[FrontendAppConfig]
          val urlBuilder = application.injector.instanceOf[UrlBuilderService]

          val authAction = new IdentifierAction(new FakeFailingAuthConnector(new BearerTokenExpired),
            mockAuditService,
            mockRegistrationConnector,
            appConfig,
            urlBuilder
          )
          val controller = new Harness(authAction, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest("", "/endpoint"))

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must startWith(appConfig.loginUrl)
        }
      }
    }

    "the user used an unsupported auth provider" - {

      "must redirect the user to the Not Registered page" in {

        val application = applicationBuilder(userAnswers = None).configure(configNoEnrolment).build()

        running(application) {
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]
          val urlBuilder = application.injector.instanceOf[UrlBuilderService]

          val authAction = new IdentifierAction(new FakeFailingAuthConnector(new UnsupportedAuthProvider),
            mockAuditService,
            mockRegistrationConnector,
            appConfig,
            urlBuilder
          )
          val controller = new Harness(authAction, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.NotRegisteredController.onPageLoad().url
        }
      }
    }

    "the user has an unsupported affinity group" - {

      "must redirect the user to the Not Registered page" in {

        val application = applicationBuilder(userAnswers = None).configure(configNoEnrolment).build()

        running(application) {
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]
          val urlBuilder = application.injector.instanceOf[UrlBuilderService]

          val authAction = new IdentifierAction(new FakeFailingAuthConnector(new UnsupportedAffinityGroup),
            mockAuditService,
            mockRegistrationConnector,
            appConfig,
            urlBuilder
          )
          val controller = new Harness(authAction, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.NotRegisteredController.onPageLoad().url
        }
      }
    }

    "the user has weak credentials" - {

      "must redirect the user to the Not Registered page" in {

        val application = applicationBuilder(userAnswers = None).configure(configNoEnrolment).build()

        running(application) {
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]
          val urlBuilder = application.injector.instanceOf[UrlBuilderService]

          val authAction = new IdentifierAction(new FakeFailingAuthConnector(new IncorrectCredentialStrength),
            mockAuditService,
            mockRegistrationConnector,
            appConfig,
            urlBuilder
          )
          val controller = new Harness(authAction, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER

          redirectLocation(result).value mustEqual routes.NotRegisteredController.onPageLoad().url
        }
      }
    }
  }
}

class FakeFailingAuthConnector @Inject()(exceptionToReturn: Throwable) extends AuthConnector {
  val serviceUrl: String = ""

  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
    Future.failed(exceptionToReturn)
}
