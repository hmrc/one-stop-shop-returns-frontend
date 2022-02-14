package models.responses

import play.api.libs.json.{Json, OFormat}

import java.time.Instant
import java.util.UUID

case class CoreErrorResponse(
                              timestamp: Instant,
                              transactionId: Option[UUID],
                              error: String,
                              errorMessage: String
                            ) {
  val asException: Exception = new Exception(s"$timestamp $transactionId $error $errorMessage")
}

object CoreErrorResponse {
  implicit val format: OFormat[CoreErrorResponse] = Json.format[CoreErrorResponse]
  val REGISTRATION_NOT_FOUND = "OSS_009"
}