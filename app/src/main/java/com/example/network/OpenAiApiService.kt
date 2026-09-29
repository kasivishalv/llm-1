package com.example.network

import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

data class ChatMessageDto(
    val role: String,
    val content: String,
    val imageBase64: String? = null,
    val reasoningContent: String? = null
)

class ChatMessageDtoAdapter {
    @ToJson
    fun toJson(writer: JsonWriter, msg: ChatMessageDto) {
        writer.beginObject()
        writer.name("role").value(msg.role)
        writer.name("content")
        if (msg.imageBase64.isNullOrBlank()) {
            writer.value(msg.content)
        } else {
            writer.beginArray()
            writer.beginObject()
            writer.name("type").value("text")
            writer.name("text").value(msg.content.ifBlank { "Please inspect and describe this image." })
            writer.endObject()

            writer.beginObject()
            writer.name("type").value("image_url")
            writer.name("image_url")
            writer.beginObject()
            val url = if (msg.imageBase64.startsWith("data:")) msg.imageBase64 else "data:image/jpeg;base64,${msg.imageBase64}"
            writer.name("url").value(url)
            writer.endObject()
            writer.endObject()

            writer.endArray()
        }
        writer.endObject()
    }

    @FromJson
    fun fromJson(reader: JsonReader): ChatMessageDto {
        var role = ""
        var content = ""
        var reasoning: String? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "role" -> {
                    if (reader.peek() == JsonReader.Token.STRING) {
                        role = reader.nextString()
                    } else {
                        reader.skipValue()
                    }
                }
                "reasoning_content", "reasoning" -> {
                    if (reader.peek() == JsonReader.Token.STRING) {
                        reasoning = reader.nextString()
                    } else {
                        reader.skipValue()
                    }
                }
                "content" -> {
                    if (reader.peek() == JsonReader.Token.STRING) {
                        content = reader.nextString()
                    } else if (reader.peek() == JsonReader.Token.BEGIN_ARRAY) {
                        reader.beginArray()
                        val sb = StringBuilder()
                        while (reader.hasNext()) {
                            if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                                reader.beginObject()
                                while (reader.hasNext()) {
                                    val key = reader.nextName()
                                    if (key == "text" && reader.peek() == JsonReader.Token.STRING) {
                                        sb.append(reader.nextString())
                                    } else {
                                        reader.skipValue()
                                    }
                                }
                                reader.endObject()
                            } else {
                                reader.skipValue()
                            }
                        }
                        reader.endArray()
                        content = sb.toString()
                    } else {
                        reader.skipValue()
                    }
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        // If standard content is empty but reasoning content was provided, use reasoning
        val effectiveContent = when {
            content.isNotBlank() -> content
            !reasoning.isNullOrBlank() -> reasoning
            else -> ""
        }

        return ChatMessageDto(
            role = role,
            content = effectiveContent,
            reasoningContent = reasoning
        )
    }
}

@JsonClass(generateAdapter = true)
data class ChatRequestDto(
    val model: String,
    val messages: List<ChatMessageDto>,
    val stream: Boolean = false,
    @param:Json(name = "max_tokens") val maxTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class UsageDto(
    @param:Json(name = "prompt_tokens") val promptTokens: Int? = null,
    @param:Json(name = "completion_tokens") val completionTokens: Int? = null,
    @param:Json(name = "total_tokens") val totalTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class ChatResponseDto(
    val id: String?,
    val choices: List<ChatChoiceDto>?,
    val usage: UsageDto? = null
)

@JsonClass(generateAdapter = true)
data class ChatChoiceDto(
    val index: Int?,
    val message: ChatMessageDto?
)

interface OpenAiApiService {
    @POST
    suspend fun createChatCompletion(
        @Url url: String,
        @Header("Authorization") authorization: String?,
        @Body request: ChatRequestDto,
        @Header("HTTP-Referer") httpReferer: String? = null,
        @Header("X-Title") xTitle: String? = null
    ): ChatResponseDto
}
