package com.mehmtcan.brainscroll.telemetry

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Calls `log_events` and `log_crash` of `supabase/migrations/20261009090000_telemetry.sql`. Throws on any failure. */
class SupabaseTelemetryApi(private val client: SupabaseClient) : TelemetryApi {

    override suspend fun sendEvents(info: PlatformInfo, events: List<EventRecord>) {
        val list = JsonArray(
            events.map { record ->
                buildJsonObject {
                    put("name", record.event.wireName)
                    put("props", JsonObject(record.props.mapValues { JsonPrimitive(it.value) }))
                }
            },
        )
        client.pluginManager.getPlugin(Postgrest).rpc(
            "log_events",
            buildJsonObject {
                put("p_platform", info.platform)
                put("p_version", info.appVersion)
                put("p_events", list)
            },
        )
    }

    override suspend fun sendCrash(info: PlatformInfo, crash: CrashReport) {
        client.pluginManager.getPlugin(Postgrest).rpc(
            "log_crash",
            buildJsonObject {
                put("p_platform", info.platform)
                put("p_version", info.appVersion)
                put("p_kind", crash.kind)
                put("p_message", crash.message)
                put("p_stack", crash.stack)
            },
        )
    }
}
