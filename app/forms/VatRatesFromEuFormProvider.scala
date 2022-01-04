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

import forms.mappings.Mappings
import models.VatRate
import play.api.data.Form
import play.api.data.Forms.list

import javax.inject.Inject

class VatRatesFromEuFormProvider @Inject() extends Mappings {

  def apply(vatRates: Seq[VatRate]): Form[List[VatRate]] =
    Form(
      "value" ->
        list(text("vatRatesFromEu.error.required"))
          .verifying(
            firstError(
              nonEmptySeq("vatRatesFromEu.error.required"),
              validVatRates(vatRates, "vatRatesFromEu.error.invalid")
            )
          )
          .transform[List[VatRate]](
            _.map(rate => vatRates.find(_.rate.toString == rate).get),
            _.map(_.rate.toString)
          )
    )
}
