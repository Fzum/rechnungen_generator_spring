package model

data class Unternehmen(val name: String, val adresse: Adresse, val uid: String, val contact: Contact) {
    override fun toString(): String =
        "$name\n${adresse.strasse}\n${adresse.stadt}\n${adresse.land}\n${contact.telNr}\n${contact.website}"
}

data class Adresse(val strasse: String, val stadt: String, val land: String) {
    override fun toString(): String {
        return "$strasse, $stadt, $land"
    }
}

data class Contact(val telNr: String, val website: String, val email: String)
