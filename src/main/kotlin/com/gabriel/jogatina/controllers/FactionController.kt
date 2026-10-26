package com.gabriel.jogatina.controllers

import com.gabriel.jogatina.services.Factions
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/factions")
class FactionController {

    @GetMapping
    fun list(): List<String> = Factions.OFFICIAL
}
