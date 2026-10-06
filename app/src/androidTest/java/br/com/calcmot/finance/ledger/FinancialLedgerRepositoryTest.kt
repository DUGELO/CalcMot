package br.com.calcmot.finance.ledger

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.ZoneId
import java.util.zip.ZipInputStream

@RunWith(AndroidJUnit4::class)
class FinancialLedgerRepositoryTest {
    private lateinit var database: CalcMotFinanceDatabase
    private lateinit var repository: RoomFinancialLedgerRepository
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, CalcMotFinanceDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomFinancialLedgerRepository(context, database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun stableOfferIsIdempotentInsideEpisodeWindow() = runBlocking {
        val first = repository.recordStableOffer(observation(atMillis = 1_000_000L))
        val duplicate = repository.recordStableOffer(observation(atMillis = 1_030_000L))
        val summary = repository.observeOfferSummary(LedgerPeriod(0L, 2_000_000L)).first()

        assertTrue(first)
        assertFalse(duplicate)
        assertEquals(1, summary.offerCount)
    }

    @Test
    fun changingUberCountdownStaysInSameOfferEpisode() = runBlocking {
        val first = repository.recordStableOffer(observation(atMillis = 1_000_000L, fingerprint = "first"))
        val countdownUpdate = repository.recordStableOffer(
            observation(
                atMillis = 1_060_000L,
                fingerprint = "countdown-update",
                pickupDistanceMeters = 700L,
                pickupTimeSeconds = 180,
                tripDistanceMeters = 4_000L,
                tripTimeSeconds = 540
            )
        )
        val offers = repository.observeOffers(LedgerPeriod(0L, 2_000_000L)).first()

        assertTrue(first)
        assertFalse(countdownUpdate)
        assertEquals(1, offers.size)
        assertEquals(1_060_000L, offers.single().lastSeenAtMillis)
    }

    @Test
    fun materiallyDifferentTripRemainsANewOffer() = runBlocking {
        repository.recordStableOffer(observation(atMillis = 1_000_000L, fingerprint = "first"))

        val inserted = repository.recordStableOffer(
            observation(
                atMillis = 1_060_000L,
                fingerprint = "different-trip",
                tripDistanceMeters = 5_000L,
                tripTimeSeconds = 780
            )
        )

        assertTrue(inserted)
        assertEquals(2, repository.observeOffers(LedgerPeriod(0L, 2_000_000L)).first().size)
    }

    @Test
    fun similarOfferOutsideContinuityWindowRemainsANewOffer() = runBlocking {
        repository.recordStableOffer(observation(atMillis = 1_000_000L, fingerprint = "first"))

        val inserted = repository.recordStableOffer(
            observation(
                atMillis = 1_000_000L + RoomFinancialLedgerRepository.OFFER_CONTINUITY_WINDOW_MILLIS + 1L,
                fingerprint = "later-offer"
            )
        )

        assertTrue(inserted)
        assertEquals(2, repository.observeOffers(LedgerPeriod(0L, 2_000_000L)).first().size)
    }

    @Test
    fun sessionStaysOpenAt89MinutesAndSplitsAt90Minutes() = runBlocking {
        val start = 10_000_000L
        repository.recordStableOffer(observation(atMillis = start, fingerprint = "first"))
        repository.recordStableOffer(
            observation(
                atMillis = start + RoomFinancialLedgerRepository.SESSION_IDLE_TIMEOUT_MILLIS - 60_000L,
                fingerprint = "second"
            )
        )
        assertEquals(1, repository.observeSessions(LedgerPeriod(0L, Long.MAX_VALUE)).first().size)

        database.deleteAllFinancialHistory()
        repository.recordStableOffer(observation(atMillis = start, fingerprint = "third"))
        repository.recordStableOffer(
            observation(
                atMillis = start + RoomFinancialLedgerRepository.SESSION_IDLE_TIMEOUT_MILLIS,
                fingerprint = "fourth"
            )
        )
        val sessions = repository.observeSessions(LedgerPeriod(0L, Long.MAX_VALUE)).first()
        assertEquals(2, sessions.size)
        assertEquals(start, sessions.last().endedAtMillis)
    }

    @Test
    fun driverCorrectionUpdatesOfficialValuesAndConfidence() = runBlocking {
        repository.recordStableOffer(observation(atMillis = 1_000_000L))
        val offer = repository.observeRecentOffers(LedgerPeriod(0L, 2_000_000L), 1).first().single()

        val corrected = repository.correctOffer(
            offer.id,
            OfferCorrection(
                fareCents = 1_250L,
                pickupDistanceMeters = 900L,
                pickupTimeSeconds = 240,
                tripDistanceMeters = 4_100L,
                tripTimeSeconds = 600
            )
        )
        val updated = repository.getOffer(offer.id)

        assertTrue(corrected)
        requireNotNull(updated)
        assertEquals(1_250L, updated.fareCents)
        assertEquals(LedgerConfidence.DRIVER_CORRECTED.name, updated.confidence)
    }

    @Test
    fun xlsxContainsOnlyStructuredColumnsAndNoSensitiveFields() = runBlocking {
        repository.recordStableOffer(observation(atMillis = 1_000_000L))
        val snapshot = repository.exportSnapshot(LedgerPeriod(0L, 2_000_000L))
        val output = ByteArrayOutputStream()
        LocalReportExporter().exportDailyXlsx(snapshot, output)

        val names = mutableSetOf<String>()
        val contents = StringBuilder()
        ZipInputStream(output.toByteArray().inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                names += entry.name
                contents.append(zip.readBytes().toString(Charsets.UTF_8))
                entry = zip.nextEntry
            }
        }
        assertTrue("xl/worksheets/sheet1.xml" in names)
        val text = contents.toString().lowercase()
        assertFalse("address" in text)
        assertFalse("passenger" in text)
        assertFalse("screenshot" in text)
        assertFalse("ocr_text" in text)
    }

    @Test
    fun pdfIsGeneratedWithoutSensitiveLabels() = runBlocking {
        repository.recordStableOffer(observation(atMillis = 1_000_000L))
        val snapshot = repository.exportSnapshot(LedgerPeriod(0L, 2_000_000L))
        val output = ByteArrayOutputStream()

        LocalReportExporter().exportDailyPdf(snapshot, output)

        val bytes = output.toByteArray()
        assertTrue(bytes.size > 500)
        assertEquals("%PDF", bytes.copyOfRange(0, 4).toString(Charsets.US_ASCII))
        val printable = bytes.toString(Charsets.ISO_8859_1).lowercase()
        assertFalse("passenger" in printable)
        assertFalse("address" in printable)
        assertFalse("screenshot" in printable)
    }

    @Test
    fun nonOperatingExpenseAffectsCashButNotOperatingProfit() = runBlocking {
        repository.addExpense(2_000L, ExpenseCategory.FOOD, "Almoço", false, 1_000_000L)
        repository.addExpense(5_000L, ExpenseCategory.FUEL, "Combustível", true, 1_000_100L)

        val summary = repository.observeExpenseSummary(LedgerPeriod(0L, 2_000_000L)).first()

        assertEquals(7_000L, summary.totalCents)
        assertEquals(5_000L, summary.operatingTotalCents)
        assertEquals(2, summary.itemCount)
    }

    @Test
    fun retentionKeepsExactlySixCalendarMonths() = runBlocking {
        val zone = ZoneId.of("America/Sao_Paulo")
        val now = LocalDate.of(2026, 8, 13).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val beforeCutoff = LocalDate.of(2026, 2, 12).atTime(23, 59).atZone(zone).toInstant().toEpochMilli()
        val atCutoff = LocalDate.of(2026, 2, 13).atStartOfDay(zone).toInstant().toEpochMilli()
        repository.recordStableOffer(observation(beforeCutoff, "old"))
        repository.recordStableOffer(
            observation(
                atMillis = atCutoff,
                fingerprint = "kept",
                tripDistanceMeters = 5_200L,
                tripTimeSeconds = 780
            )
        )

        repository.runRetention(now)

        val remaining = repository.observeOffers(LedgerPeriod(0L, Long.MAX_VALUE)).first()
        assertEquals(listOf("kept"), remaining.map { it.fingerprint })
    }

    @Test
    fun splitAndMergeSessionsReassignTheirOffers() = runBlocking {
        val start = 10_000_000L
        val splitAt = start + 20L * 60L * 1000L
        repository.recordStableOffer(observation(start, "first"))
        repository.recordStableOffer(observation(splitAt + 60_000L, "second"))
        val original = repository.observeSessions(LedgerPeriod(0L, Long.MAX_VALUE)).first().single()

        val secondSessionId = repository.splitSession(original.id, splitAt)

        requireNotNull(secondSessionId)
        val splitSessions = repository.observeSessions(LedgerPeriod(0L, Long.MAX_VALUE)).first()
        assertEquals(2, splitSessions.size)
        val splitOffers = repository.observeOffers(LedgerPeriod(0L, Long.MAX_VALUE)).first()
        assertEquals(original.id, splitOffers.first { it.fingerprint == "first" }.sessionId)
        assertEquals(secondSessionId, splitOffers.first { it.fingerprint == "second" }.sessionId)

        val mergedId = repository.mergeSessions(setOf(original.id, secondSessionId))

        assertEquals(original.id, mergedId)
        assertEquals(1, repository.observeSessions(LedgerPeriod(0L, Long.MAX_VALUE)).first().size)
        assertTrue(repository.observeOffers(LedgerPeriod(0L, Long.MAX_VALUE)).first().all { it.sessionId == mergedId })
    }

    private fun observation(
        atMillis: Long,
        fingerprint: String = "11.05|0.9|4|4.1|10",
        pickupDistanceMeters: Long = 900L,
        pickupTimeSeconds: Int = 240,
        tripDistanceMeters: Long = 4_100L,
        tripTimeSeconds: Int = 600
    ) = StableOfferObservation(
        platform = "uber",
        source = LedgerSource.UBER_ACCESSIBILITY,
        observedAtMillis = atMillis,
        fingerprint = fingerprint,
        fareCents = 1_105L,
        pickupDistanceMeters = pickupDistanceMeters,
        pickupTimeSeconds = pickupTimeSeconds,
        tripDistanceMeters = tripDistanceMeters,
        tripTimeSeconds = tripTimeSeconds,
        passengerRatingMilli = 4_950,
        classification = "GOOD",
        classificationReason = "Boa por km e por hora",
        goalPerKmMicros = 1_800_000L,
        goalPerHourCents = 3_500L,
        goalMode = "BALANCED"
    )
}
