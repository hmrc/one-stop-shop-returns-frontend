#!/bin/bash

echo ""
echo "Applying migration VatCorrectionsList"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /:period/vatCorrectionsList                        controllers.VatCorrectionsListController.onPageLoad(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/vatCorrectionsList                        controllers.VatCorrectionsListController.onSubmit(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes

echo "GET        /:period/changeVatCorrectionsList                  controllers.VatCorrectionsListController.onPageLoad(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/changeVatCorrectionsList                  controllers.VatCorrectionsListController.onSubmit(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "vatCorrectionsList.title = vatCorrectionsList" >> ../conf/messages.en
echo "vatCorrectionsList.heading = vatCorrectionsList" >> ../conf/messages.en
echo "vatCorrectionsList.checkYourAnswersLabel = vatCorrectionsList" >> ../conf/messages.en
echo "vatCorrectionsList.error.required = Select yes if vatCorrectionsList" >> ../conf/messages.en
echo "vatCorrectionsList.change.hidden = VatCorrectionsList" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryVatCorrectionsListUserAnswersEntry: Arbitrary[(VatCorrectionsListPage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[VatCorrectionsListPage.type]";\
    print "        value <- arbitrary[Boolean].map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryVatCorrectionsListPage: Arbitrary[VatCorrectionsListPage.type] =";\
    print "    Arbitrary(VatCorrectionsListPage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(VatCorrectionsListPage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Migration VatCorrectionsList completed"
