URL_MAVEN_METADATA = https://jcenter.bintray.com/com/adobe/marketing/mobile/messaging/maven-metadata.xml
FILE_NAMES_CONTAINS_VERSION = ./code/gradle.properties ./code/messaging/src/phone/java/com/adobe/marketing/mobile/MessagingConstants.java

check-version:
	if curl -H "Accept: application/xml" -H "Content-Type: application/xml" -X GET ${URL_MAVEN_METADATA} | grep -o "<latest>.*</latest>" | grep -o "[0-9]*\.[0-9]*\.[0-9]*" | xargs -Istr grep -o -i "[mouduleVersion|version].*str" ${FILE_NAMES_CONTAINS_VERSION}; then exit 1; else exit 0; fi
	