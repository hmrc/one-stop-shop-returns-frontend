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
import generators.ModelGenerators
import org.scalacheck.Arbitrary.arbitrary
import models.requests.{PaymentPeriod, PaymentRequest}
import uk.gov.hmrc.domain.Vrn

class PaymentServiceSpec extends SpecBase with ModelGenerators {

  val service = new PaymentService()

  "PaymentService.buildPaymentRequest" - {

    val amount = 1200
    val amountInPence = 120000

    "should return correct PaymentRequest with correct VRN and Period and amount" in {
      val vrn = arbitrary[Vrn].sample.value
      val expected = PaymentRequest(vrn, PaymentPeriod(completeUserAnswers.period), amountInPence)

      service.buildPaymentRequest(vrn, completeUserAnswers.period, amount) mustBe expected
    }
  }
}
