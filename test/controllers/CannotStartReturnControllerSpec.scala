/*
 * Copyright 2023 HM Revenue & Customs
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
import connectors.VatReturnConnector
import models.external.ExternalEntryUrl
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.CannotStartReturnView

import scala.concurrent.Future

class CannotStartReturnControllerSpec extends SpecBase {

  "CannotStartReturn Controller" - {

    "must return OK and the correct view for a GET" in {
      val vatReturnConnector = mock[VatReturnConnector]
      when(vatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(None)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(inject.bind[VatReturnConnector].toInstance(vatReturnConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.CannotStartReturnController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CannotStartReturnView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view()(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET and add the external backToYourAccount url that has been saved" in {
      val vatReturnConnector = mock[VatReturnConnector]
      when(vatReturnConnector.getSavedExternalEntry()(any())) thenReturn Future.successful(Right(ExternalEntryUrl(Some("example"))))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(inject.bind[VatReturnConnector].toInstance(vatReturnConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.CannotStartReturnController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CannotStartReturnView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(Some("example"))(request, messages(application)).toString
      }
    }
  }
}
