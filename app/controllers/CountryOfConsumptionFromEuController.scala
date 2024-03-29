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

package controllers

import controllers.actions._
import forms.CountryOfConsumptionFromEuFormProvider
import models.{Country, Index, Mode, Period}
import models.Country.euCountriesWithNI
import pages.CountryOfConsumptionFromEuPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.AllSalesToEuQuery
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.CountryOfConsumptionFromEuView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CountryOfConsumptionFromEuController @Inject()(
                                                      cc: AuthenticatedControllerComponents,
                                                      formProvider: CountryOfConsumptionFromEuFormProvider,
                                                      view: CountryOfConsumptionFromEuView
                                                    )(implicit ec: ExecutionContext)
  extends FrontendBaseController with SalesFromEuBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(
                  mode: Mode,
                  period: Period,
                  countryFromIndex: Index,
                  countryToIndex: Index
  ): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>
      getCountryFrom(countryFromIndex) {
        countryFrom =>

          val isOnlineMarketplace = request.registration.isOnlineMarketplace

          val form =
            formProvider(
              countryToIndex,
              request.userAnswers
                .get(AllSalesToEuQuery(countryFromIndex))
                .getOrElse(Seq.empty)
                .map(_.countryOfConsumption),
              countryFrom,
              isOnlineMarketplace
            )

          val countries = if(isOnlineMarketplace) euCountriesWithNI else euCountriesWithNI.filterNot(_ == countryFrom)
          val selectItems = Country.selectItems(countries)

          val preparedForm =
            request.userAnswers.get(CountryOfConsumptionFromEuPage(countryFromIndex, countryToIndex)) match {
              case None => form
              case Some(value) => form.fill(value)
            }

          Ok(view(
            preparedForm, mode, period, countryFromIndex, countryToIndex, countryFrom, selectItems
          ))
      }
  }

  def onSubmit(mode: Mode, period: Period, countryFromIndex: Index, countryToIndex: Index): Action[AnyContent] = cc.authAndGetData(period).async {
    implicit request =>
      getCountryFromAsync(countryFromIndex) {
        countryFrom =>

          val isOnlineMarketplace = request.registration.isOnlineMarketplace
          val form = formProvider(
            countryToIndex,
            request.userAnswers
              .get(AllSalesToEuQuery(countryFromIndex))
              .getOrElse(Seq.empty)
              .map(_.countryOfConsumption),
            countryFrom,
            isOnlineMarketplace
          )

          val countries = if(isOnlineMarketplace) euCountriesWithNI else euCountriesWithNI.filterNot(_ == countryFrom)
          val selectItems = Country.selectItems(countries)

          form.bindFromRequest().fold(
            formWithErrors =>
              Future.successful(BadRequest(view(
                formWithErrors, mode, period, countryFromIndex, countryToIndex, countryFrom, selectItems
              ))),

            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(CountryOfConsumptionFromEuPage(countryFromIndex, countryToIndex), value))
                _ <- cc.sessionRepository.set(updatedAnswers)
              } yield Redirect(CountryOfConsumptionFromEuPage(countryFromIndex, countryToIndex).navigate(mode, updatedAnswers))
          )
      }
  }
}
