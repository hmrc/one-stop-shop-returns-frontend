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

import connectors.FileUploadOutcomeConnector
import controllers.actions.*
import forms.FileUploadedFormProvider
import logging.Logging
import models.requests.DataRequest
import models.upscan.*
import models.{Mode, Period, UserAnswers}
import pages.fileUpload.{CsvValidationErrorsPage, FileReferencePage, FileUploadStatusPage, FileUploadedPage}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.fileUpload.{CsvParserService, CsvValidator}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.fileUpload.FileUploadedView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class FileUploadedController @Inject()(
                                       cc: AuthenticatedControllerComponents,
                                       formProvider: FileUploadedFormProvider,
                                       view: FileUploadedView,
                                       fileUploadOutcomeConnector: FileUploadOutcomeConnector,
                                       csvParserService: CsvParserService,
                                       csvValidator: CsvValidator
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {
  
  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period): Action[AnyContent] = cc.authAndGetData(period).async {
    implicit request =>

      val fileReference = request.userAnswers.get(FileReferencePage)
      
      fileReference match {
        case Some(ref) =>
          fileUploadOutcomeConnector.getOutcome(ref).flatMap { maybeOutcome =>
            val status = maybeOutcome.map(_.status).getOrElse("UPLOADING")
            val form = formForStatus(status)
            val preparedForm = request.userAnswers.get(FileUploadedPage).fold(form)(form.fill)
            
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(FileUploadStatusPage, status))
              _ <- cc.sessionRepository.set(updatedAnswers)
            } yield {
              Ok(view(preparedForm, mode, period, maybeOutcome))
            }
          }
        case None =>
          Future.successful(BadRequest("No file reference found in session"))
      }
  }

  def onSubmit(mode: Mode, period: Period): Action[AnyContent] = cc.authAndGetData(period).async {
    implicit request =>

      val fileReference = request.userAnswers.get(FileReferencePage)
      
      fileReference match {
        case Some(ref) =>
          fileUploadOutcomeConnector.getOutcome(ref).flatMap { maybeOutcome =>
            val status = maybeOutcome.map(_.status).getOrElse("UPLOADING")
            val form = formForStatus(status)

            form.bindFromRequest().fold(
              formWithErrors =>
                Future.successful(BadRequest(view(formWithErrors, mode, period, maybeOutcome))),

              value =>
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(FileUploadedPage, value))
                  _              <- cc.sessionRepository.set(updatedAnswers)
                  result <- if (status == "UPLOADED" && value) {
                    parseCsvAndUpdateAnswers(mode, ref, updatedAnswers)
                  } else {
                    Redirect(FileUploadedPage.navigate(mode, updatedAnswers)).toFuture
                  }
                } yield result
            )
          }
        case None =>
          Future.successful(BadRequest("No file reference found is session"))
      }
  }

  private def formForStatus(status: String): Form[Boolean] = {
    if (status == "FAILED") formProvider.failedForm else formProvider.successForm
  }

  private def parseCsvAndUpdateAnswers(mode: Mode, reference: String, answers: UserAnswers)(implicit request: DataRequest[_]): Future[Result] = {

    fileUploadOutcomeConnector.getCsv(reference).flatMap {
      case Right(csv) =>
        val rows = CsvParserService.split(csv)
        val period = answers.period
        val isOnlineMarketPlace = request.registration.isOnlineMarketplace

        csvValidator.validateOrThrow(rows, period, isOnlineMarketPlace).flatMap { _ =>
          Try(csvParserService.populateUserAnswersFromCsv(answers, csv)).flatten match {
            case Success(updatedAnswers) =>
              cc.sessionRepository.set(updatedAnswers).map { _ =>
                Redirect(FileUploadedPage.navigate(mode, updatedAnswers))
              }
            case Failure(e) =>
              logger.warn(s"Csv parsing failed", e)
              Redirect(controllers.fileUpload.routes.DataErrorController.onPageLoad(mode, answers.period)).toFuture
          }
        }.recoverWith {
          case CsvValidationException(errs) =>
            val uaWithErrors = answers.set(CsvValidationErrorsPage, errs)
            Future.fromTry(uaWithErrors).flatMap { uaWithErrors =>
              cc.sessionRepository.set(uaWithErrors).map { _ =>
                Redirect(controllers.fileUpload.routes.DataErrorController.onPageLoad(mode, answers.period))
              }
            }
        }
      case Left(_) =>
        Redirect(controllers.fileUpload.routes.DataErrorController.onPageLoad(mode, answers.period)).toFuture
    }
  }
}
