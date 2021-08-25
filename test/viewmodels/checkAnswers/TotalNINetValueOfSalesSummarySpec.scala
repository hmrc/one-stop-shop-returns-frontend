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
import models.{CheckMode, Country, SalesAtVatRate, VatRate, VatRateType}
import pages.{CountryOfConsumptionFromNiPage, SalesAtVatRateFromNiPage, VatRatesFromNiPage}
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

class TotalNINetValueOfSalesSummarySpec extends SpecBase {

  implicit val m: Messages = stubMessages()
  private val expectedAction = Seq(
    ActionItemViewModel(
      "site.change",
      routes.SalesFromNiListController.onPageLoad(CheckMode, completeSalesFromNIUserAnswers.period).url)
      .withVisuallyHiddenText("soldGoodsFromNi.change.hidden")
  )

  "TotalNetValueOfSalesFromNiSummary" - {

    "must show correct net total sales for one country with one vat rate" in {

      val answers = completeSalesFromNIUserAnswers
      val result = TotalNINetValueOfSalesSummary.row(answers)

      val expectedResult = SummaryListRowViewModel(
        "netValueOfSalesFromNi.checkYourAnswersLabel",
        ValueViewModel(HtmlContent("&pound;100")),
        expectedAction
      )

      result mustBe Some(expectedResult)
    }

    "must show correct net total sales for one country with multiple vat rates" in {

      val answers = completeSalesFromNIUserAnswers
        .set(
          VatRatesFromNiPage(index),
          List(
            VatRate(10, VatRateType.Reduced, arbitraryDate),
            VatRate(20, VatRateType.Reduced, arbitraryDate)
          )
        ).success.value
        .set(SalesAtVatRateFromNiPage(index, index), SalesAtVatRate(BigDecimal(100), BigDecimal(200))).success.value
        .set(SalesAtVatRateFromNiPage(index, index + 1), SalesAtVatRate(BigDecimal(300), BigDecimal(400))).success.value

      val result = TotalNINetValueOfSalesSummary.row(answers)

      val expectedResult = SummaryListRowViewModel(
        "netValueOfSalesFromNi.checkYourAnswersLabel",
        ValueViewModel(HtmlContent("&pound;400")),
        expectedAction
      )

      result mustBe Some(expectedResult)
    }

    "must show correct net total sales for multiple countries with vat rates" in {

      val answers = completeSalesFromNIUserAnswers
        .set(
          VatRatesFromNiPage(index),
          List(
            VatRate(10, VatRateType.Reduced, arbitraryDate),
            VatRate(20, VatRateType.Reduced, arbitraryDate)
          )
        ).success.value
        .set(SalesAtVatRateFromNiPage(index, index), SalesAtVatRate(BigDecimal(100), BigDecimal(200))).success.value
        .set(SalesAtVatRateFromNiPage(index, index + 1), SalesAtVatRate(BigDecimal(300), BigDecimal(400))).success.value
        .set(CountryOfConsumptionFromNiPage(index + 1), Country("OTH", "OtherCountry")).success.value
        .set(VatRatesFromNiPage(index + 1), List(VatRate(10, VatRateType.Reduced, arbitraryDate))).success.value
        .set(SalesAtVatRateFromNiPage(index + 1, index), SalesAtVatRate(BigDecimal(100), BigDecimal(1000))).success.value

      val result = TotalNINetValueOfSalesSummary.row(answers)

      val expectedResult = SummaryListRowViewModel(
        "netValueOfSalesFromNi.checkYourAnswersLabel",
        ValueViewModel(HtmlContent("&pound;500")),
        expectedAction
      )

      result mustBe Some(expectedResult)
    }
  }
}
