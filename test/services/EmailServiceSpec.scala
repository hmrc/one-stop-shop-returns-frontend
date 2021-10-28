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

import base.SpecBase
import connectors.EmailConnector
import models.Period
import models.Quarter.Q2
import models.emails.EmailSendingResult.EMAIL_ACCEPTED
import models.emails.{EmailToSendRequest, ReturnsConfirmationEmailNoVatOwedParameters, ReturnsConfirmationEmailParameters}
import org.mockito.ArgumentMatchers.{any, refEq}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmailServiceSpec extends SpecBase {

  private val connector = mock[EmailConnector]
  private val emailService = new EmailService(connector)
  private val maxLengthBusiness = 160
  private val maxLengthContactName = 105

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val m: Messages = stubMessages()

  "EmailService.sendConfirmationEmail" - {

    "Call sendConfirmationEmail with oss_returns_email_confirmation with the correct parameters" in {
      forAll(
        validEmails,
        safeInputsWithMaxLength(maxLengthContactName),
        safeInputsWithMaxLength(maxLengthBusiness)
      ) {
        (email: String, contactName: String, businessName: String) =>
          val paymentDeadlineString = period.paymentDeadlineDisplay
          val expectedEmailToSendRequest = EmailToSendRequest(
            List(email),
            "oss_returns_email_confirmation",
            ReturnsConfirmationEmailParameters(
              contactName,
              businessName,
              period.displayText,
              paymentDeadlineString
            )
          )

          when(connector.send(any())(any(), any())).thenReturn(Future.successful(EMAIL_ACCEPTED))

          emailService.sendConfirmationEmail(
            contactName,
            businessName,
            email,
            BigDecimal(100),
            period
          ).futureValue mustBe EMAIL_ACCEPTED

          verify(connector, times(1)).send(refEq(expectedEmailToSendRequest))(any(), any())
      }
    }

    "Call sendConfirmationEmail with oss_overdue_returns_email_confirmation with the correct parameters" in {
      val period = Period(2021, Q2)

      forAll(
        validEmails,
        safeInputsWithMaxLength(maxLengthContactName),
        safeInputsWithMaxLength(maxLengthBusiness)
      ) {
        (email: String, contactName: String, businessName: String) =>
          val paymentDeadlineString = period.paymentDeadlineDisplay
          val expectedEmailToSendRequest = EmailToSendRequest(
            List(email),
            "oss_overdue_returns_email_confirmation",
            ReturnsConfirmationEmailParameters(
              contactName,
              businessName,
              period.displayText,
              paymentDeadlineString
            )
          )

          when(connector.send(any())(any(), any())).thenReturn(Future.successful(EMAIL_ACCEPTED))

          emailService.sendConfirmationEmail(
            contactName,
            businessName,
            email,
            BigDecimal(100),
            period
          ).futureValue mustBe EMAIL_ACCEPTED

          verify(connector, times(1)).send(refEq(expectedEmailToSendRequest))(any(), any())
      }
    }

    "Call sendConfirmationEmail with oss_returns_email_confirmation_no_vat_owed with the correct parameters" in {
      forAll(
        validEmails,
        safeInputsWithMaxLength(maxLengthContactName),
        safeInputsWithMaxLength(maxLengthBusiness)
      ) {
        (email: String, contactName: String, businessName: String) =>
          val expectedEmailToSendRequest = EmailToSendRequest(
            List(email),
            "oss_returns_email_confirmation_no_vat_owed",
            ReturnsConfirmationEmailNoVatOwedParameters(contactName, period.displayText)
          )

          when(connector.send(any())(any(), any())).thenReturn(Future.successful(EMAIL_ACCEPTED))

          emailService.sendConfirmationEmail(
            contactName,
            businessName,
            email,
            BigDecimal(0),
            period
          ).futureValue mustBe EMAIL_ACCEPTED

          verify(connector, times(1)).send(refEq(expectedEmailToSendRequest))(any(), any())
      }
    }
  }
}
