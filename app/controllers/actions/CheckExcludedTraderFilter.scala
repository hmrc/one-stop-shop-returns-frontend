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

import controllers.exclusions.routes
import models.Period
import models.requests.OptionalDataRequest
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}
import services.exclusions.ExclusionService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckExcludedTraderFilterImpl(exclusionService: ExclusionService,
                                    startReturnPeriod: Period
                                   )(implicit val executionContext: ExecutionContext)
  extends ActionFilter[OptionalDataRequest] {

  override protected def filter[A](request: OptionalDataRequest[A]): Future[Option[Result]] = {
    exclusionService.findExcludedTrader(request.vrn).map {
      case Some(excludedTrader) if startReturnPeriod.lastDay.isAfter(excludedTrader.effectivePeriod.firstDay) =>
        Some(Redirect(routes.ExcludedNotPermittedController.onPageLoad()))
      case _ =>
        None
    }
  }
}

class CheckExcludedTraderFilterProvider @Inject()(exclusionService: ExclusionService)
                                                 (implicit ec: ExecutionContext) {

  def apply(startReturnPeriod: Period): CheckExcludedTraderFilterImpl =
    new CheckExcludedTraderFilterImpl(exclusionService, startReturnPeriod)

}
