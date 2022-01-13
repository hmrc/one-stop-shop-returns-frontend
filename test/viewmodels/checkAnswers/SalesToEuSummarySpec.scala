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

package viewmodels.checkAnswers

import base.SpecBase
import controllers.routes
import models.{CheckMode, CheckSecondLoopMode, CheckThirdLoopMode, Country, Index, Mode, NormalMode, VatOnSales, VatOnSalesChoice, VatRate}
import org.scalacheck.Arbitrary.arbitrary
import pages._
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.addtoalist.ListItem

class SalesToEuSummarySpec extends SpecBase {

  implicit val m: Messages = stubMessages()
  private val vatRate      = arbitrary[VatRate].sample.value
  private val country = arbitrary[Country].sample.value
  private val countryTo = arbitrary[Country].sample.value
  private val answers = completeUserAnswers
    .set(CountryOfSaleFromEuPage(Index(0)), country).success.value
    .set(CountryOfConsumptionFromEuPage(index, index), countryTo).success.value
    .set(VatRatesFromEuPage(index, index), List(vatRate)).success.value
    .set(NetValueOfSalesFromEuPage(index, index, index), BigDecimal(100)).success.value
    .set(VatOnSalesFromEuPage(index, index, index), VatOnSales(VatOnSalesChoice.Standard, BigDecimal(100))).success.value

  private def expectedChangeWithMode(mode: Mode) = routes.CheckSalesToEuController.onPageLoad(mode, answers.period, index, index).url
  private def expectedRemoveWithMode(mode: Mode) = routes.DeleteSalesToEuController.onPageLoad(mode, answers.period, index, index).url


  "SalesToEuSummary" - {

    "must show summary when AllSalesToEuQuery is not empty when in Normal Mode" in {

      val result = SalesToEuSummary.addToListRows(answers, NormalMode, index)

      val expectedResult = ListItem(
        name = HtmlFormat.escape(countryTo.name).toString,
        changeUrl = expectedChangeWithMode(CheckSecondLoopMode),
        removeUrl = expectedRemoveWithMode(NormalMode)
      )

      result mustBe Seq(expectedResult)
    }

    "must show summary when AllSalesToEuQuery is not empty when in Check Mode" in {

      val result = SalesToEuSummary.addToListRows(answers, CheckMode, index)

      val expectedResult = ListItem(
        name = HtmlFormat.escape(countryTo.name).toString,
        changeUrl = expectedChangeWithMode(CheckMode),
        removeUrl = expectedRemoveWithMode(CheckMode)
      )

      result mustBe Seq(expectedResult)
    }

    "must show summary when AllSalesToEuQuery is not empty when in Check Third Loop Mode" in {

      val result = SalesToEuSummary.addToListRows(answers, CheckThirdLoopMode, index)

      val expectedResult = ListItem(
        name = HtmlFormat.escape(countryTo.name).toString,
        changeUrl = expectedChangeWithMode(CheckThirdLoopMode),
        removeUrl = expectedRemoveWithMode(CheckThirdLoopMode)
      )

      result mustBe Seq(expectedResult)
    }

    "must not show summary when AllSalesToEuQuery is empty" in {

      val result = SalesToEuSummary.addToListRows(emptyUserAnswers, NormalMode, index)

      result mustBe List.empty
    }
  }
}