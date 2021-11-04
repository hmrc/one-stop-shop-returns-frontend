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
import connectors.ReturnStatusConnector
import models.{Index, NormalMode, Period, PeriodWithStatus, UserAnswers}
import models.Quarter.{Q1, Q2, Q3, Q4}
import models.SubmissionStatus.Complete
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.corrections.CorrectionReturnPeriodPage
import play.api.inject.bind
import play.api.libs.json.{JsArray, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.corrections.VatPeriodCorrectionsListView

import scala.concurrent.Future

class VatPeriodCorrectionsListControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private lazy val vatPeriodCorrectionsListRoute = controllers.corrections.routes.VatPeriodCorrectionsListController.onPageLoad(NormalMode, period).url

  private def addCorrectionPeriods(userAnswers: UserAnswers, periods: Seq[Period]): Option[UserAnswers] =
    periods.zipWithIndex
      .foldLeft (Option(userAnswers))((ua, indexedPeriod) =>
        ua.flatMap(_.set(CorrectionReturnPeriodPage(Index(indexedPeriod._2)), indexedPeriod._1).toOption))

  private def getStatusResponse(periods: Seq[Period]) = {
    Future.successful(Right(periods.map(period => PeriodWithStatus(period, Complete))))
  }

  private val mockReturnStatusConnector = mock[ReturnStatusConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockReturnStatusConnector)
  }

  "VatPeriodCorrectionsList Controller" - {

    "when there are no previous return periods must redirect to JourneyRecovery" in {

      when(mockReturnStatusConnector.listStatuses(any())(any()))
        .thenReturn(getStatusResponse(Seq()))

      val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
        .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
        .configure("bootstrap.filters.csrf.enabled" -> false)
        .build()

      running(application) {
        implicit val request = FakeRequest(GET, vatPeriodCorrectionsListRoute)

        val result = route(application, request).value

        val periodCorrectionsList = Seq()

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "when there are multiple previous return periods" - {

      val allPeriods = Seq(
        Period(2021, Q3),
        Period(2021, Q4),
        Period(2022, Q1)
      )

      when(mockReturnStatusConnector.listStatuses(any())(any()))
        .thenReturn(getStatusResponse(allPeriods))

      "and no corrections have been added" - {

        val completedCorrections = Seq()

        val availableCorrectionPeriods = allPeriods

        val application = applicationBuilder(userAnswers = addCorrectionPeriods(completeUserAnswers, completedCorrections))
          .configure("bootstrap.filters.csrf.enabled" -> false)
          .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, vatPeriodCorrectionsListRoute)
          val result = route(application, request).value

          "must display correct header and table and form" in {

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.corrections.routes.VatPeriodAvailableCorrectionsListController.onPageLoad(NormalMode, period).url
          }
        }
      }

      "and corrections have been added" - {

        "and there are available correction periods" - {

          val completedCorrections = Seq(
            Period(2021, Q3)
          )

          val availableCorrectionPeriods = allPeriods.diff(completedCorrections).distinct

          val ua = addCorrectionPeriods(completeUserAnswers, completedCorrections)

          val application = applicationBuilder(userAnswers = ua)
            .configure("bootstrap.filters.csrf.enabled" -> false)
            .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
            .build()

          running(application) {
            val request = FakeRequest(GET, vatPeriodCorrectionsListRoute)
            val result = route(application, request).value

            "must display correct header and table and form" in {
              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual controllers.corrections.routes.VatPeriodAvailableCorrectionsListController.onPageLoad(NormalMode, period).url
            }

            // on submit, form must be answered

          }
        }

        "and there are no available correction periods" - {

          val completedCorrections = allPeriods

          val availableCorrectionPeriods = Seq()

          val application = applicationBuilder(userAnswers = addCorrectionPeriods(completeUserAnswers, allPeriods))
            .configure("bootstrap.filters.csrf.enabled" -> false)
            .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
            .build()

          running(application) {
            val request = FakeRequest(GET, vatPeriodCorrectionsListRoute)

            val result = route(application, request).value

            "must display correct header and table and no form" in {

              status(result) mustEqual OK

              val responseString = contentAsString(result)

              val doc = Jsoup.parse(responseString)
              val header = doc.getElementsByClass("govuk-heading-xl").get(0).text()
              val tableRows = doc.getElementsByClass("govuk-table__row")

              header mustEqual "You have corrected the VAT amount for 3 return periods"
              tableRows.size() mustEqual 3

              val view = application.injector.instanceOf[VatPeriodCorrectionsListView]
              responseString mustEqual view(NormalMode, period, completedCorrections)(request, messages(application)).toString
            }

          }
        }
      }
    }
  }
}