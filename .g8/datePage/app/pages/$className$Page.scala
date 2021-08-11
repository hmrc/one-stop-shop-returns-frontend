package pages

import controllers.routes
import models.UserAnswers
import play.api.libs.json.JsPath
import play.api.mvc.Call

import java.time.LocalDate

case object $className$Page extends QuestionPage[LocalDate] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "$className;format="decap"$"

  override def navigateInNormalMode(answers: UserAnswers): Call =
    routes.IndexController.onPageLoad()
}
