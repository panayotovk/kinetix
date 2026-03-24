package com.kinetix.risk.client

import com.kinetix.common.model.Desk
import com.kinetix.common.model.DeskId
import com.kinetix.common.model.Division
import com.kinetix.common.model.DivisionId
import com.kinetix.risk.model.BookHierarchyEntry

interface HierarchyDataClient {
    suspend fun getAllDivisions(): List<Division>
    suspend fun getDesksByDivision(divisionId: DivisionId): List<Desk>
    suspend fun getAllDesks(): List<Desk>
    suspend fun getAllBookMappings(): List<BookHierarchyEntry>
}
