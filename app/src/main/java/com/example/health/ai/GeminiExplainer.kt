package com.example.health.ai

import android.content.Context
import com.example.BuildConfig
import com.example.health.model.*
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @field:Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @field:Json(name = "parts") val parts: List<GeminiPart>,
    @field:Json(name = "role") val role: String? = "user"
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @field:Json(name = "contents") val contents: List<GeminiContent>,
    @field:Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null
)

class GeminiExplainer(private val context: Context) {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    companion object {
        private const val MODEL_NAME = "gemini-3.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"
        const val MANDATORY_DISCLAIMER = "Bu bilgiler tıbbi tavsiye yerine geçmez, endişelerin varsa doktoruna danış."
    }

    /**
     * Retrieves API key from SharedPreferences (user custom) or BuildConfig.
     */
    fun getEffectiveApiKey(): String {
        val prefs = context.getSharedPreferences("health_ai_prefs", Context.MODE_PRIVATE)
        val customKey = prefs.getString("custom_gemini_api_key", null)?.trim()
        if (!customKey.isNullOrBlank()) {
            return customKey
        }
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    fun saveCustomApiKey(key: String) {
        val prefs = context.getSharedPreferences("health_ai_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("custom_gemini_api_key", key.trim()).apply()
    }

    /**
     * Generates a natural language explanation for a single metric based strictly on Rule Engine output.
     */
    suspend fun explainMetric(
        metricEvaluation: MetricEvaluation,
        date: String
    ): AiGeneratedInsight = withContext(Dispatchers.IO) {
        val criticalWarning = if (metricEvaluation.isCritical) {
            metricEvaluation.criticalAlertMessage ?: "Kritik değer tespit edildi. Lütfen derhal bir hekime veya sağlık kuruluşuna başvurun."
        } else null

        val structuredJson = """
        {
          "metric": "${metricEvaluation.metricType.displayName}",
          "value": "${metricEvaluation.formattedValue}",
          "category": "${metricEvaluation.category.name} (${metricEvaluation.category.label})",
          "statusLevel": "${metricEvaluation.statusLevel.tag}",
          "normalReferenceRange": "${metricEvaluation.normalRange}",
          "baselineComparison": "${metricEvaluation.differenceFromBaseline}",
          "clinicalRuleSummary": "${metricEvaluation.clinicalSummary}",
          "isCritical": ${metricEvaluation.isCritical}
        }
        """.trimIndent()

        val prompt = """
        Kullanıcının ${metricEvaluation.metricType.displayName} verisi Kural Motoru tarafından analiz edilip şu kategorilere ayrılmıştır:
        
        ```json
        $structuredJson
        ```
        
        Lütfen bu kural motoru sonucunu kullanıcıya sıcak, samimi, anlaşılır, Türkçe günlük dille 2-3 cümlede açıkla ve 1 adet uygulanabilir pratik öneri ver.
        Formatın şu şekilde olsun:
        AÇIKLAMA: [2-3 cümlelik samimi özet]
        ÖNERİ: [1 adet net pratik yaşam önerisi]
        """.trimIndent()

        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext generateOfflineRuleMetricFallback(metricEvaluation, date, criticalWarning)
        }

        try {
            val responseText = callGeminiApi(apiKey, prompt)
            parseAiResponse(responseText, date, metricEvaluation.metricType, criticalWarning)
        } catch (e: Exception) {
            generateOfflineRuleMetricFallback(metricEvaluation, date, criticalWarning)
        }
    }

    /**
     * Generates a comprehensive full-day health synthesis for all categorized metrics.
     */
    suspend fun explainFullDay(
        summary: StructuredHealthSummary
    ): AiGeneratedInsight = withContext(Dispatchers.IO) {
        val criticalWarning = if (summary.hasCriticalConditions && summary.criticalAlerts.isNotEmpty()) {
            "⚠️ ACİL SAĞLIK UYARISI:\n" + summary.criticalAlerts.joinToString("\n• ", prefix = "• ") +
                    "\nLütfen vakit kaybetmeden bir sağlık uzmanına danışınız."
        } else null

        val structuredJson = summary.toSanitizedLlmJson()

        val prompt = """
        Kullanıcının günün tamamına ait sağlık verileri Kural Motoru tarafından filtrelenmiş ve kategorize edilmiştir:
        
        ```json
        $structuredJson
        ```
        
        Görevin: Bu kural motoru çıktılarını sentezleyerek kullanıcıya gününün genel sağlık ve toparlanma durumunu (uyku, adım, kalp hızı, stres, oksijen) sıcak, motive edici, doğal bir dille 3-4 cümlede açıklamak ve 1-2 pratik günlük yaşam tavsiyesi vermektir.
        
        Formatın kesinlikle şu olsun:
        AÇIKLAMA: [3-4 cümlelik genel gün değerlendirmesi]
        ÖNERİ: [Güne özel pratik beslenme, hareket veya dinlenme tavsiyesi]
        """.trimIndent()

        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext generateOfflineRuleFullDayFallback(summary, criticalWarning)
        }

        try {
            val responseText = callGeminiApi(apiKey, prompt)
            parseAiResponse(responseText, summary.date, null, criticalWarning)
        } catch (e: Exception) {
            generateOfflineRuleFullDayFallback(summary, criticalWarning)
        }
    }

    private fun callGeminiApi(apiKey: String, userPrompt: String): String {
        val systemInstructionText = """
        Sen bir Samsung Health sağlık verisi çevirmenisin. Kesinlikle doktor değilsin ve asla tıbbi teşhis koymazsın.
        Girdi olarak SADECE kural motorunun ürettiği kategorize edilmiş JSON özetini alırsın. Ham telemetri verilerinden bağımsız eşik üretme; yalnızca sana verilen kural motoru kategorilerini (İDEAL, DÜŞÜK, DİKKAT, KRİTİK vb.) kullanıcıya sıcak ve anlaşılır günlük Türkçe ile aktar.
        Kritik uyarılarda doğrudan hekim yönlendirmesini vurgula. Asla reçete veya ilaç tavsiye etme.
        """.trimIndent()

        val url = "$BASE_URL?key=$apiKey"

        val jsonBody = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", systemInstructionText)))
            })
            put("contents", JSONArray().put(
                JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", userPrompt)))
                }
            ))
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.4)
                put("topP", 0.9)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val response = okHttpClient.newCall(request).execute()
        val responseBodyStr = response.body?.string() ?: throw IllegalStateException("Boş yanıt alındı")

        if (!response.isSuccessful) {
            throw IllegalStateException("API Hatası (${response.code}): $responseBodyStr")
        }

        val rootJson = JSONObject(responseBodyStr)
        val candidates = rootJson.optJSONArray("candidates")
        val firstCandidate = candidates?.optJSONObject(0)
        val content = firstCandidate?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val text = parts?.optJSONObject(0)?.optString("text")

        return text ?: "Açıklama üretilemedi."
    }

    private fun parseAiResponse(
        rawAiText: String,
        date: String,
        metricType: MetricType?,
        criticalWarning: String?
    ): AiGeneratedInsight {
        var explanation = rawAiText.trim()
        var tip = "Günlük rutinine dikkat ederek dengeli su tüketmeye ve aktif kalmaya özen göster."

        if (rawAiText.contains("AÇIKLAMA:") && rawAiText.contains("ÖNERİ:")) {
            val parts = rawAiText.split("ÖNERİ:")
            val expPart = parts[0].replace("AÇIKLAMA:", "").trim()
            val tipPart = parts.getOrNull(1)?.trim() ?: ""
            if (expPart.isNotBlank()) explanation = expPart
            if (tipPart.isNotBlank()) tip = tipPart
        } else if (rawAiText.contains("Öneri:") || rawAiText.contains("Tavsiye:")) {
            val delimiter = if (rawAiText.contains("Öneri:")) "Öneri:" else "Tavsiye:"
            val parts = rawAiText.split(delimiter)
            val expPart = parts[0].replace("Açıklama:", "").trim()
            val tipPart = parts.getOrNull(1)?.trim() ?: ""
            if (expPart.isNotBlank()) explanation = expPart
            if (tipPart.isNotBlank()) tip = tipPart
        }

        return AiGeneratedInsight(
            date = date,
            timestamp = System.currentTimeMillis(),
            metricType = metricType,
            explanationText = explanation,
            practicalTip = tip,
            disclaimer = MANDATORY_DISCLAIMER,
            criticalMedicalWarning = criticalWarning
        )
    }

    private fun generateOfflineRuleMetricFallback(
        eval: MetricEvaluation,
        date: String,
        criticalWarning: String?
    ): AiGeneratedInsight {
        val (explanation, tip) = when (eval.metricType) {
            MetricType.STEPS -> when (eval.category) {
                HealthCategory.ACHIEVED, HealthCategory.EXCELLENT ->
                    "Bugün ${eval.formattedValue} ile hareket hedefine ulaştın. Kardiyovasküler sistemin için harika bir gün oldu!" to
                            "Vücudunu esnetmek ve toparlanmayı hızlandırmak için yatmadan önce 5 dakika hafif esneme yapabilirsin."
                HealthCategory.MODERATE ->
                    "Bugün ${eval.formattedValue} attın, hedefine oldukça yakınsın ancak biraz daha hareket eklenebilir." to
                            "Akşam yemeğinden sonra 15 dakikalık kısa ve tempolu bir yürüyüş hedefini tamamlamanı sağlayacaktır."
                else ->
                    "Bugün ${eval.formattedValue} ile sedanter (hareketsiz) bir gün geçirdin. Kas ve dolaşım sağlığı için hareket önemlidir." to
                            "Saat başı masadan kalkıp 2 dakika yürümek ve merdivenleri tercih etmek gününü canlandırır."
            }
            MetricType.HEART_RATE -> when (eval.category) {
                HealthCategory.CRITICAL_LOW ->
                    "Dinlenik nabzın ${eval.formattedValue} ölçüldü. Bu değer standart aralığın çok altındadır." to
                            "Baş dönmesi veya halsizlik eşlik ediyorsa lütfen dinlenin ve hekiminize danışın."
                HealthCategory.LOW ->
                    "Dinlenik nabzın ${eval.formattedValue} ile düşük seviyede. Düzenli spor yapanlarda bu kalp kasının güçlü olduğunu gösterir." to
                            "Kendini zinde hissediyorsan mevcut egzersiz düzenini koruyabilirsin."
                HealthCategory.NORMAL ->
                    "Dinlenik kalp atış hızın ${eval.formattedValue} ile tamamen ideal ve sağlıklı referans aralığındadır." to
                            "Kalp sağlığını desteklemek için günlük yeterli su tüketimini ihmal etme."
                HealthCategory.HIGH ->
                    "Dinlenik nabzın ${eval.formattedValue} ile normalin üzerinde seyretti. Kafein, uykusuzluk veya stres yükselmiş olabilir." to
                            "Bugün kafein tüketimini sınırla ve 10 dakikalık yavaş nefes egzersizi uygula."
                else ->
                    "Dinlenik nabzın ${eval.formattedValue} ile yüksek risk sınırındadır." to
                            "Ağır fiziksel aktiviteden kaçının ve doktorunuza başvurun."
            }
            MetricType.SLEEP -> when (eval.category) {
                HealthCategory.OPTIMAL ->
                    "Bu gece ${eval.formattedValue} uyudun; biyolojik toparlanma ve bağışıklık döngün için ideal bir süre." to
                            "Güne enerjik başlamak için uyandıktan sonra bir bardak ılık su içebilirsin."
                HealthCategory.BELOW_AVERAGE, HealthCategory.VERY_LOW ->
                    "Bu gece ${eval.formattedValue} uyudun, bu kişisel ortalamanın altında. Bugün zihinsel ve fiziksel yorgunluk hissedebilirsin." to
                            "Öğleden sonra 15-20 dakikalık kısa bir şekerleme yapmak ve akşam erken yatmak toparlanmana yardımcı olur."
                else ->
                    "Bu gece ${eval.formattedValue} uyudun. Vücudun dinlenme sürecini tamamladı." to
                            "Sabah hafif esneme hareketleriyle güne başlayabilirsin."
            }
            MetricType.SPO2 -> when (eval.category) {
                HealthCategory.CRITICAL ->
                    "Kandaki oksijen satürasyonu ${eval.formattedValue} ölçüldü. Bu seviye klinik olarak dikkat gerektiren bir durumdur." to
                            "Nefes darlığı varsa derhal bir sağlık kuruluşuna müracaat ediniz."
                HealthCategory.ATTENTION ->
                    "Oksijen satürasyonun ${eval.formattedValue} ile sınırda. Ortam havası veya yüzeysel nefes alma etkili olmuş olabilir." to
                            "Bulunduğun odayı havalandır ve 5 dakika boyunca burnundan derin nefes alıp ağzından yavaşça ver."
                else ->
                    "Oksijen doygunluğun ${eval.formattedValue} ile mükemmel seviyede. Hücresel oksijenlenme son derece sağlıklı." to
                            "Açık havada yapacağın yürüyüşler bu seviyeyi korumanı destekler."
            }
            MetricType.STRESS -> when (eval.category) {
                HealthCategory.REST, HealthCategory.LOW ->
                    "Stres skorun ${eval.formattedValue} ile düşük/dinlenme seviyesinde. Otonom sinir sistemin sakin ve dengeli." to
                            "Bu sakin zihin durumunu korumak için sevdiklerinle vakit geçirebilir veya kitap okuyabilirsin."
                HealthCategory.MEDIUM ->
                    "Stres skorun ${eval.formattedValue} ile orta düzeyde. Günün getirdiği yoğunluk bedenine hafif yansımış." to
                            "Çalışma aralarında 5 dakikalık göz dinlendirme ve derin diyafram nefesi molası ver."
                else ->
                    "Stres skorun ${eval.formattedValue} ile yüksek seviyede tespit edildi. Vücudun sempatik uyarılma modunda." to
                            "Şimdi tüm işleri bırakıp 10 dakika sessiz bir ortamda 4-7-8 nefes tekniğini uygula ve ılık bir duş al."
            }
            MetricType.CALORIES -> when (eval.category) {
                HealthCategory.ACHIEVED, HealthCategory.EXCELLENT ->
                    "Bugün ${eval.formattedValue} aktif kalori yaktın ve günlük enerji tüketim hedefine ulaştın." to
                            "Kaybettiğin mineralleri geri kazanmak için elektrolit ve protein dengeli bir öğün tercih et."
                else ->
                    "Bugün ${eval.formattedValue} yaktın, hedefin biraz altındasın." to
                            "Kısa bir akşam yürüyüşü veya hafif egzersiz ile enerji harcamanı artırabilirsin."
            }
        }

        return AiGeneratedInsight(
            date = date,
            timestamp = System.currentTimeMillis(),
            metricType = eval.metricType,
            explanationText = explanation,
            practicalTip = tip,
            disclaimer = MANDATORY_DISCLAIMER,
            criticalMedicalWarning = criticalWarning
        )
    }

    private fun generateOfflineRuleFullDayFallback(
        summary: StructuredHealthSummary,
        criticalWarning: String?
    ): AiGeneratedInsight {
        val sleepEval = summary.evaluations[MetricType.SLEEP]
        val stepsEval = summary.evaluations[MetricType.STEPS]
        val hrEval = summary.evaluations[MetricType.HEART_RATE]
        val stressEval = summary.evaluations[MetricType.STRESS]

        val exp = buildString {
            append("Bugün genel sağlık hazırlık puanın %${summary.overallScore} (${summary.overallStatus.tag}). ")
            if (sleepEval != null) {
                if (sleepEval.statusLevel == HealthStatusLevel.OPTIMAL) {
                    append("Geceki ${sleepEval.formattedValue} kaliteli uykun güne zinde başlamanı sağladı. ")
                } else {
                    append("${sleepEval.formattedValue} uyku süren ortalamanın altında kaldı; gün içinde enerji düşüşleri yaşayabilirsin. ")
                }
            }
            if (stepsEval != null) {
                if (stepsEval.category == HealthCategory.ACHIEVED || stepsEval.category == HealthCategory.EXCELLENT) {
                    append("${stepsEval.formattedValue} ile kardiyo hedefini başarıyla tamamladın. ")
                } else {
                    append("Hareket seviyen ${stepsEval.formattedValue} ile hedefin gerisinde kaldı. ")
                }
            }
            if (stressEval != null && stressEval.statusLevel == HealthStatusLevel.CRITICAL) {
                append("Stres düzeyinin yüksek seyrettiği görüldü, toparlanmaya odaklanmalısın.")
            } else if (hrEval != null && hrEval.statusLevel == HealthStatusLevel.GOOD) {
                append("Dinlenik nabzın dengeli ve sağlıklı aralıkta.")
            }
        }

        val tip = if (summary.overallScore < 60) {
            "Bugün bedenini fazla zorlamadan erken saatte uyumaya, kafein tüketimini kesmeye ve bol su içmeye özen göster."
        } else {
            "Harika bir gün! Bu dengeli biyolojik ritmini korumak için akşam hafif bir yürüyüş ve kaliteli bir uyku saati planla."
        }

        return AiGeneratedInsight(
            date = summary.date,
            timestamp = System.currentTimeMillis(),
            metricType = null,
            explanationText = exp,
            practicalTip = tip,
            disclaimer = MANDATORY_DISCLAIMER,
            criticalMedicalWarning = criticalWarning
        )
    }

    /**
     * Interactive conversational AI Health Coach grounded on the day's Rule Engine evaluations.
     */
    suspend fun chatWithHealthCoach(
        userMessage: String,
        summary: StructuredHealthSummary,
        conversationHistory: List<ChatMessage>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext generateOfflineChatFallback(userMessage, summary)
        }

        val structuredJson = summary.toSanitizedLlmJson()
        val historySnippet = conversationHistory.takeLast(4).joinToString("\n") { msg ->
            "${if (msg.isUser) "Kullanıcı" else "AI Koç"}: ${msg.text}"
        }

        val prompt = """
        GÜNCEL KURAL MOTORU SAĞLIK VERİLERİ:
        ```json
        $structuredJson
        ```

        GEÇMİŞ SOHBET:
        $historySnippet

        KULLANICI SORUSU:
        "$userMessage"

        GÖREV:
        Kullanıcının sorusunu yukarıdaki kural motoru verilerini referans alarak sıcak, samimi, net ve motive edici bir dille cevapla (Maksimum 2-3 kısa paragraf).
        - Teşhis/ilaç önerisi ASLA yapma.
        - Pratik, uygulanabilir günlük yaşam veya spor/beslenme/uyku önerisi sun.
        - Yanıtının sonuna gerekirse kısa bir emoji veya cesaretlendirici bir cümle ekle.
        """.trimIndent()

        try {
            callGeminiApi(apiKey, prompt)
        } catch (e: Exception) {
            generateOfflineChatFallback(userMessage, summary)
        }
    }

    private fun generateOfflineChatFallback(userMessage: String, summary: StructuredHealthSummary): String {
        val lower = userMessage.lowercase()
        val hr = summary.evaluations[MetricType.HEART_RATE]
        val sleep = summary.evaluations[MetricType.SLEEP]
        val steps = summary.evaluations[MetricType.STEPS]
        val stress = summary.evaluations[MetricType.STRESS]
        val spo2 = summary.evaluations[MetricType.SPO2]

        return when {
            lower.contains("spor") || lower.contains("antrenman") || lower.contains("koşu") -> {
                if (summary.overallScore >= 70 && stress?.statusLevel != HealthStatusLevel.CRITICAL) {
                    "Bugünkü hazırlık puanın %${summary.overallScore} ile gayet iyi düzeyde! 🏃‍♂️ Kardiyo veya kuvvet antrenmanı yapabilirsin. Vücudunu dinlemeyi ve antrenman sonrasında yeterli su ve protein almayı unutma."
                } else {
                    "Bugün uyku veya stres verilerine göre vücudun toparlanma modunda (%${summary.overallScore} hazırlık). 🧘 Ağır ağırlık veya şiddetli kardiyo yerine tempolu bir yürüyüş veya hafif yoga/esneme tercih etmeni öneririm."
                }
            }
            lower.contains("uyku") || lower.contains("yorgun") || lower.contains("derin") -> {
                "Geceki uyku süren ${sleep?.formattedValue ?: "ölçülmedi"}. 🌙 Kaliteli derin uyku için yatmadan 1 saat önce telefon ekranını bırakmak, yatak odasını serin (18-19°C) tutmak ve akşam saatlerinde kafeini kesmek çok etkilidir."
            }
            lower.contains("nabız") || lower.contains("kalp") -> {
                "Dinlenik nabzın ${hr?.formattedValue ?: "ölçülmedi"} (${hr?.category?.label ?: "Normal"}). 🫀 Dinlenik nabzını düşürmek ve kalp kasını güçlendirmek için haftada 3-4 gün düzenli orta tempolu yürüyüş ve kaliteli uyku şarttır."
            }
            lower.contains("stres") || lower.contains("rahatla") || lower.contains("nefes") -> {
                "Güncel stres düzeyin ${stress?.formattedValue ?: "ölçülmedi"}/100. ⚡ Stresini hızla düşürmek için 'Nefes Egzersizi' sekmemizdeki 4-7-8 veya Kutu Nefesi tekniğini 3 dakika uygulayabilirsin; vagus sinirini anında uyarır."
            }
            lower.contains("oksijen") || lower.contains("spo2") -> {
                "Kandaki oksijen satürasyonun ${spo2?.formattedValue ?: "%98"}. 🫁 İdeal aralık %95-100'dür. Odanı sık sık havalandırmak ve derin diyafram nefesi almak akciğer kapasiteni canlı tutar."
            }
            else -> {
                "Bugünkü genel sağlık özetine göre hazırlık puanın %${summary.overallScore} (${summary.overallStatus.tag}). 🌟 Adım, uyku ve nabız dengen kayıt altında. Sağlıklı beslenip bol su içerek gününü harika geçirebilirsin! Başka bir sorun varsa memnuniyetle yanıtlarım."
            }
        }
    }
}
