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

package services.exclusions

import config.FrontendAppConfig
import connectors.VatReturnConnector
import logging.Logging
import models.exclusions.ExcludedTrader
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ExclusionService @Inject()(appConfig: FrontendAppConfig, connector: VatReturnConnector) extends Logging {

  def findExcludedTrader(vrn: Vrn): Future[Option[ExcludedTrader]] =
    Future.successful(appConfig.excludedTraders.find(e => e.vrn.vrn == vrn.vrn))

  def hasSubmittedFinalReturn(vrn: Vrn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    findExcludedTrader(vrn).flatMap {
      case Some(ExcludedTrader(_, _, _, effectivePeriod)) =>
        connector.get(effectivePeriod).map {
          case Right(_) => true
          case _ => false
        }
      case _ => Future.successful(false)
    }
  }
}
