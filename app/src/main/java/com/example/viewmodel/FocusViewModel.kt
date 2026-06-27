package com.example.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import java.util.Locale
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.audio.FocusAudioSynthesizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class FocusViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val goalRepository: GoalRepository = GoalRepository(database.goalDao())
    private val sessionDao = database.focusSessionDao()
    private val profileDao = database.userProfileDao()
    private val settingsDao = database.userSettingsDao()
    private val duelDao = database.duelDao()
    private val squadDao = database.squadDao()

    // Goals (already uses Room)
    val uiState: StateFlow<List<Goal>> = goalRepository.allGoals.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // All focus sessions (for heatmap calendar)
    val allSessions: StateFlow<List<FocusSession>> = sessionDao.getAllSessions().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // User profile state
    private val _language = MutableStateFlow("es")
    val language = _language.asStateFlow()

    private val _isUserLoggedIn = MutableStateFlow(false)
    val isUserLoggedIn = _isUserLoggedIn.asStateFlow()

    private val _nickname = MutableStateFlow("FocusWarrior")
    val nickname = _nickname.asStateFlow()

    private val _isRegistered = MutableStateFlow(false)
    val isRegistered = _isRegistered.asStateFlow()

    private val _customAvatarUri = MutableStateFlow<String?>(null)
    val customAvatarUri = _customAvatarUri.asStateFlow()

    private val _loginError = MutableStateFlow(false)
    val loginError = _loginError.asStateFlow()

    private val _avatarIndex = MutableStateFlow(0)
    val avatarIndex = _avatarIndex.asStateFlow()

    private val _gender = MutableStateFlow("neutral")
    val gender = _gender.asStateFlow()

    private val _xp = MutableStateFlow(0)
    val xp = _xp.asStateFlow()

    private val _level = MutableStateFlow(1)
    val level = _level.asStateFlow()

    private val _totalFocusedHours = MutableStateFlow(0f)
    val totalFocusedHours = _totalFocusedHours.asStateFlow()

    private val _totalSessions = MutableStateFlow(0)
    val totalSessions = _totalSessions.asStateFlow()

    private val _currentStreak = MutableStateFlow(0)
    val currentStreak = _currentStreak.asStateFlow()

    private val _quoteStyleStrict = MutableStateFlow(false)
    val quoteStyleStrict = _quoteStyleStrict.asStateFlow()

    private val _interests = MutableStateFlow(listOf("Programación 💻", "Diseño Gráfico 🎨", "Lectura 📚", "Deporte y Salud 🏃"))
    val interests = _interests.asStateFlow()

    // Long-term goals
    data class LongTermGoal(val id: Int, val title: String, val isCompleted: Boolean = false)

    private val _longTermGoals = MutableStateFlow<List<LongTermGoal>>(emptyList())
    val longTermGoals = _longTermGoals.asStateFlow()
    private var nextLtGoalId = 100

    // Settings
    private val _vpnShieldActive = MutableStateFlow(false)
    val vpnShieldActive = _vpnShieldActive.asStateFlow()

    private val _accessibilityLockerActive = MutableStateFlow(false)
    val accessibilityLockerActive = _accessibilityLockerActive.asStateFlow()

    private val _waTimerMinutes = MutableStateFlow(5)
    val waTimerMinutes = _waTimerMinutes.asStateFlow()

    private val _focusSleepEnabled = MutableStateFlow(true)
    val focusSleepEnabled = _focusSleepEnabled.asStateFlow()

    private val _forceSleepSimulation = MutableStateFlow(false)
    val forceSleepSimulation = _forceSleepSimulation.asStateFlow()

    // Duels and Squads (from Room)
    val activeDuels: StateFlow<List<Duel>> = duelDao.getAllDuels().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val punishmentSquads: StateFlow<List<Squad>> = squadDao.getAllSquads().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Analytics data
    private val _weeklyStats = MutableStateFlow<List<WeeklyStats>>(emptyList())
    val weeklyStats = _weeklyStats.asStateFlow()

    private val _sessionAnalytics = MutableStateFlow<List<SessionAnalytics>>(emptyList())
    val sessionAnalytics = _sessionAnalytics.asStateFlow()

    // Achievements tracking
    data class Achievement(
        val code: String,
        val titleEs: String,
        val titleEn: String,
        val descEs: String,
        val descEn: String,
        val reqType: String,
        val reqValue: Int,
        val rewardXp: Int,
        val isUnlocked: Boolean = false
    )

    private val _achievements = MutableStateFlow<List<Achievement>>(emptyList())
    val achievements = _achievements.asStateFlow()

    private var tts: TextToSpeech? = null
    private val _isTtsInitialized = MutableStateFlow(false)
    val isTtsInitialized = _isTtsInitialized.asStateFlow()

    private val synthesizer = FocusAudioSynthesizer()
    private val _currentAmbientSound = MutableStateFlow("none")
    val currentAmbientSound = _currentAmbientSound.asStateFlow()

    fun playAmbientSound(soundType: String) {
        _currentAmbientSound.value = soundType
        synthesizer.start(soundType)
    }

    fun stopAmbientSound() {
        _currentAmbientSound.value = "none"
        synthesizer.stop()
    }

    init {
        loadProfileFromDb()
        loadSettingsFromDb()
        loadAnalytics()
        initAchievements()
        
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                _isTtsInitialized.value = true
            }
        }
    }

    // ================================================================
    // DATABASE LOADING
    // ================================================================

    private fun loadProfileFromDb() {
        viewModelScope.launch {
            val profile = profileDao.getProfileOnce()
            if (profile != null) {
                _language.value = profile.language
                _avatarIndex.value = profile.avatarIndex
                _gender.value = profile.gender
                _xp.value = profile.currentXp
                _level.value = profile.level
                _totalFocusedHours.value = profile.totalFocusedSeconds / 3600f
                _totalSessions.value = profile.totalSessionsCompleted
                _currentStreak.value = profile.currentStreak
                _quoteStyleStrict.value = profile.quoteStyleStrict
                _isUserLoggedIn.value = profile.isLoggedIn
                _isRegistered.value = profile.isRegistered
                _customAvatarUri.value = profile.customAvatarUri
                _nickname.value = profile.nickname

                // Parse interests CSV
                if (profile.interests.isNotBlank()) {
                    _interests.value = profile.interests.split(",").map { it.trim() }.filter { it.isNotBlank() }
                }

                // Parse long-term goals (format: "title|completed,title|completed")
                if (profile.longTermGoals.isNotBlank()) {
                    _longTermGoals.value = profile.longTermGoals.split(";;").mapIndexed { index, entry ->
                        val parts = entry.split("||")
                        LongTermGoal(
                            id = index + 1,
                            title = parts.getOrElse(0) { "" },
                            isCompleted = parts.getOrElse(1) { "false" }.toBoolean()
                        )
                    }.filter { it.title.isNotBlank() }
                    nextLtGoalId = (_longTermGoals.value.maxOfOrNull { it.id } ?: 0) + 1
                }
            } else {
                // First launch: create default profile
                val defaultProfile = UserProfile()
                profileDao.insertProfile(defaultProfile)
                // Set default long-term goals
                _longTermGoals.value = listOf(
                    LongTermGoal(1, "Aprender Jetpack Compose y Animaciones"),
                    LongTermGoal(2, "Dominar la mentalidad estoica y concentración"),
                    LongTermGoal(3, "Desconectarse de redes sociales 4 horas al día")
                )
                nextLtGoalId = 4
                saveLongTermGoals()
            }
        }
    }

    private fun loadSettingsFromDb() {
        viewModelScope.launch {
            val settings = settingsDao.getSettingsOnce()
            if (settings != null) {
                _vpnShieldActive.value = settings.vpnShieldActive
                _accessibilityLockerActive.value = settings.accessibilityLockerActive
                _waTimerMinutes.value = settings.waTimerMinutes
                _focusSleepEnabled.value = settings.focusSleepEnabled
                _forceSleepSimulation.value = settings.forceSleepSimulation
            } else {
                settingsDao.insertSettings(UserSettings())
            }
        }
    }

    fun loadAnalytics() {
        viewModelScope.launch {
            _weeklyStats.value = sessionDao.getWeeklyStats()
            _sessionAnalytics.value = sessionDao.getSessionAnalytics()
        }
    }

    private fun initAchievements() {
        viewModelScope.launch {
            val successCount = sessionDao.getSuccessfulSessionCount()
            val totalSeconds = sessionDao.getTotalFocusedSeconds()
            val totalHours = totalSeconds / 3600.0
            val streak = _currentStreak.value
            val wonDuels = duelDao.getWonDuelsCount()

            _achievements.value = listOf(
                Achievement("F_FIRST_01", "Primer Enfoque", "First Focus",
                    "Completa tu primera sesión de enfoque productivo",
                    "Complete your first productive focus session",
                    "completed_sessions", 1, 100,
                    isUnlocked = successCount >= 1),
                Achievement("F_STU_05", "Estudiante Dedicado", "Dedicated Student",
                    "Completa 5 sesiones de enfoque sin interrupciones",
                    "Complete 5 focus sessions without interruptions",
                    "completed_sessions", 5, 250,
                    isUnlocked = successCount >= 5),
                Achievement("F_GURU_10", "Gurú del Enfoque", "Focus Guru",
                    "Acumula un total de 10 horas exitosas de enfoque",
                    "Accumulate a total of 10 successful focus hours",
                    "accumulated_hours", 10, 1000,
                    isUnlocked = totalHours >= 10),
                Achievement("F_R_HIERRO", "Racha de Hierro", "Iron Streak",
                    "Mantén una racha activa de concentración por 3 días seguidos",
                    "Maintain a focus streak for 3 consecutive days",
                    "streak_days", 3, 500,
                    isUnlocked = streak >= 3),
                Achievement("F_MARATHON", "Maratón Mental", "Mental Marathon",
                    "Acumula 25 horas de enfoque exitoso",
                    "Accumulate 25 hours of successful focus",
                    "accumulated_hours", 25, 2000,
                    isUnlocked = totalHours >= 25),
                Achievement("F_SESSIONS_20", "Imparable", "Unstoppable",
                    "Completa 20 sesiones de enfoque",
                    "Complete 20 focus sessions",
                    "completed_sessions", 20, 1500,
                    isUnlocked = successCount >= 20),
                Achievement("F_DUEL_WIN", "Gladiador", "Gladiator",
                    "Gana tu primer duelo 1v1",
                    "Win your first 1v1 duel",
                    "won_duels", 1, 300,
                    isUnlocked = wonDuels >= 1),
                Achievement("F_STREAK_7", "Semana Sagrada", "Sacred Week",
                    "Mantén una racha de enfoque por 7 días",
                    "Maintain a focus streak for 7 days",
                    "streak_days", 7, 1000,
                    isUnlocked = streak >= 7)
            )
        }
    }

    // ================================================================
    // PROFILE ACTIONS (all persist to Room)
    // ================================================================

    fun toggleLanguage() {
        val newLang = if (_language.value == "es") "en" else "es"
        _language.value = newLang
        viewModelScope.launch { profileDao.updateLanguage(newLang) }
    }

    fun setAvatar(index: Int) {
        _avatarIndex.value = index
        viewModelScope.launch { profileDao.updateAvatar(index) }
    }

    fun setGender(newGender: String) {
        _gender.value = newGender
        viewModelScope.launch { profileDao.updateGender(newGender) }
    }

    fun toggleQuoteStyle() {
        val newVal = !_quoteStyleStrict.value
        _quoteStyleStrict.value = newVal
        viewModelScope.launch { profileDao.updateQuoteStyle(newVal) }
    }

    fun addXp(amount: Int) {
        val newXp = (_xp.value + amount).coerceAtLeast(0)
        _xp.value = newXp
        val calculatedLevel = 1 + (newXp / 100)
        _level.value = calculatedLevel.coerceAtLeast(1)
        viewModelScope.launch {
            profileDao.updateXpAndLevel(_xp.value, _level.value)
            // Check if new achievements unlocked
            initAchievements()
        }
    }

    // Interests
    fun toggleInterest(interest: String) {
        val current = _interests.value
        _interests.value = if (current.contains(interest)) {
            current.filter { it != interest }
        } else {
            current + interest
        }
        saveInterests()
    }

    fun addCustomInterest(interest: String) {
        if (interest.isNotBlank() && !_interests.value.contains(interest)) {
            _interests.value = _interests.value + interest
            saveInterests()
        }
    }

    private fun saveInterests() {
        viewModelScope.launch {
            profileDao.updateInterests(_interests.value.joinToString(","))
        }
    }

    // Long-term goals
    fun addLongTermGoal(title: String) {
        if (title.isNotBlank()) {
            _longTermGoals.value = _longTermGoals.value + LongTermGoal(nextLtGoalId++, title)
            saveLongTermGoals()
        }
    }

    fun toggleLongTermGoal(id: Int) {
        _longTermGoals.value = _longTermGoals.value.map {
            if (it.id == id) it.copy(isCompleted = !it.isCompleted) else it
        }
        saveLongTermGoals()
    }

    fun deleteLongTermGoal(id: Int) {
        _longTermGoals.value = _longTermGoals.value.filter { it.id != id }
        saveLongTermGoals()
    }

    private fun saveLongTermGoals() {
        viewModelScope.launch {
            val serialized = _longTermGoals.value.joinToString(";;") { "${it.title}||${it.isCompleted}" }
            val profile = profileDao.getProfileOnce()
            if (profile != null) {
                profileDao.updateProfile(profile.copy(longTermGoals = serialized))
            }
        }
    }

    // ================================================================
    // AUTH ACTIONS
    // ================================================================

    fun registerUser(
        nickname: String,
        pin: String,
        gender: String,
        avatarIndex: Int,
        customAvatarUri: String?,
        interests: List<String>
    ) {
        viewModelScope.launch {
            val profile = profileDao.getProfileOnce() ?: UserProfile()
            val updatedProfile = profile.copy(
                nickname = nickname,
                pinHash = pin,
                gender = gender,
                avatarIndex = avatarIndex,
                customAvatarUri = customAvatarUri,
                interests = interests.joinToString(","),
                isRegistered = true,
                isLoggedIn = true
            )
            profileDao.insertProfile(updatedProfile)

            // Reload into state flows
            _nickname.value = nickname
            _gender.value = gender
            _avatarIndex.value = avatarIndex
            _customAvatarUri.value = customAvatarUri
            _interests.value = interests
            _isRegistered.value = true
            _isUserLoggedIn.value = true

            // Sync to cloud
            syncProfileToCloud()
        }
    }

    fun verifyPinAndLogin(pin: String) {
        viewModelScope.launch {
            val profile = profileDao.getProfileOnce()
            if (profile != null && profile.pinHash == pin) {
                _isUserLoggedIn.value = true
                _loginError.value = false
                profileDao.updateLoginStatus(true)
            } else {
                _loginError.value = true
            }
        }
    }

    fun login(pin: String) {
        verifyPinAndLogin(pin)
    }

    fun logout() {
        _isUserLoggedIn.value = false
        viewModelScope.launch { profileDao.updateLoginStatus(false) }
    }

    fun updateCustomAvatarUri(uri: String?) {
        _customAvatarUri.value = uri
        viewModelScope.launch {
            profileDao.updateCustomAvatarUri(uri)
        }
    }

    fun speakQuote(text: String, lang: String) {
        tts?.let {
            if (_isTtsInitialized.value) {
                val locale = if (lang == "en") Locale.US else Locale("es", "ES")
                it.language = locale
                it.speak(text, TextToSpeech.QUEUE_FLUSH, null, "motivation_quote_utterance")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
        synthesizer.stop()
    }

    // ================================================================
    // GOALS (Room, already working)
    // ================================================================

    fun addGoal(title: String, durationMinutes: Int, isPomodoro: Boolean = false) {
        viewModelScope.launch {
            goalRepository.insertGoal(Goal(title = title, durationMinutes = durationMinutes, isPomodoro = isPomodoro))
        }
    }

    fun markGoalCompleted(goal: Goal) {
        viewModelScope.launch {
            goalRepository.updateGoal(goal.copy(isCompleted = true))

            // Record real focus session
            val cal = Calendar.getInstance()
            val session = FocusSession(
                goalId = goal.id,
                goalTitle = goal.title,
                startTime = System.currentTimeMillis() - (goal.durationMinutes * 60 * 1000L),
                endTime = System.currentTimeMillis(),
                durationSeconds = goal.durationMinutes * 60,
                isSuccess = true,
                earnedXp = 50,
                dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1, // 0=Sun
                hourOfDay = cal.get(Calendar.HOUR_OF_DAY)
            )
            sessionDao.insertSession(session)

            // Update XP
            addXp(50)

            // Update focus stats
            val totalSec = sessionDao.getTotalFocusedSeconds()
            val totalCompleted = sessionDao.getSuccessfulSessionCount()
            _totalFocusedHours.value = totalSec / 3600f
            _totalSessions.value = totalCompleted
            profileDao.updateFocusStats(totalSec, totalCompleted)

            // Update streak
            updateStreak()

            // Refresh analytics
            loadAnalytics()

            // Recheck achievements
            initAchievements()

            // Sync updated stats to cloud
            syncProfileToCloud()
        }
    }

    fun recordFailedSession(goalTitle: String, durationSeconds: Int) {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            val session = FocusSession(
                goalTitle = goalTitle,
                startTime = System.currentTimeMillis() - (durationSeconds * 1000L),
                endTime = System.currentTimeMillis(),
                durationSeconds = durationSeconds,
                isSuccess = false,
                earnedXp = 0,
                dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1,
                hourOfDay = cal.get(Calendar.HOUR_OF_DAY)
            )
            sessionDao.insertSession(session)
            loadAnalytics()
        }
    }

    private suspend fun updateStreak() {
        val profile = profileDao.getProfileOnce() ?: return
        val today = System.currentTimeMillis()
        val lastDate = profile.lastSessionDate

        val newStreak = if (lastDate == null) {
            1
        } else {
            val daysBetween = (today - lastDate) / (24 * 60 * 60 * 1000)
            when {
                daysBetween < 1 -> profile.currentStreak // Same day
                daysBetween < 2 -> profile.currentStreak + 1 // Next day
                else -> 1 // Streak broken
            }
        }

        val bestStreak = maxOf(newStreak, profile.bestStreak)
        _currentStreak.value = newStreak
        profileDao.updateStreak(newStreak, bestStreak, today)
    }

    fun deleteGoal(id: Int) {
        viewModelScope.launch {
            goalRepository.deleteGoal(id)
        }
    }

    // ================================================================
    // PREDICTIVE ENGINE (Real Bayesian calculation)
    // ================================================================

    fun getPredictiveFailureProbability(): Float {
        val cal = Calendar.getInstance()
        val currentDow = cal.get(Calendar.DAY_OF_WEEK) - 1
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)

        val analytics = _sessionAnalytics.value
        val match = analytics.find { it.dayOfWeek == currentDow && it.hourOfDay == currentHour }

        if (match == null) return 0.25f // Default prior

        // Bayesian Beta-Binomial smoothing
        val alphaPrior = 1.8f
        val betaPrior = 0.6f
        val total = match.totalSessions.toFloat()
        val fails = match.failedCount.toFloat()

        return (fails + betaPrior) / (total + alphaPrior + betaPrior)
    }

    // ================================================================
    // SETTINGS (all persist to Room)
    // ================================================================

    fun toggleVpnShield() {
        val newVal = !_vpnShieldActive.value
        _vpnShieldActive.value = newVal
        viewModelScope.launch { settingsDao.updateVpnShield(newVal) }
    }

    fun toggleAccessibilityLocker() {
        val newVal = !_accessibilityLockerActive.value
        _accessibilityLockerActive.value = newVal
        viewModelScope.launch { settingsDao.updateAccessibilityLocker(newVal) }
    }

    fun setWaTimer(minutes: Int) {
        _waTimerMinutes.value = minutes
        viewModelScope.launch { settingsDao.updateWaTimer(minutes) }
    }

    fun toggleFocusSleepEnabled() {
        val newVal = !_focusSleepEnabled.value
        _focusSleepEnabled.value = newVal
        viewModelScope.launch { settingsDao.updateFocusSleep(newVal) }
    }

    fun toggleForceSleepSimulation() {
        val newVal = !_forceSleepSimulation.value
        _forceSleepSimulation.value = newVal
        viewModelScope.launch { settingsDao.updateForceSleepSimulation(newVal) }
    }

    // ================================================================
    // DUELS (persist to Room)
    // ================================================================

    fun startDuel(rivalName: String, durationHours: Int, wagerXp: Int) {
        viewModelScope.launch {
            val duel = Duel(
                rivalName = rivalName,
                rivalAvatar = listOf("🐼", "🦁", "🐉", "🐙", "🦊", "⚡", "🦉", "🧑‍🚀", "🐯").random(),
                durationHours = durationHours,
                xpWager = wagerXp,
                playerProgress = 0f,
                rivalProgress = (10..40).random() / 100f,
                status = "Active"
            )
            duelDao.insertDuel(duel)
        }
    }

    fun yieldDuel(id: Int) {
        viewModelScope.launch {
            val duels = activeDuels.value
            val duel = duels.find { it.id == id } ?: return@launch
            addXp(-duel.xpWager)
            duelDao.updateDuelStatus(id, "Lost", 0f)
            initAchievements()
        }
    }

    fun winDuel(id: Int) {
        viewModelScope.launch {
            val duels = activeDuels.value
            val duel = duels.find { it.id == id } ?: return@launch
            addXp(duel.xpWager)
            _totalFocusedHours.value += duel.durationHours
            duelDao.updateDuelStatus(id, "Won", 1.0f)

            // Update profile
            val profile = profileDao.getProfileOnce()
            if (profile != null) {
                val newSeconds = profile.totalFocusedSeconds + (duel.durationHours * 3600L)
                profileDao.updateFocusStats(newSeconds, profile.totalSessionsCompleted)
            }
            initAchievements()
        }
    }

    // ================================================================
    // SQUADS (persist to Room)
    // ================================================================

    fun createSquad(name: String, penaltyXp: Int) {
        viewModelScope.launch {
            val squad = Squad(
                name = name,
                membersCount = (3..6).random(),
                cumulativeFocusHours = 0f,
                penaltyXp = penaltyXp,
                health = 100,
                status = "Active"
            )
            squadDao.insertSquad(squad)
        }
    }

    fun triggerSquadFail(id: Int) {
        viewModelScope.launch {
            val squads = punishmentSquads.value
            val squad = squads.find { it.id == id } ?: return@launch
            val newHealth = (squad.health - 25).coerceAtLeast(0)
            val newStatus = if (newHealth == 0) "Failed" else "Active"
            if (newStatus == "Failed") {
                addXp(-squad.penaltyXp)
            } else {
                addXp(-20)
            }
            squadDao.updateSquadHealth(id, newHealth, newStatus)
        }
    }

    // ================================================================
    // MOTIVATIONAL QUOTES
    // ================================================================

    fun getRandomMotivationalQuote(lang: String, userGender: String, isStrict: Boolean = _quoteStyleStrict.value): String {
        val userInterests = _interests.value
        
        val hasCoding = userInterests.any { it.contains("Programación") || it.contains("Coding") || it.contains("Software") }
        val hasDesign = userInterests.any { it.contains("Diseño") || it.contains("Design") }
        val hasReading = userInterests.any { it.contains("Lectura") || it.contains("Reading") || it.contains("Libro") }
        val hasSport = userInterests.any { it.contains("Deporte") || it.contains("Sport") || it.contains("Salud") || it.contains("Fitness") }
        
        val categories = mutableListOf<String>()
        if (hasCoding) categories.add("coding")
        if (hasDesign) categories.add("design")
        if (hasReading) categories.add("reading")
        if (hasSport) categories.add("sport")
        
        val chosenCategory = if (categories.isNotEmpty()) categories.random() else "general"
        
        return if (lang == "es") {
            when (chosenCategory) {
                "coding" -> listOf(
                    "El código limpio requiere una mente limpia y libre de distracciones. ¡Enfócate!",
                    "Compila tus metas de hoy paso a paso, sin fugas de memoria en redes sociales.",
                    "El verdadero hacker no es el que rompe sistemas, sino el que rompe sus malos hábitos.",
                    "Cada línea de código que escribes hoy construye tu futuro como creador digital."
                ).random()
                "design" -> listOf(
                    "El diseño no es solo lo que se ve, es cómo funciona. Y tú funcionas mejor enfocado.",
                    "Simplifica tu interfaz mental. Elimina las notificaciones y céntrate en el lienzo.",
                    "Un gran diseño toma tiempo de silencio y profunda concentración creativa.",
                    "Tu atención es el pincel con el que pintas tu obra maestra. Protégela hoy."
                ).random()
                "reading" -> listOf(
                    "La lectura nutre el alma, pero el enfoque nutre la ejecución. Aplica lo aprendido hoy.",
                    "No es que leamos poco, es que nos enfocamos a medias. Mantén la vista en tu meta.",
                    "Un libro cerrado no enseña, y una mente dispersa no realiza nada grandioso.",
                    "Lee, comprende y ejecuta. El conocimiento sin acción es letra muerta."
                ).random()
                "sport" -> listOf(
                    "¡Sin dolor no hay gloria! El músculo de tu concentración se entrena en cada sesión.",
                    "Tu mente es el atleta más importante. No dejes que abandone a mitad de carrera.",
                    "Disciplina diaria. Convence a tu cerebro cansado de dar una repetición de enfoque más.",
                    "Suda la gota gorda del trabajo duro. Hoy superas tus límites de productividad."
                ).random()
                else -> {
                    if (isStrict) {
                        when (userGender) {
                            "male" -> listOf(
                                "¡Sin excusas, soldado! El dolor de la autodisciplina pesa gramos; el arrepentimiento pesa toneladas.",
                                "Apaga los ruidos banales. Un hombre sin dominio propio es como una ciudad sin murallas.",
                                "La concentración extrema es tu única salida. No busques consuelo, busca dominar tu atención.",
                                "Ejecuta con precisión de cirujano. Un segundo de vacilación y tus metas morirán hoy."
                            ).random()
                            "female" -> listOf(
                                "¡Concentración implacable, guerrera! Las excusas no construyen imperios, las horas enfocada sí.",
                                "No negocies con tu mente perezosa. Gobierna tus pensamientos o ellos te goberará a ti.",
                                "El enfoque absoluto no es una sugerencia, es el único camino hacia el liderazgo sin límites.",
                                "Desconéctate del teatro social inmediato. La maestría requiere soledad y trabajo duro."
                            ).random()
                            else -> listOf(
                                "Bloqueo absoluto. Cero fugas de energía mental. Enfócate ahora o paga el precio del arrepentimiento.",
                                "La complacencia es el enemigo silencioso del genio creativo. Trabajo duro, ahora.",
                                "Disciplina militar. Tu mayor activo táctico es tu atención inflexible. Protégela ferozmente.",
                                "No esperes inspiración externa. La autodisciplina despiadada precede al verdadero éxito."
                            ).random()
                        }
                    } else {
                        when (userGender) {
                            "male" -> listOf(
                                "¡Mantente firme, campeón! El éxito verdadero te espera al final de este temporizador.",
                                "El guerrero del enfoque no se distrae por las notificaciones placenteras.",
                                "Enfócate en construir tu mejor imperio personal, cada segundo cuenta.",
                                "Tu disciplina diaria definirá la grandeza del hombre fuerte que estás construyendo."
                            ).random()
                            "female" -> listOf(
                                "¡Brilla con toda tu fuerza, campeona! Tu potencial creativo es simplemente infinito.",
                                "La guerrera sabia protege su concentración y conquista cada una de sus metas.",
                                "La disciplina inquebrantable de hoy es la forma más alta de amor propio.",
                                "Cada minuto enfocada te acerca un paso más a la líder extraordinaria que eres."
                            ).random()
                            else -> listOf(
                                "Tu mente enfocada es tu mayor tesoro táctico. Protégela y mantenla enfocada hoy.",
                                "La paciencia y el enfoque continuo logran grandes e inesperados avances.",
                                "Un paso pequeño pero 100% enfocado es mejor que cien pasos sin rumbo claro.",
                                "Tu versión más fuerte se construye en los instantes de máximo silencio y paz."
                            ).random()
                        }
                    }
                }
            }
        } else {
            when (chosenCategory) {
                "coding" -> listOf(
                    "Clean code requires a clean mind free of immediate distractions. Focus up!",
                    "Compile your goals step by step today, with no memory leaks from social media.",
                    "A true builder doesn't just write scripts, they build ironclad discipline habits.",
                    "Every single line of code you write today shapes your digital legacy."
                ).random()
                "design" -> listOf(
                    "Design is not just what it looks like, it's how it works. And you work best focused.",
                    "Simplify your mind. Remove notifications and center on the blank canvas.",
                    "Great design demands time, silence, and deep creative flow.",
                    "Your attention is the brush for your masterpiece. Protect it fiercely today."
                ).random()
                "reading" -> listOf(
                    "Reading feeds the mind, but focus feeds execution. Apply your knowledge now.",
                    "It's not that we read too little, it's that we focus halfway. Eyes on the target.",
                    "A closed book doesn't teach, and a scattered mind accomplishes nothing great.",
                    "Read, understand, and execute. Knowledge without action is just trivia."
                ).random()
                "sport" -> listOf(
                    "No pain, no gain! The muscle of your concentration is trained in every deep work session.",
                    "Your mind is the ultimate athlete. Don't let it quit halfway through the timer.",
                    "Daily training. Force your tired brain to complete one more sprint of deep work.",
                    "Sweat the hard details. Today you push past your limit of pure productivity."
                ).random()
                else -> {
                    if (isStrict) {
                        when (userGender) {
                            "male" -> listOf(
                                "No excuses, soldier! The pain of self-discipline weighs ounces; regret weighs tons.",
                                "Silence the digital noise. A man without self-control is like a city breached without walls.",
                                "Extreme concentration is your only option. Do not look for comfort, command your mind.",
                                "Execute with ultimate surgeon precision. One second of hesitation destroys your legacy."
                            ).random()
                            "female" -> listOf(
                                "Relentless focus, warrior! Excuses do not build dynasties; focused hours do.",
                                "Do not negotiate with a lazy mind. Govern your thoughts or they will run your life.",
                                "Absolute focus is not a suggestion—it is the single path to limitless achievement.",
                                "Sever ties with immediate distraction. High performance demands silence and hard work."
                            ).random()
                            else -> listOf(
                                "Total lock. Zero intellectual leakage. Focus right now or pay the heavy tax of regret.",
                                "Complacency is the quiet killer of creative genius. Deep work, right now.",
                                "Military-grade discipline. Your attention is your primary currency. Protect it fiercely.",
                                "Do not wait for external inspiration. Unyielding self-discipline always comes first."
                            ).random()
                        }
                    } else {
                        when (userGender) {
                            "male" -> listOf(
                                "Stay robust, champion! True victory lies ahead once this timer completes.",
                                "The master of clear focus is never swayed by cheap instant notifications.",
                                "Focus on scaling your empire, every single second counts towards your destiny.",
                                "Your relentless discipline shapes the strong man you are becoming."
                            ).random()
                            "female" -> listOf(
                                "Shine with all your light, champion! Your creative potential is absolutely limitless.",
                                "The wise focus warrior secures her peace and rules over her goals gracefully.",
                                "Protecting your focus is the absolute ultimate form of self-respect and love.",
                                "Every uninterrupted session takes you one step closer to the incredible leader you are."
                            ).random()
                            else -> listOf(
                                "Your undistracted mind is your greatest superpower. Protect its flow today.",
                                "Sustained patience and focus achieve extraordinary breakthroughs.",
                                "A single focused sprint is worth far more than a hundred scattered days.",
                                "Your ultimate high-performing self is forged in deep distraction-free moments."
                            ).random()
                        }
                    }
                }
            }
        }
    }

    // ================================================================
    // CLOUD SYNC (Online Backend)
    // ================================================================

    private val _onlineRanking = MutableStateFlow<List<com.example.network.OnlineRankEntry>>(emptyList())
    val onlineRanking = _onlineRanking.asStateFlow()

    private val _isOnline = MutableStateFlow(false)
    val isOnline = _isOnline.asStateFlow()

    fun syncProfileToCloud() {
        viewModelScope.launch {
            val nick = _nickname.value
            if (nick.isBlank() || nick == "FocusWarrior") return@launch
            val success = com.example.network.FocusLockApi.syncProfile(
                nickname = nick,
                customAvatarUri = _customAvatarUri.value,
                avatarIndex = _avatarIndex.value,
                totalHours = _totalFocusedHours.value,
                xp = _xp.value
            )
            _isOnline.value = success
        }
    }

    fun fetchOnlineRanking() {
        viewModelScope.launch {
            val ranking = com.example.network.FocusLockApi.getOnlineRanking()
            _onlineRanking.value = ranking
            _isOnline.value = ranking.isNotEmpty()
        }
    }
}
