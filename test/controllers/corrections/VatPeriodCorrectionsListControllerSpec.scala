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

package controllers.corrections

import base.SpecBase
import connectors.ReturnStatusConnector
import models.Quarter.{Q1, Q3, Q4}
import models.SubmissionStatus.Complete
import models.{CheckThirdLoopMode, Country, Index, NormalMode, Period, PeriodWithStatus, UserAnswers}
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
import views.html.corrections.{VatPeriodAvailableCorrectionsListView, VatPeriodCorrectionsListView}

import scala.concurrent.Future

class VatPeriodCorrectionsListControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private lazy val vatPeriodCorrectionsListRoute = controllers.corrections.routes.VatPeriodCorrectionsListController.onPageLoad(NormalMode, period).url

  private def addCorrectionPeriods(userAnswers: UserAnswers, periods: Seq[Period]): Option[UserAnswers] =
    periods.zipWithIndex
      .foldLeft (Option(userAnswers))((ua, indexedPeriod) =>
        ua.flatMap(_.set(CorrectionReturnPeriodPage(Index(indexedPeriod._2)), indexedPeriod._1).toOption)
          .flatMap(_.set(CorrectionCountryPage(Index(indexedPeriod._2), Index(0)), Country.euCountries.head).toOption)
          .flatMap(_.set(CountryVatCorrectionPage(Index(indexedPeriod._2), Index(0)), BigDecimal(200.0)).toOption))

  private def getStatusResponse(periods: Seq[Period]) = {
    Future.successful(Right(periods.map(period => PeriodWithStatus(period, Complete))))
  }

  private val mockReturnStatusConnector = mock[ReturnStatusConnector]

  private def vatCorrectionsListUrl(index: Int) = s"/pay-vat-on-goods-sold-to-eu/northern-ireland-returns-payments/2021-Q3/third-change-vat-correction-list/$index"
  private def removePeriodCorrectionUrl(index: Int) = s"/pay-vat-on-goods-sold-to-eu/northern-ireland-returns-payments/2021-Q3/remove-period-correction/$index"
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

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "when there are previous return periods" - {

      val allPeriods = Seq(
        Period(2021, Q3),
        Period(2021, Q4),
        Period(2022, Q1)
      )

      val allPeriodsModel = Seq(
        ListItem(
          name = "1 July to 30 September 2021",
          changeUrl = vatCorrectionsListUrl(1),
          removeUrl = removePeriodCorrectionUrl(1)
        ),
        ListItem(
          name = "1 October to 31 December 2021",
          changeUrl = vatCorrectionsListUrl(2),
          removeUrl = removePeriodCorrectionUrl(2)
        ),
        ListItem(
          name = "1 January to 31 March 2022",
          changeUrl = vatCorrectionsListUrl(3),
          removeUrl = removePeriodCorrectionUrl(3)
        )
      )

      "and there are uncompleted correction periods must redirect to page with form" in {

        when(mockReturnStatusConnector.listStatuses(any())(any()))
          .thenReturn(getStatusResponse(allPeriods))

        val completedCorrections = Seq()

        val application = applicationBuilder(userAnswers = addCorrectionPeriods(completeUserAnswers, completedCorrections))
          .configure("bootstrap.filters.csrf.enabled" -> false)
          .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, vatPeriodCorrectionsListRoute)
          val result = route(application, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.corrections.routes.VatPeriodCorrectionsListWithFormController.onPageLoad(NormalMode, period).url
        }
      }

      "and there no uncompleted correction periods must display filled table and correct header" in {

        when(mockReturnStatusConnector.listStatuses(any())(any()))
          .thenReturn(getStatusResponse(allPeriods))

        val expectedTitle = "You have corrected the VAT amount for 3 return periods"
        val expectedTableRows = 3

        val application = applicationBuilder(userAnswers = addCorrectionPeriods(completeUserAnswers, allPeriods))
          .configure("bootstrap.filters.csrf.enabled" -> false)
          .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, vatPeriodCorrectionsListRoute)

          val result = route(application, request).value

          status(result) mustEqual OK
          val responseString = contentAsString(result)

          val doc = Jsoup.parse(responseString)
          doc.getElementsByClass("govuk-heading-xl").get(0).text() mustEqual expectedTitle
          doc.getElementsByClass("hmrc-add-to-a-list__contents").size() mustEqual expectedTableRows

          val view = application.injector.instanceOf[VatPeriodCorrectionsListView]
          responseString mustEqual view(NormalMode, period, allPeriodsModel)(request, messages(application)).toString
        }
      }
    }
  }
}
