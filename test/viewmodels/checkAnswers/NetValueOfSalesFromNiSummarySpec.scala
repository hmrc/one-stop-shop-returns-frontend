/*
 * Copyright 2024 HM Revenue & Customs
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
import models.{CheckFinalInnerLoopMode, CheckInnerLoopMode, CheckMode, CheckSecondInnerLoopMode, CheckSecondLoopMode, Mode, NormalMode, VatRate}
import org.scalacheck.Arbitrary.arbitrary
import pages.NetValueOfSalesFromNiPage
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

class NetValueOfSalesFromNiSummarySpec extends SpecBase {

  implicit val m: Messages = stubMessages()
  private val vatRate      = arbitrary[VatRate].sample.value
  private val answers = completeUserAnswers.set(NetValueOfSalesFromNiPage(index, index), BigDecimal(100)).success.value

  private def expectedActionWithMode(mode: Mode) = Seq(
    ActionItemViewModel(
      "site.change",
      routes.NetValueOfSalesFromNiController.onPageLoad(mode, answers.period, index, index).url
    ).withVisuallyHiddenText("netValueOfSalesFromNi.change.hidden")
    .withAttribute(("id", s"change-net-value-sales-${vatRate.rate}-percent")))


  "NetValueOfSalesFromNiSummary" - {

    "must show summary when NetValueOfSalesFromNi exists when in Normal Mode" in {

      val result = NetValueOfSalesFromNiSummary.row(answers, index, index, vatRate, NormalMode)

      val expectedResult = SummaryListRowViewModel(
        "netValueOfSalesFromNi.checkYourAnswersLabel",
        ValueViewModel(HtmlContent("&pound;100")),
        expectedActionWithMode(CheckInnerLoopMode)
      )

      result mustBe Some(expectedResult)
    }

    "must show summary when NetValueOfSalesFromNi exists when in Check Mode" in {

      val result = NetValueOfSalesFromNiSummary.row(answers, index, index, vatRate, CheckMode)

      val expectedResult = SummaryListRowViewModel(
        "netValueOfSalesFromNi.checkYourAnswersLabel",
        ValueViewModel(HtmlContent("&pound;100")),
        expectedActionWithMode(CheckFinalInnerLoopMode)
      )

      result mustBe Some(expectedResult)
    }

    "must show summary when NetValueOfSalesFromNi exists when in Check Second Loop Mode" in {

      val result = NetValueOfSalesFromNiSummary.row(answers, index, index, vatRate, CheckSecondLoopMode)

      val expectedResult = SummaryListRowViewModel(
        "netValueOfSalesFromNi.checkYourAnswersLabel",
        ValueViewModel(HtmlContent("&pound;100")),
        expectedActionWithMode(CheckSecondInnerLoopMode)
      )

      result mustBe Some(expectedResult)
    }

    "must not show summary when NetValueOfSalesFromNi doesn't exist" in {

      val result = NetValueOfSalesFromNiSummary.row(emptyUserAnswers, index, index, vatRate, NormalMode)

      result mustBe None
    }
  }
}