package com.hussain.messmobile

import com.hussain.messmobile.data.backup.BackupFile
import com.hussain.messmobile.data.local.entity.AnnouncementEntity
import com.hussain.messmobile.data.local.entity.DuesEntity
import com.hussain.messmobile.data.local.entity.ExpenseEntity
import com.hussain.messmobile.data.local.entity.MealEntity
import com.hussain.messmobile.data.local.entity.MealEntryEntity
import com.hussain.messmobile.data.local.entity.MemberEntity
import com.hussain.messmobile.data.local.entity.PaymentEntity
import com.hussain.messmobile.data.local.entity.ShoppingItemEntity
import com.hussain.messmobile.data.model.ExpenseCategory
import com.hussain.messmobile.data.model.MealType
import com.hussain.messmobile.data.model.MemberRole
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The backup file is the contract that survives an uninstall of the app. */
class BackupFileTest {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    @Test
    fun aBackupSurvivesASerializationRoundTrip() {
        val backup = sampleBackup()

        val text = json.encodeToString(BackupFile.serializer(), backup)
        val restored = json.decodeFromString(BackupFile.serializer(), text)

        assertEquals(backup, restored)
    }

    @Test
    fun theBackupKeepsTheRelationsBetweenTheRecords() {
        val backup = sampleBackup()

        val restored = json.decodeFromString(
            BackupFile.serializer(),
            json.encodeToString(BackupFile.serializer(), backup),
        )

        val member = restored.members.single()
        val meal = restored.meals.single()
        val entry = restored.mealEntries.single()

        assertEquals(member.id, meal.cookId)
        assertEquals(meal.id, entry.mealId)
        assertEquals(member.id, entry.memberId)
    }

    @Test
    fun anEmptyBackupHoldsNoRecords() {
        val backup = BackupFile()

        assertEquals(0, backup.recordCount)
        assertTrue(backup.members.isEmpty())
    }

    @Test
    fun unknownFieldsInAnOlderFileAreIgnored() {
        val text = """
            {
              "formatVersion": 1,
              "somethingNew": 42,
              "members": []
            }
        """.trimIndent()

        val restored = json.decodeFromString(BackupFile.serializer(), text)

        assertEquals(1, restored.formatVersion)
        assertTrue(restored.members.isEmpty())
    }

    @Test
    fun theRecordCountCountsEveryTable() {
        assertEquals(8, sampleBackup().recordCount)
    }

    private fun sampleBackup() = BackupFile(
        formatVersion = BackupFile.CURRENT_FORMAT_VERSION,
        appVersion = "1.0.0",
        exportedAt = 1_700_000_000_000L,
        members = listOf(
            MemberEntity(
                id = 1,
                name = "Ali",
                phone = "0700 000 000",
                role = MemberRole.Admin,
                joinedAt = 1_600_000_000_000L,
                active = true,
            ),
        ),
        meals = listOf(
            MealEntity(
                id = 2,
                date = 1_700_000_000_000L,
                type = MealType.Lunch,
                description = "Rice and chicken",
                cookId = 1,
                totalCost = 120.0,
            ),
        ),
        mealEntries = listOf(
            MealEntryEntity(
                id = 3,
                mealId = 2,
                memberId = 1,
                optedIn = true,
                eaten = true,
                amount = 120.0,
            ),
        ),
        dues = listOf(
            DuesEntity(id = 4, memberId = 1, period = "2026-11", amount = 500.0),
        ),
        payments = listOf(
            PaymentEntity(id = 5, memberId = 1, period = "2026-11", amount = 200.0, paidAt = 1_700_000_000_000L),
        ),
        expenses = listOf(
            ExpenseEntity(
                id = 6,
                description = "Groceries",
                amount = 300.0,
                category = ExpenseCategory.Grocery,
                date = 1_700_000_000_000L,
                paidByMemberId = 1,
                shared = true,
            ),
        ),
        shoppingItems = listOf(
            ShoppingItemEntity(id = 7, name = "Rice", quantity = "5 kg", estimatedCost = 20.0),
        ),
        announcements = listOf(
            AnnouncementEntity(id = 8, title = "Meeting", body = "On Sunday", createdAt = 1_700_000_000_000L),
        ),
    )
}
