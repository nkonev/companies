package name.nkonev.examples.companies

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CompaniesApplication

fun main(args: Array<String>) {
	runApplication<CompaniesApplication>(*args)
}
