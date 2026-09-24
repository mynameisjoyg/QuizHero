package com.joyg.quizhero

import com.google.gson.annotations.SerializedName

// 1. 發送配對請求
data class MatchRequest(
    @SerializedName("playerId") val playerId: String,
    @SerializedName("subject") val subject: String,
    @SerializedName("volume") val volume: String,
    @SerializedName("chapter") val chapter: String

)

// 2. 配對成功的回應
data class MatchResponse(
    @SerializedName("status") val status: String,
    @SerializedName("roomId") val roomId: String,
    @SerializedName("player1") val player1: String,
    @SerializedName("player2") val player2: String
)

// 3. 題目 Data
data class QuizQuestion(
    @SerializedName("questionId") val questionId: String,
    @SerializedName("title") val title: String,
    @SerializedName("options") val options: List<String>,
    @SerializedName("serverTimestamp") val serverTimestamp: Long
)

// 4. 發送答案 Payload
data class AnswerPayload(
    @SerializedName("playerId") val playerId: String,
    @SerializedName("questionId") val questionId: String,
    @SerializedName("selectedOption") val selectedOption: String,
    @SerializedName("clientTimestamp") val clientTimestamp: Long
)

// 5. 搶答結果 Data
data class BattleResult(
    @SerializedName("type") val type: String,
    @SerializedName("winnerId") val winnerId: String,
    @SerializedName("isCorrect") val isCorrect: Boolean,
    @SerializedName("reactionTimeMs") val reactionTimeMs: Long
)