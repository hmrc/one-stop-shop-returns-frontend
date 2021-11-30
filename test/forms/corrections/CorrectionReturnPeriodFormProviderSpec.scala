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

package forms.corrections

import forms.behaviours.OptionFieldBehaviours
import models.Quarter.{Q2, Q3, Q4}
import models.{Index, Period}
import play.api.data.FormError

class CorrectionReturnPeriodFormProviderSpec extends OptionFieldBehaviours {

  val testPeriods = Seq(Period(2021, Q2), Period(2021, Q3), Period(2021, Q4))
  val form = new CorrectionReturnPeriodFormProvider()(index, testPeriods, Seq.empty)

  val index = Index(0)

  ".value" - {

    val fieldName = "value"
    val requiredKey = "correctionReturnPeriod.error.required"

    behave like optionsField[Period](
      form,
      fieldName,
      validValues  = testPeriods,
      invalidError = FormError(fieldName, "error.invalidPeriod")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "must fail to bind when given a duplicate value" in {
      val answer = testPeriods.head
      val existingAnswers = Seq(testPeriods.head, testPeriods.tail.head)

      val duplicateForm = new CorrectionReturnPeriodFormProvider()(Index(3), testPeriods, existingAnswers)

      val result = duplicateForm.bind(Map(fieldName ->  answer.toString)).apply(fieldName)
      result.errors must contain only FormError(fieldName, "correctionReturnPeriod.error.duplicate")
    }

    "must fail to bind when not in available periods" in {
      val answer = testPeriods.head
      val existingAnswers = Seq.empty
      val availablePeriods = Seq(testPeriods.tail.head)

      val duplicateForm = new CorrectionReturnPeriodFormProvider()(Index(1), availablePeriods, existingAnswers)

      val result = duplicateForm.bind(Map(fieldName ->  answer.toString)).apply(fieldName)
      result.errors must contain only FormError(fieldName, "correctionReturnPeriod.error.required")
    }
  }
}
