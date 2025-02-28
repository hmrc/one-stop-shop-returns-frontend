/*
 * Copyright 2025 HM Revenue & Customs
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

package models.emails

import base.SpecBase
import play.api.libs.json.{JsError, JsNull, JsSuccess, Json}

class EmailToSendRequestSpec extends SpecBase {

  "EmailToSendRequest" - {

    "serialize correctly with ReturnsConfirmationEmailParameters" in {
      val emailRequest = EmailToSendRequest(
        to = List("recipient@example.com"),
        templateId = "returns-template",
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
              "templateId": "returns-template",
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

    "deserialize correctly with ReturnsConfirmationEmailParameters" in {
      val validJson = Json.parse(
        """
            {
              "to": ["recipient@example.com"],
              "templateId": "returns-template",
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
        templateId = "returns-template",
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

    "serialize correctly with ReturnsConfirmationEmailNoVatOwedParameters" in {
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

    "deserialize correctly with ReturnsConfirmationEmailNoVatOwedParameters" in {
      val validJson = Json.parse(
        """
                {
                  "to": ["recipient@example.com"],
                  "templateId": "returns-template",
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
        templateId = "returns-template",
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
              "templateId": "returns-template",
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
              "templateId": "returns-template",
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
        "templateId" -> "returns-template",
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
              "templateId": "returns-template",
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