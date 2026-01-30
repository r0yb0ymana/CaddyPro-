package caddypro.domain.navcaddy.llm

import com.google.gson.annotations.SerializedName

/**
 * Response from the LLM classification service.
 *
 * Contains both the parsed classification result and metadata
 * for observability and performance monitoring.
 *
 * Spec reference: navcaddy-engine.md R2, R8
 *
 * @property rawResponse Unparsed JSON response from LLM
 * @property latencyMs Time taken for classification in milliseconds
 * @property modelName Model identifier (e.g., "gemini-3-flash")
 * @property tokenCount Total tokens used (input + output)
 * @property timestamp Response timestamp in milliseconds since epoch
 */
data class LLMResponse(
    @SerializedName("raw_response")
    val rawResponse: String,

    @SerializedName("latency_ms")
    val latencyMs: Long,

    @SerializedName("model_name")
    val modelName: String,

    @SerializedName("token_count")
    val tokenCount: Int? = null,

    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
) {
    init {
        require(latencyMs >= 0) { "Latency must be non-negative" }
        tokenCount?.let {
            require(it >= 0) { "Token count must be non-negative" }
        }
    }
}
