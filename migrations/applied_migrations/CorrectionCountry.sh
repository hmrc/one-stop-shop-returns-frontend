#!/bin/bash

echo ""
echo "Applying migration CorrectionCountry"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /:period/correctionCountry                        controllers.CorrectionCountryController.onPageLoad(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/correctionCountry                        controllers.CorrectionCountryController.onSubmit(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes

echo "GET        /:period/changeCorrectionCountry                  controllers.CorrectionCountryController.onPageLoad(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/changeCorrectionCountry                  controllers.CorrectionCountryController.onSubmit(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "correctionCountry.title = correctionCountry" >> ../conf/messages.en
echo "correctionCountry.heading = correctionCountry" >> ../conf/messages.en
echo "correctionCountry.checkYourAnswersLabel = correctionCountry" >> ../conf/messages.en
echo "correctionCountry.error.required = Enter correctionCountry" >> ../conf/messages.en
echo "correctionCountry.error.length = CorrectionCountry must be 100 characters or less" >> ../conf/messages.en
echo "correctionCountry.change.hidden = CorrectionCountry" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryCorrectionCountryUserAnswersEntry: Arbitrary[(CorrectionCountryPage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[CorrectionCountryPage.type]";\
    print "        value <- arbitrary[String].suchThat(_.nonEmpty).map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryCorrectionCountryPage: Arbitrary[CorrectionCountryPage.type] =";\
    print "    Arbitrary(CorrectionCountryPage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(CorrectionCountryPage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Migration CorrectionCountry completed"
