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

package controllers.actions

import config.FrontendAppConfig
import controllers.exclusions.routes
import models.Period
import models.requests.OptionalDataRequest
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}
import services.PeriodService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckExcludedTraderFilterImpl(periodService: PeriodService,
                                    startReturnPeriod: Period,
                                    frontendAppConfig: FrontendAppConfig
                                   )(implicit val executionContext: ExecutionContext)
  extends ActionFilter[OptionalDataRequest] {

  override protected def filter[A](request: OptionalDataRequest[A]): Future[Option[Result]] = {
    if (frontendAppConfig.exclusionsEnabled) {
      request.registration.excludedTrader match {
        case Some(excludedTrader) if startReturnPeriod.lastDay.isAfter(periodService.getNextPeriod(excludedTrader.effectivePeriod).firstDay) =>
          Future.successful(Some(Redirect(routes.ExcludedNotPermittedController.onPageLoad())))
        case _ =>
          Future.successful(None)
      }
    } else {
      Future.successful(None)
    }
  }
}

class CheckExcludedTraderFilterProvider @Inject()(periodService: PeriodService, frontendAppConfig: FrontendAppConfig)
                                                 (implicit ec: ExecutionContext) {

  def apply(startReturnPeriod: Period): CheckExcludedTraderFilterImpl =
    new CheckExcludedTraderFilterImpl(periodService, startReturnPeriod, frontendAppConfig)

}
