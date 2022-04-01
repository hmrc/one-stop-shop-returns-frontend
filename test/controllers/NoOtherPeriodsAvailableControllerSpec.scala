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
import models.SessionData
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.external.ExternalReturnUrlQuery
import repositories.SessionRepository
import views.html.NoOtherPeriodsAvailableView

import scala.concurrent.Future

class NoOtherPeriodsAvailableControllerSpec extends SpecBase {

  "NoOtherPeriodsAvailable Controller" - {

    "must return OK and the correct view for a GET" in {
      val sessionRepository = mock[SessionRepository]
      when(sessionRepository.get(any())) thenReturn(Future.successful(Seq.empty))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[SessionRepository].toInstance(sessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.NoOtherPeriodsAvailableController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[NoOtherPeriodsAvailableView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view()(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET and add the external backToYourAccount url that has been saved" in {

      val sessionRepository = mock[SessionRepository]
      when(sessionRepository.get(any())) thenReturn(Future.successful(Seq(SessionData("id").set(ExternalReturnUrlQuery.path, "example").get)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[SessionRepository].toInstance(sessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.NoOtherPeriodsAvailableController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[NoOtherPeriodsAvailableView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(Some("example"))(request, messages(application)).toString
      }
    }
  }
}
