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

import models.UserAnswers
import play.api.i18n.Messages
import queries.AllSalesFromNiQuery
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.CurrencyFormatter
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object TotalNetValueOfSalesSummary extends CurrencyFormatter {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(AllSalesFromNiQuery).map {
      allSales =>

        val totalNetValueOfSalesFromNi = allSales.map{ saleFromNi =>
          saleFromNi.vatRates.map(_.sales.map(_.netValueOfSales).getOrElse(BigDecimal(0))).sum
        }.sum

        SummaryListRowViewModel(
          key     = "checkYourAnswers.netValueOfSales.checkYourAnswersLabel",
          value   = ValueViewModel(HtmlContent(currencyFormat(totalNetValueOfSalesFromNi))),
          actions = Seq.empty
        )
    }
}
