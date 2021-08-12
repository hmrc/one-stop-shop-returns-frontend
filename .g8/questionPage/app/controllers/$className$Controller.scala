package controllers

import controllers.actions._
import forms.$className$FormProvider
import javax.inject.Inject
import models.{Mode, Period}
import pages.$className$Page
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.$className$View

import scala.concurrent.{ExecutionContext, Future}

class $className$Controller @Inject()(
                                      cc: AuthenticatedControllerComponents,
                                      formProvider: $className$FormProvider,
                                      view: $className$View
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()
  protected val controllerComponents: MessagesControllerComponents = cc
  
  def onPageLoad(mode: Mode, period: Period): Action[AnyContent] = cc.authAndGetData(period) {
    implicit request =>

      val preparedForm = request.userAnswers.get($className$Page) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode, period))
  }

  def onSubmit(mode: Mode, period: Period): Action[AnyContent] = cc.authAndGetData(period).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode, period))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set($className$Page, value))
            _              <- cc.sessionRepository.set(updatedAnswers)
          } yield Redirect($className$Page.navigate(mode, updatedAnswers))
      )
  }
}
