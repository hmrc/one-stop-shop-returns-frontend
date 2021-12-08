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

package controllers

import base.SpecBase
import config.FrontendAppConfig
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.SavedProgressView

import java.time.{Clock, LocalDate, ZoneId}
import java.time.format.DateTimeFormatter

class SavedProgressControllerSpec extends SpecBase {

  "SavedProgress Controller" - {

    "must return OK and the correct view for a GET" in {
      val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
      val mockAppConfig: FrontendAppConfig = mock[FrontendAppConfig]
      when(mockAppConfig.cacheTtl) thenReturn 1
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.SavedProgressController.onPageLoad(period).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SavedProgressView]
        val clock = application.injector.instanceOf(classOf[Clock])
        val date = clock.instant().atZone(clock.getZone).toLocalDate
        status(result) mustEqual OK
        contentAsString(result) mustEqual view(period, date.format(dateFormatter))(request, messages(application)).toString
      }
    }
  }
}