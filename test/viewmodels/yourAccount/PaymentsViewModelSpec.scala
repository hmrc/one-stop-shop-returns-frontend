/*
 * Copyright 2023 HM Revenue & Customs
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

package viewmodels.yourAccount

import base.SpecBase
import models.Period
import models.Quarter.{Q1, Q3, Q4}
import models.financialdata.{Payment, PaymentStatus}

import java.time.LocalDate

class PaymentsViewModelSpec extends SpecBase{

  private val paymentDue = Payment(period, BigDecimal(1000), LocalDate.now, PaymentStatus.Unpaid)
  val period1 = Period(2021, Q3)
  val period2 = Period(2021, Q4)
  val period3 = Period(2022, Q1)

  "must return correct view model when" - {
    val app = applicationBuilder().build()

    "there is no payments due or overdue" in {
      val result = PaymentsViewModel(Seq.empty, Seq.empty)(messages(app))
      result.sections mustBe Seq(PaymentsSection(Seq("You do not owe anything right now.")))
      result.link must not be defined
      result.warning must not be defined
    }

    "there is one due payment" in {
      val result = PaymentsViewModel(Seq(paymentDue), Seq.empty)(messages(app))
      result.sections mustBe Seq(PaymentsSection(
        Seq(
          s"""You owe <span class="govuk-body govuk-!-font-weight-bold">&pound;1,000</span> for ${period.displayShortText(messages(app))}. You must pay this by ${period.paymentDeadlineDisplay}."""
        ),
        Some("Due Payments")
      ))
      result.link mustBe defined
      result.warning mustBe defined
    }

    "there is one due payment with unknown status" in {
      val result = PaymentsViewModel(Seq(paymentDue.copy(paymentStatus = PaymentStatus.Unknown)), Seq.empty)(messages(app))
      result.sections mustBe Seq(PaymentsSection(
        Seq(
          s"""You may still owe VAT for ${period.displayShortText(messages(app))}. You must pay this by ${period.paymentDeadlineDisplay}."""
        ),
        Some("Due Payments")
      ))
      result.link mustBe defined
      result.warning mustBe defined
    }

    "there is one overdue payment" in {
      val result = PaymentsViewModel(Seq.empty, Seq(paymentDue))(messages(app))
      result.sections mustBe Seq(PaymentsSection(
        Seq(
          s"""You owe <span class="govuk-body govuk-!-font-weight-bold">&pound;1,000</span> for ${period.displayShortText(messages(app))}, which was due by ${period.paymentDeadlineDisplay}."""
        ),
        Some("Overdue Payments")
      ))
      result.link mustBe defined
      result.warning mustBe defined
    }

    "there is one overdue payment with unknown status" in {
      val result = PaymentsViewModel(Seq.empty, Seq(paymentDue.copy(paymentStatus = PaymentStatus.Unknown)))(messages(app))
      result.sections mustBe Seq(PaymentsSection(
        Seq(
          s"""You may still owe VAT for ${period.displayShortText(messages(app))}, which was due by ${period.paymentDeadlineDisplay}."""
        ),
        Some("Overdue Payments")
      ))
      result.link mustBe defined
      result.warning mustBe defined
    }

    "there is one due payment, and two overdue payments, one with unknown status" in {
      val result = PaymentsViewModel(Seq(paymentDue.copy(period = period3)), Seq(paymentDue.copy(period = period1, paymentStatus = PaymentStatus.Unknown), paymentDue.copy(period = period2)))(messages(app))
      result.sections mustBe Seq(
        PaymentsSection(
        Seq(
          s"""You owe <span class="govuk-body govuk-!-font-weight-bold">&pound;1,000</span> for ${period3.displayShortText(messages(app))}. You must pay this by ${period3.paymentDeadlineDisplay}.""",
        ),
          Some("Due Payments")),
        PaymentsSection(
          Seq(
            s"""You may still owe VAT for ${period1.displayShortText(messages(app))}, which was due by ${period1.paymentDeadlineDisplay}.""",
            s"""You owe <span class="govuk-body govuk-!-font-weight-bold">&pound;1,000</span> for ${period2.displayShortText(messages(app))}, which was due by ${period2.paymentDeadlineDisplay}."""
          ),
          Some("Overdue Payments")
        )
      )
      result.link mustBe defined
      result.warning mustBe defined
    }
  }

}
