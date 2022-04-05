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

package controllers.test

import connectors.test.TestOnlyConnector
import controllers.actions.AuthenticatedControllerComponents
import models.Period
import models.external.{ContinueReturn, ExternalRequest, ReturnsHistory, StartReturn, YourAccount}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.external.ExternalService
import uk.gov.hmrc.auth.core.InternalError
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TestOnlyController @Inject()(testConnector: TestOnlyConnector,
                                   externalService: ExternalService,
                               cc: AuthenticatedControllerComponents)(implicit ec: ExecutionContext) extends FrontendController(cc) {

  def deleteAccounts(): Action[AnyContent] = Action.async { implicit request =>
    testConnector.dropAccounts()
      .map(_ => Ok("Perf Tests Accounts MongoDB deleted"))
      .recover {
        case _: NotFoundException => Ok("Perf Tests Accounts did not exist")
      }
  }

  private val externalRequest = ExternalRequest("BTA", "/business-account")

  def yourAccountFromExternal(): Action[AnyContent] = cc.auth  {
    implicit request =>
      externalService.getExternalResponse(externalRequest, request.userId, YourAccount.name) match {
        case Right(_) => Redirect(YourAccount.url)
        case Left(_) => InternalServerError
      }

  }

  def startReturnFromExternal(period: Period): Action[AnyContent] = cc.auth {
    implicit request =>
      externalService.getExternalResponse(externalRequest, request.userId, StartReturn.name, Some(period)) match {
        case Right(_) => Redirect(StartReturn.url(period))
        case Left(_) => InternalServerError
      }
  }

  def continueReturnFromExternal(period: Period): Action[AnyContent] = cc.auth {
    implicit request =>
      externalService.getExternalResponse(externalRequest, request.userId, ContinueReturn.name, Some(period)) match {
        case Right(_) => Redirect(ContinueReturn.url(period))
        case Left(_) => InternalServerError
      }
  }

  def returnsHistoryFromExternal(): Action[AnyContent] = cc.auth {
    implicit request =>
      externalService.getExternalResponse(externalRequest, request.userId, ReturnsHistory.name) match {
        case Right(_) => Redirect(ReturnsHistory.url)
        case Left(_) => InternalServerError
      }
  }

}