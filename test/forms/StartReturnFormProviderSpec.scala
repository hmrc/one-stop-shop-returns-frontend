/*
 * Copyright 2022 HM Revenue & Customs
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

import forms.behaviours.BooleanFieldBehaviours
import models.Period
import models.Quarter.Q3
import play.api.data.FormError
import play.api.i18n.Messages
import play.api.test.StubMessagesFactory

class StartReturnFormProviderSpec extends BooleanFieldBehaviours with StubMessagesFactory {

  val requiredKey = "startReturn.error.required"
  val invalidKey = "error.boolean"
  val period = Period(2021, Q3)
  implicit val m: Messages = stubMessages()

  val form = new StartReturnFormProvider()(period)

  ".value" - {

    val fieldName = "value"

    behave like booleanField(
      form,
      fieldName,
      invalidError = FormError(fieldName, invalidKey, Seq(period.displayText))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey, Seq(period.displayText))
    )
  }
}
