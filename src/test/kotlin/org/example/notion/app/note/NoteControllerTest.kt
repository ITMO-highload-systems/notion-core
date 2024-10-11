package org.example.notion.app.note

import com.jayway.jsonpath.JsonPath
import org.example.notion.AbstractIntegrationTest
import org.example.notion.app.user.dto.UserResponseDto
import org.example.notion.app.userPermission.entity.Permission
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class NoteControllerTest : AbstractIntegrationTest() {

    companion object {
        private val FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }

    lateinit var testUser: UserResponseDto

    @BeforeEach
    fun setUp() {
        testUser = createUser()
    }

    @Test
    fun `create note - valid params - success create`() {
        val response = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/note").header("user-id", testUser.userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(mapOf("title" to "title", "description" to "description")))
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.title").value("title")).andExpect(jsonPath("$.description").value("description"))
            .andExpect(jsonPath("$.owner").value(testUser.userId)).andReturn()

        val createdAtStr = JsonPath.read<String>(response.response.contentAsString, "$.created_at")
        val updatedAtStr = JsonPath.read<String>(response.response.contentAsString, "$.updated_at")

        val createdAt = LocalDateTime.parse(createdAtStr)
        val updatedAt = LocalDateTime.parse(updatedAtStr)

        assert(createdAtStr.isNotEmpty()) { "createdAt should not be empty" }
        assert(updatedAtStr.isNotEmpty()) { "updatedAt should not be empty" }

        assertTrue(updatedAt.minusSeconds(createdAt.second.toLong()).second < 1)
    }

    @Test
    fun `create note - title null - failed create`() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/note").header("user-id", testUser.userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(mapOf("title" to null, "description" to "description")))
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    fun `create note - user not exist - failed create`() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/note").header("user-id", "12345").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(mapOf("title" to "title", "description" to "description")))
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    fun `update note - user not exist - forbidden update`() {
        mockMvc.perform(
            MockMvcRequestBuilders.put("/api/note").header("user-id", "12345").contentType(MediaType.APPLICATION_JSON)
                .content(
                    mapper.writeValueAsString(
                        mapOf(
                            "noteId" to "12345", "owner" to "123", "title" to "title", "description" to "description"
                        )
                    )
                )
        ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    fun `update note - note not exist - forbidden update`() {
        mockMvc.perform(
            MockMvcRequestBuilders.put("/api/note").header("user-id", testUser.userId)
                .contentType(MediaType.APPLICATION_JSON).content(
                    mapper.writeValueAsString(
                        mapOf(
                            "noteId" to "12345", "owner" to "123", "title" to "title", "description" to "description"
                        )
                    )
                )
        ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    fun `update note - valid params - success update`() {
        val note = createNote(testUser.userId)
        val newTitle = note.title + "1"
        val newDescription = note.description + "1"
        mockMvc.perform(
            MockMvcRequestBuilders.put("/api/note").header("user-id", testUser.userId)
                .contentType(MediaType.APPLICATION_JSON).content(
                    mapper.writeValueAsString(
                        mapOf(
                            "note_id" to note.noteId,
                            "owner" to note.owner,
                            "title" to newTitle,
                            "description" to newDescription
                        )
                    )
                )
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.title").value(newTitle)).andExpect(jsonPath("$.description").value(newDescription))
            .andExpect(jsonPath("$.owner").value(note.owner))
            .andExpect(jsonPath("$.created_at", `is`(note.createdAt.format(FORMATTER))))
            .andExpect(jsonPath("$.updated_at", not(`is`(note.updatedAt.format(FORMATTER)))))
    }

    @Test
    fun `update note - owner change not existing owner - failed update`() {
        val note = createNote(testUser.userId)
        val newTitle = note.title + "1"
        val newDescription = note.description + "1"
        val notExistingNewOwner = "12345"
        mockMvc.perform(
            MockMvcRequestBuilders.put("/api/note").header("user-id", testUser.userId)
                .contentType(MediaType.APPLICATION_JSON).content(
                    mapper.writeValueAsString(
                        mapOf(
                            "note_id" to note.noteId,
                            "owner" to notExistingNewOwner,
                            "title" to newTitle,
                            "description" to newDescription
                        )
                    )
                )
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    fun `update note - owner change existing owner - success update`() {
        val note = createNote(testUser.userId)
        val newTitle = note.title + "1"
        val newDescription = note.description + "1"
        val existingNewOwner = createUser()
        mockMvc.perform(
            MockMvcRequestBuilders.put("/api/note").header("user-id", testUser.userId)
                .contentType(MediaType.APPLICATION_JSON).content(
                    mapper.writeValueAsString(
                        mapOf(
                            "note_id" to note.noteId,
                            "owner" to existingNewOwner.userId,
                            "title" to newTitle,
                            "description" to newDescription
                        )
                    )
                )
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.note_id").value(note.noteId)).andExpect(jsonPath("$.title").value(newTitle))
            .andExpect(jsonPath("$.description").value(newDescription))
            .andExpect(jsonPath("$.owner").value(existingNewOwner.userId))
            .andExpect(jsonPath("$.created_at", `is`(note.createdAt.format(FORMATTER))))
            .andExpect(jsonPath("$.updated_at", not(`is`(note.updatedAt.format(FORMATTER)))))

        val notes = getNoteById(existingNewOwner.userId, note.noteId)

        assertEquals(note.noteId, notes.noteId)
        assertEquals(newTitle, notes.title)
        assertEquals(newDescription, notes.description)
        assertEquals(existingNewOwner.userId, notes.owner)
        assertEquals(note.createdAt, notes.createdAt)
        assertNotEquals(note.updatedAt, notes.updatedAt)

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/note/my").header("user-id", testUser.userId)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isEmpty)

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/note/my").header("user-id", existingNewOwner.userId)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isNotEmpty)
    }

    @Test
    fun `update note - executor change existing owner - forbidden update`() {
        val note = createNote(testUser.userId)
        val newTitle = note.title + "1"
        val newDescription = note.description + "1"
        val executor = createUser()
        createPermission(testUser.userId, executor.userId, note.noteId, Permission.EXECUTOR)
        val newOwner = createUser()
        mockMvc.perform(
            MockMvcRequestBuilders.put("/api/note").header("user-id", executor.userId)
                .contentType(MediaType.APPLICATION_JSON).content(
                    mapper.writeValueAsString(
                        mapOf(
                            "note_id" to note.noteId,
                            "owner" to newOwner.userId,
                            "title" to newTitle,
                            "description" to newDescription
                        )
                    )
                )
        ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    fun `update note - executor valid update - success update`() {
        val note = createNote(testUser.userId)
        val newTitle = note.title + "1"
        val newDescription = note.description + "1"
        val executor = createUser()
        createPermission(testUser.userId, executor.userId, note.noteId, Permission.EXECUTOR)
        mockMvc.perform(
            MockMvcRequestBuilders.put("/api/note").header("user-id", executor.userId)
                .contentType(MediaType.APPLICATION_JSON).content(
                    mapper.writeValueAsString(
                        mapOf(
                            "note_id" to note.noteId,
                            "owner" to note.owner,
                            "title" to newTitle,
                            "description" to newDescription
                        )
                    )
                )
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.title").value(newTitle)).andExpect(jsonPath("$.description").value(newDescription))
            .andExpect(jsonPath("$.owner").value(note.owner))
            .andExpect(jsonPath("$.created_at", `is`(note.createdAt.format(FORMATTER))))
            .andExpect(jsonPath("$.updated_at", not(`is`(note.updatedAt.format(FORMATTER)))))
    }

    @Test
    fun `update note - less than executor valid update - forbidden update`() {
        val note = createNote(testUser.userId)
        val newTitle = note.title + "1"
        val newDescription = note.description + "1"
        val executor = createUser()
        createPermission(testUser.userId, executor.userId, note.noteId, Permission.WRITER)
        mockMvc.perform(
            MockMvcRequestBuilders.put("/api/note").header("user-id", executor.userId)
                .contentType(MediaType.APPLICATION_JSON).content(
                    mapper.writeValueAsString(
                        mapOf(
                            "note_id" to note.noteId,
                            "owner" to note.owner,
                            "title" to newTitle,
                            "description" to newDescription
                        )
                    )
                )
        ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    fun `delete by note id - owner delete not existing note -forbidden delete`() {
        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/note/1234").header("user-id", testUser.userId)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    fun `delete by note id - owner delete existing note - success delete`() {
        val note = createNote(testUser.userId)
        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/note/${note.noteId}").header("user-id", testUser.userId)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    fun `delete by note id - owner delete twice existing note - forbidden delete`() {
        val note = createNote(testUser.userId)
        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/note/${note.noteId}").header("user-id", testUser.userId)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)

        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/note/${note.noteId}").header("user-id", testUser.userId)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    fun `delete by note id - not owner delete existing note -forbidden delete`() {
        val note = createNote(testUser.userId)
        val newUser = createUser()
        createPermission(testUser.userId, newUser.userId, note.noteId, Permission.WRITER)
        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/note/${note.noteId}").header("user-id", newUser.userId)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    fun `delete by note id - not existing owner delete existing note -forbidden delete`() {
        val note = createNote(testUser.userId)
        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/note/${note.noteId}").header("user-id", "11234")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    fun `get note by id - note not exist - forbidden get`() {

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/note/1234").header("user-id", testUser.userId)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isForbidden)

    }

    @Test
    fun `get note by id - user not exist - forbidden get`() {
        val note = createNote(testUser.userId)
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/note/${note.noteId}").header("user-id", "1234")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isForbidden)

    }

    @Test
    fun `get note by id - valid input - success get`() {
        val note = createNote(testUser.userId)

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/note/${note.noteId}").header("user-id", testUser.userId)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.owner").value(note.owner))
            .andExpect(jsonPath("$.note_id").value(note.noteId))
            .andExpect(jsonPath("$.title").value(note.title))
            .andExpect(jsonPath("$.description").value(note.description))
            .andExpect(jsonPath("$.created_at", `is`(note.createdAt.format(FORMATTER))))
            .andExpect(jsonPath("$.updated_at", `is`(note.updatedAt.format(FORMATTER))))
    }

    @Test
    fun `get notes by owner - no notes - success get`() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/note/my").header("user-id", testUser.userId)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isEmpty)
    }

    @Test
    fun `get notes by owner - 1 note - success get`() {
        val note = createNote(testUser.userId)
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/note/my").header("user-id", testUser.userId)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isNotEmpty).andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("[0].owner").value(note.owner))
            .andExpect(jsonPath("[0].note_id").value(note.noteId))
            .andExpect(jsonPath("[0].title").value(note.title))
            .andExpect(jsonPath("[0].description").value(note.description))
            .andExpect(jsonPath("[0].created_at", `is`(note.createdAt.format(FORMATTER))))
            .andExpect(jsonPath("[0].updated_at", `is`(note.updatedAt.format(FORMATTER))))
    }

    @Test
    fun `get notes by owner - not exist owner - not found get`() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/note/my").header("user-id", "1234").contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
    }

}