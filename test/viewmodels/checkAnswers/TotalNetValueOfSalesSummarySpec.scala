package viewmodels.checkAnswers

import base.SpecBase
import models.{Country, VatRate}
import models.VatRateType.Reduced
import pages.{CountryOfConsumptionFromNiPage, NetValueOfSalesFromNiPage, VatOnSalesFromNiPage, VatRatesFromNiPage}
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

class TotalNetValueOfSalesSummarySpec extends SpecBase {

  implicit val m: Messages = stubMessages()

  "TotalNetValueOfSalesSummary" - {

    "must show correct net total sales for one country with one vat rate" in {

      val answers = completeUserAnswers
      val result = TotalNetValueOfSalesSummary.row(answers)

      val expectedResult = SummaryListRowViewModel(
        "checkYourAnswers.netValueOfSales.checkYourAnswersLabel",
        ValueViewModel(HtmlContent("&pound;100")),
        Seq.empty
      )

      result mustBe Some(expectedResult)
    }

    "must show correct net total sales for one country with multiple vat rates" in {
      val answers = completeUserAnswers
        .set(
          VatRatesFromNiPage(index),
          List(
            VatRate(10, Reduced, arbitraryDate),
            VatRate(20, Reduced, arbitraryDate)
          )
        ).success.value
        .set(NetValueOfSalesFromNiPage(index, index), BigDecimal(100)).success.value
        .set(VatOnSalesFromNiPage(index, index), BigDecimal(200)).success.value
        .set(NetValueOfSalesFromNiPage(index, index + 1), BigDecimal(300)).success.value
        .set(VatOnSalesFromNiPage(index, index + 1), BigDecimal(400)).success.value

      val result = TotalNetValueOfSalesSummary.row(answers)
      val expectedResult = SummaryListRowViewModel(
        "checkYourAnswers.netValueOfSales.checkYourAnswersLabel",
        ValueViewModel(HtmlContent("&pound;400")),
        Seq.empty
      )

      result mustBe Some(expectedResult)
    }

    "must show correct net total sales for multiple countries with multiple vat rates" in {
      val answers = completeUserAnswers
        .set(
          VatRatesFromNiPage(index),
          List(
            VatRate(10, Reduced, arbitraryDate),
            VatRate(20, Reduced, arbitraryDate)
          )
        ).success.value
        .set(NetValueOfSalesFromNiPage(index, index), BigDecimal(100)).success.value
        .set(VatOnSalesFromNiPage(index, index), BigDecimal(200)).success.value
        .set(NetValueOfSalesFromNiPage(index, index + 1), BigDecimal(300)).success.value
        .set(VatOnSalesFromNiPage(index, index + 1), BigDecimal(400)).success.value
        .set(CountryOfConsumptionFromNiPage(index + 1), Country("OTH", "OtherCountry")).success.value
        .set(VatRatesFromNiPage(index + 1), List(VatRate(10, Reduced, arbitraryDate))).success.value
        .set(NetValueOfSalesFromNiPage(index + 1, index), BigDecimal(100)).success.value
        .set(VatOnSalesFromNiPage(index + 1, index), BigDecimal(1000)).success.value

      val result = TotalNetValueOfSalesSummary.row(answers)
      val expectedResult = SummaryListRowViewModel(
        "checkYourAnswers.netValueOfSales.checkYourAnswersLabel",
        ValueViewModel(HtmlContent("&pound;500")),
        Seq.empty
      )

      result mustBe Some(expectedResult)
    }
  }
}
