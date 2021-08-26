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

import controllers.routes
import models.{CheckMode, UserAnswers}
import play.api.i18n.Messages
import queries.AllSalesFromEuQuery
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.CurrencyFormatter
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object TotalEUNetValueOfSalesSummary extends CurrencyFormatter {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] = {
    answers.get(AllSalesFromEuQuery).map {
      allSalesFromEu =>

        val totalNetSalesFromEu =
          allSalesFromEu.map {
            saleFromEu =>
              saleFromEu.salesFromCountry.map {
                salesToCountry =>
                  salesToCountry.salesAtVatRate.map(_.netValueOfSales).sum
              }.sum
          }.sum

        SummaryListRowViewModel(
          key     = "netValueOfSalesFromEu.checkYourAnswersLabel",
          value   = ValueViewModel(HtmlContent(currencyFormat(totalNetSalesFromEu))),
          actions = Seq(
            ActionItemViewModel("site.change", routes.SalesFromEuListController.onPageLoad(CheckMode, answers.period).url)
              .withVisuallyHiddenText(messages("soldGoodsFromEu.change.hidden"))
          )
        )
    }
  }
}