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

package services

import cats.implicits._
import connectors.VatReturnConnector
import models._
import models.domain.EuTaxIdentifierType.Vat
import models.domain.{EuTaxIdentifier, SalesDetails, SalesFromEuCountry, SalesToCountry, VatRate => DomainVatRate, VatRateType => DomainVatRateType}
import models.registration.{EuVatRegistration, Registration, RegistrationWithFixedEstablishment}
import models.requests.{SaveForLaterRequest, VatReturnRequest}
import pages._
import play.api.i18n.Lang.logger
import queries._
import services.corrections.CorrectionService
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SaveForLaterService @Inject()() {

  def fromUserAnswers(answers: UserAnswers, vrn: Vrn, period: Period): SaveForLaterRequest =
    SaveForLaterRequest(vrn, period, answers.data)

}
