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

package forms

import javax.inject.Inject

import forms.mappings.Mappings
import play.api.data.Form
import play.api.data.Forms._
import models.SalesDetailsFromEu

class SalesDetailsFromEuFormProvider @Inject() extends Mappings {

   def apply(): Form[SalesDetailsFromEu] = Form(
     mapping(
      "netValueOfSales" -> text("salesDetailsFromEu.error.netValueOfSales.required")
        .verifying(maxLength(100, "salesDetailsFromEu.error.netValueOfSales.length")),
      "vatOnSales" -> text("salesDetailsFromEu.error.vatOnSales.required")
        .verifying(maxLength(100, "salesDetailsFromEu.error.vatOnSales.length"))
    )(SalesDetailsFromEu.apply)(SalesDetailsFromEu.unapply)
   )
 }
