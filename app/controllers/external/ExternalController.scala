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

package controllers.external

import controllers.actions.AuthenticatedControllerComponents
import models.Period
import models.external._
import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, MessagesControllerComponents}
import services.external.ExternalService
import uk.gov.hmrc.play.bootstrap.controller.WithJsonBody
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.Future

class ExternalController @Inject()(
                                    externalService: ExternalService,
                                    cc: AuthenticatedControllerComponents
                                  ) extends FrontendBaseController with WithJsonBody with Logging {

  override protected def controllerComponents: MessagesControllerComponents = cc

  def onExternal(page: String, period: Option[Period] = None, lang: Option[String] = None): Action[JsValue] =  cc.auth.async(parse.json) {
    implicit request =>
      withJsonBody[ExternalRequest] {
      externalRequest =>
      externalService.getExternalResponse(externalRequest, request.userId, page, period, lang) match {
        case Right(response) => Future.successful(Ok(Json.toJson(response)))
        case Left(_) => Future.successful(NotFound)
      }
    }
  }


}
