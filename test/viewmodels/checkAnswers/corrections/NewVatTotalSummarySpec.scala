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
import models.{CheckMode, Country}
import pages.corrections.{CorrectionCountryPage, CorrectionReturnPeriodPage, CountryVatCorrectionPage}
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

class NewVatTotalSummarySpec extends SpecBase {

  implicit val m: Messages = stubMessages()

  private val expectedAction = Seq.empty

  private val answers = completeUserAnswers.set(CorrectionCountryPage(index, index), Country("DE", "Germany")).success.value
    .set(CorrectionReturnPeriodPage(index), period).success.value
    .set(CountryVatCorrectionPage(index, index), BigDecimal(100)).success.value

  private val originalAmount = BigDecimal(10)

  "NewVatTotalSummary" - {

    "must show summary when CountryVatCorrection exists" in {

      val result = NewVatTotalSummary.row(answers, index, index, originalAmount)

      val expectedResult = SummaryListRowViewModel(
        "newVatTotal.checkYourAnswersLabel",
        ValueViewModel(HtmlContent("&pound;110")),
        expectedAction
      )

      result mustBe Some(expectedResult)
    }

    "must not show summary when CountryVatCorrection doesn't exist" in {

      val result = NewVatTotalSummary.row(completeUserAnswers, index, index, originalAmount)

      result mustBe None
    }
  }
}