package com.silofinance.silo.about

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.silofinance.silo.BuildConfig
import com.silofinance.silo.R
import com.silofinance.silo.databinding.FragmentAboutBinding
import com.silofinance.silo.db.AccountDb
import com.silofinance.silo.db.CategoryDb
import com.silofinance.silo.db.TransactionDb


class AboutFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding: FragmentAboutBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_about, container, false)
        binding.lifecycleOwner = this  // Allows the binding to observe LiveData updates

        // Set up the ViewModel and similar
        val application = requireNotNull(this.activity).application  // Reference to the application context
        val accountDataSource = AccountDb.getInstance(application).accountDbDao  // Get a reference to AccountDbDao, correctly linked with the AccountDb
        val categoryDataSource = CategoryDb.getInstance(application).categoryDbDao  // Get a reference to CategoryDbDao, correctly linked with the CategoryDb
        val transactionDataSource = TransactionDb.getInstance(application).transactionDbDao  // Get a reference to TransactionDbDao, correctly linked with the TransactionDb
        val viewModelFactory = AboutViewModelFactory(accountDataSource, categoryDataSource, transactionDataSource, application)  // Create an instance of the ViewModelFactory associated with this fragment
        val viewModel = ViewModelProvider(this, viewModelFactory).get(AboutViewModel::class.java)  // Get a reference to the ViewModel associated with this fragment
        binding.viewModel = viewModel

        // Set the version TextView to say "Version x.x.x" for the right version name
        val versionName = BuildConfig.VERSION_NAME
        val versionText = binding.fabVersion.text.toString().plus(versionName)
        binding.fabVersion.text = versionText

        // Try to open the app's listing on the google play store. If the google play store app in not installed, go to the website listing
        binding.fabRate.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${requireActivity().packageName}")))
            } catch (e: ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=${requireActivity().packageName}")))
            }
            Toast.makeText(context, "Thanks :)", Toast.LENGTH_SHORT).show() //todo extract
        }

        // Send an email intent if an email app is installed
        binding.fabSuggest.setOnClickListener{
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:silodeveloper@gmail.com")
            intent.putExtra(Intent.EXTRA_SUBJECT,"Silo Suggestion")
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(context, "Email me at silodeveloper@gmail.com", Toast.LENGTH_LONG).show() //todo extract
            }
        }

        // Send an email intent if an email app is installed
        binding.fabEmail.setOnClickListener{
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:silodeveloper@gmail.com")
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(context, "Email me at silodeveloper@gmail.com", Toast.LENGTH_LONG).show() //todo extract
            }
        }

        // Open the pastebin url to the terms & conditions. See backup at end of file.
        binding.fabTerms.setOnClickListener{
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.google.com/document/d/1HQcAC4QPMWvl0KjGmbWda5AQyO6i4_VV83LYAn-D6fA"))
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(context, "Find the Terms & Conditions https://docs.google.com/document/d/1HQcAC4QPMWvl0KjGmbWda5AQyO6i4_VV83LYAn-D6fA", Toast.LENGTH_LONG).show() //todo extract
            }
        }

        // Open the pastebin url to the privacy policy. See backup at end of file.
        binding.fabPrivacy.setOnClickListener{
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.google.com/document/d/1A3QZ_rUxTMvP2X1iXyOzusXXkJiHqSbvyqHs0z-YQy0"))
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(context, "Find the Privacy Policy at https://docs.google.com/document/d/1A3QZ_rUxTMvP2X1iXyOzusXXkJiHqSbvyqHs0z-YQy0", Toast.LENGTH_LONG).show() //todo extract
            }
        }

        // Send an email intent if an email app is installed
        binding.fabBug.setOnClickListener{
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:silodeveloper@gmail.com")
            intent.putExtra(Intent.EXTRA_SUBJECT,"Silo Bug Report ($versionText)")
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(context, "Email me at silodeveloper@gmail.com", Toast.LENGTH_LONG).show() //todo extract
            }
        }

        binding.fabDatabases.setOnClickListener {
            ClearDatabasesDialog.newInstance().show(childFragmentManager, "ClearDatabasesDialogTag")
        }

        return binding.root
    }
}

/**

Privacy Policy

James Batchelor built the Silo app as a Free app. This SERVICE is provided by James Batchelor (“The Developer”, “We” or “Us”) at no cost and is intended for use as is.

This page is used to inform visitors regarding my policies with the collection, use, and disclosure of Personal Information if anyone decided to use my Service.

If you choose to use my Service, then you agree to the collection and use of information in relation to this policy. The Personal Information that I collect is used for providing and improving the Service. I will not use or share your information with anyone except as described in this Privacy Policy.

The terms used in this Privacy Policy have the same meanings as in our Terms and Conditions, which is accessible at the Silo app unless otherwise defined in this Privacy Policy.

Information Collection and Use

For a better experience, while using our Service, I may require you to provide us with certain personally identifiable information, including but not limited to financial transaction category names, the money available in each financial transaction category, financial transactions (the amount, the date, the category and the account(s) used), financial institution account names and financial institution account balances. The information that I request will be retained on your device and is not collected by me in any way.

The information that I request will be used to display a log of financial transactions, display financial institution account names, calculate updates to financial institution account balances,  display financial institution account balances, display financial transaction category names, calculated updates to the available money in each financial transaction category and display the money available in each financial transaction category.

The app does use third party services that may collect information used to identify you.

Link to privacy policy of third party service provider used by the app:

• Google Play Services (https://www.google.com/policies/privacy/)

Log Data

I want to inform you that whenever you use my Service, in a case of an error in the app I collect data and information (through third party products) on your phone called Log Data. This Log Data may include information such as your device name, operating system version, the configuration of the app when utilizing my Service, the time and date of your use of the Service, and other statistics.

Cookies
Cookies are files with a small amount of data that are commonly used as anonymous unique identifiers. These are sent to your browser from the websites that you visit and are stored on your device's internal memory.

This Service does not use these “cookies” explicitly. However, the app may use third party code and libraries that use “cookies” to collect information and improve their services. You have the option to either accept or refuse these cookies and know when a cookie is being sent to your device. If you choose to refuse our cookies, you may not be able to use some portions of this Service.

Service Providers

I may employ third-party companies and individuals due to the following reasons:

• To facilitate our Service;
• To provide the Service on our behalf;
• To perform Service-related services; or
• To assist us in analyzing how our Service is used.

I want to inform users of this Service that these third parties have access to your Personal Information. The reason is to perform the tasks assigned to them on our behalf. However, they are obligated not to disclose or use the information for any other purpose. An example of such a Service Provider is a keyboard app used to input your Personal Information.

Security

I value your trust in providing us your Personal Information, thus we are striving to use commercially acceptable means of protecting it. But remember that no method of transmission over the internet, or method of electronic storage is 100% secure and reliable, and I cannot guarantee its absolute security. The Silo app stores and processes personal data that you have provided to us, in order to provide my Service. The personal data is stored in an encrypted location on the local storage of the device, or an encrypted location on external storage if the app is installed on such external storage i.e. an external SD card. It’s your responsibility to keep your phone and access to the app secure. We therefore recommend that you do not jailbreak or root your phone, which is the process of removing software restrictions and limitations imposed by the official operating system of your device. It could make your phone vulnerable to malware/viruses/malicious programs, compromise your phone’s security features and it could mean that the Silo app won’t work properly or at all.

Links to Other Sites

This Service may contain links to other sites. If you click on a third-party link, you will be directed to that site. Note that these external sites are not operated by me. Therefore, I strongly advise you to review the Privacy Policy of these websites. I have no control over and assume no responsibility for the content, privacy policies, or practices of any third-party sites or services.

Children’s Privacy

These Services do not address anyone under the age of 13. I do not knowingly collect personally identifiable information from children under 13. If you are under 13, do not provide any information to the Silo app.

Control Over and Access to Your Personal Information

All Personal Information is stored in an encrypted location on the local storage of the device, or an external storage if the app is installed on such external storage i.e. an external SD card. This Personal Information is displayed in the Silo app in the “Budget”, “Accounts” and “Transactions” pages. No Personal Information is sent to a central server or in any other way extracted from your device by us. All Personal Information is stored until either deleted by you, or until the Silo app is uninstalled from the device, in which case all Personal Information is deleted.

All Personal Information can be deleted at once by going to the Silo app “About Silo” page and tapping “Clear All Databases”. Individual financial transactions can be deleted by going to the “Transactions” page, tapping and holding the transaction to edit, and then choosing delete. Individual financial institution accounts can be deleted by going to the “Accounts” page, tapping then pencil icon on the top right to go to the “Edit Accounts” page, hiding a financial institution account by tapping on the hide icon and then deleting the financial institution account from the “Hidden” tab in the “Edit Accounts” page. Individual financial transaction categories can be deleted by going to the “Budget” page, tapping then pencil icon on the top right to go to the “Edit Budget” page, hiding a financial transaction category by tapping on the hide icon and then deleting the financial transaction category from the “Hidden” tab in the “edit accounts” page. No other Personal Information is stored.

Changes to This Privacy Policy

I may update our Privacy Policy from time to time. Thus, you are advised to review this page periodically for any changes. I will notify you of any changes by posting the new Privacy Policy on this page.

This policy is effective as of 2020-10-09

Contact Us

If you have any questions or suggestions about my Privacy Policy, do not hesitate to contact me at silodeveloper@gmail.com.

This privacy policy page was created at privacypolicytemplate.net and modified/generated by App Privacy Policy Generator (https://app-privacy-policy-generator.firebaseapp.com/)

 */






/**

Terms & Conditions

By downloading or using the Silo app, these terms will automatically apply to you – you should make sure therefore that you read them carefully before using the app. You’re not allowed to copy, or modify the app, any part of the app, or our trademarks in any way. You’re not allowed to attempt to extract the source code of the app, and you also shouldn’t try to translate the app into other languages, or make derivative versions. The app itself, and all the trade marks, copyright, database rights and other intellectual property rights related to it, still belong to James Batchelor (“The Developer”, “We” or “Us”).

The developer is committed to ensuring that the app is as useful and efficient as possible. For that reason, we reserve the right to make changes to the app or to charge for its services, at any time and for any reason. We will never charge you for the app or its services without making it very clear to you exactly what you’re paying for.

I value your trust in providing us your Personal Information, thus we are striving to use commercially acceptable means of protecting it. But remember that no method of transmission over the internet, or method of electronic storage is 100% secure and reliable, and I cannot guarantee its absolute security. The Silo app stores and processes personal data that you have provided to us, in order to provide my Service. The personal data is stored in an encrypted location on the local storage of the device, or an external storage if the app is installed on such external storage i.e. an external SD card. It’s your responsibility to keep your phone and access to the app secure. We therefore recommend that you do not jailbreak or root your phone, which is the process of removing software restrictions and limitations imposed by the official operating system of your device. It could make your phone vulnerable to malware/viruses/malicious programs, compromise your phone’s security features and it could mean that the Silo app won’t work properly or at all.

The app does use third party services that declare their own Terms and Conditions.
Link to Terms and Conditions of third party service provider used by the app:

- Google Play Services (https://policies.google.com/terms)

You should be aware that there are certain things that the developer will not take responsibility for. The developer cannot always take responsibility for the way you use the app i.e. You need to make sure that your device stays charged – if it runs out of battery and you can’t turn it on to avail the Service, the developer cannot accept responsibility.

With respect to the developer’s responsibility for your use of the app, when you’re using the app, it’s important to bear in mind that although we endeavor to ensure that it is updated and correct at all times, we do rely on third parties to provide information to us so that we can make it available to you. The developer accepts no liability for any loss, direct or indirect, you experience as a result of relying wholly on this functionality of the app.

At some point, we may wish to update the app. The app is currently available on Android – the requirements for the system (and for any additional systems we decide to extend the availability of the app to) may change, and you’ll need to download the updates if you want to keep using the app. The developer does not promise that it will always update the app so that it is relevant to you and/or works with the Android version that you have installed on your device. However, you promise to always accept updates to the application when offered to you. We may also wish to stop providing the app, and may terminate use of it at any time without giving notice of termination to you. Unless we tell you otherwise, upon any termination, (a) the rights and licenses granted to you in these terms will end; (b) you must stop using the app, and (if needed) delete it from your device.

Changes to This Terms and Conditions

I may update our Terms and Conditions from time to time. Thus, you are advised to review this page periodically for any changes. I will notify you of any changes by posting the new Terms and Conditions on this page.

These terms and conditions are effective as of 2020-10-09

Contact Us

If you have any questions or suggestions about my Terms and Conditions, do not hesitate to contact me at silodeveloper@gmail.com.
This Terms and Conditions page was generated by App Privacy Policy Generator (https://app-privacy-policy-generator.firebaseapp.com/).

 */