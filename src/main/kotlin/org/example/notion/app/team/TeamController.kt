package org.example.notion.app.team

import org.example.notion.app.team.dto.TeamCreateDto
import org.example.notion.app.team.dto.TeamDto
import org.example.notion.app.team.service.TeamService
import org.example.notion.app.user.UserContext
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/v1/team")
class TeamController(
    private val teamService: TeamService
) {

    @GetMapping("{teamId}")
    fun getByTeamId(@PathVariable("teamId") teamId: Long,  @RequestHeader("user-id") userId: Long): ResponseEntity<TeamDto> {
        UserContext.setCurrentUser(userId)
        return ResponseEntity.ok().body(teamService.getByTeamId(teamId))
    }

    @GetMapping("my")
    fun getMyTeams(@RequestHeader("user-id") userId: Long): ResponseEntity<List<TeamDto>> {
        UserContext.setCurrentUser(userId)
        return ResponseEntity.ok().body(teamService.getMyTeams())
    }

    @PostMapping
    fun createTeam(
        @Validated @RequestBody teamCreateDto: TeamCreateDto,
        @RequestHeader("user-id") userId: Long
    ): ResponseEntity<TeamDto> {
        UserContext.setCurrentUser(userId)
        return ResponseEntity<TeamDto>(teamService.createTeam(teamCreateDto), HttpStatus.CREATED)
    }

    @PutMapping
    fun updateTeam(
        @Validated @RequestBody teamDto: TeamDto,
        @RequestHeader("user-id") userId: Long
    ): ResponseEntity<TeamDto> {
        UserContext.setCurrentUser(userId)
        return ResponseEntity.ok().body(teamService.update(teamDto))
    }

    @DeleteMapping("{teamId}")
    fun deleteTeam(@PathVariable("teamId") teamId: Long,  @RequestHeader("user-id") userId: Long): ResponseEntity<Void> {
        UserContext.setCurrentUser(userId)
        teamService.deleteByTeamId(teamId)
        return ResponseEntity.ok().build()
    }


}