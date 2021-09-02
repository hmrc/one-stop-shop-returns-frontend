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

package viewmodels.previousReturn

import models.domain.VatReturn
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object PreviousReturnSummary {

  def rows(vatReturn: VatReturn)(implicit messages: Messages): Seq[SummaryListRow] = {
    Seq(
      vatOwedRow(vatReturn),
      dateSubmittedRow(vatReturn),
      dateDueRow(vatReturn),
      returnReferenceNumber(vatReturn),
      paymentReferenceNumber()
    ).flatten
  }

  private[this] def vatOwedRow(vatReturn: VatReturn)(implicit messages: Messages): Option[SummaryListRow] = {
      Some(SummaryListRowViewModel(
        key = "previousReturn.vatOwed.label",
        value = ValueViewModel(HtmlFormat.escape(vatReturn.toString).toString),
        actions = Seq.empty
      ))
  }

  private[this] def dateSubmittedRow(vatReturn: VatReturn)(implicit messages: Messages): Option[SummaryListRow] = {
    Some(SummaryListRowViewModel(
      key = "previousReturn.dateSubmitted.label",
      value = ValueViewModel(HtmlFormat.escape(vatReturn.submissionReceived.toString).toString),
      actions = Seq.empty
    ))
  }

  private[this] def dateDueRow(vatReturn: VatReturn)(implicit messages: Messages): Option[SummaryListRow] = {
    Some(SummaryListRowViewModel(
      key = "previousReturn.dateDue.label",
      value = ValueViewModel(HtmlFormat.escape(vatReturn.submissionReceived.toString).toString),
      actions = Seq.empty
    ))
  }

  private[this] def returnReferenceNumber(vatReturn: VatReturn)(implicit messages: Messages): Option[SummaryListRow] = {
    Some(SummaryListRowViewModel(
      key = "previousReturn.returnReference.label",
      value = ValueViewModel(HtmlFormat.escape(vatReturn.reference.value).toString),
      actions = Seq.empty
    ))
  }

  private[this] def paymentReferenceNumber()(implicit messages: Messages): Option[SummaryListRow] = {
    None
  }


}
