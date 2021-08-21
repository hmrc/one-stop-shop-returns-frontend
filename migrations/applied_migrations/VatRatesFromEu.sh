#!/bin/bash

echo ""
echo "Applying migration VatRatesFromEu"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /:period/vatRatesFromEu                        controllers.VatRatesFromEuController.onPageLoad(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/vatRatesFromEu                        controllers.VatRatesFromEuController.onSubmit(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes

echo "GET        /:period/changeVatRatesFromEu                  controllers.VatRatesFromEuController.onPageLoad(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/changeVatRatesFromEu                  controllers.VatRatesFromEuController.onSubmit(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "vatRatesFromEu.title = vatRatesFromEu" >> ../conf/messages.en
echo "vatRatesFromEu.heading = vatRatesFromEu" >> ../conf/messages.en
echo "vatRatesFromEu.option1 = Option 1" >> ../conf/messages.en
echo "vatRatesFromEu.option2 = Option 2" >> ../conf/messages.en
echo "vatRatesFromEu.checkYourAnswersLabel = vatRatesFromEu" >> ../conf/messages.en
echo "vatRatesFromEu.error.required = Select vatRatesFromEu" >> ../conf/messages.en
echo "vatRatesFromEu.change.hidden = VatRatesFromEu" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryVatRatesFromEuUserAnswersEntry: Arbitrary[(VatRatesFromEuPage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[VatRatesFromEuPage.type]";\
    print "        value <- arbitrary[VatRatesFromEu].map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryVatRatesFromEuPage: Arbitrary[VatRatesFromEuPage.type] =";\
    print "    Arbitrary(VatRatesFromEuPage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to ModelGenerators"
awk '/trait ModelGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryVatRatesFromEu: Arbitrary[VatRatesFromEu] =";\
    print "    Arbitrary {";\
    print "      Gen.oneOf(VatRatesFromEu.values)";\
    print "    }";\
    next }1' ../test/generators/ModelGenerators.scala > tmp && mv tmp ../test/generators/ModelGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(VatRatesFromEuPage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Migration VatRatesFromEu completed"
