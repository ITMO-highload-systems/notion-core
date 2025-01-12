package org.example.notion.app.note

import org.example.notion.app.exceptions.EntityNotFoundException
import org.example.notion.app.note.dto.NoteCreateDto
import org.example.notion.app.note.dto.NoteDto
import org.example.notion.app.note.dto.NoteUpdateDto
import org.example.notion.app.note.mapper.NoteMapper
import org.example.notion.app.paragraph.service.ParagraphService
import org.example.notion.app.user.UserService
import org.example.notion.app.userPermission.UserPermissionService
import org.example.notion.app.userPermission.entity.Permission
import org.example.notion.permission.PermissionService
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NoteService(
    private val noteRepository: NoteRepository,
    private val noteMapper: NoteMapper,
    private val userService: UserService,
    private val permissionService: PermissionService,
    private val paragraphService: ParagraphService,
    @Lazy
    private val userPermissionService: UserPermissionService,
    ) {

    @Transactional
    fun create(noteDto: NoteCreateDto): NoteDto {
        val entity = noteMapper.toEntity(noteDto)

        val save = noteRepository.save(entity)
        return noteMapper.toDto(save)
    }

    @Transactional
    fun getByNoteId(noteId: Long): NoteDto {
        permissionService.requireUserPermission(noteId, Permission.READER)
        val note =
            noteRepository.findByNoteId(noteId) ?: throw EntityNotFoundException("Note with id $noteId not found")

        return noteMapper.toDto(note)
    }


    fun getByOwnerId(): List<NoteDto> {
        val ownerId = userService.getCurrentUser()
        return noteRepository.findByOwner(ownerId).let {
            it.map { el -> noteMapper.toDto(el) }
        }
    }

    @Transactional
    fun deleteByNoteId(noteId: Long) {
        permissionService.requireOwnerPermission(noteId)
        paragraphService.deleteParagraphByNoteId(noteId)
        userPermissionService.deleteByNoteId(noteId)
        noteRepository.deleteByNoteId(noteId)
    }

    @Transactional
    fun update(noteDto: NoteUpdateDto): NoteDto {
        permissionService.requireUserPermission(noteDto.noteId, Permission.EXECUTOR)
        val note = noteRepository.findByNoteId(noteDto.noteId)
            ?: throw EntityNotFoundException("Note with id ${noteDto.noteId} not found")

        if (noteDto.owner != note.owner) {
            permissionService.requireOwnerPermission(noteDto.noteId)
            userService.requireUserExistence(noteDto.owner)
        }

        val newEntity = noteMapper.toEntity(noteDto, note.createdAt)
        val saved = noteRepository.save(newEntity)


        return noteMapper.toDto(saved)
    }

    fun isOwner(noteId: Long, userId: String): Boolean {
        val result = noteRepository.findByNoteId(noteId) ?:
            throw EntityNotFoundException("Note with id $noteId does not exist")

        return result.owner == userId
    }
}

