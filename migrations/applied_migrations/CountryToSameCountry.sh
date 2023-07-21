#!/bin/bash

echo ""
echo "Applying migration CountryToSameCountry"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /:period/countryToSameCountry                       controllers.CountryToSameCountryController.onPageLoad(period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "countryToSameCountry.title = countryToSameCountry" >> ../conf/messages.en
echo "countryToSameCountry.heading = countryToSameCountry" >> ../conf/messages.en

echo "Migration CountryToSameCountry completed"
