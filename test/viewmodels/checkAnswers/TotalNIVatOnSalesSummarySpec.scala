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

package viewmodels.checkAnswers

import base.SpecBase
import controllers.routes
import models.{CheckMode, Country, VatRate, VatRateType}
import pages.{CountryOfConsumptionFromNiPage, NetValueOfSalesFromNiPage, VatOnSalesFromNiPage, VatRatesFromNiPage}
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

class TotalNIVatOnSalesSummarySpec extends SpecBase {

  implicit val m: Messages = stubMessages()
  private val expectedAction = Seq(
    ActionItemViewModel(
      "site.change",
      routes.SalesFromNiListController.onPageLoad(CheckMode, completeUserAnswers.period).url)
      .withVisuallyHiddenText("vatOnSalesFromNi.change.hidden")
  )

  "TotalVatOnSalesFromNiSummary" - {

    "must show correct vat total for one country with one vat rate" in {

      val answers = completeUserAnswers
      val result = TotalNIVatOnSalesSummary.row(answers)

      val expectedResult = SummaryListRowViewModel(
        "vatOnSalesFromNi.checkYourAnswersLabel",
        ValueViewModel(HtmlContent("&pound;1,000")),
        expectedAction
      )

      result mustBe Some(expectedResult)
    }

    "must show correct vat total sales for one country with multiple vat rates" in {

      val answers = completeUserAnswers
        .set(
          VatRatesFromNiPage(index),
          List(
            VatRate(10, VatRateType.Reduced, arbitraryDate),
            VatRate(20, VatRateType.Reduced, arbitraryDate)
          )
        ).success.value
        .set(NetValueOfSalesFromNiPage(index, index), BigDecimal(100)).success.value
        .set(VatOnSalesFromNiPage(index, index), BigDecimal(200)).success.value
        .set(NetValueOfSalesFromNiPage(index, index + 1), BigDecimal(300)).success.value
        .set(VatOnSalesFromNiPage(index, index + 1), BigDecimal(400)).success.value

      val result = TotalNIVatOnSalesSummary.row(answers)

      val expectedResult = SummaryListRowViewModel(
        "vatOnSalesFromNi.checkYourAnswersLabel",
        ValueViewModel(HtmlContent("&pound;600")),
        expectedAction
      )

      result mustBe Some(expectedResult)
    }

    "must show correct vat total sales for multiple countries with vat rates" in {

      val answers = completeUserAnswers
        .set(
          VatRatesFromNiPage(index),
          List(
            VatRate(10, VatRateType.Reduced, arbitraryDate),
            VatRate(20, VatRateType.Reduced, arbitraryDate)
          )
        ).success.value
        .set(NetValueOfSalesFromNiPage(index, index), BigDecimal(100)).success.value
        .set(VatOnSalesFromNiPage(index, index), BigDecimal(200)).success.value
        .set(NetValueOfSalesFromNiPage(index, index + 1), BigDecimal(300)).success.value
        .set(VatOnSalesFromNiPage(index, index + 1), BigDecimal(400)).success.value
        .set(CountryOfConsumptionFromNiPage(index + 1), Country("OTH", "OtherCountry")).success.value
        .set(VatRatesFromNiPage(index + 1), List(VatRate(10, VatRateType.Reduced, arbitraryDate))).success.value
        .set(NetValueOfSalesFromNiPage(index + 1, index), BigDecimal(100)).success.value
        .set(VatOnSalesFromNiPage(index + 1, index), BigDecimal(1000)).success.value

      val result = TotalNIVatOnSalesSummary.row(answers)

      val expectedResult = SummaryListRowViewModel(
        "vatOnSalesFromNi.checkYourAnswersLabel",
        ValueViewModel(HtmlContent("&pound;1,600")),
        expectedAction
      )

      result mustBe Some(expectedResult)
    }
  }
}
