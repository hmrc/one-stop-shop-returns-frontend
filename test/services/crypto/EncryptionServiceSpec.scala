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

package services.crypto

import base.SpecBase
import org.scalatest.matchers.should.Matchers.should
import play.api.Configuration
import play.api.test.Helpers.running


class EncryptionServiceSpec extends SpecBase {
  "EncryptionService" - {
    "must encrypt text value" in {
      val textToEncrypt: String = "Test String"
      val application = applicationBuilder().build()
      running(application) {
        val configuration: Configuration = application.configuration
        val service: EncryptionService = new EncryptionService(configuration)
        val result = service.encryptField(textToEncrypt)
        result mustBe a[String]
        result should not equal textToEncrypt
      }
    }
    "must decrypt text value" in {
      val textToEncrypt: String = "Test String"
      val application = applicationBuilder().build()
      running(application) {
        val configuration: Configuration = application.configuration
        val service: EncryptionService = new EncryptionService(configuration)
        val encryptedValue = service.encryptField(textToEncrypt)
        val result = service.decryptField(encryptedValue)
        result mustBe a[String]
        result mustBe textToEncrypt
      }
    }
    "must throw a Security Exception if text value can't be decrypted" in {
      val textToEncrypt: String = "Test String"
      val application = applicationBuilder().build()
      running(application) {
        val configuration: Configuration = application.configuration
        val service: EncryptionService = new EncryptionService(configuration)
        val invalidEncryptedValue = service.encryptField(textToEncrypt) + "any"
        val result = intercept[SecurityException](service.decryptField(invalidEncryptedValue))
        result.getMessage mustBe "Unable to decrypt value"
      }
    }
  }
}