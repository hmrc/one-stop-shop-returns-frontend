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

import models.TotalVatToCountry
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryListRow}
import utils.CurrencyFormatter
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object VatOwedToEuCountriesSummary extends CurrencyFormatter {

  def row(totalVatToCountries: List[TotalVatToCountry])(implicit messages: Messages): Seq[SummaryListRow] =
    Seq(SummaryListRowViewModel(
      key     = "checkYourAnswersLabel.country",
      value   =
        ValueViewModel("checkYourAnswersLabel.amount").withCssClass("govuk-!-font-weight-bold"),
      actions = Seq.empty
    )) ++
    totalVatToCountries.map {
      totalVatToCountry =>
        SummaryListRowViewModel(
          key     = Key(totalVatToCountry.country.name).withCssClass("govuk-!-font-weight-regular"),
          value   = ValueViewModel(HtmlContent(currencyFormat(totalVatToCountry.totalVat))),
          actions = Seq.empty
        )
    }
}
