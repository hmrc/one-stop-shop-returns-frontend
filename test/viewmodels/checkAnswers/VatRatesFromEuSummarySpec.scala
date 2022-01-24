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
import models.{CheckLoopMode, CheckMode, CheckSecondLoopMode, CheckThirdLoopMode, Mode, NormalMode, VatRate}
import org.scalacheck.Arbitrary.arbitrary
import pages.VatRatesFromEuPage
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

class VatRatesFromEuSummarySpec extends SpecBase {

  implicit val m: Messages = stubMessages()
  private val vatRate      = arbitrary[VatRate].sample.value
  private val answers = completeUserAnswers.set(VatRatesFromEuPage(index, index), List(vatRate)).success.value

  private def expectedActionWithMode(mode: Mode) = Seq(
    ActionItemViewModel("site.change", routes.VatRatesFromEuController.onPageLoad(mode, answers.period, index, index).url)
      .withVisuallyHiddenText("vatRatesFromEu.change.hidden")
      .withAttribute(("id", "change-vat-rates"))

  )


  "VatRatesFromEuSummary" - {

    "must show summary when VatRatesFromEu exists when in Normal Mode" in {

      val result = VatRatesFromEuSummary.row(answers, index, index, NormalMode)

      val expectedResult = SummaryListRowViewModel(
        "vatRatesFromEu.checkYourAnswersLabel",
        ValueViewModel(HtmlContent(vatRate.rateForDisplay)),
        expectedActionWithMode(CheckLoopMode)
      )

      result mustBe Some(expectedResult)
    }

    "must show summary when VatRatesFromEu exists when in Check Mode" in {

      val result = VatRatesFromEuSummary.row(answers, index, index, CheckMode)

      val expectedResult = SummaryListRowViewModel(
        "vatRatesFromEu.checkYourAnswersLabel",
        ValueViewModel(HtmlContent(vatRate.rateForDisplay)),
        expectedActionWithMode(CheckMode)
      )

      result mustBe Some(expectedResult)
    }

    "must show summary when VatRatesFromEu exists when in Check Second Loop Mode" in {

      val result = VatRatesFromEuSummary.row(answers, index, index, CheckSecondLoopMode)

      val expectedResult = SummaryListRowViewModel(
        "vatRatesFromEu.checkYourAnswersLabel",
        ValueViewModel(HtmlContent(vatRate.rateForDisplay)),
        expectedActionWithMode(CheckSecondLoopMode)
      )

      result mustBe Some(expectedResult)
    }

    "must show summary when VatRatesFromEu exists when in Check Third Loop Mode" in {

      val result = VatRatesFromEuSummary.row(answers, index, index, CheckThirdLoopMode)

      val expectedResult = SummaryListRowViewModel(
        "vatRatesFromEu.checkYourAnswersLabel",
        ValueViewModel(HtmlContent(vatRate.rateForDisplay)),
        expectedActionWithMode(CheckThirdLoopMode)
      )

      result mustBe Some(expectedResult)
    }

    "must not show summary when VatRatesFromEu doesn't exist" in {

      val result = VatRatesFromEuSummary.row(emptyUserAnswers, index, index, NormalMode)

      result mustBe None
    }
  }
}