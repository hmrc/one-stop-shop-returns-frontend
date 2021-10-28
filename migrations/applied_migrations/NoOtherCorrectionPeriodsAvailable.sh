#!/bin/bash

echo ""
echo "Applying migration NoOtherCorrectionPeriodsAvailable"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /:period/noOtherCorrectionPeriodsAvailable                       controllers.NoOtherCorrectionPeriodsAvailableController.onPageLoad(period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "noOtherCorrectionPeriodsAvailable.title = noOtherCorrectionPeriodsAvailable" >> ../conf/messages.en
echo "noOtherCorrectionPeriodsAvailable.heading = noOtherCorrectionPeriodsAvailable" >> ../conf/messages.en

echo "Migration NoOtherCorrectionPeriodsAvailable completed"
