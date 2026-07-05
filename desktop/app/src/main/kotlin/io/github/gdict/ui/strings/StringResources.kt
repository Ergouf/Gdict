package io.github.gdict.ui.strings

interface StringResources {
    val appName: String

    val navSearch: String
    val navFavorites: String
    val navLearning: String
    val navProfile: String

    val searchHint: String
    val recentSearches: String
    val wordOfTheDay: String

    val myVocabulary: String
    val removeBookmark: String
    val removeBookmarkConfirm: (String) -> String
    val remove: String
    val flashcard: String

    val addWordsToFavoritesFirst: String
    val tapToReveal: String
    val noVocabularyYet: String
    val allCaughtUp: String
    val readyToReview: String
    val startReview: (Int) -> String
    val sessionComplete: String
    val reviewedOf: (Int, Int) -> String
    val reviewAgain: String
    val skip: String

    val statNew: String
    val statDue: String
    val statLearned: String

    val profile: String
    val manageYourSettings: String
    val sectionDictionaries: String
    val dictionaryManagement: String
    val dictionaryManagementDesc: String
    val sectionStudy: String
    val flashcardReview: String
    val flashcardReviewDesc: String
    val sectionAppearance: String
    val darkMode: String
    val darkModeDesc: String
    val language: String
    val languageDesc: String
    val sectionAbout: String
    val versionInfo: String
    val projectRepository: String
    val clearData: String
    val clearDataDesc: String
    val confirmClear: String
    val confirmClearMessage: String
    val clear: String
    val cancel: String
    val selectLanguage: String
    val langEnglish: String
    val langSimplifiedChinese: String
    val langTraditionalChinese: String
    val langFollowSystem: String

    val dictionaries: String
    val scanImport: String
    val diagnostics: String
    val noDictionaries: String
    val tapToAddDictionaries: String
    val addDictionary: String
    val dictionaryName: String
    val dictionaryPath: String
    val dictionaryPathHint: String
    val scanFolderForDictionaries: String
    val selectFile: String
    val scanErrorNoDict: String
    val selectDictionariesToImport: String
    val importCount: (Int) -> String

    val tabOrigin: String
    val tabExamples: String
    val tabSynonyms: String
    val saved: String
    val addToFavorites: String

    val clearDataFailed: (String) -> String
    val bookmarkFailed: (String) -> String
    val scanDirFailed: (String) -> String
    val addDictFailed: (String) -> String
    val importDictFailed: (String, String) -> String
    val importResult: (Int, Int) -> String
    val importException: (String) -> String
    val removeDictFailed: (String) -> String
    val toggleDictFailed: (String) -> String
    val searchFailed: (String) -> String

    val wordOfTheDayWelcome: String
    val wordOfTheDayWelcomeDesc: String
    val wordOfTheDayDictionary: String
    val wordOfTheDayDictionaryDesc: String

    val back: String
    val copy: String
    val close: String
    val diagnosticResult: String
    val add: String
    val noDefinition: String

    val dictionariesAdded: (Int) -> String
    val noDictionariesYet: String
    val tapToAddMdxHint: String
    val removeDictionary: String
    val confirmRemoveDictionary: (String) -> String
    val selectFolderHint: String
    val dictionaryFolderPath: String
    val scanAndImport: String
    val noMdxInFolder: String
    val batchImport: String
    val foundDictsSelectImport: (Int) -> String
    val selectMdxFile: String
    val selectDictionaryFolder: String

    val sectionSupport: String
    val supportDeveloper: String
    val supportDeveloperDesc: String
    val donationAlipay: String
    val donationWechat: String
}
