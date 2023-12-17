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

package services.exclusions

import connectors.VatReturnConnector
import logging.Logging
import models.exclusions.ExcludedTrader
import models.Period
import models.registration.Registration
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ExclusionService @Inject()(connector: VatReturnConnector) extends Logging {

  def hasSubmittedFinalReturn(registration: Registration)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    registration.excludedTrader match {
      case Some(ExcludedTrader(_, _, effectivePeriod, _)) =>
        connector.get(effectivePeriod).map {
          case Right(_) => true
          case _ => false
        }
      case _ => Future.successful(false)
    }
  }

  def currentReturnIsFinal(registration: Registration, currentPeriod: Period)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {

    hasSubmittedFinalReturn(registration).flatMap {
      case true =>
        Future.successful(false)
      case _ =>
        registration.excludedTrader match {
          case Some(ExcludedTrader(_, _, effectivePeriod, _))
            if currentPeriod == effectivePeriod => Future.successful(true)
          case _ =>
            Future.successful(false)
        }
    }
  }
}
