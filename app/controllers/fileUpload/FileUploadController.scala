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

import controllers.actions.*
import forms.FileUploadFormProvider
import models.{Mode, Period}
import pages.fileUpload.{FileUploadPage, FileUploadedPage}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.api.Environment
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.fileUpload.FileUploadView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FileUploadController @Inject()(
                                       cc: AuthenticatedControllerComponents,
                                       formProvider: FileUploadFormProvider,
                                       view: FileUploadView,
                                       environment: Environment
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()
  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>

      val period = request.userAnswers.period
      
      Ok(view(form, mode, period))
  }

  def onSubmit(mode: Mode, period: Period): Action[AnyContent] = cc.authAndGetData(period).async {
    implicit request =>

      val period = request.userAnswers.period

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode, period))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(FileUploadPage, value))
            cleanup <- Future.fromTry(updatedAnswers.remove(FileUploadedPage))
            _              <- cc.sessionRepository.set(cleanup)
          } yield Redirect(FileUploadPage.navigate(mode, cleanup))
      )
  }
  
  def downloadTemplate(period: Period): Action[AnyContent] = cc.authAndGetData(period).async { _=>
    Future.successful(
      Ok.sendFile(
        content = environment.getFile("conf/template/OSS return template.ods"),
        inline = false
      )
    )
  }
}
