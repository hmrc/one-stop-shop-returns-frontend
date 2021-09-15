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
import connectors.VatReturnConnector
import generators.Generators
import models.Quarter.Q3
import models.responses.NotFound
import models.{Period, SubmissionStatus}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.IndexView

import scala.concurrent.Future

class IndexControllerSpec extends SpecBase with MockitoSugar with Generators with BeforeAndAfterEach {

  private val vatReturnConnector = mock[VatReturnConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(vatReturnConnector)
    super.beforeEach()
  }

  "Index Controller" - {

    "must return OK and the correct view" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[VatReturnConnector].toInstance(vatReturnConnector)
        ).build()

      when(vatReturnConnector.get(any())(any())) thenReturn Future.successful(Left(NotFound))

      running(application) {
        val request = FakeRequest(GET, routes.IndexController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[IndexView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(
          registration.registeredCompanyName,
          registration.vrn.vrn,
          Period(2021, Q3),
          SubmissionStatus.Due
        )(request, messages(application)).toString
      }
    }
  }
}