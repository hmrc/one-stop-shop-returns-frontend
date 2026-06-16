/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers.fileUpload

import config.FrontendAppConfig
import connectors.UpscanInitiateConnector
import controllers.actions.*
import forms.FileUploadFormProvider
import models.upscan.UpscanRedirectError
import models.{Mode, Period}
import pages.fileUpload.{DataErrorPage, FileReferencePage, FileUploadedPage}
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.api.Environment
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.fileUpload.FileUploadView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FileUploadController @Inject()(
                                       cc: AuthenticatedControllerComponents,
                                       formProvider: FileUploadFormProvider,
                                       view: FileUploadView,
                                       upscanInitiateConnector: UpscanInitiateConnector,
                                       appConfig: FrontendAppConfig,
                                       environment: Environment
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()
  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period): Action[AnyContent] = cc.authAndGetData(period).async {
    implicit request =>

      val period = request.userAnswers.period
      val redirectError = UpscanRedirectError.fromQuery(request)

      redirectError match {
        case Some(redirectErr) =>
          val msg = errorMessage(Some(redirectErr)).getOrElse("")
          Redirect(routes.FileUploadController.onPageLoad(mode, period)).flashing("upscanError" -> msg).toFuture

        case None =>
          val errorMsg: Option[String] = request.flash.get("upscanError"). filter(_.nonEmpty)

          upscanInitiateConnector.initiateV2(
            redirectOnSuccess = Some(appConfig.successEndPointTarget(period.toString)),
            redirectOnError = Some(appConfig.errorEndPointTarget(period.toString))
          ).flatMap { initiateResponse =>

            for {
              cleanedAnswers <- Future.fromTry(request.userAnswers.remove(FileUploadedPage))
              dataErrorAnswers <- Future.fromTry(cleanedAnswers.remove(DataErrorPage))
              updatedAnswers <- Future.fromTry(dataErrorAnswers.set(FileReferencePage, initiateResponse.fileReference.reference))
              _ <- cc.sessionRepository.set(updatedAnswers)
            } yield {
              Ok(view(
                form,
                mode,
                period,
                postTarget = initiateResponse.postTarget,
                formFields = initiateResponse.formFields,
                errorMessage = errorMsg
              ))
            }
          }
      }
  }
  
  def downloadTemplate(period: Period): Action[AnyContent] = cc.authAndGetData(period).async { _=>
    Future.successful(
      Ok.sendFile(
        content = environment.getFile("conf/template/OSS return template.ods"),
        inline = false
      )
    )
  }

  private def errorMessage(redirectError: Option[UpscanRedirectError])(implicit messages: Messages): Option[String] =
    redirectError.map {

      case UpscanRedirectError("EntityTooLarge", _) =>
        messages("fileUploaded.redirectError.EntityTooLarge")

      case UpscanRedirectError("InvalidArgument", Some(msg)) if msg.toLowerCase.contains("file") && msg.toLowerCase.contains("not found") =>
        messages("fileUpload.redirectError.fileNotFound")

      case UpscanRedirectError("InvalidArgument", _) =>
        messages("fileUpload.redirectError.invalidType")

      case _ =>
        messages("fileUploaded.redirectError.default")
    }
}
