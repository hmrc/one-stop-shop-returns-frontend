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

package viewmodels.checkAnswers.corrections

import base.SpecBase
import models.{CheckLoopMode, CheckMode, CheckSecondLoopMode, CheckThirdLoopMode, Country, Index, Mode, NormalMode}
import pages.corrections.{CorrectionCountryPage, CorrectionReturnPeriodPage, CountryVatCorrectionPage}
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.addtoalist.ListItem
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

class VatPeriodCorrectionsListSummarySpec extends SpecBase {

  implicit val m: Messages = stubMessages()

  private val answers = completeUserAnswers.set(CorrectionCountryPage(index, index), Country("DE", "Germany")).success.value
    .set(CorrectionReturnPeriodPage(index), period).success.value
    .set(CountryVatCorrectionPage(index, index), BigDecimal(100)).success.value

  private def expectedResult(mode: Mode, currentMode: Mode) = ListItem(
    name = "1 July site.to 30 September 2021",
    changeUrl = controllers.corrections.routes.VatCorrectionsListController.onPageLoad(mode, period, index).url,
    removeUrl = controllers.corrections.routes.RemovePeriodCorrectionController.onPageLoad(currentMode, period, index).url
  )

  "VatPeriodCorrectionsListSummary" - {

    "must show summary when completed periods exist when in Normal Mode" in {

      val result = VatPeriodCorrectionsListSummary.getCompletedRows(answers, NormalMode)

      result mustBe Seq(expectedResult(CheckThirdLoopMode, NormalMode))
    }

    "must show summary when completed periods exist when in Check Mode" in {

      val result = VatPeriodCorrectionsListSummary.getCompletedRows(answers, CheckMode)

      result mustBe Seq(expectedResult(CheckMode, CheckMode))
    }

    "must not show summary when completed periods don't exist" in {

      val result = VatPeriodCorrectionsListSummary.getCompletedRows(completeUserAnswers, NormalMode)

      result mustBe List.empty
    }
  }
}