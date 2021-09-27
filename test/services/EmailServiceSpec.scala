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
import models.ReturnReference
import models.emails.EmailSendingResult.EMAIL_ACCEPTED
import models.emails.{EmailToSendRequest, ReturnsConfirmationEmailNoVatOwedParameters, ReturnsConfirmationEmailParameters}
import org.mockito.ArgumentMatchers.{any, refEq}
import org.mockito.Mockito.{times, verify, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmailServiceSpec extends SpecBase {

  private val connector = mock[EmailConnector]
  private val emailService = new EmailService(connector)

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "EmailService.sendConfirmationEmail" - {

    "Call sendConfirmationEmail with oss_returns_email_confirmation with the correct parameters" in {

      val maxLengthBusiness = 160
      val maxLengthContactName = 105
      val totalVatOnSales = BigDecimal(100)

      forAll(
        validEmails,
        safeInputsWithMaxLength(maxLengthContactName),
        safeInputsWithMaxLength(maxLengthBusiness)
      ) {
        (email: String, contactName: String, businessName: String) =>
          val returnReference = arbitrary[ReturnReference].sample.value
          val stringPeriod = period.toString
          val paymentDeadlineString = period.paymentDeadlineDisplay
          val vatOwed = totalVatOnSales.toString
          val returnReferenceString = returnReference.value

          val expectedEmailToSendRequest = EmailToSendRequest(
            List(email),
            "oss_returns_email_confirmation",
            ReturnsConfirmationEmailParameters(
              contactName,
              businessName,
              stringPeriod,
              paymentDeadlineString,
              vatOwed,
              returnReferenceString)
          )

          when(connector.send(any())(any(), any())).thenReturn(Future.successful(EMAIL_ACCEPTED))

          emailService.sendConfirmationEmail(
            contactName,
            businessName,
            email,
            returnReferenceString,
            totalVatOnSales,
            period
          ).futureValue mustBe EMAIL_ACCEPTED

          verify(connector, times(1)).send(refEq(expectedEmailToSendRequest))(any(), any())
      }
    }

    "Call sendConfirmationEmail with oss_returns_email_confirmation_no_vat_owed with the correct parameters" in {

      val maxLengthContactName = 105
      val maxLengthBusiness = 160
      val totalVatOnSales = BigDecimal(0)

      forAll(
        validEmails,
        safeInputsWithMaxLength(maxLengthContactName),
        safeInputsWithMaxLength(maxLengthBusiness)
      ) {
        (email: String, contactName: String, businessName: String) =>
          val stringPeriod = period.toString
          val returnReference = arbitrary[ReturnReference].sample.value
          val returnReferenceString = returnReference.value

          val expectedEmailToSendRequest = EmailToSendRequest(
            List(email),
            "oss_returns_email_confirmation_no_vat_owed",
            ReturnsConfirmationEmailNoVatOwedParameters(
              contactName,
              stringPeriod,
              returnReferenceString
            )
          )

          when(connector.send(any())(any(), any())).thenReturn(Future.successful(EMAIL_ACCEPTED))

          emailService.sendConfirmationEmail(
            contactName,
            businessName,
            email,
            returnReferenceString,
            totalVatOnSales,
            period
          ).futureValue mustBe EMAIL_ACCEPTED

          verify(connector, times(1)).send(refEq(expectedEmailToSendRequest))(any(), any())
      }
    }
  }
}
