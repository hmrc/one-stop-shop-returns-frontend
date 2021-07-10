package controllers

import controllers.actions._
import javax.inject.Inject
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.$className$View

class $className$Controller @Inject()(
                                       cc: AuthenticatedControllerComponents,
                                       view: $className$View
                                     ) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = cc.authAndGetData {
    implicit request =>
      Ok(view())
  }
}
