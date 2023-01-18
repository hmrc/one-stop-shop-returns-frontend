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

package viewmodels.checkAnswers

import base.SpecBase
import controllers.routes
import models.{CheckMode, CheckSecondLoopMode, Country, Mode, NormalMode, VatOnSales, VatOnSalesChoice, VatRate}
import org.scalacheck.Arbitrary.arbitrary
import pages._
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.addtoalist.ListItem

class SalesFromNiSummarySpec extends SpecBase {

  implicit val m: Messages = stubMessages()
  private val vatRate      = arbitrary[VatRate].sample.value
  private val country = arbitrary[Country].sample.value
  private val answers = completeUserAnswers
    .set(CountryOfConsumptionFromNiPage(index), country).success.value
    .set(VatRatesFromNiPage(index), List(vatRate)).success.value
    .set(NetValueOfSalesFromNiPage(index, index), BigDecimal(100)).success.value
    .set(VatOnSalesFromNiPage(index, index), VatOnSales(VatOnSalesChoice.Standard, BigDecimal(100))).success.value

  private def expectedChangeWithMode(mode: Mode) = routes.CheckSalesFromNiController.onPageLoad(mode, answers.period, index).url
  private def expectedRemoveWithMode(mode: Mode) = routes.DeleteSalesFromNiController.onPageLoad(mode, answers.period, index).url


  "SalesFromNiSummary" - {

    "must show summary when AllSalesFromNiQuery is not empty when in Normal Mode" in {

      val result = SalesFromNiSummary.addToListRows(answers, NormalMode)

      val expectedResult = ListItem(
        name = HtmlFormat.escape(country.name).toString,
        changeUrl = expectedChangeWithMode(CheckSecondLoopMode),
        removeUrl = expectedRemoveWithMode(NormalMode)
      )

      result mustBe Seq(expectedResult)
    }

    "must show summary when AllSalesFromNiQuery is not empty when in Check Mode" in {

      val result = SalesFromNiSummary.addToListRows(answers, CheckMode)

      val expectedResult = ListItem(
        name = HtmlFormat.escape(country.name).toString,
        changeUrl = expectedChangeWithMode(CheckMode),
        removeUrl = expectedRemoveWithMode(CheckMode)
      )

      result mustBe Seq(expectedResult)
    }

    "must not show summary when AllSalesFromNiQuery is empty" in {

      val result = SalesFromNiSummary.addToListRows(emptyUserAnswers, NormalMode)

      result mustBe List.empty
    }
  }
}