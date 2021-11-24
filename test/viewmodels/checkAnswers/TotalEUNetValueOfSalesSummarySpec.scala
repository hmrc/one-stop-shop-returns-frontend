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
import models.CheckMode
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

class TotalEUNetValueOfSalesSummarySpec extends SpecBase {

  implicit val m: Messages = stubMessages()

  private val expectedAction = Seq(
    ActionItemViewModel(
      "site.change",
      routes.SalesFromEuListController.onPageLoad(CheckMode, completeUserAnswers.period).url)
      .withVisuallyHiddenText("soldGoodsFromEu.changeEUSales.hidden")
      .withAttribute(("id", "change-sales-excluding-vat-eu"))

  )

  "TotalEUNetValueOfSalesSummary" - {

    "must show summary when totalEUNetValueOfSales exists" in {

      val result = TotalEUNetValueOfSalesSummary.row(completeUserAnswers, Some(BigDecimal(100)))

      val expectedResult = SummaryListRowViewModel(
        "netValueOfSalesFromEu.checkYourAnswersLabel",
        ValueViewModel(HtmlContent("&pound;100")),
        expectedAction
      )

      result mustBe Some(expectedResult)
    }

    "must not show summary when totalEUNetValueOfSales doesn't exist" in {

      val result = TotalEUNetValueOfSalesSummary.row(completeUserAnswers, None)

      result mustBe None
    }
  }
}