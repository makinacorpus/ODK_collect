ODK_collect
===========

ODK Collect is a phone-based replacement for your paper forms. It renders a form, survey, or algorithm into prompts that support complex logic, repeating questions, and multiple languages. Data types include text, location, photos, video, audio, and barcodes. Collect is part of the Open Data Kit tool suite, go to http://opendatakit.org to find out more.
(official description of ODK_Collect from the Play Store)

This application is based on ODK_Collect but it's not the version you can find in the store. This version uses the look of the last android versions (3.0 and more) with backward support, improves the geolocalisation UI and fixes a few bugs.

User aspects : 

You can find the installation file at /bin/ODKCollect_Modifie.apk
If you cannot install it, check that you have not the version from the play store installed on your phone, and if so, uninstall it. If it still doesn't work, go in System Settings -> Security -> Device Administration and if unchecked, check "Unknown sources".

Forms can be defined in xls using a syntax documented here : http://formhub.org/syntax/ and then uploaded on a server that supports it, like http://formhub.org/


Technical aspects : 

Main modifications compared to the play store version : 
 - Usage of the action bar in most activities instead of buttons particularly hard to use on tiny screens.
 - The styles are now defined in the /res/values/theme.xml file
 - Usage of the map is now default for geopoint questions (see settings to use old interface). The map uses the Google Maps Android API v2. See : 
		src/org/odk/collect/android/activities/GeoPointmapActivity.java
		src/org/odk/collect/android/widgets/GeoPointWidget.java
		res/layout/geopoint_layout.xml

Libraries : 
 - Already used in the play store version : see LICENSE.txt
 - For the global "holo" look and the action bar : actionbarsherlock
See : http://actionbarsherlock.com/
 - For the map in geopoint widgets : google-play-services_lib
See : https://developers.google.com/maps/documentation/android/

Bugs : 
 - In a form, when several questions are related by a relevance constraint in the same group with a field-list appearance : if you give an answer to a question that should make another question appear, unless you leave the current view and come back on it, the other question won't appear.

Improvements to add :
 - When in a repeating group, it would be useful to be able to customize the name of each instance of the group.
 - Have a widget to enter a time without a date.


