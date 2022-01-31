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

package viewmodels.checkAnswers.corrections

import base.SpecBase
import models.{CheckLoopMode, CheckMode, CheckSecondLoopMode, Country, NormalMode}
import pages.corrections.{CorrectionCountryPage, CorrectionReturnPeriodPage, CountryVatCorrectionPage}
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

class CountryVatCorrectionSummarySpec extends SpecBase {

  implicit val m: Messages = stubMessages()

  private val expectedActionCheckLoop = Seq(
    ActionItemViewModel("site.change", controllers.corrections.routes.CountryVatCorrectionController.onPageLoad(CheckLoopMode, period, index, index, false).url)
      .withVisuallyHiddenText("countryVatCorrection.change.hidden")
      .withAttribute(("id", "change-correction-amount"))
  )

  private val expectedActionCheckSecondLoop = Seq(
    ActionItemViewModel("site.change", controllers.corrections.routes.CountryVatCorrectionController.onPageLoad(CheckSecondLoopMode, period, index, index, false).url)
      .withVisuallyHiddenText("countryVatCorrection.change.hidden")
      .withAttribute(("id", "change-correction-amount"))
  )

  private val expectedActionCheckMode = Seq(
    ActionItemViewModel("site.change", controllers.corrections.routes.CountryVatCorrectionController.onPageLoad(CheckMode, period, index, index, false).url)
      .withVisuallyHiddenText("countryVatCorrection.change.hidden")
      .withAttribute(("id", "change-correction-amount"))
  )

  private val answers = completeUserAnswers.set(CorrectionCountryPage(index, index), Country("DE", "Germany")).success.value
    .set(CorrectionReturnPeriodPage(index), period).success.value
    .set(CountryVatCorrectionPage(index, index), BigDecimal(100)).success.value

  "CountryVatCorrectionSummary" - {

    "must show summary when CountryVatCorrection exists when in Normal Mode" in {

      val result = CountryVatCorrectionSummary.row(answers, index, index, NormalMode)

      val expectedResult = SummaryListRowViewModel(
        "countryVatCorrection.checkYourAnswersLabel",
        ValueViewModel(HtmlContent("&pound;100")),
        expectedActionCheckLoop
      )

      result mustBe Some(expectedResult)
    }

    "must show summary when CountryVatCorrection exists when in Check Mode" in {

      val result = CountryVatCorrectionSummary.row(answers, index, index, CheckMode)

      val expectedResult = SummaryListRowViewModel(
        "countryVatCorrection.checkYourAnswersLabel",
        ValueViewModel(HtmlContent("&pound;100")),
        expectedActionCheckMode
      )

      result mustBe Some(expectedResult)
    }

    "must show summary when CountryVatCorrection exists when in Check Second Loop Mode" in {

      val result = CountryVatCorrectionSummary.row(answers, index, index, CheckSecondLoopMode)

      val expectedResult = SummaryListRowViewModel(
        "countryVatCorrection.checkYourAnswersLabel",
        ValueViewModel(HtmlContent("&pound;100")),
        expectedActionCheckSecondLoop
      )

      result mustBe Some(expectedResult)
    }

    "must not show summary when CountryVatCorrection doesn't exist" in {

      val result = CountryVatCorrectionSummary.row(completeUserAnswers, index, index, NormalMode)

      result mustBe None
    }
  }
}