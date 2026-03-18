# Plan: Phase 8 - AI Insights Engine via MediaPipe LLM

**TL;DR:** Integrate Google MediaPipe LLM on-device inference to generate 3 monthly financial insights and an "Explain My Spending" summary. Create AIManager layer that abstracts LLM interactions, transforms Room DB data into structured text, and caches results locally. HomeViewModel triggers insight generation on dashboard load with graceful fallback to rule-based templates. UI adds AI Insights card + button to HomeScreen.

---

## Steps

### **Phase 1: Foundation & Dependencies** *(no dependencies on later phases)*
1. Add MediaPipe LLM & Google AI SDK to [app/build.gradle.kts](app/build.gradle.kts)
2. Create DB cache layer: [AIInsightEntity.kt](app/src/main/java/com/example/flexfi/data/local/entities/AIInsightEntity.kt), [AIInsightDao.kt](app/src/main/java/com/example/flexfi/data/local/dao/AIInsightDao.kt), update [FlexFiDatabase.kt](app/src/main/java/com/example/flexfi/data/local/FlexFiDatabase.kt) (version 15→16)
3. Create [AIInsightRepository.kt](app/src/main/java/com/example/flexfi/data/repository/AIInsightRepository.kt) following existing repository pattern

### **Phase 2: Core AI Layer** *(depends on Phase 1)*
4. Create [FinancialDataSummarizer.kt](app/src/main/java/com/example/flexfi/ai/FinancialDataSummarizer.kt) — transforms monthly expenses/settlements into structured text format (total spend, category breakdown, top merchants, streak)
5. Create [PromptBuilder.kt](app/src/main/java/com/example/flexfi/ai/PromptBuilder.kt) — builds insights & explain prompts with exact requirements embedded
6. Create [InsightParser.kt](app/src/main/java/com/example/flexfi/ai/InsightParser.kt) — parses LLM output into List<String> (3 insights max 15 words each), validates constraints
7. Create [FallbackInsights.kt](app/src/main/java/com/example/flexfi/ai/FallbackInsights.kt) — 12–15 deterministic rule-based insight templates, selected via (dataHash % templates.size)
8. Create [ModelManager.kt](app/src/main/java/com/example/flexfi/ai/ModelManager.kt) — singleton handling model download to app files dir on first launch, lifecycle management
9. Create [AIManager.kt](app/src/main/java/com/example/flexfi/ai/AIManager.kt) — core orchestrator: queries DB via repositories → summarizes data → validates cache → invokes LLM (Dispatchers.Default) or fallback → caches result

**Phase 2 flow**: Data (Room) → Summarizer → Prompt → LLM → Parser → Cache → StateFlow

### **Phase 3: ViewModel & UI Integration** *(depends on Phase 1, Phase 2)*
10. Update [HomeViewModel.kt](app/src/main/java/com/example/flexfi/ui/screens/home/HomeViewModel.kt) — add `aiInsights`, `aiExplanation`, `aiLoading` StateFlows; trigger `aiManager.generateInsights()` on init
11. Create [AIInsightsCard.kt](app/src/main/java/com/example/flexfi/ui/components/AIInsightsCard.kt) — Compose component showing 3 insights, loading spinner, "Explain My Spending" button
12. Update [HomeScreen.kt](app/src/main/java/com/example/flexfi/ui/screens/home/HomeScreen.kt) — add AIInsightsCard after balances, add bottom sheet for explanation
13. Update [MainActivity.kt](app/src/main/java/com/example/flexfi/MainActivity.kt) — initialize ModelManager & AIManager, pass to ViewModelFactories
14. Implement non-blocking model download UI (background async, optional toast on first run)

### **Phase 4: Verification & Polish** *(depends on Phase 3)*
15. Integration tests: insights appear in ≤5sec, caching works (same insights on rapid refresh), fallback works offline, no ANRs
16. Add comprehensive logging to AIManager & ModelManager for debugging

---

## Relevant Files

### **New Files (8 AI layer + DB + UI)**
- [app/src/main/java/com/example/flexfi/ai/AIManager.kt](app/src/main/java/com/example/flexfi/ai/AIManager.kt)
- [app/src/main/java/com/example/flexfi/ai/ModelManager.kt](app/src/main/java/com/example/flexfi/ai/ModelManager.kt)
- [app/src/main/java/com/example/flexfi/ai/FinancialDataSummarizer.kt](app/src/main/java/com/example/flexfi/ai/FinancialDataSummarizer.kt)
- [app/src/main/java/com/example/flexfi/ai/PromptBuilder.kt](app/src/main/java/com/example/flexfi/ai/PromptBuilder.kt)
- [app/src/main/java/com/example/flexfi/ai/InsightParser.kt](app/src/main/java/com/example/flexfi/ai/InsightParser.kt)
- [app/src/main/java/com/example/flexfi/ai/FallbackInsights.kt](app/src/main/java/com/example/flexfi/ai/FallbackInsights.kt)
- [app/src/main/java/com/example/flexfi/data/local/entities/AIInsightEntity.kt](app/src/main/java/com/example/flexfi/data/local/entities/AIInsightEntity.kt)
- [app/src/main/java/com/example/flexfi/data/local/dao/AIInsightDao.kt](app/src/main/java/com/example/flexfi/data/local/dao/AIInsightDao.kt)
- [app/src/main/java/com/example/flexfi/data/repository/AIInsightRepository.kt](app/src/main/java/com/example/flexfi/data/repository/AIInsightRepository.kt)
- [app/src/main/java/com/example/flexfi/ui/components/AIInsightsCard.kt](app/src/main/java/com/example/flexfi/ui/components/AIInsightsCard.kt)

### **Files to Modify**
- [app/build.gradle.kts](app/build.gradle.kts) — add dependencies
- [app/src/main/java/com/example/flexfi/data/local/FlexFiDatabase.kt](app/src/main/java/com/example/flexfi/data/local/FlexFiDatabase.kt) — bump version, add entity
- [app/src/main/java/com/example/flexfi/ui/screens/home/HomeViewModel.kt](app/src/main/java/com/example/flexfi/ui/screens/home/HomeViewModel.kt) — add insight flows
- [app/src/main/java/com/example/flexfi/ui/screens/home/HomeScreen.kt](app/src/main/java/com/example/flexfi/ui/screens/home/HomeScreen.kt) — add card & sheet
- [app/src/main/java/com/example/flexfi/MainActivity.kt](app/src/main/java/com/example/flexfi/MainActivity.kt) — initialize AIManager

---

## Verification

1. **Insights Generation**: AIManager.generateInsights() → 3 insights in StateFlow ≤5sec ✓
2. **Caching**: Generate once, refresh twice → 2nd & 3rd use cache (verify timestamps) ✓
3. **Fallback**: Simulate offline/disabled LLM → deterministic fallback insights appear (no random) ✓
4. **Threading**: No ANRs; LLM runs on Dispatchers.Default ✓
5. **UI**: AIInsightsCard visible, styled consistently, bottom sheet works, loading spinner animates ✓
6. **No Main-Thread Blocking**: Choreographer monitor during insight generation ✓
7. **Currency**: Insights display in user's local currency (not USD base) ✓

---

## Key Decisions

| Decision | Rationale |
|----------|-----------|
| **App files dir storage** | Keeps APK small; runtime download acceptable for 1B model |
| **24h cache TTL** | Balance freshness vs. repeated computation; hash-based validation |
| **Async model download** | Non-blocking; background on app startup |
| **Temperature=0.2** | Low temp → deterministic, factual outputs; no creative randomness |
| **Fallback determinism** | Use data hash (not Random) → reproducible insights |
| **Manual DI (no Hilt)** | Matches existing FlexFi pattern; wire in MainActivity |
| **Generate on dashboard load** | Insights fresh when user opens HomeScreen; cached on revisits |

---

## Extended Implementation Details

### **FinancialDataSummarizer Output Format**

```
Monthly Financial Summary:
- Total Spend: ₹12,500
- Previous Month: ₹10,000
- Month-over-Month Change: +25%

Category Breakdown:
- Food: ₹5,000 (40%)
- Transport: ₹2,000 (16%)
- Shopping: ₹3,000 (24%)
- Other: ₹2,500 (20%)

Top Merchants:
- Zomato: ₹3,200
- Uber: ₹1,800
- Amazon: ₹1,500

Spending Behavior:
- Streak: 5 days (logged expenses)
- Average Daily Spend: ₹417
- Highest Category: Food
```

### **Insights Prompt (Exact)**

```
You are a financial analytics engine.

Analyze the given data and generate EXACTLY 3 insights.

Rules:
- Max 15 words per insight
- No advice
- Only observations
- Output numbered list (1. ... 2. ... 3. ...)
- Keep insights factual and data-driven

Data:
{DATA}
```

### **Explain Prompt (Exact)**

```
You are a personal finance assistant.

Explain the user's spending in simple terms.

Rules:
- Max 4 lines
- Clear and helpful
- No generic advice
- Use the user's local currency
- Focus on key patterns or changes

Data:
{DATA}
```

### **InsightParser Logic**

```kotlin
// Input: LLM response string
// Output: List<String> (exactly 3 insights, or fallback if parsing fails)

// Parsing steps:
1. Split by newline
2. Filter lines that start with "1.", "2.", "3." or just digits
3. Extract text after the numbering prefix (e.g., "1. " → captured text)
4. Remove trailing punctuation (periods, exclamation)
5. Validate: each insight ≤ 15 words (split by spaces, count)
6. If any insight > 15 words, truncate and add "..."
7. If < 3 insights parsed, fill with fallback templates
8. Return List<String> of length 3
```

### **FallbackInsights Templates** (Deterministic Selection)

```kotlin
val templates = listOf(
    "Food remains your highest spending category this month",
    "Transport expenses increased by approximately 20% from last month",
    "Shopping category accounts for over 25% of total spending",
    "Daily spending has been consistent with less than 10% variance",
    "Top three merchants represent 45% of your total expenses",
    "You have tracked expenses for 5 consecutive days this month",
    "Discretionary spending has remained stable compared to previous period",
    "Category distribution suggests a balanced spending across multiple areas",
    "Recent expenses show a pattern of higher weekend spending",
    "Payment methods diversified across cash, card, and digital platforms",
    "Average transaction size indicates preference for smaller transactions",
    "Spending spike detected in the current week compared to baseline",
)

// Selection: index = (dataHash.hashCode() % templates.size)
// Same data → always same index → always same 3 insights
// Select templates[index], templates[(index+1)%size], templates[(index+2)%size]
```

### **AIManager.generateInsights() Flow**

```kotlin
suspend fun generateInsights(): Flow<List<String>> = flow {
    try {
        // 1. Emit loading state
        emit(emptyList()) // or use a separate loading StateFlow
        
        // 2. Query data from repositories
        val personalExpenses = personalExpenseRepository.getExpensesInRange(
            userPhone, 
            startOfCurrentMonth, 
            endOfCurrentMonth
        ).firstOrNull() ?: emptyList()
        
        val settlements = expenseRepository.getSettlementsInRange(
            userPhone,
            startOfCurrentMonth,
            endOfCurrentMonth
        ).firstOrNull() ?: emptyList()
        
        val streak = streakRepository.getCurrentStreak().firstOrNull() ?: 0
        
        // 3. Summarize financial data
        val dataString = financialDataSummarizer.summarizeMonthlyData(
            personalExpenses,
            settlements,
            streak
        )
        val dataHash = dataString.hashCode()
        
        // 4. Check cache
        val cached = aiInsightRepository.getInsightIfValid(dataHash)
        if (cached != null) {
            emit(cached)
            return@flow
        }
        
        // 5. Build prompt
        val prompt = promptBuilder.buildInsightsPrompt(dataString)
        
        // 6. Invoke LLM (non-blocking)
        val response = withContext(Dispatchers.Default) {
            llmInference.generateText(
                prompt,
                maxTokens = 256,
                temperature = 0.2f,
                topK = 40
            )
        }
        
        // 7. Parse output
        val insights = insightParser.parseInsights(response)
        
        // 8. Cache and emit
        aiInsightRepository.cacheInsights(insights, dataHash)
        emit(insights)
        
    } catch (e: Exception) {
        // 9. Fallback on error
        Log.e("AIManager", "LLM error, using fallback", e)
        val fallback = fallbackInsights.getInsights(dataHash)
        emit(fallback)
    }
}
```

### **Database Schema: AIInsightEntity**

```kotlin
@Entity(tableName = "ai_insights")
data class AIInsightEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val insights: String,                  // JSON serialized List<String>
    val dataHash: Long,                    // Hash of input data (for cache matching)
    val generatedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = 0L               // currentTimeMillis() + 24h
)

// DAO methods:
// - suspend fun insertOrUpdate(entity: AIInsightEntity): Long
// - suspend fun getByDataHash(dataHash: Long): AIInsightEntity?
// - suspend fun deleteExpired(now: Long): Int
```

### **UIModel for AIInsightsCard**

```kotlin
// Pass from ViewModel to Composable
data class AIInsightsUiState(
    val insights: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
```

### **AIInsightsCard Composable**

```kotlin
@Composable
fun AIInsightsCard(
    insights: List<String>,
    isLoading: Boolean,
    onExplainClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MutableInteractionSource().collectIsFocusedAsState().value.let {
                if (it) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "💡 AI Insights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Insights List
            if (insights.isNotEmpty()) {
                insights.forEach { insight ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "•",
                            modifier = Modifier.padding(end = 8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = insight,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else if (!isLoading) {
                Text(
                    text = "No insights available yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Explain Button
            Button(
                onClick = onExplainClick,
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Text("Explain My Spending", fontSize = 12.sp)
            }
        }
    }
}
```

### **Bottom Sheet for Explanation**

```kotlin
// In HomeScreen:
var showExplainSheet by remember { mutableStateOf(false) }

if (showExplainSheet) {
    ModalBottomSheet(
        onDismissRequest = { showExplainSheet = false }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Your Spending Summary",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = viewModel.aiExplanation.value ?: "Loading...",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Button(
                onClick = { showExplainSheet = false },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Close")
            }
        }
    }
}
```

### **Model Download Logic (ModelManager)**

```kotlin
// On app startup (MainActivity or App class):
// Launch background coroutine
viewModelScope.launch(Dispatchers.IO) {
    val modelDir = context.filesDir / "ai_models"
    val modelPath = modelDir / "gemma-1b-quantized.bin"
    
    if (!modelPath.exists()) {
        try {
            // Download from HuggingFace or CDN
            val downloadUrl = "https://huggingface.co/.../gemma-1b-gguf/resolve/main/model.bin"
            val file = downloadFile(downloadUrl, modelPath)
            
            // Initialize LLM engine
            llmInference = LlmInference.createFromFile(file)
            
            Log.d("ModelManager", "Model loaded successfully")
        } catch (e: Exception) {
            Log.e("ModelManager", "Model download/init failed", e)
            // AI features will gracefully degrade; fallback active
        }
    } else {
        // Load existing model
        llmInference = LlmInference.createFromFile(modelPath)
    }
}
```

### **HomeViewModel Integration**

```kotlin
class HomeViewModel(
    private val personalExpenseRepository: PersonalExpenseRepository,
    private val expenseRepository: ExpenseRepository,
    private val streakRepository: StreakRepository,
    private val aiManager: AIManager  // New dependency
) : ViewModel() {
    
    // Existing state...
    private val _balances = MutableStateFlow<DashboardBalances?>(null)
    val balances: StateFlow<DashboardBalances?> = _balances.asStateFlow()
    
    // New AI state
    private val _aiInsights = MutableStateFlow<List<String>>(emptyList())
    val aiInsights: StateFlow<List<String>> = _aiInsights.asStateFlow()
    
    private val _aiExplanation = MutableStateFlow<String?>(null)
    val aiExplanation: StateFlow<String?> = _aiExplanation.asStateFlow()
    
    private val _aiLoading = MutableStateFlow(true)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()
    
    init {
        loadDashboardData()
        generateAIInsights()  // Trigger on dashboard load
    }
    
    private fun generateAIInsights() {
        viewModelScope.launch {
            _aiLoading.value = true
            try {
                aiManager.generateInsights().collect { insights ->
                    _aiInsights.value = insights
                    _aiLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "AI insights error", e)
                _aiLoading.value = false
            }
        }
    }
    
    fun explainSpending() {
        viewModelScope.launch {
            try {
                aiManager.explainSpending().collect { explanation ->
                    _aiExplanation.value = explanation
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "AI explanation error", e)
                _aiExplanation.value = "Unable to generate explanation"
            }
        }
    }
}
```

---

## Testing Checklist

- [ ] AIManager queries repositories without blocking main thread
- [ ] Insights appear within 5 seconds of HomeScreen load
- [ ] Same data (within 24h) returns cached insights
- [ ] Cache expiry and refresh work correctly
- [ ] Fallback determinism: same data always produces same fallback insights
- [ ] LLM response parsing handles edge cases (extra whitespace, numbering variations)
- [ ] Bottom sheet appears and dismisses smoothly
- [ ] Loading spinner animates during insight generation
- [ ] Model downloads on first app launch (background, no blocking)
- [ ] Spending explanation appears in bottom sheet with proper formatting
- [ ] No ANRs or crashes during concurrent operations
- [ ] Currency display matches user preference (not USD base)
- [ ] Logs capture prompts, responses, errors for debugging
