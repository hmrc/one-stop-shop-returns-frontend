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
import forms.DataErrorFormProvider
import models.upscan.*
import models.{Mode, Period}
import pages.fileUpload.{CsvValidationErrorsPage, DataErrorPage}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.fileUpload.DataErrorView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DataErrorController @Inject()(
                                     override val messagesApi: MessagesApi,
                                     cc: AuthenticatedControllerComponents,
                                     formProvider: DataErrorFormProvider,
                                     view: DataErrorView
                                   )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()
  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>
      
      val errors: Seq[CsvError] = request.userAnswers.get(CsvValidationErrorsPage).getOrElse(Nil)
      val (errorMessage, hasMultipleTypes, errorSummaries) = errorMessages(errors)
      
      val preparedForm = request.userAnswers.get(DataErrorPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode, period, errors, errorMessage, hasMultipleTypes, errorSummaries))
  }

  def onSubmit(mode: Mode, period: Period): Action[AnyContent] = cc.authAndGetData(period).async {
    implicit request =>

      val errors: Seq[CsvError] = request.userAnswers.get(CsvValidationErrorsPage).getOrElse(Nil)
      val (errorMessage, hasMultipleTypes, errorSummaries) = errorMessages(errors)

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode, period, errors, errorMessage, hasMultipleTypes, errorSummaries))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(DataErrorPage, value))
            _              <- cc.sessionRepository.set(updatedAnswers)
          } yield Redirect(DataErrorPage.navigate(mode, updatedAnswers))
      )
  }
  
  private def errorMessages(errors: Seq[CsvError])(implicit messages: Messages): (Seq[String], Boolean, Seq[String]) = {
    
    val errorType = errors.map(_.getClass.getSimpleName).distinct
    val cells = errors.map(_.cellRef).distinct.sorted.mkString(", ")
    val hasMultipleTypes = errorType.size >= 2
    
    if (hasMultipleTypes) {
      val groupedErrors: Seq[String] = 
        errors
          .groupBy {
            case _: CsvError.InvalidCountry => "incorrectCountry"
            case _: CsvError.InvalidCharacter => "invalidCharacter"
            case _: CsvError.InvalidNumberFormat => "invalidNumber"
            case _: CsvError.NegativeNumber => "negativeNumber"
            case _: CsvError.TooMayDecimals => "tooManyDecimals"
            case _: CsvError.BlankCell => "blankCell"
            case _: CsvError.VatRateNotAllowed => "incorrectVatRate"
            case _: CsvError.DuplicateVatRate => "duplicateVatRate" 
            case _: CsvError.TooManyColumns => "tooManyColumns"
            case _: CsvError.SameToAndFromCountry => "sameToAndFromCountry"
          }
          .toSeq
          .sortBy(_._1)
          .map {
            case (key, errs) =>
              val count = errs.map(_.cellRef).distinct.size
              val groupedCells = errs.map(_.cellRef).distinct.sorted.mkString(", ")
              key match {
                case "incorrectVatRate" =>
                  val countries = errs.collect { case e: CsvError.VatRateNotAllowed => e.country }.distinct.sorted.mkString(", ")
                  
                  if (count > 1) {
                    messages(s"dataError.errorMessage.$key.p1.plural", count)
                  } else {
                    messages(s"dataError.errorMessage.$key.p1", countries)
                  }

                case _ =>
                  if (count > 1) {
                    messages(s"dataError.errorMessage.$key.p1.plural", count)
                  } else {
                    messages(s"dataError.errorMessage.$key.p1", groupedCells)
                  }
              }
          }
      (Seq(messages("dataError.errorMessage.genericErrors.p2")), true, groupedErrors)
    } else {
      val count = errors.size
      
      val errorMessage: Seq[String] = errors.headOption match {

        case Some(_: CsvError.InvalidCountry) =>
          if (count > 1) {
            Seq(
              messages("dataError.errorMessage.incorrectCountry.p1.plural", count),
              messages("dataError.errorMessage.incorrectCountry.p2.plural")
            )
          } else {
            Seq(
              messages("dataError.errorMessage.incorrectCountry.p1", cells),
              messages("dataError.errorMessage.incorrectCountry.p2")
            )
          }

        case Some(_: CsvError.InvalidCharacter) =>
          if (count > 1) {
            Seq(messages("dataError.errorMessage.invalidCharacter.p1.plural", count))
          } else {
            Seq(messages("dataError.errorMessage.invalidCharacter.p1", cells))
          }

        case Some(_: CsvError.InvalidNumberFormat) =>
          if (count > 1) {
            Seq(messages("dataError.errorMessage.invalidNumber.p1.plural", count))
          } else {
            Seq(messages("dataError.errorMessage.invalidNumber.p1", cells))
          }

        case Some(_: CsvError.NegativeNumber) =>
          if (count > 1){
            Seq(messages("dataError.errorMessage.negativeNumber.p1.plural", count))
          } else {
            Seq(messages("dataError.errorMessage.negativeNumber.p1", cells))
          }

        case Some(_: CsvError.TooMayDecimals) =>
          if (count > 1) {
            Seq(messages("dataError.errorMessage.tooManyDecimals.p1.plural", count))
          } else {
            Seq(messages("dataError.errorMessage.tooManyDecimals.p1", cells))
          }

        case Some(_: CsvError.BlankCell) =>
          if (count > 1) {
            Seq(messages("dataError.errorMessage.blankCell.p1.plural", count))
          } else {
            Seq(messages("dataError.errorMessage.blankCell.p1", cells))
          }

        case Some(_: CsvError.VatRateNotAllowed) =>
          val countries = errors.collect {
            case e: CsvError.VatRateNotAllowed =>
              e.country
          }.distinct.sorted.mkString(", ")
          
          if (count > 1) {
            Seq(messages("dataError.errorMessage.incorrectVatRate.p1.plural", count))
          } else {
            Seq(messages("dataError.errorMessage.incorrectVatRate.p1", countries))
          }

        case Some(_: CsvError.DuplicateVatRate) =>
          val countryFrom = errors.collect {
            case e: CsvError.DuplicateVatRate =>
              e.countryFrom
          }.distinct.sorted.mkString(", ")

          val countryTo = errors.collect {
            case e: CsvError.DuplicateVatRate =>
              e.countryTo
          }.distinct.sorted.mkString(", ")
          
          if (count > 1) {
            Seq(messages("dataError.errorMessage.duplicateVatRate.p1.plural", count))
          } else {
            Seq(messages("dataError.errorMessage.duplicateVatRate.p1", countryFrom, countryTo))
          }

        case Some(_: CsvError.TooManyColumns) =>
          if (count > 1) {
            Seq(messages("dataError.errorMessage.tooManyColumns.p1.plural", count))
          } else {
            Seq(messages("dataError.errorMessage.tooManyColumns.p1", cells))
          }

        case Some(_: CsvError.SameToAndFromCountry) =>
          Seq(messages("dataError.errorMessage.sameToAndFromCountry.p1"))

        case _ =>
          Seq(messages("dataError.errorMessage.genericError.p1"))
      }
      (errorMessage, false, Nil)
    }
  }
}
