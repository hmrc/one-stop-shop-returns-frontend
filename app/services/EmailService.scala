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

package services

import config.Constants.{returnsConfirmationNoVatOwedTemplateId, returnsConfirmationTemplateId}
import connectors.EmailConnector
import models.Period
import models.emails.{EmailSendingResult, EmailToSendRequest, ReturnsConfirmationEmailNoVatOwedParameters, ReturnsConfirmationEmailParameters}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailService @Inject()(
  emailConnector: EmailConnector
)(implicit executionContext: ExecutionContext) {

  def sendConfirmationEmail(
   contactName: String,
   businessName: String,
   emailAddress: String,
   returnReference: String,
   totalVatOnSales: BigDecimal,
   period: Period
  )(implicit hc: HeaderCarrier): Future[EmailSendingResult] = {

   val vatOwed = totalVatOnSales > 0

   val emailParameters = {
     if(vatOwed) {
       ReturnsConfirmationEmailParameters(
         contactName,
         businessName,
         period.toString,
         format(period.paymentDeadline),
         totalVatOnSales.toString(),
         returnReference
      )
    } else {
       ReturnsConfirmationEmailNoVatOwedParameters(
         contactName,
         period.toString,
         returnReference
      )
    }
   }
   emailConnector.send(
     EmailToSendRequest(
      List(emailAddress),
      if(vatOwed) returnsConfirmationTemplateId else returnsConfirmationNoVatOwedTemplateId,
      emailParameters
     )
   )
  }

  private def format(date: LocalDate) = {
   val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
   date.format(formatter)
  }
}
