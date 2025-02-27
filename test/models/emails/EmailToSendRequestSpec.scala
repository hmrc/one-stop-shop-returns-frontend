package models.emails

import base.SpecBase
import play.api.libs.json.{JsError, JsNull, JsSuccess, Json}

class EmailToSendRequestSpec extends SpecBase {

  "EmailToSendRequest" - {

    "serialize correctly with RegistrationConfirmation" in {
      val emailRequest = EmailToSendRequest(
        to = List("recipient@example.com"),
        templateId = "registration-template",
        parameters = ReturnsConfirmationEmailParameters(
          recipientName_line1 = "John Doe",
          businessName = "XYZ Corp",
          period = "Period",
          paymentDeadline = "Payment deadline"
        ),
        force = true
      )

      val expectedJson = Json.parse(
        """
            {
              "to": ["recipient@example.com"],
              "templateId": "registration-template",
              "parameters": {
               "recipientName_line1": "John Doe",
               "businessName": "XYZ Corp",
               "period": "Period",
               "paymentDeadline": "Payment deadline"
              },
              "force": true
            }
          """
      )

      Json.toJson(emailRequest) mustBe expectedJson
    }

    "deserialize correctly with RegistrationConfirmation" in {
      val validJson = Json.parse(
        """
            {
              "to": ["recipient@example.com"],
              "templateId": "registration-template",
              "parameters": {
               "recipientName_line1": "John Doe",
               "businessName": "XYZ Corp",
               "period": "Period",
               "paymentDeadline": "Payment deadline"
              },
              "force": true
            }
          """
      )

      val expectedEmailRequest = EmailToSendRequest(
        to = List("recipient@example.com"),
        templateId = "registration-template",
        parameters = ReturnsConfirmationEmailParameters(
          recipientName_line1 = "John Doe",
          businessName = "XYZ Corp",
          period = "Period",
          paymentDeadline = "Payment deadline"
        ),
        force = true
      )

      validJson.validate[EmailToSendRequest] mustBe JsSuccess(expectedEmailRequest)
    }

    "serialize correctly with AmendRegistrationConfirmation" in {
      val emailRequest = EmailToSendRequest(
        to = List("user@example.com"),
        templateId = "amend-template",
        parameters = ReturnsConfirmationEmailNoVatOwedParameters(
          recipientName_line1 = "Alice Smith",
          period = "Period"
        ),
        force = false
      )

      val expectedJson = Json.parse(
        """
            {
              "to": ["user@example.com"],
              "templateId": "amend-template",
              "parameters": {
                "recipientName_line1": "Alice Smith",
                "period": "Period"
              },
              "force": false
            }
          """
      )

      Json.toJson(emailRequest) mustBe expectedJson
    }

    "deserialize correctly with AmendRegistrationConfirmation" in {
      val validJson = Json.parse(
        """
                {
                  "to": ["recipient@example.com"],
                  "templateId": "registration-template",
                  "parameters": {
                    "recipientName_line1": "Alice Smith",
                    "period": "Period"
                  },
                  "force": true
                }
              """
      )

      val expectedEmailRequest = EmailToSendRequest(
        to = List("recipient@example.com"),
        templateId = "registration-template",
        parameters = ReturnsConfirmationEmailNoVatOwedParameters(
          recipientName_line1 = "Alice Smith",
          period = "Period"
        ),
        force = true
      )

      validJson.validate[EmailToSendRequest] mustBe JsSuccess(expectedEmailRequest)
    }

    "fail to deserialize when a required field is missing" in {
      val invalidJson = Json.parse(
        """
            {
              "templateId": "registration-template",
              "parameters": {
                "recipientName_line1": "John Doe",
                "businessName": "XYZ Corp",
                 "period": "Period",
                 "paymentDeadline": "Payment deadline"
              },
              "force": true
            }
          """
      )

      invalidJson.validate[EmailToSendRequest] mustBe a[JsError]
    }

    "fail to deserialize when parameters field is invalid" in {
      val invalidJson = Json.parse(
        """
            {
              "to": ["recipient@example.com"],
              "templateId": "registration-template",
              "parameters": {
                "unknownField": "invalid data"
              },
              "force": true
            }
          """
      )

      invalidJson.validate[EmailToSendRequest] mustBe a[JsError]
    }

    "fail to deserialize when parameters field is null" in {
      val invalidJson = Json.obj(
        "to" -> JsNull,
        "templateId" -> "registration-template",
        "parameters" -> Json.obj(
          "unknownField" -> "invalid data"
        ),
        "force" -> true
      )

      invalidJson.validate[EmailToSendRequest] mustBe a[JsError]
    }

    "fail to deserialize when an unexpected field is present" in {

      val invalidJson = Json.parse(
        """
            {
              "to": ["recipient@example.com"],
              "templateId": "registration-template",
              "parameters": {
                "unknownField": "invalid data"
              },
              "force": true,
              "extraField": "unexpected"
            }
          """
      )

      invalidJson.validate[EmailToSendRequest] mustBe a[JsError]
    }

    "handle missing values" in {

      val invalidJson = Json.obj()

      invalidJson.validate[EmailToSendRequest] mustBe a[JsError]
    }

    "handle empty string values" in {

      val invalidJson = Json.parse(
        """
                            {
                              "to": [""],
                              "templateId": "",
                              "parameters": {
                                "unknownField": ""
                              },
                              "force": true
                            }
                          """
      )

      invalidJson.validate[EmailToSendRequest] mustBe a[JsError]
    }
  }
}