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

package models.registration

import models.Country
import play.api.libs.json.{Json, OFormat, Reads, Writes}

sealed trait EuTaxRegistration

object EuTaxRegistration {

  implicit val reads: Reads[EuTaxRegistration] =
    RegistrationWithFixedEstablishment.format.widen[EuTaxRegistration] orElse
      EuVatRegistration.format.widen[EuTaxRegistration] orElse
      RegistrationWithoutFixedEstablishment.format.widen[EuTaxRegistration]


  implicit val writes: Writes[EuTaxRegistration] = Writes {
    case v: EuVatRegistration                     => Json.toJson(v)(EuVatRegistration.format)
    case fe: RegistrationWithFixedEstablishment   => Json.toJson(fe)(RegistrationWithFixedEstablishment.format)
    case w: RegistrationWithoutFixedEstablishment => Json.toJson(w)(RegistrationWithoutFixedEstablishment.format)
  }
}

final case class EuVatRegistration(
                                    country: Country,
                                    vatNumber: String
                                  ) extends EuTaxRegistration

object EuVatRegistration {

  implicit val format: OFormat[EuVatRegistration] =
    Json.format[EuVatRegistration]
}

final case class RegistrationWithFixedEstablishment(
                                                     country: Country,
                                                     taxIdentifier: EuTaxIdentifier,
                                                     fixedEstablishment: FixedEstablishment
                                                   ) extends EuTaxRegistration

object RegistrationWithFixedEstablishment {
  implicit val format: OFormat[RegistrationWithFixedEstablishment] =
    Json.format[RegistrationWithFixedEstablishment]
}

final case class RegistrationWithoutFixedEstablishment(country: Country) extends EuTaxRegistration

object RegistrationWithoutFixedEstablishment {
  implicit val format: OFormat[RegistrationWithoutFixedEstablishment] =
    Json.format[RegistrationWithoutFixedEstablishment]
}
