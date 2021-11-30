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

package viewmodels.previousReturn.corrections

import models.corrections.{CorrectionPayload, CorrectionToCountry}
import play.api.i18n.Messages
import viewmodels.govuk.summarylist._
import viewmodels.implicits._
import viewmodels.TitledSummaryList
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryListRow}
import utils.CurrencyFormatter._


object CorrectionSummary {

  def getAllCorrections(maybeCorrectionPayload: Option[CorrectionPayload])(implicit messages: Messages): Map[String, Seq[TitledSummaryList]] = {
    maybeCorrectionPayload match {
      case Some(correctionPayload) => (for {
        correction <- correctionPayload.corrections
      } yield Map(
        messages("previousReturn.correction.correctionPeriod.title", correction.correctionReturnPeriod.displayText) ->
          summaryRowsOfCorrectionToCountry(correction.correctionsToCountry)
      )).flatten.toMap
      case _ => Map.empty
    }
  }

  private[this] def summaryRowsOfCorrectionToCountry(correctionsToCountry: List[CorrectionToCountry])(implicit messages: Messages): Seq[TitledSummaryList] = {
    for {
      correctionToCountry <- correctionsToCountry
    } yield TitledSummaryList(
      title = messages("previousReturn.correction.correctionToCountry.title", correctionToCountry.correctionCountry.name),
      list = SummaryListViewModel(
        rows = rows(correctionToCountry.countryVatCorrection)
      )
    )
  }

  private[this] def rows(correctionAmount: BigDecimal)(implicit messages: Messages): Seq[SummaryListRow] =
    Seq(
      SummaryListRowViewModel(
        key     = Key("previousReturn.correction.correctionToCountry.previousDeclared.label")
          .withCssClass("govuk-!-font-weight-regular"),
        value   = ValueViewModel(HtmlContent("TODO")),
      ),
      SummaryListRowViewModel(
        key     = Key("previousReturn.correction.correctionToCountry.correctionAmount.label")
          .withCssClass("govuk-!-font-weight-regular"),
        value   = ValueViewModel(HtmlContent(currencyFormat(correctionAmount))),
      )
    )

}
