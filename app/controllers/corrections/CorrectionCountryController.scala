/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.corrections

import controllers.actions.*
import forms.corrections.CorrectionCountryFormProvider
import models.corrections.CorrectionToCountry
import models.requests.DataRequest
import models.{Country, Index, Mode, Period, UserAnswers}
import pages.corrections.CorrectionCountryPage
import play.api.i18n.I18nSupport
import play.api.i18n.Lang.logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.corrections.{AllCorrectionCountriesQuery, PreviouslyDeclaredCorrectionAmount, PreviouslyDeclaredCorrectionAmountQuery}
import services.VatReturnService
import uk.gov.hmrc.http.HttpException
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.corrections.CorrectionCountryView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CorrectionCountryController @Inject()(
                                             cc: AuthenticatedControllerComponents,
                                             formProvider: CorrectionCountryFormProvider,
                                             view: CorrectionCountryView,
                                             vatReturnService: VatReturnService
                                           )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with CorrectionBaseController {


  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(mode: Mode, period: Period, periodIndex: Index, countryIndex: Index): Action[AnyContent] =
    cc.authAndGetDataAndCorrectionEligible(period).async {
      implicit request =>
        getCorrectionReturnPeriod(periodIndex) { correctionReturnPeriod =>
          val form = formProvider(countryIndex,
            request.userAnswers
              .get(AllCorrectionCountriesQuery(periodIndex)).getOrElse(Seq.empty).map(_.correctionCountry))

          val preparedForm = request.userAnswers.get(CorrectionCountryPage(periodIndex, countryIndex)) match {
            case None => form
            case Some(value) => form.fill(value)
          }
          Future.successful(Ok(view(preparedForm, mode, request.userAnswers.period, periodIndex, correctionReturnPeriod, countryIndex)))
        }
    }

  def onSubmit(mode: Mode, period: Period, periodIndex: Index, countryIndex: Index): Action[AnyContent] =
    cc.authAndGetDataAndCorrectionEligible(period).async { implicit request =>
      getCorrectionReturnPeriod(periodIndex) { correctionReturnPeriod =>
        val form = formProvider(countryIndex, request.userAnswers
          .get(AllCorrectionCountriesQuery(periodIndex)).getOrElse(Seq.empty).map(_.correctionCountry))

        form.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(view(
              formWithErrors,
              mode,
              request.userAnswers.period,
              periodIndex,
              correctionReturnPeriod,
              countryIndex
            ))),

          value =>
            (for {
              previouslyDeclaredCorrectionAmount <- vatReturnService.getLatestVatAmountForPeriodAndCountry(value, correctionReturnPeriod)
              updatedAnswers <- updateUserAnswers(value, periodIndex, countryIndex, previouslyDeclaredCorrectionAmount)
              _ <- cc.sessionRepository.set(updatedAnswers)
            } yield Redirect(CorrectionCountryPage(periodIndex, countryIndex).navigate(mode, updatedAnswers)))
              .recover {
                case e: HttpException =>
                  logger.error(s"Error retrieving VAT return for period $correctionReturnPeriod: ${e.getMessage}")
                  throw e
              }
        )
      }
    }

  private def updateUserAnswers(
                                 country: Country,
                                 periodIndex: Index,
                                 countryIndex: Index,
                                 previouslyDeclaredCorrectionAmount: PreviouslyDeclaredCorrectionAmount
                               )(implicit request: DataRequest[AnyContent]): Future[UserAnswers] = {
    for {
      updated <- Future.fromTry(
        request.userAnswers.set(CorrectionCountryPage(periodIndex, countryIndex), country)
          .flatMap(_.set(
            PreviouslyDeclaredCorrectionAmountQuery(periodIndex, countryIndex),
            previouslyDeclaredCorrectionAmount
          ))
      )
    } yield updated
  }
}
