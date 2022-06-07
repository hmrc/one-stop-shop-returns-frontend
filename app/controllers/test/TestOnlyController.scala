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
import models.external._
import play.api.mvc.{Action, AnyContent}
import services.external.ExternalService
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

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

  def yourAccountFromExternal(lang: Option[String] = None): Action[AnyContent] = cc.auth  {
    implicit request =>
      externalService.getExternalResponse(externalRequest, request.userId, YourAccount.name, language = lang) match {
        case Right(response) => Redirect(response.redirectUrl)
        case Left(_) => InternalServerError
      }

  }

  def startReturnFromExternal(period: Period, lang: Option[String] = None): Action[AnyContent] = cc.auth {
    implicit request =>
      externalService.getExternalResponse(externalRequest, request.userId, StartReturn.name, Some(period), language = lang) match {
        case Right(response) => Redirect(response.redirectUrl)
        case Left(_) => InternalServerError
      }
  }

  def continueReturnFromExternal(period: Period, lang: Option[String] = None): Action[AnyContent] = cc.auth {
    implicit request =>
      externalService.getExternalResponse(externalRequest, request.userId, ContinueReturn.name, Some(period), language = lang) match {
        case Right(response) => Redirect(response.redirectUrl)
        case Left(_) => InternalServerError
      }
  }

  def returnsHistoryFromExternal(lang: Option[String] = None): Action[AnyContent] = cc.auth {
    implicit request =>
      externalService.getExternalResponse(externalRequest, request.userId, ReturnsHistory.name, language = lang) match {
        case Right(response) => Redirect(response.redirectUrl)
        case Left(_) => InternalServerError
      }
  }

}