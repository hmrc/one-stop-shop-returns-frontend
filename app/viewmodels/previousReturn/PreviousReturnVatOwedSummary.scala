/*
 * Copyright 2025 HM Revenue & Customs
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

package viewmodels.previousReturn

import models.Country.getCountryName
import models.etmp.EtmpVatReturn
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryListRow}
import utils.CurrencyFormatter
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object PreviousReturnVatOwedSummary {

  def row(etmpVatReturn: EtmpVatReturn)(implicit messages: Messages): Seq[SummaryListRow] = {
    etmpVatReturn.balanceOfVATDueForMS.filter(balanceOfVatDue => balanceOfVatDue.totalVATDueGBP > 0)
      .map { etmpVatReturnBalanceOfVatDue =>

        val country = getCountryName(etmpVatReturnBalanceOfVatDue.msOfConsumption)

        val value = ValueViewModel(
          content = HtmlContent(
            CurrencyFormatter.currencyFormat(etmpVatReturnBalanceOfVatDue.totalVATDueGBP)
          )
        ).withCssClass("govuk-table__cell--numeric")

        SummaryListRowViewModel(
          key = Key(country),
          value = value
        )
      }
  }
}
