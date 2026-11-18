with open("mobile/composeApp/src/commonMain/kotlin/com/railgaadi/app/i18n/IndicTransliteration.kt", "r") as f:
    c = f.read()

# Fix TAMIL_MAP
old_tamil = """    private val TAMIL_MAP = mapOf(
        'ख' to "க", 'ग' to "க", 'घ' to "க",
        'छ' to "ச", 'ज' to "ஜ", 'झ' to "ச",
        'ठ' to "ட", 'ड' to "ட", 'ढ' to "ட",
        'थ' to "த", 'द' to "த", 'ध' to "த",
        'फ' to "ப", 'ब' to "ப", 'भ' to "ப",
        'श' to "ஷ", 'ष' to "ஷ", 'स' to "ஸ",
        'ज़' to "ஜ", 'फ़' to "ப"
    )"""

new_tamil = """    private val TAMIL_MAP = mapOf(
        'ख' to "க", 'ग' to "க", 'घ' to "க",
        'छ' to "ச", 'ज' to "ஜ", 'झ' to "ச",
        'ठ' to "ட", 'ड' to "ட", 'ढ' to "ட",
        'थ' to "த", 'द' to "த", 'ध' to "த",
        'फ' to "ப", 'ब' to "ப", 'भ' to "ப",
        'श' to "ஷ", 'ष' to "ஷ", 'स' to "ஸ"
    )"""
c = c.replace(old_tamil, new_tamil)

# Fix Regex escapes
c = c.replace('Regex("[\\s,]+")', 'Regex("""[\\s,]+""")')

with open("mobile/composeApp/src/commonMain/kotlin/com/railgaadi/app/i18n/IndicTransliteration.kt", "w") as f:
    f.write(c)

print("Fixed IndicTransliteration.kt")
