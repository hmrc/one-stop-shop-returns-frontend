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

package controllers

import connectors.VatReturnConnector
import controllers.actions.AuthenticatedControllerComponents
import models.{Mode, Period}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.TitledSummaryList
import viewmodels.previousReturn.PreviousReturnSummary
import views.html.PreviousReturnView
import viewmodels.govuk.summarylist._

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PreviousReturnController @Inject()(
                                           override val messagesApi: MessagesApi,
                                           cc: AuthenticatedControllerComponents,
                                           view: PreviousReturnView,
                                           vatReturnConnector: VatReturnConnector
                                         )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period): Action[AnyContent] = cc.authAndGetData(period).async {
    implicit request =>
      vatReturnConnector.get(period).map {
        case Right(vatReturn) =>
          val mainList = SummaryListViewModel(
            rows = PreviousReturnSummary.rows(vatReturn))
          val salesList: Seq[TitledSummaryList] = ???

          Ok(view(mode, vatReturn, mainList, salesList))
        case _ =>
          ???
      }

  }

}
