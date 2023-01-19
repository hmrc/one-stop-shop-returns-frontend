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

package forms

import forms.behaviours.OptionFieldBehaviours
import models.Period
import models.Quarter.{Q1, Q3, Q4}
import play.api.data.FormError

class WhichVatPeriodToPayFormProviderSpec extends OptionFieldBehaviours {

  val form = new WhichVatPeriodToPayFormProvider()()

  val validPeriods = Seq(Period(2021, Q3), Period(2021, Q4), Period(2022, Q1))

  ".value" - {

    val fieldName = "value"
    val requiredKey = "whichVatPeriodToPay.error.required"

    behave like optionsField[Period](
      form,
      fieldName,
      validValues  = validPeriods,
      invalidError = FormError(fieldName, "error.invalidPeriod")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
