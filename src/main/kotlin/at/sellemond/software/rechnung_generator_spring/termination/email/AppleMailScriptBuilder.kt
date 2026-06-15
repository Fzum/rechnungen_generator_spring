package at.sellemond.software.rechnung_generator_spring.termination.email

import java.nio.file.Path

/**
 * Builds the AppleScript that opens a *visible* Apple Mail draft pre-filled with recipient/subject/
 * body and one attachment per file. It deliberately never emits a `send` command — the draft is left
 * for the operator to review and send manually. Pure and unit-tested; the adapter runs it via
 * `osascript`.
 */
object AppleMailScriptBuilder {

    fun build(recipient: String, subject: String, body: String, attachments: List<Path>): String {
        val attachmentLines = attachments.joinToString("\n") { path ->
            "        make new attachment with properties {file name:(POSIX file \"${escape(path.toString())}\")} at after the last paragraph"
        }
        return buildString {
            appendLine("tell application \"Mail\"")
            appendLine(
                "    set newMessage to make new outgoing message with properties " +
                    "{subject:\"${escape(subject)}\", content:\"${escape(body)}\", visible:true}"
            )
            appendLine("    tell newMessage")
            appendLine("        make new to recipient at end of to recipients with properties {address:\"${escape(recipient)}\"}")
            if (attachmentLines.isNotEmpty()) appendLine(attachmentLines)
            appendLine("        delay 1")
            appendLine("    end tell")
            appendLine("    activate")
            append("end tell")
        }
    }

    private fun escape(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")
}
