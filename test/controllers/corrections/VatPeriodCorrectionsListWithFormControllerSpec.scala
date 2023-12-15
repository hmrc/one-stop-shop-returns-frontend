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

package controllers.corrections

import base.SpecBase
import connectors.ReturnStatusConnector
import forms.corrections.VatPeriodCorrectionsListFormProvider
import models.{CheckThirdLoopMode, Country, Index, NormalMode, PeriodWithStatus, StandardPeriod, SubmissionStatus, UserAnswers}
import models.Quarter.{Q1, Q3, Q4}
import models.SubmissionStatus.{Complete, Overdue}
import models.responses.UnexpectedResponseStatus
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
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.addtoalist.ListItem
import views.html.corrections.VatPeriodAvailableCorrectionsListView

import scala.concurrent.Future

class VatPeriodCorrectionsListWithFormControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private lazy val vatPeriodCorrectionsListRoute = controllers.corrections.routes.VatPeriodCorrectionsListWithFormController.onPageLoad(NormalMode, period).url

  private def addCorrectionPeriods(userAnswers: UserAnswers, periods: Seq[StandardPeriod]): Option[UserAnswers] =
    periods.zipWithIndex
      .foldLeft(Option(userAnswers))((ua, indexedPeriod) =>
        ua.flatMap(_.set(CorrectionReturnPeriodPage(Index(indexedPeriod._2)), indexedPeriod._1).toOption)
          .flatMap(_.set(CorrectionCountryPage(Index(indexedPeriod._2), Index(0)), Country.euCountries.head).toOption)
          .flatMap(_.set(CountryVatCorrectionPage(Index(indexedPeriod._2), Index(0)), BigDecimal(200.0)).toOption))

  private def getStatusResponse(periods: Seq[StandardPeriod]): Future[Right[Nothing, Seq[PeriodWithStatus]]] =
    getStatusResponse(periods, Complete)

  private def getStatusResponse(periods: Seq[StandardPeriod], status: SubmissionStatus): Future[Right[Nothing, Seq[PeriodWithStatus]]] = {
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
    StandardPeriod(2021, Q3),
    StandardPeriod(2021, Q4),
    StandardPeriod(2022, Q1)
  )

  "VatPeriodCorrectionsListWithFormController" - {

    "must throw an exception when Return Status Connector returns an error" in {

      when(mockReturnStatusConnector.listStatuses(any())(any())) thenReturn Future.successful(Left(UnexpectedResponseStatus(1, "error")))

      val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
        .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
        .build()

      running(application) {
        implicit val request = FakeRequest(GET, vatPeriodCorrectionsListRoute)
        val result = route(application, request).value

        whenReady(result.failed) { exp => exp mustBe a[Exception] }
      }
    }

    "when there are no previous return periods must redirect to JourneyRecovery" in {

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

    "when there are previous return periods" - {

      "and no corrections have been added must display empty table and form" in {

        val expectedTitle = "You have not corrected the VAT amount for any return periods"
        val expectedTableRows = 0

        val completedCorrections = List.empty[StandardPeriod]
        val completedCorrectionsModel = Seq.empty[ListItem]

        val application = applicationBuilder(userAnswers = addCorrectionPeriods(completeUserAnswers, completedCorrections))
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

          doc.getElementsByClass("govuk-heading-xl").get(0).text() mustEqual expectedTitle
          doc.getElementsByClass("govuk-table__row").size() mustBe expectedTableRows

          val view = application.injector.instanceOf[VatPeriodAvailableCorrectionsListView]
          responseString mustEqual view(form, NormalMode, period, completedCorrectionsModel, List.empty)(request, messages(application)).toString
        }
      }

      "and corrections have been added" - {

        "and there are uncompleted correction periods must display filled table and form" in {

          val expectedTitle = "You have corrected the VAT amount for one return period"
          val expectedTableRows = 1
          val periodQ3 = StandardPeriod(2021, Q3)

          val completedCorrections = List(periodQ3)

          val completedCorrectionsModel = Seq(
            ListItem(
              name = "1 July to 30 September 2021",
              changeUrl = controllers.corrections.routes.VatCorrectionsListController.onPageLoad(CheckThirdLoopMode, periodQ3, index).url,
              removeUrl = controllers.corrections.routes.RemovePeriodCorrectionController.onPageLoad(NormalMode, periodQ3, index).url
            )

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

            doc.getElementsByClass("govuk-heading-xl").get(0).text() mustEqual expectedTitle
            doc.getElementsByClass("hmrc-add-to-a-list__contents").size() mustEqual expectedTableRows

            val view = application.injector.instanceOf[VatPeriodAvailableCorrectionsListView]
            responseString mustEqual view(form, NormalMode, period, completedCorrectionsModel, List.empty)(request, messages(application)).toString
          }
        }

        "and there are no uncompleted correction periods must redirect to page without form" in {

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

    "and there are no uncompleted correction periods must redirect to page without form for a POST" in {

      when(mockReturnStatusConnector.listStatuses(any())(any()))
        .thenReturn(getStatusResponse(allPeriods, Complete))

      val application = applicationBuilder(userAnswers = addCorrectionPeriods(completeUserAnswers, allPeriods))
        .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
        .build()

      running(application) {
        val request = FakeRequest(POST, controllers.corrections.routes.VatPeriodCorrectionsListWithFormController.onSubmit(NormalMode, period, false).url)

        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.corrections.routes.VatPeriodCorrectionsListController.onPageLoad(NormalMode, period).url
      }
    }

    "when there are no previous return periods must redirect to JourneyRecovery for a POST" in {

      when(mockReturnStatusConnector.listStatuses(any())(any()))
        .thenReturn(getStatusResponse(allPeriods, Overdue))

      val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
        .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
        .build()

      running(application) {
        implicit val request = FakeRequest(POST, controllers.corrections.routes.VatPeriodCorrectionsListWithFormController.onSubmit(NormalMode, period, false).url)
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must throw an exception when the Return Status Connector returns an Unexpected Response" in {

      when(mockReturnStatusConnector.listStatuses(any())(any())) thenReturn Future.successful(Left(UnexpectedResponseStatus(1, "error")))

      val application = applicationBuilder(userAnswers = addCorrectionPeriods(completeUserAnswers, allPeriods))
        .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
        .build()

      running(application) {
        val request = FakeRequest(POST, controllers.corrections.routes.VatPeriodCorrectionsListWithFormController.onSubmit(NormalMode, period, false).url)

        val result = route(application, request).value

        whenReady(result.failed) { exp => exp mustBe a[Exception] }
      }
    }
  }
}
