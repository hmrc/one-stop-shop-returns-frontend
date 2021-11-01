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

package controllers.corrections

import base.SpecBase
import models.{Index, NormalMode, Period}
import models.Quarter.{Q1, Q2, Q3, Q4}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.corrections.CorrectionReturnPeriodPage
import play.api.libs.json.{JsArray, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.corrections.VatPeriodCorrectionsListView

class VatPeriodCorrectionsListControllerSpec extends SpecBase with MockitoSugar {

  private lazy val vatPeriodCorrectionsListRoute = controllers.corrections.routes.VatPeriodCorrectionsListController.onPageLoad(NormalMode, period).url

  "VatPeriodCorrectionsList Controller" - {

    "must return OK and the correct view for a GET when there are no corrections" in {

      val application = applicationBuilder(userAnswers = Some(completeUserAnswers)).build()

      running(application) {
        implicit val request = FakeRequest(GET, vatPeriodCorrectionsListRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[VatPeriodCorrectionsListView]

        val periodCorrectionsList = Seq()

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(NormalMode, period, periodCorrectionsList)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when there is one correction" in {

      val noCorrections = Json.obj(
        "corrections" -> Json.arr(
          Seq(
            Json.obj(
              "correctionReturnPeriod" -> Json.toJson(Period(2021, Q1))
            )
          )
        )
      )

      val newUa = completeUserAnswers.set(CorrectionReturnPeriodPage(Index(0)), Period(2021, Q1))

      val application = applicationBuilder(userAnswers = newUa.toOption).build()

      running(application) {
        val request = FakeRequest(GET, vatPeriodCorrectionsListRoute)

        val view = application.injector.instanceOf[VatPeriodCorrectionsListView]

        val periodCorrectionsList = Seq(
          Period(2021, Q1)
        )

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(NormalMode, period, periodCorrectionsList)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when there is more than one correction" in {

      val noCorrections = Json.obj(
        "corrections" -> Json.arr(
          Seq(
            Json.obj(
              "correctionReturnPeriod" -> Json.toJson(Period(2021, Q1))
            ),
            Json.obj(
              "correctionReturnPeriod" -> Json.toJson(Period(2021, Q2))
            )
          )
        )
      )

      val newUa = completeUserAnswers.set(CorrectionReturnPeriodPage(Index(0)), Period(2021, Q1))
        .flatMap(ua => ua.set(CorrectionReturnPeriodPage(Index(1)), Period(2021, Q2)))

      val application = applicationBuilder(userAnswers = newUa.toOption).build()

      running(application) {
        val request = FakeRequest(GET, vatPeriodCorrectionsListRoute)

        val view = application.injector.instanceOf[VatPeriodCorrectionsListView]

        val periodCorrectionsList = Seq(
          Period(2021, Q1),
          Period(2021, Q2)
        )

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(NormalMode, period, periodCorrectionsList)(request, messages(application)).toString
      }
    }

  }
}
