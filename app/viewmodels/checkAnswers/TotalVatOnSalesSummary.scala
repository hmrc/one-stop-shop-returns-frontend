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

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.CurrencyFormatter
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object TotalVatOnSalesSummary extends CurrencyFormatter {

  def row(totalVatOnSales: BigDecimal)(implicit messages: Messages): Option[SummaryListRow] =
    Some(SummaryListRowViewModel(
      key     = "checkYourAnswers.vatOnSales.checkYourAnswersLabel",
      value   =
        ValueViewModel(HtmlContent(currencyFormat(totalVatOnSales)))
          .withCssClass("govuk-!-font-weight-bold"),
      actions = Seq.empty
    ))
}
