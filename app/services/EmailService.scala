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

package services

import config.Constants.{overdueReturnsConfirmationTemplateId, returnsConfirmationNoVatOwedTemplateId, returnsConfirmationTemplateId}
import connectors.EmailConnector
import models.Period
import models.emails.{EmailSendingResult, EmailToSendRequest, ReturnsConfirmationEmailNoVatOwedParameters, ReturnsConfirmationEmailParameters}
import play.api.i18n.Messages
import uk.gov.hmrc.http.HeaderCarrier

import java.time.format.DateTimeFormatter
import java.time.{Clock, LocalDate}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailService @Inject()(
                              emailConnector: EmailConnector,
                              clock: Clock
                            )(implicit executionContext: ExecutionContext) {

  def sendConfirmationEmail(
                             contactName: String,
                             businessName: String,
                             emailAddress: String,
                             totalVatOnSales: BigDecimal,
                             period: Period
                           )(implicit hc: HeaderCarrier, messages: Messages): Future[EmailSendingResult] = {
    val vatOwed = totalVatOnSales > 0

    val templateId =
      (vatOwed, period.paymentDeadline.isBefore(LocalDate.now(clock))) match {
        case (true, true) => overdueReturnsConfirmationTemplateId
        case (true, false) => returnsConfirmationTemplateId
        case _ => returnsConfirmationNoVatOwedTemplateId
      }

    val emailParameters =
      if (vatOwed) {
        ReturnsConfirmationEmailParameters(
          contactName,
          businessName,
          period.displayText,
          format(period.paymentDeadline)
        )
      } else {
        ReturnsConfirmationEmailNoVatOwedParameters(contactName, period.displayText)
      }

    emailConnector.send(
      EmailToSendRequest(
        List(emailAddress),
        templateId,
        emailParameters
      )
    )
  }

  private def format(date: LocalDate) = {
    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
    date.format(formatter)
  }
}
