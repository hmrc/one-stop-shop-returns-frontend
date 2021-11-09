#!/bin/bash

echo ""
echo "Applying migration VatPayableForCountry"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /:period/vatPayableForCountry                        controllers.VatPayableForCountryController.onPageLoad(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/vatPayableForCountry                        controllers.VatPayableForCountryController.onSubmit(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes

echo "GET        /:period/changeVatPayableForCountry                  controllers.VatPayableForCountryController.onPageLoad(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/changeVatPayableForCountry                  controllers.VatPayableForCountryController.onSubmit(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "vatPayableForCountry.title = vatPayableForCountry" >> ../conf/messages.en
echo "vatPayableForCountry.heading = vatPayableForCountry" >> ../conf/messages.en
echo "vatPayableForCountry.checkYourAnswersLabel = vatPayableForCountry" >> ../conf/messages.en
echo "vatPayableForCountry.error.required = Select yes if vatPayableForCountry" >> ../conf/messages.en
echo "vatPayableForCountry.change.hidden = VatPayableForCountry" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryVatPayableForCountryUserAnswersEntry: Arbitrary[(VatPayableForCountryPage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[VatPayableForCountryPage.type]";\
    print "        value <- arbitrary[Boolean].map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryVatPayableForCountryPage: Arbitrary[VatPayableForCountryPage.type] =";\
    print "    Arbitrary(VatPayableForCountryPage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(VatPayableForCountryPage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Migration VatPayableForCountry completed"
