package com.gabriel.jogatina.services

import java.time.Instant

/** Período [from, to) sobre a data da partida; um limite nulo fica aberto. */
data class Period(val from: Instant? = null, val to: Instant? = null) {
    init {
        if (from != null && to != null) {
            require(from.isBefore(to)) { "'from' precisa ser anterior a 'to'" }
        }
    }
}
