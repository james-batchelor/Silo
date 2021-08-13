package com.silofinance.silo

import android.os.Build
import android.os.Bundle
import androidx.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import androidx.navigation.ui.*
import com.silofinance.silo.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)  // Binding is used extensively in this app. See https://developer.android.com/topic/libraries/view-binding

        // Setup the navigation component, and set the starting fragment
        val navController = findNavController(R.id.nav_host_fragment)
        val navInflater = navController.navInflater
        val graph = navInflater.inflate(R.navigation.navigation)
        val settingsSharedPrefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val settingsHome = settingsSharedPrefs.getString("settings_home", "")
        graph.startDestination = when (settingsHome) {
            "budget" -> R.id.budgetFragment  // These are from res.navigation.navigation
            "accounts" -> R.id.accountsFragment
            "transactions" -> R.id.transactionsFragment
            else -> R.id.budgetFragment
        }
        navController.graph = graph

        // ActionBar
        setSupportActionBar(binding.toolbarLayout)
        setupActionBarWithNavController(navController, binding.drawerLayout)

        // Navigation Drawer
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.budgetFragment,
                R.id.accountsFragment,
                R.id.transactionsFragment,
                //R.id.chartsFragment,
                //R.id.backupsFragment,
                R.id.aboutFragment,
                R.id.settingsFragment
            ), binding.drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navDrawer.setupWithNavController(navController)

        // Change the navigation bar colour (the bar at the bottom with triangle/circle/square)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {  // Leave in, in case the API is dropped for whatever reason (ie if desugaring is used to move API to pre 26 (LocalDate usage))
            window.navigationBarColor = ContextCompat.getColor(applicationContext, R.color.colorPrimary)
        }
    }

    /** Navigates (and is called) when the toolbar's hamburger menu or back arrow icons are pressed */
    override fun onSupportNavigateUp(): Boolean {
        hideKeyboard()
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    /** If the navigation drawer is open then have the back button close it */
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(binding.navDrawer)) {
            binding.drawerLayout.close()
        } else {
            super.onBackPressed()
        }
    }
}


/** TODO - Wishlist
 * Changelog on first update
 * Sort transactions by date (asc/desc)/value (desc/asc)/category (same as budget)/account (same as accounts)/cleared. Choose priority 1-5, and then the hidden always 6th priority is id
 * Filter transactions by date range/value range/account/category/cleared/note contains x and/or y and/or z. Spinners with checkboxes for accounts/categories? Or multi selectable recyclers. Probably save into shared prefs. For both of these (filter and sort), have a reset button. Maybe edit the menu icons on the toolbar so that if a sort or filter is applied, the icons change and turn into reset buttons. Maybe dialogs in corner and no greyed out background, like solid file explorer?
 * Backups. Not really needed until charts, but really don't want to have charts around for a while before I can get around to backups so...
 * Charts
 * Dark mode. See discord, league, simple note
 * Category groups. Then the expense category recycler is grouped too (see paper designs)
 * Budget history mode
 * Transactions in future as a dropdown box
 * Repeating transactions. Will need a menu icon to manage repeats in transactions
 * Sub budgets (so you can tap the third column of a fund I guess, maybe target too. It'll open a section to add a sub budget, where you can plan how to spend what you saved. Ie a new wardrobe, or a holiday. Pretty big expansion)
 */

/** README - notes to future devs
 * ================EXTREMELY IMPORTANT!!!!!!!!!!!!!!!!!!!!! When increasing the version number of the app, even if nothing changes in the databases, the database version numbers need to increase and a migration needs to be provided (Even if no changes to the db's were made). See https://medium.com/androiddevelopers/understanding-migrations-with-room-f01e04b07929, and also read the linked testing migrations article at the end.
 * If working on translating, in many of the xml's it says where strings can be extracted. This is not always the case for the kotlin files, so as I was coding I tried to leave to-do extract tags. I expect that I got them all but I'm not sure...
 * I'm using doubles for currencies because it's easy to work with. Yes, there's lack of precision, but the only operators that act on the values are + and -, so you'll need billions of transactions for the errors to add up and shift a value by 0.01 (the minimum precision displayed). That's not completely true, comparison operators are also used (< > etc). When these are used, an offset of 0.005 is applied as necessary. The point is, division and multiplication aren't a problem.
 * Dates and spinners don't survive reconfiguration. Not a big deal, but fix if time.
 * If adding an expense from the transactions window would place the item at the top of the list, the list wont scroll to show it.
 */































