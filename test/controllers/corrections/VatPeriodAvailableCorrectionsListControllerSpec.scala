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
import forms.corrections.{UndeclaredCountryCorrectionFormProvider, VatPeriodCorrectionsListFormProvider}
import models.Quarter.{Q1, Q3, Q4}
import models.SubmissionStatus.{Complete, Overdue}
import models.{Country, Index, NormalMode, Period, PeriodWithStatus, SubmissionStatus, UserAnswers}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.corrections.{CorrectionCountryPage, CorrectionReturnPeriodPage, CountryVatCorrectionPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.corrections.{VatPeriodAvailableCorrectionsListView, VatPeriodCorrectionsListView}

import scala.concurrent.Future

class VatPeriodAvailableCorrectionsListControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private lazy val vatPeriodCorrectionsListRoute = controllers.corrections.routes.VatPeriodAvailableCorrectionsListController.onPageLoad(NormalMode, period).url

  private def addCorrectionPeriods(userAnswers: UserAnswers, periods: Seq[Period]): Option[UserAnswers] =
    periods.zipWithIndex
      .foldLeft (Option(userAnswers))((ua, indexedPeriod) =>
        ua.flatMap(_.set(CorrectionReturnPeriodPage(Index(indexedPeriod._2)), indexedPeriod._1).toOption)
          .flatMap(_.set(CorrectionCountryPage(Index(indexedPeriod._2), Index(0)), Country.euCountries.head).toOption)
          .flatMap(_.set(CountryVatCorrectionPage(Index(indexedPeriod._2), Index(0)), BigDecimal(200.0)).toOption))

  private def getStatusResponse(periods: Seq[Period]): Future[Right[Nothing, Seq[PeriodWithStatus]]] =
    getStatusResponse(periods, Complete)

  private def getStatusResponse(periods: Seq[Period], status: SubmissionStatus): Future[Right[Nothing, Seq[PeriodWithStatus]]] = {
    Future.successful(Right(periods.map(period => PeriodWithStatus(period, status))))
  }

  private val mockReturnStatusConnector = mock[ReturnStatusConnector]

  private val formProvider = new VatPeriodCorrectionsListFormProvider()
  private val form = formProvider()

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockReturnStatusConnector)
  }

  val allPeriods = Seq(
    Period(2021, Q3),
    Period(2021, Q4),
    Period(2022, Q1)
  )

  "VatPeriodCorrectionsList Controller" - {

    "when there are no previous return periods must redirect to JourneyRecovery" in {

      Mockito.reset(mockReturnStatusConnector)

      when(mockReturnStatusConnector.listStatuses(any())(any()))
        .thenReturn(getStatusResponse(allPeriods, Overdue))

      val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
        .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
        .build()

      running(application) {
        implicit val request = FakeRequest(GET, vatPeriodCorrectionsListRoute)
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "when there are multiple previous return periods" - {

      "and no corrections have been added" - {

        val completedCorrections = List.empty[Period]

        val application = applicationBuilder(userAnswers = addCorrectionPeriods(completeUserAnswers, completedCorrections))
          .configure("bootstrap.filters.csrf.enabled" -> false)
          .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
          .build()

        when(mockReturnStatusConnector.listStatuses(any())(any()))
          .thenReturn(getStatusResponse(allPeriods))

        running(application) {
          val request = FakeRequest(GET, vatPeriodCorrectionsListRoute)
          val result = route(application, request).value

          "must display correct header without table and form" in {
            status(result) mustEqual OK
            val responseString = contentAsString(result)
            val doc = Jsoup.parse(responseString)
            val header = doc.getElementsByClass("govuk-heading-xl").get(0).text()
            doc.getElementsByClass("govuk-table__row").size() mustBe 0
            header mustEqual "You have not corrected the VAT amount for any return periods"
            val view = application.injector.instanceOf[VatPeriodAvailableCorrectionsListView]
            responseString mustEqual view(form, NormalMode, period, completedCorrections)(request, messages(application)).toString
          }
        }
      }

      "and corrections have been added" - {

        "and there are available correction periods must display correct header and table and form" in {

          val completedCorrections = List(
            Period(2021, Q3)
          )

          val ua = addCorrectionPeriods(completeUserAnswers, completedCorrections)

          val application = applicationBuilder(userAnswers = ua)
            .configure("bootstrap.filters.csrf.enabled" -> false)
            .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
            .build()

          when(mockReturnStatusConnector.listStatuses(any())(any()))
            .thenReturn(getStatusResponse(allPeriods))

          running(application) {
            val request = FakeRequest(GET, vatPeriodCorrectionsListRoute)

            val result = route(application, request).value

            status(result) mustEqual OK

            val responseString = contentAsString(result)
            val doc = Jsoup.parse(responseString)
            val header = doc.getElementsByClass("govuk-heading-xl").get(0).text()
            val tableRows = doc.getElementsByClass("govuk-table__row")

            header mustEqual "You have corrected the VAT amount for one return period"
            tableRows.size() mustEqual 1

            val view = application.injector.instanceOf[VatPeriodAvailableCorrectionsListView]
            responseString mustEqual view(form, NormalMode, period, completedCorrections)(request, messages(application)).toString
          }

          // on submit, form must be answered

        }

        "and there are no available correction periods must display correct header and table and no for"  in {

          when(mockReturnStatusConnector.listStatuses(any())(any()))
            .thenReturn(getStatusResponse(allPeriods))

          val application = applicationBuilder(userAnswers = addCorrectionPeriods(completeUserAnswers, allPeriods))
            .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
            .build()

          running(application) {
            val request = FakeRequest(GET, vatPeriodCorrectionsListRoute)

            val result = route(application, request).value
            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.corrections.routes.VatPeriodCorrectionsListController.onPageLoad(NormalMode, period).url
          }
        }
      }
    }
  }
}
