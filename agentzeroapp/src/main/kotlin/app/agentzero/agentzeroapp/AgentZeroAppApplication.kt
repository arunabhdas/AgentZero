package app.agentzero.agentzeroapp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AgentZeroAppApplication

fun main(args: Array<String>) {
	runApplication<AgentZeroAppApplication>(*args)
}
