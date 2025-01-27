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

package controllers.actions

import config.FrontendAppConfig
import connectors.VatReturnConnector
import controllers.routes
import models.domain.VatReturn
import models.requests.OptionalDataRequest
import models.responses.NotFound
import models.{Period, StandardPeriod}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}
import repositories.CachedVatReturnRepository
import services.ObligationsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckReturnsFilterImpl(
                              period: Period,
                              repository: CachedVatReturnRepository,
                              connector: VatReturnConnector,
                              obligationsService: ObligationsService,
                              frontendAppConfig: FrontendAppConfig
                            )(implicit val executionContext: ExecutionContext)
  extends ActionFilter[OptionalDataRequest] {

  override protected def filter[A](request: OptionalDataRequest[A]): Future[Option[Result]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    if (frontendAppConfig.strategicReturnApiEnabled) {
      obligationsService.getFulfilledObligations(request.vrn).flatMap { obligations =>
        val hasSubmittedReturn = obligations.exists {
          obligationDetails =>
            val fulfilledPeriod = Period.fromEtmpPeriodKey(obligationDetails.periodKey)
            fulfilledPeriod == period
        }

        if (hasSubmittedReturn) {
          Some(Redirect(routes.PreviousReturnController.onPageLoad(period))).toFuture
        } else {
          None.toFuture
        }
      }
    } else {
      getLegacyVatReturnAndCache(request)
    }
  }

  private def getLegacyVatReturnAndCache(request: OptionalDataRequest[_])(implicit hc: HeaderCarrier): Future[Option[Result]] = {
    def getFromConnectorAndCache: Future[Option[VatReturn]] =
      connector.get(period) flatMap {
        case Right(vatReturn) =>
          repository.set(request.userId, StandardPeriod.fromPeriod(period), Some(vatReturn)).map(_ => Some(vatReturn))
        case Left(NotFound) =>
          repository.set(request.userId, StandardPeriod.fromPeriod(period), None).map(_ => None)
        case error =>
          val message = s"Error when getting vat return ${error}"
          val exception = new Exception(message)
          throw new Exception(exception.getMessage, exception)
      }

    val maybeVatReturn: Future[Option[VatReturn]] =
      repository.get(request.userId, StandardPeriod.fromPeriod(period))
        .flatMap(_.fold(getFromConnectorAndCache)(_.vatReturn.toFuture))

    maybeVatReturn.map {
      case Some(_) => Some(Redirect(routes.PreviousReturnController.onPageLoad(period)))
      case _ => None
    }
  }
}

class CheckReturnsFilterProvider @Inject()(
                                            repository: CachedVatReturnRepository,
                                            connector: VatReturnConnector,
                                            obligationsService: ObligationsService,
                                            frontendAppConfig: FrontendAppConfig
                                          )(implicit ec: ExecutionContext) {

  def apply(period: Period): CheckReturnsFilterImpl =
    new CheckReturnsFilterImpl(period, repository, connector, obligationsService, frontendAppConfig)
}