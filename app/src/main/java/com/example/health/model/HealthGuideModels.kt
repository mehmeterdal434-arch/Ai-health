package com.example.health.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.ui.theme.*

data class HealthTopicGuide(
    val id: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val category: String,
    @DrawableRes val imageRes: Int,
    val themeColor: Color,
    val shortSummary: String,
    val biologicalRole: String,
    val idealRanges: String,
    val tips: List<String>,
    val mythBuster: String,
    val whyItMattersInSamsungHealth: String
)

object HealthKnowledgeBase {
    val topics: List<HealthTopicGuide> = listOf(
        HealthTopicGuide(
            id = "heart_rate",
            title = "Dinlenik Kalp Hızı (RHR)",
            subtitle = "Kardiyovasküler Kondisyonun En Güçlü Aynası",
            emoji = "🫀",
            category = "Kardiyovasküler",
            imageRes = R.drawable.img_cardio_guide_1786913697500,
            themeColor = StatusCriticalRed,
            shortSummary = "Vücudun tamamen istirahatteyken kalbin 1 dakikada attığı sayı. Düşük dinlenik nabız genellikle güçlü ve verimli bir kalp kasına işaret eder.",
            biologicalRole = "Kalp kası güçlendikçe her atımda daha fazla kan pompalar (stroke volume artar). Bu sayede kalbin dinlenirken daha az atması yeterli olur.",
            idealRanges = "• Genel Yetişkinler: 60 - 80 bpm\n• Sporcular / Kondisyonlular: 45 - 60 bpm\n• 100+ bpm: Taşikardi riski (Stres, dehidrasyon veya enfeksiyon)",
            tips = listOf(
                "Düzenli aerobik kardiyo (koşu, yüzme, tempolu yürüyüş) yapın.",
                "Akşamları ağır yemek ve kafein tüketimini kesin.",
                "Yeterli hidrasyon sağlayın; susuzluk nabzı 5-10 bpm yükseltebilir.",
                "Kronik stres ve uykusuzluğu yönetin."
            ),
            mythBuster = "Efsane: 'Düşük nabız her zaman tehlikelidir.' Gerçek: Sporcularda 45-50 bpm dinlenik nabız üstün kondisyon göstergesidir; ancak bayılma/baş dönmesi varsa bradikardi araştırılmalıdır.",
            whyItMattersInSamsungHealth = "Samsung Health ve Galaxy Watch sabah uyanır uyanmaz ilk 5 dakikalık en durağan anı ölçerek gerçek bazal nabzınızı kaydeder."
        ),
        HealthTopicGuide(
            id = "sleep_stages",
            title = "Uyku ve Derin Uyku Oranı",
            subtitle = "Hücresel Yenilenme ve Beyin Detoksu",
            emoji = "🌙",
            category = "Toparlanma",
            imageRes = R.drawable.img_sleep_guide_1786913688015,
            themeColor = StatusLowBlue,
            shortSummary = "Uyku sadece dinlenme değil; bağışıklığın onarıldığı, anıların pekiştirildiği ve toksinlerin beyinden temizlendiği aktif bir biyolojik süreçtir.",
            biologicalRole = "Derin Uyku (N3 Evresi) sırasında büyüme hormonu (HGH) salgılanır, kas dokuları ve organlar yenilenir. REM uykusu ise zihinsel tazelenme ve duygusal dengeyi sağlar.",
            idealRanges = "• Toplam Uyku: 7 - 9 Saat\n• Derin Uyku Oranı: Toplam uykunun %15 - %25'i\n• REM Uykusu Oranı: Toplam uykunun %20 - %25'i",
            tips = listOf(
                "Yatmadan en az 60 dakika önce mavi ışıklı ekranları bırakın.",
                "Yatak odasını 18-20°C arasında serin ve tamamen karanlık tutun.",
                "Hafta sonları dahil her gün aynı saatte uyanmaya özen gösterin.",
                "Yatmadan 4 saat önce alkol ve ağır yemeklerden kaçının."
            ),
            mythBuster = "Efsane: 'Hafta içi uykusuzluğunu hafta sonu çok uyuyarak telafi edebilirsin.' Gerçek: Biyolojik sirkadiyen ritim telafi kabul etmez; 'sosyal jetlag' metabolizmayı yorar.",
            whyItMattersInSamsungHealth = "Samsung Health gelişmiş uyku puanı (Sleep Score), derin/REM/hafif evre dağılımınızı ve gece içi uyanıklık sürenizi analiz eder."
        ),
        HealthTopicGuide(
            id = "blood_oxygen",
            title = "Kandaki Oksijen (SpO2)",
            subtitle = "Hücresel Solunum ve Akciğer Verimliliği",
            emoji = "🫁",
            category = "Solunum & Vital",
            imageRes = R.drawable.img_health_hero_1786913676242,
            themeColor = StatusOptimalTeal,
            shortSummary = "Kırmızı kan hücrelerindeki hemoglobinin ne kadarının oksijen taşıdığını gösteren hayati doygunluk yüzdesidir.",
            biologicalRole = "Hücrelerin ATP (enerji) üretebilmesi için sürekli oksijene ihtiyacı vardır. %95 altı SpO2 dokuların yeterince oksijenlenemediğini gösterir.",
            idealRanges = "• İdeal Sağlıklı Aralık: %95 - %100\n• Hafif Düşük / Takip: %92 - %94\n• Kritik Hipoksemi: <%90 (Acil tıbbi değerlendirme gerektirir)",
            tips = listOf(
                "Derin diyafram nefesi egzersizleri yaparak akciğer kapasitesini artırın.",
                "Sigara ve pasif dumandan uzak durun.",
                "Odanızı düzenli havalandırın.",
                "Gece uykuda sürekli <%92 oluyorsa uyku apnesi testi yaptırın."
            ),
            mythBuster = "Efsane: 'SpO2 %100 olunca süper enerjiye sahip olursun.' Gerçek: %96-99 arası tamamen mükemmeldir; %100 olması ekstra bir güç vermez.",
            whyItMattersInSamsungHealth = "Galaxy Watch sensörleri kırmızı ve kızılötesi LED ışıklarıyla gece boyunca ve anlık olarak kılcal damarlardaki oksijen kırılmasını ölçer."
        ),
        HealthTopicGuide(
            id = "stress_hrv",
            title = "Stres Seviyesi & HRV Dengesi",
            subtitle = "Otonom Sinir Sistemi ve Kortizol Yönetimi",
            emoji = "⚡",
            category = "Zihin & Beden",
            imageRes = R.drawable.img_stress_breath_1786913705829,
            themeColor = StatusAttentionYellow,
            shortSummary = "Kalp atımları arasındaki milisaniyelik değişkenlik (HRV) üzerinden sempatik (savaş-kaç) ve parasempatik (dinlen-sindir) sistem dengesini ölçer.",
            biologicalRole = "Yüksek HRV ve düşük stres skoru, vücudun çevresel baskılara esnek uyum sağladığını ve vagus sinirinin güçlü çalıştığını gösterir.",
            idealRanges = "• Düşük Stres / Dinlenme: 0 - 30 Skor (Optimal Toparlanma)\n• Orta Stres: 31 - 65 Skor (Normal Günlük Yük)\n• Yüksek Stres: 66 - 100 Skor (Aktif Rahatlama Şart)",
            tips = listOf(
                "Günde 5-10 dakika 4-7-8 veya Kutu Nefesi tekniği uygulayın.",
                "Doğada kısa bir yürüyüş kortizol seviyesini %20 düşürebilir.",
                "Gün içinde 90 dakikada bir 2 dakikalık mikro molalar verin.",
                "Magnezyum ve papatya/melisa gibi sakinleştirici bitki çayları deneyin."
            ),
            mythBuster = "Efsane: 'Stres her zaman kötüdür.' Gerçek: Kısa süreli akut stres (eustress) odaklanmayı ve sporda performansı artırır; tehlikeli olan kronik dinlenmeyen strestir.",
            whyItMattersInSamsungHealth = "Samsung Health optik nabız sensöründen HRV (Heart Rate Variability) analizi yaparak stres seviyenizi 0-100 skoruyla gerçek zamanlı izler."
        ),
        HealthTopicGuide(
            id = "daily_steps",
            title = "Günlük Adım & NEAT",
            subtitle = "Sporsuz Günlük Hareketlilik ve Metabolizma",
            emoji = "🚶",
            category = "Aktivite",
            imageRes = R.drawable.img_health_hero_1786913676242,
            themeColor = StatusGoodGreen,
            shortSummary = "NEAT (Non-Exercise Activity Thermogenesis) yani spor salonu dışındaki tüm hareketler, günlük kalori yakımının en büyük değişken kısmını oluşturur.",
            biologicalRole = "Sürekli hareket etmek insülin duyarlılığını artırır, damar içi kan dolaşımını hızlandırır ve lenfatik drenajı destekler.",
            idealRanges = "• Hareketsiz (Sedanter): <5.000 adım\n• Düşük Aktif: 5.000 - 7.499 adım\n• Sağlıklı Hedef: 8.000 - 10.000 adım\n• Çok Aktif: 12.000+ adım",
            tips = listOf(
                "Asansör yerine merdivenleri tercih edin.",
                "Telefon görüşmelerini yürüyerek yapın.",
                "Her 1 saatlik oturmanın ardından 2 dakika ayağa kalkıp gerinin.",
                "Toplu taşımadan 1 durak önce inip kalan mesafeyi yürüyün."
            ),
            mythBuster = "Efsane: 'Günde mutlaka tam 10.000 adım atmak tıbbi bir zorunluluktur.' Gerçek: 10.000 sayısı 1964 Tokyo Olimpiyatları pazarlamasından gelmiştir; araştırmalar 7.500-8.500 adımın maksimum sağlık faydasını sağladığını gösteriyor.",
            whyItMattersInSamsungHealth = "Samsung Health telefonun jiroskop ve ivmeölçer sensörleriyle her adımı sayar ve temponuza göre aktif dakikaları sınıflandırır."
        ),
        HealthTopicGuide(
            id = "active_calories",
            title = "Aktif Kalori ve Enerji Dengesi",
            subtitle = "Metabolik Tüketim ve BMR Ayrımı",
            emoji = "🔥",
            category = "Metabolizma",
            imageRes = R.drawable.img_cardio_guide_1786913697500,
            themeColor = StatusCriticalRed,
            shortSummary = "Bazal metabolizma (BMR - sadece hayatta kalmak için harcanan enerji) haricinde, yürüme, koşma ve egzersizle fazladan yaktığınız net kalori.",
            biologicalRole = "Aktif kalori yakımı mitokondri yoğunluğunu artırır, glikojen depolarını boşaltarak yağ oksidasyonunu hızlandırır.",
            idealRanges = "• Hafif Gün: 250 - 400 kcal\n• Sağlıklı Standart: 450 - 650 kcal\n• Yoğun Spor Günü: 750 - 1200+ kcal",
            tips = listOf(
                "Aralıklı yüksek yoğunluklu kardiyo (HIIT) ile 'afterburn' (EPOC) etkisini tetikleyin.",
                "Kas kütlesini artırarak dinlenik metabolizma hızınızı yükseltin.",
                "Beslenmenizde yeterli protein tüketerek kas yıkımını engelleyin."
            ),
            mythBuster = "Efsane: 'Saatin gösterdiği toplam kalori sadece spordan gelir.' Gerçek: Günlük toplam harcamanızın %65'i sadece nefes alıp uyurken yaktığınız bazal metabolizmadır.",
            whyItMattersInSamsungHealth = "Samsung Health yaş, kilo, boy ve kalp atış hızı verilerini harmanlayarak harcanan aktif kaloriyi hassasiyetle hesaplar."
        )
    )
}
