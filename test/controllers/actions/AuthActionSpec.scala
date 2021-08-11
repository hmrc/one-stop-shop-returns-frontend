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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{Action, AnyContent, DefaultActionBuilder, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier
import TestAuthRetrievals._
import controllers.routes

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AuthActionSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private type RetrievalsType = Option[Credentials] ~ Enrolments
  private val vatEnrolment = Enrolments(Set(Enrolment("HMRC-MTD-VAT", Seq(EnrolmentIdentifier("VRN", "123456789")), "Activated")))

  class Harness(authAction: AuthenticatedIdentifierAction, defaultAction: DefaultActionBuilder) extends  {
    def onPageLoad(): Action[AnyContent] = (defaultAction andThen authAction) { _ => Results.Ok }
  }

  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(mockAuthConnector)
  }

  "Auth Action" - {

    "when the user has logged in with a VAT enrolment" - {

      "must succeed" in {

        val application = applicationBuilder(None).build()
        when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(testCredentials) ~ vatEnrolment))

        running(application) {
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]
          val appConfig     = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(mockAuthConnector, appConfig)
          val controller = new Harness(authAction, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe OK
        }
      }
    }

    "when the user has logged in without a VAT enrolment" - {

      "must be redirected to the Unauthorised page" in {

        val application = applicationBuilder(None).build()
        when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(testCredentials) ~ Enrolments(Set.empty)))

        running(application) {
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]
          val appConfig     = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(mockAuthConnector, appConfig)
          val controller = new Harness(authAction, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad().url
        }
      }
    }

    "when the user hasn't logged in" - {

      "must redirect the user to log in " in {

        val application = applicationBuilder(None).build()

        running(application) {
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]
          val appConfig     = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(new FakeFailingAuthConnector(new MissingBearerToken), appConfig)
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

          val authAction = new AuthenticatedIdentifierAction(new FakeFailingAuthConnector(new BearerTokenExpired), appConfig)
          val controller = new Harness(authAction, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must startWith(appConfig.loginUrl)
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
