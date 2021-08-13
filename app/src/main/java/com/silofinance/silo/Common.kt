package com.silofinance.silo

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.text.*
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.regex.Pattern


/** Adds put/getDouble to SharedPreferences. See https://stackoverflow.com/questions/16319237/cant-put-double-sharedpreferences */
fun SharedPreferences.Editor.putDouble(key: String, double: Double): SharedPreferences.Editor = putLong(key, java.lang.Double.doubleToRawLongBits(double))  // May not actually return SharedPreferences.Editor, the IDE suggested to put it in...
fun SharedPreferences.getDouble(key: String, default: Double) = java.lang.Double.longBitsToDouble(getLong(key, java.lang.Double.doubleToRawLongBits(default)))


/** Takes a value and formats it into a currency String, eg 1234.5 to £1,234.50 */
fun currencyFormat(value: Double, context: Context): String {
    val settingsSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
    val settingsCurrency = settingsSharedPrefs.getString("settings_currency", "")

    val numberFormat = when (settingsCurrency) {
        "1,234.56" -> NumberFormat.getCurrencyInstance(Locale.ENGLISH)
        "1.234,56" -> NumberFormat.getCurrencyInstance(Locale.GERMAN)
        else -> NumberFormat.getCurrencyInstance(Locale.ENGLISH)
    }
    numberFormat.maximumFractionDigits = 2
    numberFormat.currency = Currency.getInstance("GBP")

    if (-0.005 <= value && value < 0.0) {  // Display 0.00 instead of -0.00
        var string = numberFormat.format(-value)
        string = string.replace("£", "")  // Remove the currency sign
        return string
    }
    var string = numberFormat.format(value)
    string = string.replace("£", "")  // Remove the currency sign
    return string
}


/** Takes an EpochDay and formats it to a date string */
fun epochDayToDateString(value: Long, context: Context): String {
    val settingsSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
    val settingsDate = settingsSharedPrefs.getString("settings_date", "")

    val dateTimeFormatter = when (settingsDate) {
        "dmySlash" -> DateTimeFormatter.ofPattern("dd/MM/yy")
        "dmyDot" -> DateTimeFormatter.ofPattern("dd.MM.yy")
        "mdySlash" -> DateTimeFormatter.ofPattern("MM/dd/yy")
        "mdyDot" -> DateTimeFormatter.ofPattern("MM.dd.yy")
        "ymdSlash" -> DateTimeFormatter.ofPattern("yy/MM/dd")
        "ymdDot" -> DateTimeFormatter.ofPattern("yy.MM.dd")
        else -> DateTimeFormatter.ofPattern("dd/MM/yy")
    }

    val localDate = LocalDate.ofEpochDay(value)
    return localDate.format(dateTimeFormatter)
}


/** Close the soft input keyboard. See https://stackoverflow.com/questions/41790357/close-hide-the-android-soft-keyboard-with-kotlin */
fun View.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}
fun Fragment.hideKeyboard() {
    view?.let { activity?.hideKeyboard(it) }
}
fun Activity.hideKeyboard() {
    hideKeyboard(currentFocus ?: View(this))
}
fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}


/** Restricts amount inputs to 2dp (and up to order E+8). This allows for - signs, but whether or not the EditText will accept them is determined in each layout
  * with the android:inputType attribute. See https://stackoverflow.com/questions/5357455/limit-decimal-places-in-android-edittext/24632346#24632346 */
class DecimalDigitsInputFilter : InputFilter {

    private val digitsBeforeDecimal: Int = 8
    private val digitsAfterDecimal: Int = 2
    private val pattern: Pattern


    override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence? {
        val replacement = source.subSequence(start, end).toString()
        val newVal = (dest.subSequence(0, dstart).toString() + replacement + dest.subSequence(dend, dest.length).toString())
        val matcher = pattern.matcher(newVal)
        if (matcher.matches()) return null
        return if (TextUtils.isEmpty(source)) {
            dest.subSequence(dstart, dend)
        } else ""
    }

    init {  // Don't try to clean this up, or if you do then expect the class to stop working
        pattern = Pattern.compile(
            "-?[0-9]{0," + digitsBeforeDecimal + "}+((\\.[0-9]{0," + digitsAfterDecimal
                    + "})?)||(\\.)?"
        )
    }
}


/** This only lets 1 input go in an EditText. If the EditText starts off empty, it does nothing. If the EditText already has something in it, then it checks
 * how the text changes. If the text ends up empty then it does nothing. If the text ends up not empty, it reverts the change. This locks in inputs until they
 * are deleted. For an explanation of the override fun's check out https://stackoverflow.com/a/42951863 */
fun getEmojiTextWatcher(editText: EditText) : TextWatcher {
    return object : TextWatcher {

        var beforeText = ""
        var blockTheChange = false
        var justBlockedAChange = false
        var a = false

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            beforeText = s.toString()
            if (s!!.isNotEmpty()) blockTheChange = true
        }
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (s!!.isEmpty()) blockTheChange = false
        }
        override fun afterTextChanged(s: Editable?) {
            if (justBlockedAChange) {  // Prevents infinite loop as the setText in the else if will make the TextWatcher trigger itself
                justBlockedAChange = false
            } else if (a) {
                justBlockedAChange = true
            } else if (blockTheChange) {
                a = true
                editText.setText(beforeText)
            }
        }
    }
}


/** Restricts inputs to emoji's (and some other symbols, it's not too strong). Inspiration taken from https://stackoverflow.com/a/31048120
 * To do this I went through the list of all emoji's on 1/10/2020, logging their type when when pressed (see the Log commented out below). The most common
 * pattern is 19 19, then 28 6, 19 19 6, and 19 19 16 are all fairly common. 28 is rarer but has important emoji's (eg electrical symbol, fuel pump, coffee).
 * 28 also happens to correspond to OTHER_SYMBOL's like the TM character, which is a bit annoying, and no attempt has been made to block these.
 * See https://developer.android.com/reference/java/lang/Character for Character.XXX.toInt()'s correspond to which codes. Make sure the final is labelled
 * with General category "xx" in the Unicode specification. There are other codes that I left out because those emoji's are virtually useless, and I wanted
 * to avoid unintentionally including extra things. They are all single number codes, they are: 24, 25, 9, 2, 20.
 *
 *
 * Addition: The filter will now stop you reusing emoji's. This used to be its own function, but it makes sense to do it all in one go here (to me). */
class EmojiInputFilter(val context: Context): InputFilter {

    private val aEmojiSharedPref = context.getSharedPreferences(context.getString(R.string.a_emoji_pref_file_key), Context.MODE_PRIVATE)
    private val cEmojiSharedPref = context.getSharedPreferences(context.getString(R.string.c_emoji_pref_file_key), Context.MODE_PRIVATE)


    override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence? {

        // Check the input isn't already an emoji in use
        aEmojiSharedPref?.all?.forEach {
            if (source == it.value) {
                Toast.makeText(context, "Emoji already in use", Toast.LENGTH_SHORT).show()  // todo extract
                return ""
            }
        }
        cEmojiSharedPref?.all?.forEach {
            if (source == it.value) {
                Toast.makeText(context, "Emoji already in use", Toast.LENGTH_SHORT).show()  // todo extract
                return ""
            }
        }

        // Check that the input is (probably) an emoji
        for (i in start until end) {
            val type: Int = Character.getType(source[i])
            //Log.e("tag", type.toString())  // Used to find the tags of emoji's
            if (type != Character.SURROGATE.toInt()  // 19
                && type != Character.OTHER_SYMBOL.toInt()  // 28
                && type != Character.FORMAT.toInt()  // 16
                && type != Character.NON_SPACING_MARK.toInt()  // 6
            ) {
                return ""  // So unless ALL the char types of source's chars are one of the above, delete what was entered (or really enter "")
            }
        }
        return null
    }
}

/** Same as the EmojiInputFilter above, but here we allow an input if the input is the emoji the object already holds */
class EditEmojiInputFilter(private val context: Context, private val itemEmoji: String): InputFilter {

    private val aEmojiSharedPref = context.getSharedPreferences(context.getString(R.string.a_emoji_pref_file_key), Context.MODE_PRIVATE)
    private val cEmojiSharedPref = context.getSharedPreferences(context.getString(R.string.c_emoji_pref_file_key), Context.MODE_PRIVATE)


    override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence? {

        // Check the input isn't already an emoji in use, UNLESS the input is the emoji that the object already holds
        aEmojiSharedPref?.all?.forEach {
            if (source == it.value && source != itemEmoji) {
                Toast.makeText(context, "Emoji already in use", Toast.LENGTH_SHORT).show()  // todo extract
                return ""
            }
        }
        cEmojiSharedPref?.all?.forEach {
            if (source == it.value && source != itemEmoji) {
                Toast.makeText(context, "Emoji already in use", Toast.LENGTH_SHORT).show()  // todo extract
                return ""
            }
        }

        // Check that the input is (probably) an emoji
        for (i in start until end) {
            val type: Int = Character.getType(source[i])
            //Log.e("tag", type.toString())  // Used to find the tags of emoji's
            if (type != Character.SURROGATE.toInt()  // 19
                && type != Character.OTHER_SYMBOL.toInt()  // 28
                && type != Character.FORMAT.toInt()  // 16
                && type != Character.NON_SPACING_MARK.toInt()  // 6
            ) {
                return ""  // So unless ALL the char types of source's chars are one of the above, delete what was entered (or really enter "")
            }
        }
        return null
    }
}
