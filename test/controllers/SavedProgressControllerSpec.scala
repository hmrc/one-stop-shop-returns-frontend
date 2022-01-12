/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers

import base.SpecBase
import cats.data.Validated.Valid
import config.FrontendAppConfig
import connectors.{SaveForLaterConnector, SavedUserAnswers}
import models.requests.SaveForLaterRequest
import models.responses.{ConflictFound, UnexpectedResponseStatus}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.SaveForLaterService
import views.html.SavedProgressView

import java.time.format.DateTimeFormatter
import java.time.{Clock, Instant, LocalDate, ZoneId}
import scala.concurrent.Future

class SavedProgressControllerSpec extends SpecBase {

  "SavedProgress Controller" - {

    "must return OK and the correct view for a GET and clear user-answers after return submitted" in {

      val instantDate = Instant.now
      val stubClock: Clock = Clock.fixed(instantDate, ZoneId.systemDefault)
      val date = LocalDate.now(stubClock).plusDays(28)

      val dateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

      val mockAppConfig: FrontendAppConfig = mock[FrontendAppConfig]
      val mockSaveForLaterConnector = mock[SaveForLaterConnector]
      val mockSaveForLaterService = mock[SaveForLaterService]
      val mockSessionRepository = mock[SessionRepository]

      val savedAnswers = SavedUserAnswers(
        vrn,
        period,
        Json.toJson("hello"),
        instantDate
      )

      val savedAnswersRequest = SaveForLaterRequest(
        vrn,
        period,
        Json.toJson("hello"),
        instantDate
      )

      when(mockAppConfig.cacheTtl) thenReturn 1
      when(mockSaveForLaterConnector.submit(any())(any())) thenReturn Future.successful(Right(savedAnswers))
      when(mockSaveForLaterService.fromUserAnswers(any(), any(), any())) thenReturn Valid(savedAnswersRequest)
      when(mockSessionRepository.clear(any())) thenReturn Future.successful(true)

      val app = applicationBuilder(userAnswers = Some(completeUserAnswers.copy(lastUpdated = instantDate)))
        .overrides(
          bind[SaveForLaterConnector].toInstance(mockSaveForLaterConnector),
          bind[SaveForLaterService].toInstance(mockSaveForLaterService),
          bind[SessionRepository].toInstance(mockSessionRepository),
        ).build()

      running(app) {

        val request = FakeRequest(GET, routes.SavedProgressController.onPageLoad(period, "test").url)

        val result = route(app, request).value

        val view = app.injector.instanceOf[SavedProgressView]


        status(result) mustEqual OK
        verify(mockSessionRepository, times(1)).clear(eqTo(completeUserAnswers.userId))
        contentAsString(result) mustEqual view(period, date.format(dateTimeFormatter), "test")(request, messages(app)).toString
      }
    }

    "must redirect to Your Account Controller when Save For Later Connector returns ConflictFound" in {

      val mockSaveForLaterConnector = mock[SaveForLaterConnector]
      val mockSessionRepository = mock[SessionRepository]

      when(mockSaveForLaterConnector.submit(any())(any())) thenReturn Future.successful(Left(ConflictFound))
      when(mockSessionRepository.clear(any())) thenReturn Future.successful(false)

      val app = applicationBuilder(userAnswers = Some(completeUserAnswers))
              .overrides(
                bind[SaveForLaterConnector].toInstance(mockSaveForLaterConnector),
                bind[SessionRepository].toInstance(mockSessionRepository)
              ).build()

      running(app) {

        val request = FakeRequest(GET, routes.SavedProgressController.onPageLoad(period, "test").url)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.YourAccountController.onPageLoad().url
        verify(mockSessionRepository, times(0)).clear(eqTo(completeUserAnswers.userId))
      }
    }

    "must redirect to Journey Recovery Controller when Save For Later Connector returns Error Response" in {

      val mockSaveForLaterConnector = mock[SaveForLaterConnector]
      val mockSessionRepository = mock[SessionRepository]

      when(mockSaveForLaterConnector.submit(any())(any())) thenReturn Future.successful(Left(UnexpectedResponseStatus(1, "error")))
      when(mockSessionRepository.clear(any())) thenReturn Future.successful(false)

      val app = applicationBuilder(userAnswers = Some(completeUserAnswers))
        .overrides(
          bind[SaveForLaterConnector].toInstance(mockSaveForLaterConnector),
          bind[SessionRepository].toInstance(mockSessionRepository)
        ).build()

      running(app) {

        val request = FakeRequest(GET, routes.SavedProgressController.onPageLoad(period, "test").url)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        verify(mockSessionRepository, times(0)).clear(eqTo(completeUserAnswers.userId))
      }
    }

    //    "must redirect to Your Account Controller when the Save for Later Service returns Invalid" in {
//
//      val mockSaveForLaterService = mock[SaveForLaterService]
//      val mockSessionRepository = mock[SessionRepository]
//
//      when(mockSaveForLaterService.fromUserAnswers(any(), any(), any())) thenReturn Invalid(NonEmptyChain(DataMissingError())))
//      when(mockSessionRepository.clear(any())) thenReturn Future.successful(false)
//
//      val app = applicationBuilder(userAnswers = Some(completeUserAnswers))
//        .overrides(
//          bind[SaveForLaterService].toInstance(mockSaveForLaterService),
//          bind[SessionRepository].toInstance(mockSessionRepository),
//        ).build()
//
//      running(app) {
//
//        val request = FakeRequest(GET, routes.SavedProgressController.onPageLoad(period, "test").url)
//
//        val result = route(app, request).value
//
//        status(result) mustEqual SEE_OTHER
//        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
//        verify(mockSessionRepository, times(0)).clear(eqTo(completeUserAnswers.userId))
//      }
//    }
  }
}
