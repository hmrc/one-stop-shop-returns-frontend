/*
 * Copyright 2024 HM Revenue & Customs
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

package models.registration

import models.exclusions.ExcludedTrader
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.domain.Vrn

import java.time.LocalDate

case class Registration(
                         vrn: Vrn,
                         registeredCompanyName: String,
                         vatDetails: VatDetails,
                         euRegistrations: Seq[EuTaxRegistration],
                         contactDetails: ContactDetails,
                         commencementDate: LocalDate,
                         isOnlineMarketplace: Boolean,
                         excludedTrader: Option[ExcludedTrader],
                         transferringMsidEffectiveFromDate: Option[LocalDate],
                         unusableStatus: Option[Boolean] = None
                       )

object Registration {

  implicit val format: OFormat[Registration] = Json.format[Registration]
}
