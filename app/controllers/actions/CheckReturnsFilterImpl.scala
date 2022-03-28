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

package controllers.actions

import connectors.VatReturnConnector
import controllers.routes
import models.Period
import models.requests.OptionalDataRequest
import models.responses.NotFound
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}
import repositories.CachedVatReturnRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckReturnsFilterImpl(period: Period, repository: CachedVatReturnRepository, connector: VatReturnConnector)
                            (implicit val executionContext: ExecutionContext)
  extends ActionFilter[OptionalDataRequest] {
  
  override protected def filter[A](request: OptionalDataRequest[A]): Future[Option[Result]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    repository.get(request.userId, period) flatMap {
      case Some(cachedVatReturn) if cachedVatReturn.vatReturn.isDefined =>
        Future.successful(Some(Redirect(routes.PreviousReturnController.onPageLoad(period))))
      case Some(_) =>
        Future.successful(None)
      case None =>
        connector.get(period) flatMap  {
          case Right(vatReturn) =>
            repository.set(request.userId, period, Some(vatReturn)).map {
             _ =>  Some(Redirect(routes.PreviousReturnController.onPageLoad(period)))
            }
          case Left(NotFound) =>
            repository.set(request.userId, period, None).map(_ => None)
          case _ =>
            Future.successful(None)
        }
    }
  }
}

class CheckReturnsFilterProvider @Inject()(repository: CachedVatReturnRepository,
                                           connector: VatReturnConnector)
                                          (implicit ec: ExecutionContext) {

 def apply(period: Period): CheckReturnsFilterImpl =
   new CheckReturnsFilterImpl(period, repository, connector)
}