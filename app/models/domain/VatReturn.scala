/*
 * Copyright 2024 HM Revenue & Customs
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

package models.domain

import models.{PartialReturnPeriod, PaymentReference, Period, ReturnReference}
import play.api.i18n.Messages
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.domain.Vrn

import java.time.{Instant, LocalDate}

case class VatReturn(
                      vrn: Vrn,
                      period: Period,
                      reference: ReturnReference,
                      paymentReference: PaymentReference,
                      startDate: Option[LocalDate],
                      endDate: Option[LocalDate],
                      salesFromNi: List[SalesToCountry],
                      salesFromEu: List[SalesFromEuCountry],
                      submissionReceived: Instant,
                      lastUpdated: Instant
                    ) {
  def displayStartEnd(implicit messages: Messages): String = {
    val periodToDisplay = (startDate, endDate) match {
      case (Some(sd), Some(ed)) =>
        PartialReturnPeriod(sd, ed, period.year, period.quarter)
      case _ => period
    }

    periodToDisplay.displayText
  }
}

object VatReturn {

  implicit val format: OFormat[VatReturn] = Json.format[VatReturn]
}
