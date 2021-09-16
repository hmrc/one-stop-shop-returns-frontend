#!/bin/bash

echo ""
echo "Applying migration NoOtherPeriodsAvailable"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /:period/noOtherPeriodsAvailable                       controllers.NoOtherPeriodsAvailableController.onPageLoad(period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "noOtherPeriodsAvailable.title = noOtherPeriodsAvailable" >> ../conf/messages.en
echo "noOtherPeriodsAvailable.heading = noOtherPeriodsAvailable" >> ../conf/messages.en

echo "Migration NoOtherPeriodsAvailable completed"
