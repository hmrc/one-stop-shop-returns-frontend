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

package queries

import models.{Index, VatRateAndSales}
import pages.PageConstants.{salesFromCountry, salesFromEu, salesFromNi, vatRates}
import play.api.libs.json.JsPath

case class AllNiVatRateAndSalesQuery(countryIndex: Index)
  extends Gettable[Seq[VatRateAndSales]] with Settable[Seq[VatRateAndSales]] {

  override def path: JsPath =
    JsPath \ salesFromNi \ countryIndex.position \ vatRates

}