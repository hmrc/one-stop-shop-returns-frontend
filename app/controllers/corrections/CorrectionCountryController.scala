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

import config.FrontendAppConfig
import connectors.VatReturnConnector
import controllers.actions.*
import forms.corrections.CorrectionCountryFormProvider
import models.corrections.CorrectionToCountry
import models.domain.VatReturn
import models.etmp.EtmpVatReturn
import models.requests.DataRequest
import models.{Country, Index, Mode, Period, UserAnswers}
import pages.corrections.CorrectionCountryPage
import play.api.i18n.I18nSupport
import play.api.i18n.Lang.logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.corrections.{AllCorrectionCountriesQuery, PreviouslyDeclaredCorrectionAmount, PreviouslyDeclaredCorrectionAmountQuery}
import services.corrections.CorrectionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.corrections.CorrectionCountryView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CorrectionCountryController @Inject()(
                                             cc: AuthenticatedControllerComponents,
                                             formProvider: CorrectionCountryFormProvider,
                                             vatReturnConnector: VatReturnConnector,
                                             view: CorrectionCountryView,
                                             correctionService: CorrectionService,
                                             config: FrontendAppConfig
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
            for {
              updatedAnswers <- updateUserAnswers(value, periodIndex, countryIndex, correctionReturnPeriod)
              _ <- cc.sessionRepository.set(updatedAnswers)
              vatReturnResult <- vatReturnConnector.get(correctionReturnPeriod)
              vatEtmpReturnResult <- vatReturnConnector.getEtmpVatReturn(correctionReturnPeriod)
              correctionsForPeriod <- correctionService.getCorrectionsForPeriod(correctionReturnPeriod)
              
            } yield {
              val vatReturnResultToUse = if(config.strategicReturnApiEnabled) vatEtmpReturnResult else vatReturnResult

              vatReturnResultToUse match {
                case Right(vatReturn) => Redirect(CorrectionCountryPage(periodIndex, countryIndex)
                  .navigate(mode, updatedAnswers, allRecipientCountries(vatReturn, correctionsForPeriod), config.strategicReturnApiEnabled))

                case Left(value) =>
                  logger.error(s"Error retrieving VAT return for period $correctionReturnPeriod: $value")
                  throw new Exception(value.toString)
              }
            }
        )
      }
    }

  private def updateUserAnswers(country: Country, periodIndex: Index, countryIndex: Index, correctionReturnPeriod: Period)
                               (implicit request: DataRequest[AnyContent]): Future[UserAnswers] = {
    if (config.strategicReturnApiEnabled) {
      for {
        (isPreviouslyDeclared, accumulativeVatForCountryTotalAmount) <- correctionService
          .getAccumulativeVatForCountryTotalAmount(request.vrn, country, correctionReturnPeriod)

        updated <- Future.fromTry(
          request.userAnswers.set(CorrectionCountryPage(periodIndex, countryIndex), country)
            .flatMap(_.set(
              PreviouslyDeclaredCorrectionAmountQuery(periodIndex, countryIndex),
              PreviouslyDeclaredCorrectionAmount(isPreviouslyDeclared, accumulativeVatForCountryTotalAmount)
            ))
        )
      } yield updated
    } else {
      Future.fromTry(request.userAnswers.set(CorrectionCountryPage(periodIndex, countryIndex), country))
    }
  }

  private val allRecipientCountries: (VatReturn, Seq[CorrectionToCountry]) => Seq[Country] = (vatReturn, correctionsForPeriod) => {
    val countriesFromNi = vatReturn.salesFromNi.map(sales => sales.countryOfConsumption)
    val countriesFromEU = vatReturn.salesFromEu.flatMap(recipientCountries => recipientCountries.sales.map(_.countryOfConsumption))
    (countriesFromNi ::: countriesFromEU ::: correctionsForPeriod.map(_.correctionCountry).toList).distinct
  }
  
}
