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
import models.{CheckMode, Country, Index, SalesAtVatRate}
import pages.{CountryOfConsumptionFromEuPage, CountryOfSaleFromEuPage, SalesAtVatRateFromEuPage, SoldGoodsFromEuPage, VatRatesFromEuPage}
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

class TotalEUNetValueOfSalesSummarySpec extends SpecBase {

  implicit val m: Messages = stubMessages()

  private val index0 = Index(0)
  private val index1 = Index(1)

  private val expectedAction = Seq(
    ActionItemViewModel(
      "site.change",
      routes.SalesFromEuListController.onPageLoad(CheckMode, completeUserAnswers.period).url)
      .withVisuallyHiddenText("soldGoodsFromEu.change.hidden")
  )

  "TotalEUNetValueOfSalesSummary" - {

    "must show correct net total sales for one country from, one country to with one vat rate" in {

      val result = TotalEUNetValueOfSalesSummary.row(completeUserAnswers)

      val expectedResult = SummaryListRowViewModel(
        "netValueOfSalesFromEu.checkYourAnswersLabel",
        ValueViewModel(HtmlContent("&pound;100")),
        expectedAction
      )

      result mustBe Some(expectedResult)
    }

    "must show correct net total sales for one country from, one country to with multiple vat rates" in {
      val answers = completeUserAnswers
        .set(VatRatesFromEuPage(index, index), List(twentyPercentVatRate, fivePercentVatRate)).success.value
        .set(SalesAtVatRateFromEuPage(index, index, index), SalesAtVatRate(BigDecimal(100), BigDecimal(20))).success.value
        .set(SalesAtVatRateFromEuPage(index, index, index + 1), SalesAtVatRate(BigDecimal(200), BigDecimal(20))).success.value

      val result = TotalEUNetValueOfSalesSummary.row(answers)

      val expectedResult = SummaryListRowViewModel(
        "netValueOfSalesFromEu.checkYourAnswersLabel",
        ValueViewModel(HtmlContent("&pound;300")),
        expectedAction
      )

      result mustBe Some(expectedResult)
    }

    "must show correct net total sales for one country from, multiple countries to with multiple vat rates" in {
      val answers = completeUserAnswers
        .set(VatRatesFromEuPage(index, index), List(twentyPercentVatRate, fivePercentVatRate)).success.value
        .set(SalesAtVatRateFromEuPage(index, index, index), SalesAtVatRate(BigDecimal(100), BigDecimal(20))).success.value
        .set(SalesAtVatRateFromEuPage(index, index, index + 1), SalesAtVatRate(BigDecimal(200), BigDecimal(20))).success.value
        .set(CountryOfConsumptionFromEuPage(index, index + 1), Country("DK", "Denmark")).success.value
        .set(VatRatesFromEuPage(index, index + 1), List(twentyPercentVatRate)).success.value
        .set(SalesAtVatRateFromEuPage(index, index + 1, index), SalesAtVatRate(BigDecimal(100), BigDecimal(20))).success.value

      val result = TotalEUNetValueOfSalesSummary.row(answers)

      val expectedResult = SummaryListRowViewModel(
        "netValueOfSalesFromEu.checkYourAnswersLabel",
        ValueViewModel(HtmlContent("&pound;400")),
        expectedAction
      )

      result mustBe Some(expectedResult)
    }

    "must show correct net total sales for multiple country from, multiple countries to with multiple vat rates" in {
      val answers = emptyUserAnswers
        .set(SoldGoodsFromEuPage,true).success.value
        //countries from
        .set(CountryOfSaleFromEuPage(index0), Country("HR", "Croatia")).success.value
        .set(CountryOfSaleFromEuPage(index1), Country("EE", "Estonia")).success.value

        //countries to
        .set(CountryOfConsumptionFromEuPage(index0, index0), Country("BE", "Belgium")).success.value
        .set(CountryOfConsumptionFromEuPage(index0, index1), Country("DK", "Denmark")).success.value
        .set(CountryOfConsumptionFromEuPage(index1, index0), Country("BE", "Belgium")).success.value
        .set(CountryOfConsumptionFromEuPage(index1, index1), Country("DK", "Denmark")).success.value

        //vat rates
        .set(VatRatesFromEuPage(index0, index0), List(twentyPercentVatRate)).success.value
        .set(VatRatesFromEuPage(index0, index1), List(twentyPercentVatRate)).success.value
        .set(VatRatesFromEuPage(index1, index0), List(twentyPercentVatRate)).success.value
        .set(VatRatesFromEuPage(index1, index1), List(twentyPercentVatRate)).success.value

        //sales at vat rate
        .set(SalesAtVatRateFromEuPage(index0, index0, index0), SalesAtVatRate(BigDecimal(100), BigDecimal(20))).success.value
        .set(SalesAtVatRateFromEuPage(index0, index1, index0), SalesAtVatRate(BigDecimal(200), BigDecimal(20))).success.value
        .set(SalesAtVatRateFromEuPage(index1, index0, index0), SalesAtVatRate(BigDecimal(300), BigDecimal(20))).success.value
        .set(SalesAtVatRateFromEuPage(index1, index1, index0), SalesAtVatRate(BigDecimal(400), BigDecimal(20))).success.value

      val result = TotalEUNetValueOfSalesSummary.row(answers)

      val expectedResult = SummaryListRowViewModel(
        "netValueOfSalesFromEu.checkYourAnswersLabel",
        ValueViewModel(HtmlContent("&pound;1,000")),
        expectedAction
      )

      result mustBe Some(expectedResult)
    }
  }
}