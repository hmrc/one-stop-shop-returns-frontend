#!/bin/bash

echo ""
echo "Applying migration CountryVatCorrection"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /:period/countryVatCorrection                  controllers.CountryVatCorrectionController.onPageLoad(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/countryVatCorrection                  controllers.CountryVatCorrectionController.onSubmit(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes

echo "GET        /:period/changeCountryVatCorrection                        controllers.CountryVatCorrectionController.onPageLoad(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/changeCountryVatCorrection                        controllers.CountryVatCorrectionController.onSubmit(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "countryVatCorrection.title = CountryVatCorrection" >> ../conf/messages.en
echo "countryVatCorrection.heading = CountryVatCorrection" >> ../conf/messages.en
echo "countryVatCorrection.checkYourAnswersLabel = CountryVatCorrection" >> ../conf/messages.en
echo "countryVatCorrection.error.nonNumeric = Enter your countryVatCorrection using numbers" >> ../conf/messages.en
echo "countryVatCorrection.error.required = Enter your countryVatCorrection" >> ../conf/messages.en
echo "countryVatCorrection.error.wholeNumber = Enter your countryVatCorrection using whole numbers" >> ../conf/messages.en
echo "countryVatCorrection.error.outOfRange = CountryVatCorrection must be between {0} and {1}" >> ../conf/messages.en
echo "countryVatCorrection.change.hidden = CountryVatCorrection" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryCountryVatCorrectionUserAnswersEntry: Arbitrary[(CountryVatCorrectionPage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[CountryVatCorrectionPage.type]";\
    print "        value <- arbitrary[Int].map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryCountryVatCorrectionPage: Arbitrary[CountryVatCorrectionPage.type] =";\
    print "    Arbitrary(CountryVatCorrectionPage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(CountryVatCorrectionPage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Migration CountryVatCorrection completed"
