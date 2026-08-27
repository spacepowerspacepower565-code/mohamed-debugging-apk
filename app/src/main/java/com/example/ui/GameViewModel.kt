package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.Build
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ProgressRepository
    private val vibrator = application.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?

    private val prefs = application.getSharedPreferences("missions_prefs", Context.MODE_PRIVATE)

    // Live state tracking for missions
    private val _hintsUsedCount = MutableStateFlow(prefs.getInt("hints_used_count", 0))
    val hintsUsedCount: StateFlow<Int> = _hintsUsedCount.asStateFlow()

    private val _playTimeMinutes = MutableStateFlow(prefs.getInt("play_time_minutes", 12)) // Start with realistic 12 mins
    val playTimeMinutes: StateFlow<Int> = _playTimeMinutes.asStateFlow()

    private val _consecutiveNoHelp = MutableStateFlow(prefs.getInt("consecutive_no_help", 3)) // Start with realistic 3 consecutive
    val consecutiveNoHelp: StateFlow<Int> = _consecutiveNoHelp.asStateFlow()

    private val _dailyChallengeFinished = MutableStateFlow(prefs.getBoolean("daily_challenge_finished", false))
    val dailyChallengeFinished: StateFlow<Boolean> = _dailyChallengeFinished.asStateFlow()

    // Claimed states
    private val _mission1Claimed = MutableStateFlow(prefs.getBoolean("m1_claimed", false))
    val mission1Claimed: StateFlow<Boolean> = _mission1Claimed.asStateFlow()

    private val _mission2Claimed = MutableStateFlow(prefs.getBoolean("m2_claimed", false))
    val mission2Claimed: StateFlow<Boolean> = _mission2Claimed.asStateFlow()

    private val _mission3Claimed = MutableStateFlow(prefs.getBoolean("m3_claimed", false))
    val mission3Claimed: StateFlow<Boolean> = _mission3Claimed.asStateFlow()

    private val _mission4Claimed = MutableStateFlow(prefs.getBoolean("m4_claimed", false))
    val mission4Claimed: StateFlow<Boolean> = _mission4Claimed.asStateFlow()

    private val _mission5Claimed = MutableStateFlow(prefs.getBoolean("m5_claimed", false))
    val mission5Claimed: StateFlow<Boolean> = _mission5Claimed.asStateFlow()

    val userProgress: StateFlow<UserProgress> = flow {
        val database = AppDatabase.getDatabase(application)
        val repo = ProgressRepository(database.userProgressDao())
        emitAll(repo.progressFlow.filterNotNull())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserProgress()
    )

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ProgressRepository(database.userProgressDao())
        viewModelScope.launch {
            repository.getProgressOnce() // Initializes DB entry
        }

        // Ticker to increment playtime in minutes automatically every 60 seconds
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(60_000)
                if (_playTimeMinutes.value < 45) {
                    val newVal = _playTimeMinutes.value + 1
                    _playTimeMinutes.value = newVal
                    prefs.edit().putInt("play_time_minutes", newVal).apply()
                }
            }
        }
    }

    fun incrementHintsUsed() {
        val newVal = _hintsUsedCount.value + 1
        _hintsUsedCount.value = newVal
        prefs.edit().putInt("hints_used_count", newVal).apply()
    }

    fun completeDailyChallenge() {
        if (!_dailyChallengeFinished.value) {
            _dailyChallengeFinished.value = true
            prefs.edit().putBoolean("daily_challenge_finished", true).apply()
        }
    }

    fun incrementConsecutiveNoHelp() {
        val newVal = (_consecutiveNoHelp.value + 1).coerceAtMost(10)
        _consecutiveNoHelp.value = newVal
        prefs.edit().putInt("consecutive_no_help", newVal).apply()
    }

    fun resetConsecutiveNoHelp() {
        _consecutiveNoHelp.value = 0
        prefs.edit().putInt("consecutive_no_help", 0).apply()
    }

    fun claimMission(missionNumber: Int) {
        viewModelScope.launch {
            val progress = repository.getProgressOnce()
            when (missionNumber) {
                1 -> {
                    if (!_mission1Claimed.value && progress.unlockedBackgrounds.split(",").size > 1) {
                        _mission1Claimed.value = true
                        prefs.edit().putBoolean("m1_claimed", true).apply()
                        val updated = progress.copy(gems = progress.gems + 50)
                        repository.updateProgress(updated)
                        triggerSound()
                        Toast.makeText(getApplication(), "تم استلام مكافأة المهمة الأولى: +50 جوهرة! 💎", Toast.LENGTH_SHORT).show()
                    }
                }
                2 -> {
                    if (!_mission2Claimed.value && _dailyChallengeFinished.value) {
                        _mission2Claimed.value = true
                        prefs.edit().putBoolean("m2_claimed", true).apply()
                        val updated = progress.copy(gems = progress.gems + 75)
                        repository.updateProgress(updated)
                        triggerSound()
                        Toast.makeText(getApplication(), "تم استلام مكافأة المهمة الثانية: +75 جوهرة! 💎", Toast.LENGTH_SHORT).show()
                    }
                }
                3 -> {
                    if (!_mission3Claimed.value && _hintsUsedCount.value >= 3) {
                        _mission3Claimed.value = true
                        prefs.edit().putBoolean("m3_claimed", true).apply()
                        val updated = progress.copy(gems = progress.gems + 100)
                        repository.updateProgress(updated)
                        triggerSound()
                        Toast.makeText(getApplication(), "تم استلام مكافأة المهمة الثالثة: +100 جوهرة! 💎", Toast.LENGTH_SHORT).show()
                    }
                }
                4 -> {
                    if (!_mission4Claimed.value && _playTimeMinutes.value >= 45) {
                        _mission4Claimed.value = true
                        prefs.edit().putBoolean("m4_claimed", true).apply()
                        val updated = progress.copy(gems = progress.gems + 125)
                        repository.updateProgress(updated)
                        triggerSound()
                        Toast.makeText(getApplication(), "تم استلام مكافأة المهمة الرابعة: +125 جوهرة! 💎", Toast.LENGTH_SHORT).show()
                    }
                }
                5 -> {
                    if (!_mission5Claimed.value && _consecutiveNoHelp.value >= 10) {
                        _mission5Claimed.value = true
                        prefs.edit().putBoolean("m5_claimed", true).apply()
                        val updated = progress.copy(gems = progress.gems + 150)
                        repository.updateProgress(updated)
                        triggerSound()
                        Toast.makeText(getApplication(), "تم استلام مكافأة المهمة الخامسة: +150 جوهرة! 💎", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun resetMissions() {
        val editor = prefs.edit()
        _hintsUsedCount.value = 0
        editor.putInt("hints_used_count", 0)
        _playTimeMinutes.value = 12 // reset to simulated starting value
        editor.putInt("play_time_minutes", 12)
        _consecutiveNoHelp.value = 3 // reset to starting value
        editor.putInt("consecutive_no_help", 3)
        _dailyChallengeFinished.value = false
        editor.putBoolean("daily_challenge_finished", false)
        
        _mission1Claimed.value = false
        editor.putBoolean("m1_claimed", false)
        _mission2Claimed.value = false
        editor.putBoolean("m2_claimed", false)
        _mission3Claimed.value = false
        editor.putBoolean("m3_claimed", false)
        _mission4Claimed.value = false
        editor.putBoolean("m4_claimed", false)
        _mission5Claimed.value = false
        editor.putBoolean("m5_claimed", false)
        editor.apply()
        
        triggerSound()
        Toast.makeText(getApplication(), "تم تحديث وإعادة ضبط المهام اليومية! 🌟", Toast.LENGTH_SHORT).show()
    }

    // Modal, Popup, Screen States
    private val _currentScreen = MutableStateFlow(GameScreen.MAIN_MENU)
    val currentScreen: StateFlow<GameScreen> = _currentScreen.asStateFlow()

    private val _showDailyRewardDialog = MutableStateFlow(false)
    val showDailyRewardDialog: StateFlow<Boolean> = _showDailyRewardDialog.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private val _showRanksDialog = MutableStateFlow(false)
    val showRanksDialog: StateFlow<Boolean> = _showRanksDialog.asStateFlow()

    private val _showShopDialog = MutableStateFlow(false)
    val showShopDialog: StateFlow<Boolean> = _showShopDialog.asStateFlow()

    private val _showNameChangeDialog = MutableStateFlow(false)
    val showNameChangeDialog: StateFlow<Boolean> = _showNameChangeDialog.asStateFlow()

    private val _showTutorialDialog = MutableStateFlow(false)
    val showTutorialDialog: StateFlow<Boolean> = _showTutorialDialog.asStateFlow()

    // Level active states
    private val _selectedRiddleLevelId = MutableStateFlow(1)
    val selectedRiddleLevelId: StateFlow<Int> = _selectedRiddleLevelId.asStateFlow()

    private val _riddleInput = MutableStateFlow("")
    val riddleInput: StateFlow<String> = _riddleInput.asStateFlow()

    private val _isCheckingRiddle = MutableStateFlow(false)
    val isCheckingRiddle: StateFlow<Boolean> = _isCheckingRiddle.asStateFlow()

    private val _showRiddleSuccessDialog = MutableStateFlow(false)
    val showRiddleSuccessDialog: StateFlow<Boolean> = _showRiddleSuccessDialog.asStateFlow()

    // Word Swipe Play States
    private val _selectedWordLevelId = MutableStateFlow(1)
    val selectedWordLevelId: StateFlow<Int> = _selectedWordLevelId.asStateFlow()

    private val _swipedWord = MutableStateFlow("")
    val swipedWord: StateFlow<String> = _swipedWord.asStateFlow()

    private val _foundWords = MutableStateFlow<Set<String>>(emptySet())
    val foundWords: StateFlow<Set<String>> = _foundWords.asStateFlow()

    private val _selectedGridCells = MutableStateFlow<List<Int>>(emptyList())
    val selectedGridCells: StateFlow<List<Int>> = _selectedGridCells.asStateFlow()

    private val _foundWordPaths = MutableStateFlow<List<List<Int>>>(emptyList())
    val foundWordPaths: StateFlow<List<List<Int>>> = _foundWordPaths.asStateFlow()

    private val _swipeAttemptCount = MutableStateFlow(0)
    val swipeAttemptCount: StateFlow<Int> = _swipeAttemptCount.asStateFlow()

    private val _isVerifying = MutableStateFlow(false)
    val isVerifying: StateFlow<Boolean> = _isVerifying.asStateFlow()

    private val _verificationResult = MutableStateFlow<Boolean?>(null)
    val verificationResult: StateFlow<Boolean?> = _verificationResult.asStateFlow()

    private val _showWordSuccessDialog = MutableStateFlow(false)
    val showWordSuccessDialog: StateFlow<Boolean> = _showWordSuccessDialog.asStateFlow()

    private val _lastCompletedType = MutableStateFlow("") // "words" or "riddles"
    val lastCompletedType: StateFlow<String> = _lastCompletedType.asStateFlow()

    private val _lastCompletedLevelId = MutableStateFlow(1)
    val lastCompletedLevelId: StateFlow<Int> = _lastCompletedLevelId.asStateFlow()

    private val _lastCompletedReward = MutableStateFlow(10)
    val lastCompletedReward: StateFlow<Int> = _lastCompletedReward.asStateFlow()

    // Sound asset simulated play trigger (since physical raw resources can complicate basic Compose scaffolding)
    private val _soundPlayTrigger = MutableStateFlow(0)
    val soundPlayTrigger: StateFlow<Int> = _soundPlayTrigger.asStateFlow()

    fun navigateTo(screen: GameScreen) {
        _currentScreen.value = screen
        _riddleInput.value = ""
        _isCheckingRiddle.value = false
        triggerSound()
    }

    fun toggleDailyRewardDialog(show: Boolean) {
        _showDailyRewardDialog.value = show
        triggerSound()
    }

    fun toggleSettingsDialog(show: Boolean) {
        _showSettingsDialog.value = show
        triggerSound()
    }

    fun toggleRanksDialog(show: Boolean) {
        _showRanksDialog.value = show
        triggerSound()
    }

    fun toggleShopDialog(show: Boolean) {
        _showShopDialog.value = show
        triggerSound()
    }

    fun toggleNameChangeDialog(show: Boolean) {
        _showNameChangeDialog.value = show
        triggerSound()
    }

    fun toggleTutorialDialog(show: Boolean) {
        _showTutorialDialog.value = show
        triggerSound()
    }

    fun selectRiddleLevel(levelId: Int) {
        _selectedRiddleLevelId.value = levelId
        _riddleInput.value = ""
        _isCheckingRiddle.value = false
        _currentScreen.value = GameScreen.RIDDLE_PLAY
        triggerSound()
    }

    fun selectWordLevel(levelId: Int) {
        _selectedWordLevelId.value = levelId
        _swipedWord.value = ""
        _foundWords.value = emptySet()
        _selectedGridCells.value = emptyList()
        _foundWordPaths.value = emptyList()
        _currentScreen.value = GameScreen.WORD_PLAY
        triggerSound()
    }

    fun updateRiddleInput(input: String) {
        _riddleInput.value = input
    }

    fun triggerVibration() {
        if (userProgress.value.vibrationEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(150)
            }
        }
    }

    fun triggerSound() {
        if (userProgress.value.soundEnabled) {
            _soundPlayTrigger.value = _soundPlayTrigger.value + 1
        }
    }

    fun updateUsername(newName: String) {
        viewModelScope.launch {
            val progress = repository.getProgressOnce()
            val updated = progress.copy(username = newName.take(15))
            repository.updateProgress(updated)
            _showNameChangeDialog.value = false
            triggerSound()
        }
    }

    fun toggleSound(enabled: Boolean) {
        viewModelScope.launch {
            val progress = repository.getProgressOnce()
            repository.updateProgress(progress.copy(soundEnabled = enabled))
        }
    }

    fun toggleVibration(enabled: Boolean) {
        viewModelScope.launch {
            val progress = repository.getProgressOnce()
            repository.updateProgress(progress.copy(vibrationEnabled = enabled))
            triggerVibration()
        }
    }

    fun claimDailyReward() {
        viewModelScope.launch {
            val progress = repository.getProgressOnce()
            val now = System.currentTimeMillis()
            val updated = progress.copy(
                gems = progress.gems + 50,
                lastDailyClaim = now,
                streak = progress.streak + 1
            )
            repository.updateProgress(updated)
            _showDailyRewardDialog.value = false
            triggerSound()
            Toast.makeText(getApplication(), "حصلت على 50 جوهرة مكافأة!", Toast.LENGTH_SHORT).show()
        }
    }

    fun buyBackground(bgId: String, cost: Int) {
        viewModelScope.launch {
            val progress = repository.getProgressOnce()
            if (progress.gems >= cost) {
                val currentUnlocks = progress.unlockedBackgrounds.split(",").toMutableSet()
                currentUnlocks.add(bgId)
                val updated = progress.copy(
                    gems = progress.gems - cost,
                    unlockedBackgrounds = currentUnlocks.joinToString(","),
                    selectedBackground = bgId
                )
                repository.updateProgress(updated)
                triggerSound()
                Toast.makeText(getApplication(), "تم شراء وتفعيل الخلفية بنجاح!", Toast.LENGTH_SHORT).show()
            } else {
                triggerVibration()
                Toast.makeText(getApplication(), "ليس لديك جواهر كافية!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun selectBackground(bgId: String) {
        viewModelScope.launch {
            val progress = repository.getProgressOnce()
            if (progress.unlockedBackgrounds.split(",").contains(bgId)) {
                repository.updateProgress(progress.copy(selectedBackground = bgId))
                triggerSound()
            }
        }
    }

    // Riddle Evaluation using Gemini API and Keyword fallback
    fun submitRiddleAnswer() {
        val currentInput = riddleInput.value.trim()
        if (currentInput.isEmpty()) return

        val level = GameData.getRiddleForLevel(selectedRiddleLevelId.value)
        _isCheckingRiddle.value = true

        viewModelScope.launch {
            // First we do local keyword checking
            var isCorrect = false
            for (keyword in level.answerKeywords) {
                if (currentInput.contains(keyword, ignoreCase = true)) {
                    isCorrect = true
                    break
                }
            }

            // If local check failed, check with Gemini AI under the hood
            if (!isCorrect) {
                isCorrect = GeminiClient.verifyAnswerWithAI(level.question, level.answer, currentInput)
            }

            _isCheckingRiddle.value = false
            if (isCorrect) {
                val progress = repository.getProgressOnce()
                // Award 5 gems and unlock next level
                val nextLevel = selectedRiddleLevelId.value + 1
                val maxRiddleUnlocked = if (nextLevel > progress.currentRiddleLevel) nextLevel else progress.currentRiddleLevel
                val updated = progress.copy(
                    gems = progress.gems + 5,
                    currentRiddleLevel = if (maxRiddleUnlocked <= 200) maxRiddleUnlocked else 200
                )
                repository.updateProgress(updated)

                completeDailyChallenge()
                incrementConsecutiveNoHelp()

                // Navigate to beautiful dedicated congratulations screen
                _lastCompletedType.value = "riddles"
                _lastCompletedLevelId.value = selectedRiddleLevelId.value
                _lastCompletedReward.value = 5
                _currentScreen.value = GameScreen.CONGRATULATIONS
                triggerSound()
            } else {
                triggerVibration()
                _riddleInput.value = ""
                Toast.makeText(getApplication(), "إجابة خاطئة! حاول مجدداً ❌", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun dismissRiddleSuccess() {
        _showRiddleSuccessDialog.value = false
        _riddleInput.value = ""
        // Move to levels screen
        _currentScreen.value = GameScreen.RIDDLES_MAP
        triggerSound()
    }

    // Word Connect gesture functions
    fun startSwipe(index: Int, letter: String) {
        _swipeAttemptCount.value = _swipeAttemptCount.value + 1
        _selectedGridCells.value = listOf(index)
        _swipedWord.value = letter
        triggerSound()
    }

    fun continueSwipe(index: Int, letter: String) {
        val current = _selectedGridCells.value
        if (current.isEmpty()) {
            startSwipe(index, letter)
            return
        }
        val lastIndex = current.last()
        if (lastIndex == index) return

        // Back-tracing: If swiping back to the second-to-last cell, undo the last selection
        if (current.size >= 2 && current[current.size - 2] == index) {
            _selectedGridCells.value = current.dropLast(1)
            _swipedWord.value = _swipedWord.value.dropLast(1)
            triggerSound()
            return
        }

        // Add the adjacent letter if it's not already selected
        if (!current.contains(index) && areCellsAdjacent(lastIndex, index)) {
            val updated = current.toMutableList()
            updated.add(index)
            _selectedGridCells.value = updated
            val newWord = _swipedWord.value + letter
            _swipedWord.value = newWord
            triggerSound()
        }
    }

    private fun checkAndSubmitWordOnSwipe(swiped: String, path: List<Int>): Boolean {
        if (swiped.isEmpty()) return false
        val level = GameData.getWordLevel(selectedWordLevelId.value)
        val wordsToFind = level.wordsToFind

        val reversedSwiped = swiped.reversed()
        val foundWord = when {
            wordsToFind.contains(swiped) -> swiped
            wordsToFind.contains(reversedSwiped) -> reversedSwiped
            else -> null
        }

        if (foundWord != null && !_foundWords.value.contains(foundWord)) {
            val newlyFound = _foundWords.value.toMutableSet()
            newlyFound.add(foundWord)
            _foundWords.value = newlyFound

            // Capture the currently swiped cells path
            _foundWordPaths.value = _foundWordPaths.value + listOf(path.toList())

            triggerSound()
            Toast.makeText(getApplication(), "أحسنت: $foundWord! 🎉", Toast.LENGTH_SHORT).show()

            // Reset current swipe trackers immediately inside continueSwipe so the correct word commits!
            _swipedWord.value = ""
            _selectedGridCells.value = emptyList()

            // Check if all words are found for this level
            if (newlyFound.size == wordsToFind.size) {
                viewModelScope.launch {
                    val progress = repository.getProgressOnce()
                    val nextWordLevel = selectedWordLevelId.value + 1
                    val maxWordUnlocked = if (nextWordLevel > progress.currentWordLevel) nextWordLevel else progress.currentWordLevel
                    val updated = progress.copy(
                        gems = progress.gems + 10,
                        currentWordLevel = if (maxWordUnlocked <= 1000) maxWordUnlocked else 1000
                    )
                    repository.updateProgress(updated)

                    completeDailyChallenge()
                    incrementConsecutiveNoHelp()

                    // Navigate to beautiful dedicated congratulations screen
                    _lastCompletedType.value = "words"
                    _lastCompletedLevelId.value = selectedWordLevelId.value
                    _lastCompletedReward.value = 10
                    _currentScreen.value = GameScreen.CONGRATULATIONS
                    triggerSound()
                }
            }
            return true
        }
        return false
    }

    private fun areCellsAdjacent(index1: Int, index2: Int): Boolean {
        val r1 = index1 / 4
        val c1 = index1 % 4
        val r2 = index2 / 4
        val c2 = index2 % 4
        // Support horizontal, vertical, and diagonal adjacency (8-directional) to fully match procedural generation and solver
        return Math.abs(r1 - r2) <= 1 && Math.abs(c1 - c2) <= 1
    }

    fun cancelWordSwipe() {
        _swipedWord.value = ""
        _selectedGridCells.value = emptyList()
    }

    fun finishWordSwipe() {
        val swiped = _swipedWord.value
        if (swiped.isEmpty()) return
        if (_isVerifying.value) return

        val level = GameData.getWordLevel(selectedWordLevelId.value)
        val wordsToFind = level.wordsToFind

        // Support bidirectional swipes (Arabic right-to-left can start from either end!)
        val reversedSwiped = swiped.reversed()
        val foundWord = when {
            wordsToFind.contains(swiped) -> swiped
            wordsToFind.contains(reversedSwiped) -> reversedSwiped
            else -> null
        }

        viewModelScope.launch {
            _isVerifying.value = true
            if (foundWord != null && !_foundWords.value.contains(foundWord)) {
                _verificationResult.value = true
                triggerSound()

                // Delayed feedback for correct word (600ms green glow and white flash pop)
                kotlinx.coroutines.delay(600)

                val newlyFound = _foundWords.value.toMutableSet()
                newlyFound.add(foundWord)
                _foundWords.value = newlyFound

                // Capture the currently swiped cells path
                val swipedPath = _selectedGridCells.value.toList()
                _foundWordPaths.value = _foundWordPaths.value + listOf(swipedPath)

                // Update Progress and award gems
                val progress = repository.getProgressOnce()
                val updatedGems = progress.gems + 10

                // Reset trackers
                _swipedWord.value = ""
                _selectedGridCells.value = emptyList()
                _isVerifying.value = false
                _verificationResult.value = null

                // Check if all words are found for this level
                if (newlyFound.size == wordsToFind.size) {
                    val nextWordLevel = selectedWordLevelId.value + 1
                    val maxWordUnlocked = if (nextWordLevel > progress.currentWordLevel) nextWordLevel else progress.currentWordLevel
                    val updated = progress.copy(
                        gems = updatedGems,
                        currentWordLevel = if (maxWordUnlocked <= 1000) maxWordUnlocked else 1000
                    )
                    repository.updateProgress(updated)

                    // Navigate to beautiful dedicated congratulations screen
                    _lastCompletedType.value = "words"
                    _lastCompletedLevelId.value = selectedWordLevelId.value
                    _lastCompletedReward.value = 10
                    _currentScreen.value = GameScreen.CONGRATULATIONS
                    triggerSound()
                } else {
                    val updated = progress.copy(gems = updatedGems)
                    repository.updateProgress(updated)
                }
            } else if (foundWord != null && _foundWords.value.contains(foundWord)) {
                // Already solved, treat it as invalid/incorrect drag to let them redo or show simple feedback
                _verificationResult.value = false
                triggerVibration()
                kotlinx.coroutines.delay(600)
                _swipedWord.value = ""
                _selectedGridCells.value = emptyList()
                _isVerifying.value = false
                _verificationResult.value = null
            } else {
                _verificationResult.value = false
                triggerVibration()
                kotlinx.coroutines.delay(600)
                _swipedWord.value = ""
                _selectedGridCells.value = emptyList()
                _isVerifying.value = false
                _verificationResult.value = null
            }
        }
    }

    private fun findWordPathIndices(word: String, grid: List<String>): List<Int>? {
        val n = 4
        fun getNeighbors(idx: Int): List<Int> {
            val r = idx / n
            val c = idx % n
            val list = mutableListOf<Int>()
            for (dr in -1..1) {
                for (dc in -1..1) {
                    if (dr == 0 && dc == 0) continue
                    val nr = r + dr
                    val nc = c + dc
                    if (nr in 0 until n && nc in 0 until n) {
                        list.add(nr * n + nc)
                    }
                }
            }
            return list
        }

        fun dfs(currentIndex: Int, charIndex: Int, visited: Set<Int>): List<Int>? {
            if (charIndex == word.length) return emptyList()
            if (charIndex < 0 || currentIndex < 0 || currentIndex >= grid.size) return null
            val nextChar = word[charIndex].toString()
            for (neighbor in getNeighbors(currentIndex)) {
                if (neighbor !in visited && neighbor < grid.size && grid[neighbor] == nextChar) {
                    val path = dfs(neighbor, charIndex + 1, visited + neighbor)
                    if (path != null) {
                        return listOf(neighbor) + path
                    }
                }
            }
            return null
        }

        for (i in grid.indices) {
            if (grid[i] == word[0].toString()) {
                val path = dfs(i, 1, setOf(i))
                if (path != null) {
                    return listOf(i) + path
                }
            }
        }
        return null
    }

    fun skipOrUseHint() {
        viewModelScope.launch {
            val progress = repository.getProgressOnce()
            if (progress.gems >= 30) {
                val level = GameData.getWordLevel(selectedWordLevelId.value)
                val remaining = level.wordsToFind.filterNot { _foundWords.value.contains(it) }
                if (remaining.isNotEmpty()) {
                    val hintWord = remaining.first()
                    val newlyFound = _foundWords.value.toMutableSet()
                    newlyFound.add(hintWord)
                    _foundWords.value = newlyFound

                    // Auto-calculate path indices for hintWord and add to foundWordPaths!
                    val computedPath = findWordPathIndices(hintWord, level.gridLetters)
                    if (computedPath != null) {
                        _foundWordPaths.value = _foundWordPaths.value + listOf(computedPath)
                    }

                    triggerSound()

                    val updatedProgress = progress.copy(gems = progress.gems - 30)
                    repository.updateProgress(updatedProgress)

                    incrementHintsUsed()
                    resetConsecutiveNoHelp()

                    if (newlyFound.size == level.wordsToFind.size) {
                        _lastCompletedType.value = "words"
                        _lastCompletedLevelId.value = selectedWordLevelId.value
                        _lastCompletedReward.value = 0 // Hint completed doesn't reward new multiplier gems
                        _currentScreen.value = GameScreen.CONGRATULATIONS
                    }
                }
            } else {
                triggerVibration()
                Toast.makeText(getApplication(), "ليس لديك جواهر كافية لتلميح!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun useTargetBooster() {
        viewModelScope.launch {
            val progress = repository.getProgressOnce()
            if (progress.gems >= 100) {
                val level = GameData.getWordLevel(selectedWordLevelId.value)
                val remaining = level.wordsToFind.filterNot { _foundWords.value.contains(it) }
                if (remaining.isNotEmpty()) {
                    val hintWord = remaining.first()
                    val newlyFound = _foundWords.value.toMutableSet()
                    newlyFound.add(hintWord)
                    _foundWords.value = newlyFound

                    val computedPath = findWordPathIndices(hintWord, level.gridLetters)
                    if (computedPath != null) {
                        _foundWordPaths.value = _foundWordPaths.value + listOf(computedPath)
                    }

                    triggerSound()

                    val updatedProgress = progress.copy(gems = progress.gems - 100)
                    repository.updateProgress(updatedProgress)

                    incrementHintsUsed()
                    resetConsecutiveNoHelp()

                    if (newlyFound.size == level.wordsToFind.size) {
                        _lastCompletedType.value = "words"
                        _lastCompletedLevelId.value = selectedWordLevelId.value
                        _lastCompletedReward.value = 0
                        _currentScreen.value = GameScreen.CONGRATULATIONS
                    } else {
                        Toast.makeText(getApplication(), "تم استخدام منشط الهدف على كلمة: $hintWord 🎯", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                triggerVibration()
                Toast.makeText(getApplication(), "تحتاج 100 💎 لتفعيل الهدف الرياضي!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun useFireBooster() {
        viewModelScope.launch {
            val progress = repository.getProgressOnce()
            if (progress.gems >= 150) {
                val level = GameData.getWordLevel(selectedWordLevelId.value)
                val remaining = level.wordsToFind.filterNot { _foundWords.value.contains(it) }
                if (remaining.isNotEmpty()) {
                    val hintWord = remaining.first()
                    val newlyFound = _foundWords.value.toMutableSet()
                    newlyFound.add(hintWord)
                    _foundWords.value = newlyFound

                    val computedPath = findWordPathIndices(hintWord, level.gridLetters)
                    if (computedPath != null) {
                        _foundWordPaths.value = _foundWordPaths.value + listOf(computedPath)
                    }

                    triggerSound()

                    val updatedProgress = progress.copy(gems = progress.gems - 150)
                    repository.updateProgress(updatedProgress)

                    incrementHintsUsed()
                    resetConsecutiveNoHelp()

                    if (newlyFound.size == level.wordsToFind.size) {
                        _lastCompletedType.value = "words"
                        _lastCompletedLevelId.value = selectedWordLevelId.value
                        _lastCompletedReward.value = 0
                        _currentScreen.value = GameScreen.CONGRATULATIONS
                    } else {
                        Toast.makeText(getApplication(), "تم تنشيط قوة النار على كلمة: $hintWord 🔥", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                triggerVibration()
                Toast.makeText(getApplication(), "تحتاج 150 💎 لتفعيل كرة اللهب!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun dismissWordSuccess() {
        _showWordSuccessDialog.value = false
        _swipedWord.value = ""
        _foundWords.value = emptySet()
        _selectedGridCells.value = emptyList()
        _foundWordPaths.value = emptyList()
        _currentScreen.value = GameScreen.WORDS_MAP
        triggerSound()
    }

    fun nextLevelFromSuccess() {
        val nextLvl = _lastCompletedLevelId.value + 1
        if (_lastCompletedType.value == "words") {
            selectWordLevel(nextLvl)
        } else {
            selectRiddleLevel(nextLvl)
        }
    }

    fun backToMapFromSuccess() {
        if (_lastCompletedType.value == "words") {
            _currentScreen.value = GameScreen.WORDS_MAP
        } else {
            _currentScreen.value = GameScreen.RIDDLES_MAP
        }
        triggerSound()
    }
}

enum class GameScreen {
    MAIN_MENU,
    RIDDLES_MAP,
    WORDS_MAP,
    RIDDLE_PLAY,
    WORD_PLAY,
    CONGRATULATIONS
}

enum class BackgroundTheme(val displayName: String, val bgId: String, val cost: Int) {
    DEFAULT("الرئيسية الجبلية", "DEFAULT", 0),
    SUNSET("غروب الصحراء الدافئ", "SUNSET", 100),
    FOREST("أعماق الغابة الخضراء", "FOREST", 150),
    OCEAN("هدوء المحيط الداكن", "OCEAN", 200),
    GALAXY("الفضاء الكوني الشاسع", "GALAXY", 300),
    ANIME_SAKURA("نسيم الكرز والربيع الياباني 🌸", "ANIME_SAKURA", 350),
    CYBERPUNK_NEON("نيون مدينة المستقبل السيبرانية 🌆", "CYBERPUNK_NEON", 450)
}
