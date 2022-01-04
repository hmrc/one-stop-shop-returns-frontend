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
import models.{CheckFinalInnerLoopMode, CheckInnerLoopMode, CheckMode, CheckSecondInnerLoopMode, CheckSecondLoopMode, Mode, NormalMode, VatOnSales, VatOnSalesChoice, VatRate}
import org.scalacheck.Arbitrary.arbitrary
import pages.VatOnSalesFromNiPage
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

class VatOnSalesFromNiSummarySpec extends SpecBase {

  implicit val m: Messages = stubMessages()
  private val vatRate      = arbitrary[VatRate].sample.value
  private val answers = completeUserAnswers.set(VatOnSalesFromNiPage(index, index), VatOnSales(VatOnSalesChoice.Standard, BigDecimal(100))).success.value

  private def expectedActionWithMode(mode: Mode) = Seq(
    ActionItemViewModel(
      "site.change",
      routes.VatOnSalesFromNiController.onPageLoad(mode, answers.period, index, index).url
    ).withVisuallyHiddenText("vatOnSalesFromNi.change.hidden")
      .withAttribute(("id", s"change-vat-on-sales-${vatRate.rate}-percent"))
  )


  "VatOnSalesFromNiSummary" - {

    "must show summary when VatOnSalesFromNi exists when in Normal Mode" in {

      val result = VatOnSalesFromNiSummary.row(answers, index, index, vatRate, NormalMode)

      val expectedResult = SummaryListRowViewModel(
        "vatOnSalesFromNi.checkYourAnswersLabel",
        ValueViewModel(HtmlContent("&pound;100")),
        expectedActionWithMode(CheckInnerLoopMode)
      )

      result mustBe Some(expectedResult)
    }

    "must show summary when VatOnSalesFromNi exists when in Check Mode" in {

      val result = VatOnSalesFromNiSummary.row(answers, index, index, vatRate, CheckMode)

      val expectedResult = SummaryListRowViewModel(
        "vatOnSalesFromNi.checkYourAnswersLabel",
        ValueViewModel(HtmlContent("&pound;100")),
        expectedActionWithMode(CheckFinalInnerLoopMode)
      )

      result mustBe Some(expectedResult)
    }

    "must show summary when VatOnSalesFromNi exists when in Check Second Loop Mode" in {

      val result = VatOnSalesFromNiSummary.row(answers, index, index, vatRate, CheckSecondLoopMode)

      val expectedResult = SummaryListRowViewModel(
        "vatOnSalesFromNi.checkYourAnswersLabel",
        ValueViewModel(HtmlContent("&pound;100")),
        expectedActionWithMode(CheckSecondInnerLoopMode)
      )

      result mustBe Some(expectedResult)
    }

    "must not show summary when VatOnSalesFromNi doesn't exist" in {

      val result = VatOnSalesFromNiSummary.row(emptyUserAnswers, index, index, vatRate, NormalMode)

      result mustBe None
    }
  }
}