package org.example.notion.permission

import org.example.notion.app.exceptions.ForbiddenException
import org.example.notion.app.note.NoteService
import org.example.notion.app.team.dto.TeamDto
import org.example.notion.app.team.service.TeamService
import org.example.notion.app.teamUser.TeamUserService
import org.example.notion.app.teamUser.dto.TeamUserMyResponseDto
import org.example.notion.app.user.UserService
import org.example.notion.app.userPermission.TeamPermissionService
import org.example.notion.app.userPermission.UserPermissionService
import org.example.notion.app.userPermission.entity.Permission
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PermissionService(
    private val userService: UserService,
    @Lazy
    private val noteService: NoteService,
    @Lazy
    private val userPermissionService: UserPermissionService,
    @Lazy
    private val teamService: TeamService,
    @Lazy
    private val teamUserService: TeamUserService,
    @Lazy
    private val teamPermissionService: TeamPermissionService
) {
    fun requireUserPermission(noteId: Long, permission: Permission) {
        requireUserPermission(userService.getCurrentUser(), noteId, permission)
    }

    @Transactional
    fun requireUserPermission(userId: Long, noteId: Long, permission: Permission) {
        if (userPermissionService.isHasUserPermission(userId, noteId, permission) ||
            isHasRoleInTeam(userId, noteId, permission)
        ) {
            return
        }
        throw ForbiddenException("Current user doesn't have permission $permission for note $noteId")
    }

    @Transactional
    fun isHasRoleInTeam(
        userId: Long,
        noteId: Long,
        permission: Permission
    ): Boolean {
        val myTeams:List<Long> = (teamService.getTeamsByUser(userId).map { it.teamId }.toList() + teamUserService.findByUserId(userId).map { it.teamId }.toList())
        return teamPermissionService.findByNoteIdUnsafe(noteId).filter { t -> t.permission == permission }
            .any { team -> myTeams.contains(team.teamId) }
    }

    fun requireOwnerPermission(userId: Long, noteId: Long) {
        if (!noteService.isOwner(noteId, userId))
            throw ForbiddenException("Forbidden permission for user $userId must be owner of $noteId")
    }

    fun requireOwnerPermission(noteId: Long) {
        requireOwnerPermission(userService.getCurrentUser(), noteId)
    }
}