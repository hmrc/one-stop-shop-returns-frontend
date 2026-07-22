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

package controllers.test

import controllers.actions.AuthenticatedControllerComponents
import logging.Logging
import models.Period
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.fileUpload.CsvParserService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import scala.util.{Failure, Success}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TestOnlyCsvToUserAnswersController @Inject()(
                                                    cc: AuthenticatedControllerComponents,
                                                    csvParser: CsvParserService
                                                  )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  private val inlineCsv: String =

    """"HM Revenue and Customs logo","","",""
      |"One Stop Shop VAT return","","",""
      |"CountryFrom", "CountryTo","VAT % rate","Total eligible sales","Total VAT due"
      |"Northern Ireland","Germany","12.50%","£1200","£140"
      |"Northern Ireland","France","15","33,333","£4423"
      |"Austria","France","10%","150.01","£15"
      |"France","Austria","12.50%","£1200","£140"
      |""".stripMargin

  def populateUserAnswersFromCsv(period: Period): Action[AnyContent] = cc.authAndGetData(period).async {
    implicit request =>

      CsvParserService.split(inlineCsv) match {
        case Success(csvRows) => {
          Future.fromTry(csvParser.populateUserAnswersFromCsv(request.userAnswers, csvRows))
            .flatMap { updatedAnswers =>
              cc.sessionRepository.set(updatedAnswers).map { _ =>
                Redirect(controllers.routes.CheckYourAnswersController.onPageLoad(period).url)
              }
            }
            .recover { case _ =>
              logger.error("Failed to populate user answers from CSV")
              InternalServerError
            }
        }
        case Failure(exception) =>
          throw exception
      }


  }
}
