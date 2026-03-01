package at.sellemond.software.rechnung_generator_spring.util

class RechnungUidGenerator {
    companion object {
        fun getUid(unternehmen: String, year: Int, month: Int) =
            "Rechnung ${unternehmen}-$year/${if (month < 10) "0$month" else month}"
    }
}