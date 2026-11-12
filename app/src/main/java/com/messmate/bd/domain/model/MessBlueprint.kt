package com.messmate.bd.domain.model

import java.time.LocalTime
import java.time.YearMonth

/**
 * Everything the app needs to **create** a brand new mess sheet.
 *
 * This is the input of [com.messmate.bd.data.remote.SpreadsheetProvisioner].
 */
data class MessBlueprint(
    val messName: String,
    val monthYear: YearMonth,
    val managerName: String,
    val managerEmail: String,
    /** Members that should be present from day one (the manager is always first). */
    val members: List<BlueprintMember>,
    /** 0.5 or 1.0 – how many meals one breakfast counts as. */
    val breakfastWeight: Double = 1.0,
    val breakfastCutoff: LocalTime = LocalTime.of(7, 0),
    val lunchCutoff: LocalTime = LocalTime.of(10, 0),
    val dinnerCutoff: LocalTime = LocalTime.of(17, 0),
)

/** A member that is written into the `Members` tab when the mess is created. */
data class BlueprintMember(
    val memberId: String,
    val name: String,
    val gmail: String,
    val role: Role = Role.MEMBER,
    val seatRent: Double = 0.0,
    /** Balance carried over from the previous month. */
    val prevBalance: Double = 0.0,
    val activeStatus: ActiveStatus = ActiveStatus.ACTIVE,
)

/** The outcome of provisioning a new spreadsheet. */
data class ProvisionedMess(
    val spreadsheetId: String,
    val spreadsheetUrl: String,
    val fileName: String,
    val members: List<BlueprintMember>,
)
