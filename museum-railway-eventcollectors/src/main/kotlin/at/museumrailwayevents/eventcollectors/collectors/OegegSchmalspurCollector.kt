package at.museumrailwayevents.eventcollectors.collectors

import at.museumrailwayevents.eventcollectors.collectors.dateParser.DateParser
import at.museumrailwayevents.eventcollectors.collectors.dateParser.DateParser.convertMonthNameToMonth
import at.museumrailwayevents.eventcollectors.collectors.dateParser.DateParser.dateRegex
import at.museumrailwayevents.eventcollectors.collectors.dateParser.DateParser.monthWrittenRegex
import at.museumrailwayevents.eventcollectors.collectors.dateParser.DateParser.yearRegex
import at.museumrailwayevents.eventcollectors.collectors.dateParser.DateParser.zoneOffset
import at.museumrailwayevents.eventcollectors.service.JsoupCrawler
import base.boudicca.SemanticKeys
import base.boudicca.model.Event
import org.jsoup.Jsoup
import java.time.*
import java.util.*

class OegegSchmalspurCollector(val jsoupCrawler: JsoupCrawler) : MuseumRailwayEventCollector(
    operatorId = "oegeg",
    locationId = "oegeg_schmalspur",
    url = "https://www.oegeg.at/termine/termine-schmalspur-steyrtalbahn/"
) {
    private val eventTitle = "Steyrtal Museumsbahn"

    override fun collectEvents(): List<Event> {
        val document = Jsoup.connect(url).get()
        val eventBoxes = document.select("div.j-textWithImage")

        // dates on this website can be split having multiple dates in the same line
        // eg. Sonntag, 4., 11., 18. und 25. Juni 2023
        val events = mutableListOf<Event>()

        val zuglokKeyword = "Zuglok"
        eventBoxes.forEach { eventBox ->
            val dateString = eventBox.select("span").eachText().firstOrNull {
                dateRegex.containsMatchIn(it) && monthWrittenRegex.containsMatchIn(it.lowercase()) && !it.contains(
                    zuglokKeyword
                )
            }
            val zuglokString = eventBox.select("span").eachText().firstOrNull {
                it.contains(zuglokKeyword)
            }
            val imageElement = document.select("img").first();
            val pictureUrl = imageElement?.absUrl("src");
            val description = eventBox.select("p").eachText().joinToString("\n")

            if (dateString == null) {
                // some of the boxes are just used for additional info
                // if there are no dates, then skip it
                return@forEach
            }

            val year = DateParser.findSingleYearOrAssumeDefault(dateString)
            val month = DateParser.findSingleWrittenMonth(dateString)
            val dates = dateRegex.findAll(dateString).toList()

            dates.forEach { dateMatch ->
                val dateValue = dateMatch.mapToDateValue()
                val startDate =
                    OffsetDateTime.of(
                        year,
                        month.value,
                        dateValue,
                        0, 0, 0, 0,
                        zoneOffset,
                    )
                val additionalData = mutableMapOf<String, String>()

                val parsedZuglok = parseZuglok(dateValue, zuglokString)
                if (parsedZuglok != null) {
                    additionalData["lokomotive"] = parsedZuglok
                }
                if (pictureUrl != null) {
                    additionalData[SemanticKeys.PICTUREURL] = pictureUrl
                }
                additionalData[SemanticKeys.DESCRIPTION] = description

                events.add(createEvent(eventTitle, startDate, additionalData))
            }
        }

        return events
    }


    private fun parseZuglok(dateValueOfEvent: Int, zuglokString: String?): String? {
        if (zuglokString == null) {
            return null
        }

        val date = dateRegex.findAll(zuglokString).firstOrNull()?.mapToDateValue() ?: return null
        if (date == dateValueOfEvent) {
            val trimmedString = zuglokString.split(":").last().trim()
            if (trimmedString.isBlank()) {
                return null
            } else {
                return trimmedString
            }
        }
        return null
    }

    override fun getName(): String = "ÖGEG Schmalspur Termine"
}

private fun MatchResult.mapToDateValue(): Int = value.trim('.').toInt()