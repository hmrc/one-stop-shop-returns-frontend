/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AuthActionSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private type RetrievalsType = Option[Credentials] ~ Enrolments ~ Option[AffinityGroup] ~ ConfidenceLevel ~ Option[CredentialRole]
  private val vatEnrolment = Enrolments(Set(Enrolment("HMRC-MTD-VAT", Seq(EnrolmentIdentifier("VRN", "123456789")), "Activated")))

  class Harness(authAction: IdentifierAction, defaultAction: DefaultActionBuilder) extends  {
    def onPageLoad(): Action[AnyContent] = (defaultAction andThen authAction) { _ => Results.Ok }
  }

  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(mockAuthConnector)
  }

  "Auth Action" - {

    "when the user is logged in as an Organisation Admin with a VAT enrolment and strong credentials" - {

      "must succeed" in {

        val application = applicationBuilder(None).build()
        when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(testCredentials) ~ vatEnrolment ~ Some(Organisation) ~ ConfidenceLevel.L50 ~ Some(User)))

        running(application) {
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]
          val appConfig     = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new IdentifierAction(mockAuthConnector, appConfig)
          val controller = new Harness(authAction, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe OK
        }
      }
    }

    "when the user is logged in as an Individual with a VAT enrolment, strong credentials and confidence level 250" - {

      "must succeed" in {

        val application = applicationBuilder(None).build()

        running(application) {
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]

          when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(testCredentials) ~ vatEnrolment ~ Some(Individual) ~ ConfidenceLevel.L250 ~ None))

          val action = new IdentifierAction(mockAuthConnector, appConfig)
          val controller = new Harness(action, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustEqual OK
        }
      }
    }

    "when the user has logged in as an Organisation Assistant with a VAT enrolment and strong credentials" - {

      "must be redirected to the Unauthorised page" in {

        val application = applicationBuilder(None).build()

        running(application) {
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]

          when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(testCredentials) ~ vatEnrolment ~ Some(Organisation) ~ ConfidenceLevel.L50 ~ Some(Assistant)))

          val action = new IdentifierAction(mockAuthConnector, appConfig)
          val controller = new Harness(action, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }
    }

    "when the user has logged in as an Organisation Admin with strong credentials but no vat enrolment" - {

      "must be redirected to the Unauthorised page" in {

        val application = applicationBuilder(None).build()

        running(application) {
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]

          when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(testCredentials) ~ Enrolments(Set.empty) ~ Some(Organisation) ~ ConfidenceLevel.L50 ~ Some(User)))

          val action = new IdentifierAction(mockAuthConnector, appConfig)
          val controller = new Harness(action, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }
    }

    "when the user has logged in as an Individual with a VAT enrolment and strong credentials, but confidence level less then 200" - {

      "must be redirected to the Unauthorised page" in {

        val application = applicationBuilder(None).build()

        running(application) {
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]

          when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(testCredentials) ~ vatEnrolment ~ Some(Individual) ~ ConfidenceLevel.L200 ~ None))

          val action = new IdentifierAction(mockAuthConnector, appConfig)
          val controller = new Harness(action, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }
    }

    "when the user has logged in as an Individual without a VAT enrolment" - {

      "must be redirected to the Unauthorised page" in {

        val application = applicationBuilder(None).build()

        running(application) {
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]

          when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(testCredentials) ~ Enrolments(Set.empty) ~ Some(Individual) ~ ConfidenceLevel.L250 ~ None))

          val action = new IdentifierAction(mockAuthConnector, appConfig)
          val controller = new Harness(action, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
        }
      }
    }

    "when the user hasn't logged in" - {

      "must redirect the user to log in " in {

        val application = applicationBuilder(None).build()

        running(application) {
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]
          val appConfig     = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new IdentifierAction(new FakeFailingAuthConnector(new MissingBearerToken), appConfig)
          val controller = new Harness(authAction, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must startWith(appConfig.loginUrl)
        }
      }
    }

    "the user's session has expired" - {

      "must redirect the user to log in " in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]
          val appConfig     = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new IdentifierAction(new FakeFailingAuthConnector(new BearerTokenExpired), appConfig)
          val controller = new Harness(authAction, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must startWith(appConfig.loginUrl)
        }
      }
    }

    "the user used an unsupported auth provider" - {

      "must redirect the user to the Unauthorised page" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]

          val authAction = new IdentifierAction(new FakeFailingAuthConnector(new UnsupportedAuthProvider), appConfig)
          val controller = new Harness(authAction, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }
    }

    "the user has an unsupported affinity group" - {

      "must redirect the user to the Unauthorised page" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]

          val authAction = new IdentifierAction(new FakeFailingAuthConnector(new UnsupportedAffinityGroup), appConfig)
          val controller = new Harness(authAction, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }
    }

    "the user has weak credentials" - {

      "must redirect the user to the Unauthorised page" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]

          val authAction = new IdentifierAction(new FakeFailingAuthConnector(new IncorrectCredentialStrength), appConfig)
          val controller = new Harness(authAction, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER

          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
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
